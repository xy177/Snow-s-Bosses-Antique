package xy177.snowsbosses.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.content.WorkerBeeEffects;
import xy177.snowsbosses.content.MechasentEquipment;
import xy177.snowsbosses.network.Visuals;

public final class MechasentWorkerBee extends EntityMob {
    private static final DataParameter<Integer> ACTION = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> START = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> OWNER = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> TARGET = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> SLEEPING = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> PLAYER_BOUND = EntityDataManager.createKey(MechasentWorkerBee.class, DataSerializers.BOOLEAN);
    private long repairDeadline;
    private UUID ownerId;
    private UUID targetId;
    private boolean bindingChecked;
    private boolean reviveFull;
    private boolean clearing;
    private int sleepTicks;
    private int activeTicks;
    private int cooldown;
    private int previousAction;
    private Vec3d destination;
    private Vec3d restPosition;

    public MechasentWorkerBee(World world) {
        super(world);
        setSize(0.8125F, 0.625F);
        setNoGravity(true);
        enablePersistence();
        experienceValue = 0;
    }

    public static double n(String key, double value) { return Balance.number("worker_bee." + key, value); }
    public static int seconds(String key, double value) { return Math.max(1, (int) Math.ceil(n(key, value) * 20)); }
    public int action() { return dataManager.get(ACTION); }
    public int actionAge() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    public boolean sleeping() { return dataManager.get(SLEEPING); }
    public boolean boundTo(Mechasent owner) { return owner.getUniqueID().equals(ownerId); }
    public boolean raidAlly() { Mechasent owner = owner(); return owner != null && owner.raidSpawned(); }
    public boolean isPlayerBound() { return dataManager.get(PLAYER_BOUND); }
    public EntityPlayer playerOwner() {
        if (!isPlayerBound()) return null;
        Entity entity = world.isRemote ? world.getEntityByID(dataManager.get(OWNER)) : world.getPlayerEntityByUUID(ownerId);
        return entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
    }
    public boolean friendly(Entity entity) {
        EntityPlayer player = playerOwner();
        return entity != null && (entity == this || (isPlayerBound() ? player != null && MechasentEquipment.allies(player,entity)
                : Mechasent.protectedTarget(raidAlly(),entity)) || super.isOnSameTeam(entity));
    }

    public void bind(EntityPlayer player) {
        targetTasks.taskEntries.removeIf(entry -> entry.action instanceof EntityAINearestAttackableTarget);
        ownerId = player.getUniqueID(); bindingChecked = true; dataManager.set(OWNER,player.getEntityId());
        dataManager.set(PLAYER_BOUND,true); repairDeadline = MechasentEquipment.time(player)+900;
    }
    @Override public boolean isOnSameTeam(Entity entity) { return friendly(entity); }
    @Override public boolean canBePushed() { return !sleeping(); }
    @Override public float getEyeHeight() { return height * 0.5F; }
    @Override protected boolean canDespawn() { return false; }
    @Override protected ResourceLocation getLootTable() { return null; }
    @Override public boolean isAIDisabled() { return sleeping() || super.isAIDisabled(); }

    @Override protected void entityInit() {
        super.entityInit();
        dataManager.register(ACTION, 0); dataManager.register(START, 0);
        dataManager.register(OWNER, -1); dataManager.register(TARGET, -1);
        dataManager.register(SLEEPING, false); dataManager.register(PLAYER_BOUND,false);
    }

    @Override protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(n("maxHealth", 100));
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(n("armor", 0));
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(n("armorToughness", 0));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.28);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32);
    }

    @Override protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
    }

    @Override public void setAttackTarget(EntityLivingBase target) { super.setAttackTarget(friendly(target) ? null : target); }

    public void bind(Mechasent owner) {
        ownerId = owner.getUniqueID(); bindingChecked = true; dataManager.set(OWNER, owner.getEntityId());
    }

    public Mechasent owner() {
        Entity entity = world.isRemote ? world.getEntityByID(dataManager.get(OWNER))
                : ownerId == null ? null : ((WorldServer) world).getEntityFromUuid(ownerId);
        return entity instanceof Mechasent && entity.isEntityAlive() ? (Mechasent) entity : null;
    }

    public EntityLivingBase lockedTarget() {
        Entity entity = world.isRemote ? world.getEntityByID(dataManager.get(TARGET))
                : targetId == null ? null : ((WorldServer) world).getEntityFromUuid(targetId);
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    public static boolean invalidLockTarget(EntityLivingBase entity) {
        return entity.isPotionActive(MobEffects.INVISIBILITY) || FFDPowderSnowEvents.getFrozenPercent(entity) > 0;
    }

    private void startAction(int next, EntityLivingBase target) {
        dataManager.set(ACTION, next); dataManager.set(START, (int) world.getTotalWorldTime());
        targetId = target.getUniqueID(); dataManager.set(TARGET, target.getEntityId()); previousAction = next;
        Mechasent owner = owner();
        if (next == 4 && owner != null) owner.strengthen(1800);
        if (owner != null && (next == 3 || next == 4 || next == 6)) owner.broadcast(next == 3 ? 6 : next == 4 ? 13 : 12);
        if (next == 5 && owner != null) owner.workers().begin(target);
    }

    public void finishAction() {
        if (action() == 3) WorkerBeeEffects.unlock(lockedTarget(), this);
        dataManager.set(ACTION, 0); dataManager.set(TARGET, -1); targetId = null; cooldown = 70;
    }

    private void chooseAction(EntityLivingBase target, Mechasent owner) {
        EntityLivingBase source = owner == null ? this : owner;
        double health = MathHelper.clamp((double) source.getHealth() / source.getMaxHealth(), 0, 1);
        double[] weights = {0, 3, 3, 6 * health, 6 * (1 - health),
                1 + (health < 2.0 / 3 ? 1 : 0) + (health < 1.0 / 3 ? 1 : 0),
                owner == null ? 0 : 2 * (4 - owner.arms())};
        double total = 0;
        for (int next = 1; next < weights.length; next++) {
            if (next == previousAction || next == 5 && invalidLockTarget(target)
                    || next == 4 && (owner == null || owner.workers().supported())
                    || next == 6 && (owner == null || owner.arms() >= 4 || activeTicks < 2400)) weights[next] = 0;
            total += weights[next];
        }
        double choice = rand.nextDouble() * total;
        for (int next = 1; next < weights.length; next++) {
            if ((choice -= weights[next]) < 0) {
                startAction(next, next == 4 || next == 6 ? owner : target);
                return;
            }
        }
    }

    @Override public void onLivingUpdate() {
        if (isPlayerBound()) {
            if (!world.isRemote) playerTick();
            super.onLivingUpdate(); setNoGravity(true); fallDistance=0;
            if(!world.isRemote && !isDead){
                EntityLivingBase target=action()==4 || action()==6?playerOwner():action()!=0?lockedTarget():getAttackTarget();
                if(target!=null)face(target);
            }
            return;
        }
        if (!world.isRemote) {
            if (ownerId != null && WorkerBeeSwarm.ownerWasRemoved(world, ownerId)) { setDead(); return; }
            if (!bindingChecked) {
                bindingChecked = true;
                Mechasent closest = null; double distance = 32 * 32;
                for (Mechasent candidate : world.getEntitiesWithinAABB(Mechasent.class, getEntityBoundingBox().grow(32))) {
                    if (candidate.isEntityAlive() && getDistanceSq(candidate) < distance) { closest = candidate; distance = getDistanceSq(candidate); }
                }
                if (closest != null) bind(closest);
            }
            Mechasent owner = owner();
            dataManager.set(OWNER, owner == null ? -1 : owner.getEntityId());
            if (sleeping()) {
                motionX = motionY = motionZ = 0;
                if (restPosition == null) restPosition = getPositionVector();
                if (--sleepTicks <= 0) {
                    dataManager.set(SLEEPING, false);
                    if (reviveFull) setHealth(getMaxHealth());
                    reviveFull = false; cooldown = 70; teleportNearOwner();
                }
            } else if (!super.isAIDisabled()) {
                activeTicks = Math.min(Integer.MAX_VALUE, activeTicks + 1);
                if (cooldown > 0) cooldown--;
                EntityLivingBase target = getAttackTarget();
                if (owner != null && owner.getAttackTarget() != null) { setAttackTarget(owner.getAttackTarget()); target = getAttackTarget(); }
                if (target != null && (!target.isEntityAlive() || friendly(target))) { setAttackTarget(null); target = null; }
                if (owner != null && Math.hypot(posX - owner.posX, posZ - owner.posZ) > 8) teleportNearOwner();
                if (action() == 0 && owner != null && owner.workers().active()) {
                    EntityLivingBase shared = owner.workers().target();
                    if (shared != null) startAction(5, shared);
                }
                if (action() == 0 && target != null && cooldown == 0) chooseAction(target, owner);
                if (action() != 0) actionTick(owner);
                fly(target, owner);
            }
        }
        super.onLivingUpdate();
        setNoGravity(true); fallDistance = 0;
        if (sleeping()) {
            motionX = motionY = motionZ = 0;
            if (!world.isRemote && restPosition != null) setPosition(restPosition.x, restPosition.y, restPosition.z);
        } else if (!world.isRemote) {
            EntityLivingBase face = action() == 4 || action() == 6 ? owner() : action() >= 3 ? lockedTarget() : getAttackTarget();
            if (face != null) face(face);
        }
    }

    private void playerTick() {
        EntityPlayer player=playerOwner();
        if (player==null || !player.isEntityAlive() || !MechasentEquipment.bound(player,this)) { setDead(); return; }
        dataManager.set(OWNER,player.getEntityId());
        if (cooldown>0) cooldown--;
        if (Math.hypot(posX-player.posX,posZ-player.posZ)>10 || Math.abs(posY-player.posY)>8) teleportNearOwner();
        EntityLivingBase target=getAttackTarget();
        if (target!=null && (!target.isEntityAlive() || friendly(target) || getDistanceSq(target)>1600)) { setAttackTarget(null); target=null; }
        if (ticksExisted%20==0) {
            EntityLivingBase threat=player.getRevengeTarget();
            if (threat==null || !threat.isEntityAlive() || friendly(threat)) threat=player.getLastAttackedEntity();
            if (threat!=null && threat.isEntityAlive() && !friendly(threat) && getDistanceSq(threat)<1024) target=threat;
            if (target==null) {
                double nearest=1024;
                for (EntityLivingBase candidate:world.getEntitiesWithinAABB(EntityLivingBase.class,getEntityBoundingBox().grow(24))) {
                    double distance=getDistanceSq(candidate);
                    if (distance<nearest && MechasentEquipment.hostileTo(player,candidate)) { target=candidate; nearest=distance; }
                }
            }
            setAttackTarget(target);
        }
        if (MechasentEquipment.time(player)>=repairDeadline && action()!=6) startAction(6,player);
        if (action()==0 && cooldown==0 && target!=null) {
            double health=MathHelper.clamp(player.getHealth()/player.getMaxHealth(),0,1);
            double[] weights={0,3,3,6*health,6*(1-health),MechasentEquipment.fullSet(player) && !invalidLockTarget(target) ? 1+(health<2.0/3?1:0)+(health<1.0/3?1:0):0,2};
            double sum=0;
            for(int i=1;i<weights.length;i++){ if(i==previousAction) weights[i]=0; sum+=weights[i]; }
            double choice=rand.nextDouble()*sum;
            for(int i=1;i<weights.length;i++) if((choice-=weights[i])<0){startAction(i,i==4||i==6?player:target);break;}
        }
        int action=action(),age=actionAge();
        EntityLivingBase locked=lockedTarget();
        if(action!=0 && (locked==null || !locked.isEntityAlive() || action!=4 && action!=6 && friendly(locked))) finishAction();
        else if(action==1 || action==2){ if(age==30 || action==2&&(age==40||age==50)) shoot(locked,action==2); if(age>=(action==1?31:51)) finishAction(); }
        else if(action==3){ WorkerBeeEffects.lock(locked,this); if(age>=160) finishAction(); }
        else if(action==4){ player.heal(player.getMaxHealth()*0.1F/200); if(age>=200) finishAction(); }
        else if(action==5){
            if(!MechasentEquipment.fullSet(player)||invalidLockTarget(locked)) finishAction();
            else{
                addPotionEffect(new PotionEffect(MobEffects.GLOWING,5,0,false,false));
                locked.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,5,1,false,false));
                locked.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE,5,1,false,false));
                if(age>=240){world.spawnEntity(new WorkerBeeBeam(world,this,false,locked.posX,locked.posY,locked.posZ));finishAction();}
            }
        } else if(action==6){
            addPotionEffect(new PotionEffect(MobEffects.GLOWING,5,0,false,false));
            player.addPotionEffect(new PotionEffect(MobEffects.GLOWING,5,0,false,false));
            if(age>=160){teleportNearOwner();setDead();return;}
        }
        Vec3d goal;
        if(action()==0 && target==null || action()==4 || action()==6){
            double angle=world.getTotalWorldTime()*0.04;
            goal=player.getPositionVector().addVector(Math.cos(angle)*3,2.8,Math.sin(angle)*3);
        }else{
            if(destination==null || ticksExisted%25==0 || collidedHorizontally){
                double angle=rand.nextDouble()*Math.PI*2;
                EntityLivingBase focus=target==null?player:target;
                destination=focus.getPositionEyes(1).addVector(Math.cos(angle)*5,1+rand.nextDouble()*2,Math.sin(angle)*5);
            }
            goal=destination;
        }
        Vec3d velocity=goal.subtract(getPositionVector()).normalize().scale(0.22);
        motionX+=(velocity.x-motionX)*0.15;motionY+=(velocity.y-motionY)*0.15;motionZ+=(velocity.z-motionZ)*0.15;
    }

    private void actionTick(Mechasent owner) {
        int next = action(), age = actionAge();
        EntityLivingBase target = lockedTarget();
        if (target == null || !target.isEntityAlive() || next != 4 && next != 6 && friendly(target)) { finishAction(); return; }
        dataManager.set(TARGET, target.getEntityId());
        if (next == 1) {
            if (age == 30) shoot(target, false);
            if (age >= 31) finishAction();
        } else if (next == 2) {
            if (age == 30 || age == 40 || age == 50) shoot(target, true);
            if (age >= 51) finishAction();
        } else if (next == 3) {
            WorkerBeeEffects.lock(target, this);
            if (age >= 160) finishAction();
        } else if (next == 4) {
            if (owner == null || age >= 200 || owner.workers().healer() != this) finishAction();
        } else if (next == 5) {
            if (invalidLockTarget(target)) {
                if (owner != null) owner.workers().interrupt(); else finishAction();
            } else if (ownerId != null && (owner == null || !owner.workers().active())) finishAction();
            else {
                addPotionEffect(new PotionEffect(MobEffects.GLOWING, 5, 0, false, false));
                target.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 5, 1, false, false));
                target.addPotionEffect(new PotionEffect(MobEffects.MINING_FATIGUE, 5, 1, false, false));
                if (ownerId == null && age >= 240) {
                    world.spawnEntity(new WorkerBeeBeam(world, this, false, target.posX, target.posY, target.posZ));
                    finishAction();
                }
            }
        } else if (next == 6) {
            if (owner == null || owner.arms() >= 4) { finishAction(); return; }
            addPotionEffect(new PotionEffect(MobEffects.GLOWING, 5, 0, false, false));
            owner.addPotionEffect(new PotionEffect(MobEffects.GLOWING, 5, 0, false, false));
            if (age >= 160) {
                boolean arrived = moveSafely(owner.posX, owner.posY + 6.3, owner.posZ);
                for (int attempt = 0; !arrived && attempt < 12; attempt++) {
                    arrived = moveSafely(owner.posX + (rand.nextDouble() - 0.5), owner.posY + 6.3 + rand.nextDouble(),
                            owner.posZ + (rand.nextDouble() - 0.5));
                }
                if (arrived) { owner.restoreArm(); setDead(); }
                else if (age >= 180) finishAction();
            }
        }
    }

    private void shoot(EntityLivingBase target, boolean burst) {
        world.spawnEntity(new WorkerBeeLaser(world, this, target, burst));
        playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 0.6F, burst ? 1.5F : 1.1F);
    }

    private void fly(EntityLivingBase target, Mechasent owner) {
        Vec3d goal;
        if (target == null && action() == 0 && owner != null) {
            List<MechasentWorkerBee> orbiters = new ArrayList<>();
            for (MechasentWorkerBee bee : owner.workers().members()) if (!bee.sleeping() && bee.action() == 0 && bee.getAttackTarget() == null) orbiters.add(bee);
            int index = Math.max(0, orbiters.indexOf(this));
            double angle = world.getTotalWorldTime() * 0.04 + Math.PI * 2 * index / Math.max(1, orbiters.size());
            goal = new Vec3d(owner.posX + Math.cos(angle) * 4, owner.posY + 8.3, owner.posZ + Math.sin(angle) * 4);
        } else {
            if (destination == null || ticksExisted % 25 == 0 || getPositionVector().squareDistanceTo(destination) < 0.4 || collidedHorizontally) {
                double angle = rand.nextDouble() * Math.PI * 2;
                if (target == null) destination = getPositionVector().addVector((rand.nextDouble() - 0.5) * 8, (rand.nextDouble() - 0.5) * 4, (rand.nextDouble() - 0.5) * 8);
                else destination = new Vec3d(target.posX + Math.cos(angle) * (5 + rand.nextDouble() * 2),
                        target.posY + target.getEyeHeight() + 1 + rand.nextDouble() * 2, target.posZ + Math.sin(angle) * (5 + rand.nextDouble() * 2));
                if (owner != null) {
                    double dx = destination.x - owner.posX, dz = destination.z - owner.posZ, distance = Math.hypot(dx, dz);
                    if (distance > 7.5) destination = new Vec3d(owner.posX + dx / distance * 7.5, destination.y, owner.posZ + dz / distance * 7.5);
                }
            }
            goal = destination;
        }
        Vec3d delta = goal.subtract(getPositionVector());
        double distance = delta.lengthVector(), speed = Math.min(0.28, distance * 0.2);
        Vec3d desired = distance < 1.0E-6 ? Vec3d.ZERO : delta.scale(speed / distance);
        if (world.rayTraceBlocks(getPositionEyes(1), getPositionEyes(1).add(desired.scale(5)), false, true, false) != null) {
            desired = new Vec3d(desired.x * 0.2, 0.16, desired.z * 0.2);
        }
        motionX += (desired.x - motionX) * 0.25; motionY += (desired.y - motionY) * 0.25; motionZ += (desired.z - motionZ) * 0.25;
        if (target == null && action() == 0 && desired.lengthSquared() > 1.0E-5) {
            rotationYaw = (float) Math.toDegrees(Math.atan2(-desired.x, desired.z)); rotationYawHead = renderYawOffset = rotationYaw; rotationPitch = 0;
        }
    }

    private void face(EntityLivingBase target) {
        double dx = target.posX - posX, dy = target.posY + target.getEyeHeight() - posY - getEyeHeight(), dz = target.posZ - posZ;
        rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        rotationPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        rotationYawHead = renderYawOffset = rotationYaw;
    }

    @Override public void travel(float strafe, float vertical, float forward) {
        if (world.isRemote) return;
        if (!sleeping()) move(MoverType.SELF, motionX, motionY, motionZ);
        motionX *= 0.98; motionY *= 0.98; motionZ *= 0.98;
    }

    private boolean moveSafely(double x, double y, double z) {
        AxisAlignedBB box = getEntityBoundingBox().offset(x - posX, y - posY, z - posZ);
        if (y < 0 || box.maxY >= world.getActualHeight() || !world.getWorldBorder().contains(box)
                || !world.isAreaLoaded(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))
                || !world.getCollisionBoxes(this, box).isEmpty() || !world.checkNoEntityCollision(box, this) || world.containsAnyLiquid(box)) return false;
        if (world.getEntityByID(getEntityId()) == this) {
            teleportEffects();
            setPositionAndUpdate(x, y, z);
            teleportEffects();
        } else setPosition(x, y, z);
        motionX = motionY = motionZ = 0; destination = null; fallDistance = 0;
        return true;
    }

    private void teleportEffects() {
        playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1, 1);
        Visuals.particles(world, "worker_bee_teleport", posX, posY, posZ, 20, 0, 0);
    }

    public boolean teleportNearOwner() {
        EntityLivingBase owner = isPlayerBound() ? playerOwner() : owner();
        if (owner == null) return false;
        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2, radius = 3 + rand.nextDouble() * 2;
            double y = owner.posY + (isPlayerBound() ? 2.0 : 6.3) + (attempt < 20 ? 1 : -1) + (rand.nextDouble() - 0.5) * 2;
            if (moveSafely(owner.posX + Math.cos(angle) * radius, y, owner.posZ + Math.sin(angle) * radius)) return true;
        }
        return false;
    }

    private void dodge() {
        for (int attempt = 0; attempt < 24; attempt++) {
            Vec3d direction = new Vec3d(rand.nextDouble() * 2 - 1, rand.nextDouble() - 0.5, rand.nextDouble() * 2 - 1).normalize().scale(2);
            if (moveSafely(posX + direction.x, posY + direction.y, posZ + direction.z)) return;
        }
    }

    private void disable(int ticks, boolean fullHealth) {
        if (isPlayerBound()) { setDead(); return; }
        if (sleeping() && reviveFull) return;
        Mechasent owner = owner();
        if (owner != null && action() == 5) owner.workers().interrupt();
        finishAction();
        sleepTicks = Math.max(sleepTicks, ticks); reviveFull |= fullHealth; activeTicks = 0;
        restPosition = getPositionVector(); dataManager.set(SLEEPING, true);
        motionX = motionY = motionZ = 0; getNavigator().clearPath();
    }

    @Override public void setHealth(float health) {
        if (health <= 0 && !world.isRemote && !clearing) {
            super.setHealth(1); disable(2400, true);
        } else super.setHealth(health);
    }

    @Override public boolean attackEntityFrom(DamageSource source, float amount) {
        if (world.isRemote) return false;
        if (source == DamageSource.OUT_OF_WORLD) {
            clearing = true;
            try { return super.attackEntityFrom(source, amount); } finally { clearing = false; }
        }
        if (sleeping() || friendly(source.getTrueSource())) return false;
        Entity attacker = source.getTrueSource();
        if (source instanceof EntityDamageSource && attacker instanceof EntityLivingBase && attacker == source.getImmediateSource()
                && !source.isProjectile() && !source.isExplosion() && !source.isMagicDamage()) { dodge(); return false; }
        boolean hit = super.attackEntityFrom(source, amount);
        if (source == DamageSource.LIGHTNING_BOLT && !isDead) disable(600, false);
        return hit;
    }

    @Override public void onStruckByLightning(EntityLightningBolt lightning) { attackEntityFrom(DamageSource.LIGHTNING_BOLT, 5); }
    @Override public void onKillCommand() { clearing = true; try { super.onKillCommand(); } finally { clearing = false; } }
    @Override public void setDead() {
        if (!world.isRemote && isPlayerBound() && getServer()!=null) {
            EntityPlayer player=getServer().getPlayerList().getPlayerByUUID(ownerId);
            if(player!=null) MechasentEquipment.release(player,getUniqueID());
        }
        if (!world.isRemote) {
            Mechasent owner = owner(); if (owner != null && action() == 5) owner.workers().interrupt();
            if (action() == 3) WorkerBeeEffects.unlock(lockedTarget(), this);
        }
        super.setDead();
    }

    @Override public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        if (ownerId != null) data.setUniqueId("WorkerOwner", ownerId);
        if (targetId != null) data.setUniqueId("WorkerTarget", targetId);
        data.setBoolean("PlayerBound",isPlayerBound()); data.setLong("PlayerRepairDeadline",repairDeadline);
        data.setBoolean("WorkerBindingChecked", bindingChecked); data.setBoolean("WorkerReviveFull", reviveFull);
        data.setInteger("WorkerSleep", sleepTicks); data.setInteger("WorkerActive", activeTicks);
        data.setInteger("WorkerCooldown", cooldown); data.setInteger("WorkerPreviousAction", previousAction);
        data.setInteger("WorkerAction", action()); data.setInteger("WorkerActionAge", actionAge());
    }

    @Override public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        ownerId = data.hasUniqueId("WorkerOwner") ? data.getUniqueId("WorkerOwner") : null;
        targetId = data.hasUniqueId("WorkerTarget") ? data.getUniqueId("WorkerTarget") : null;
        dataManager.set(PLAYER_BOUND,data.getBoolean("PlayerBound"));
        if(isPlayerBound())targetTasks.taskEntries.removeIf(entry -> entry.action instanceof EntityAINearestAttackableTarget); repairDeadline=data.getLong("PlayerRepairDeadline");
        bindingChecked = data.getBoolean("WorkerBindingChecked"); reviveFull = data.getBoolean("WorkerReviveFull");
        sleepTicks = Math.max(0, data.getInteger("WorkerSleep")); activeTicks = Math.max(0, data.getInteger("WorkerActive"));
        cooldown = Math.max(0, data.getInteger("WorkerCooldown")); previousAction = MathHelper.clamp(data.getInteger("WorkerPreviousAction"), 0, 6);
        dataManager.set(ACTION, MathHelper.clamp(data.getInteger("WorkerAction"), 0, 6));
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("WorkerActionAge")));
        dataManager.set(SLEEPING, sleepTicks > 0); if (sleepTicks > 0) restPosition = getPositionVector();
    }
}
