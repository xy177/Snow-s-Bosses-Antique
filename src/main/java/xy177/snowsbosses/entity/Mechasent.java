package xy177.snowsbosses.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntityVex;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.farmersfuturedelight.common.raid.FFDRaidCaptains;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.MechasentRaids;
import xy177.snowsbosses.content.WorkerBeeEffects;
import xy177.snowsbosses.network.Visuals;

public final class Mechasent extends EntityMob {
    public static final String RAID_TAG = "SnowRaidMechasent";
    private static final DataParameter<Integer> ACTION = EntityDataManager.createKey(Mechasent.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> START = EntityDataManager.createKey(Mechasent.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> ARMS = EntityDataManager.createKey(Mechasent.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> HIDDEN = EntityDataManager.createKey(Mechasent.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> VOICE_COMBAT = EntityDataManager.createKey(Mechasent.class, DataSerializers.BOOLEAN);
    private int teleportCooldown;
    private int roarCooldown;
    private int defenseCooldown;
    private int heavyCooldown;
    private int blockSoundCooldown;
    private int previousAction;
    private boolean raidSpawned;
    private boolean breakArm;
    private int targetMemory;
    private int strongerTicks;
    private int broadcastCooldown;
    private int broadcastHealthBand;
    private boolean broadcastSpawned;
    private boolean broadcastTarget;
    private boolean broadcastStronger;
    private boolean teleportBroadcastChecked;
    private WorkerBeeSwarm workers;
    private final BossInfoServer bar;

    public Mechasent(World world) {
        super(world);
        bar = new BossInfoServer(new TextComponentTranslation("entity.snows_bosses.mechasent.name"), BossInfo.Color.WHITE, BossInfo.Overlay.PROGRESS);
        enablePersistence();
        setSize(1.9F, 6.3F);
        stepHeight = 0.6F;
        experienceValue = 100;
        defenseCooldown = 180;
    }

    public static double n(String key, double fallback) { return Balance.number("mechasent." + key, fallback); }
    public int action() { return dataManager.get(ACTION); }
    public int actionAge() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    public int arms() { return dataManager.get(ARMS); }
    public boolean hidden() { return dataManager.get(HIDDEN); }
    public boolean innerVoiceCombat() { return isEntityAlive() && dataManager.get(VOICE_COMBAT); }
    public boolean raidSpawned() { return raidSpawned; }
    public void markRaidSpawned() { raidSpawned = true; getEntityData().setBoolean(RAID_TAG, true); }
    public boolean stronger() { return strongerTicks > 0 || "Stronger Mob".equals(getDisplayName().getUnformattedText()); }
    public void strengthen(int ticks) {
        if (ticks > 0 && !stronger()) broadcast(4);
        strongerTicks = Math.max(strongerTicks, ticks);
        broadcastStronger = stronger();
    }
    public WorkerBeeSwarm workers() { if (workers == null) workers = new WorkerBeeSwarm(this); return workers; }
    public void restoreArm() {
        if (arms() < 4) { dataManager.set(ARMS, arms() + 1); broadcast(14); }
    }
    public void workerLaserRing() { wave(1, posX, posZ, 0, 0, 15, n("defense.laserDamageMultiplier", 0.5)); }

    public void broadcast(int clip) {
        if (!world.isRemote && clip >= 1 && clip <= MechasentRaids.BROADCASTS.length && clip != 5
                && (clip == 17 || isEntityAlive())) Visuals.innerVoice(this, clip);
        if (world.isRemote || clip < 1 || clip > MechasentRaids.BROADCASTS.length
                || clip != 17 && (!isEntityAlive() || broadcastCooldown > 0)) return;
        playSound(MechasentRaids.BROADCASTS[clip - 1], 1, 1);
        if (clip != 17) broadcastCooldown = Math.max(0, (int) Math.ceil(n("broadcastCooldown", 3) * 20));
    }

    private void broadcastState(EntityLivingBase target) {
        if (broadcastCooldown > 0) broadcastCooldown--;
        if (!broadcastSpawned) { broadcastSpawned = true; broadcast(1); }
        boolean hasTarget = target != null;
        if (hasTarget != broadcastTarget) { broadcastTarget = hasTarget; broadcast(hasTarget ? 7 : 8); }
        boolean enhanced = stronger();
        if (enhanced && !broadcastStronger) broadcast(4);
        broadcastStronger = enhanced;
        int band = getHealth() < getMaxHealth() / 3 ? 2 : getHealth() < getMaxHealth() * 2 / 3 ? 1 : 0;
        if (band > broadcastHealthBand) broadcast(band == 2 ? 16 : 15);
        broadcastHealthBand = band;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(ACTION, 0);
        dataManager.register(START, 0);
        dataManager.register(ARMS, 4);
        dataManager.register(HIDDEN, false);
        dataManager.register(VOICE_COMBAT, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(n("maxHealth", 350));
        getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(n("armor", 16));
        getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).setBaseValue(n("armorToughness", 0));
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(n("attackDamage", 16));
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.2);
        getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(32.0);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
    }

    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
        tasks.addTask(4, new EntityAIWander(this, 0.8) {
            @Override public boolean shouldExecute() { return action() == 0 && getAttackTarget() == null && super.shouldExecute(); }
            @Override public boolean shouldContinueExecuting() { return action() == 0 && getAttackTarget() == null && super.shouldContinueExecuting(); }
        });
        tasks.addTask(5, new EntityAILookIdle(this) {
            @Override public boolean shouldExecute() { return action() == 0 && getAttackTarget() == null && super.shouldExecute(); }
            @Override public boolean shouldContinueExecuting() { return action() == 0 && getAttackTarget() == null && super.shouldContinueExecuting(); }
        });
    }

    public static boolean protectedTarget(boolean raid, Entity entity) {
        if (entity instanceof Mechasent || entity instanceof MechasentWorkerBee && !((MechasentWorkerBee)entity).isPlayerBound()) return true;
        if (!raid || !(entity instanceof EntityLivingBase)) return false;
        EntityLivingBase living = (EntityLivingBase) entity;
        if (FFDRaidCaptains.isRaider(living)) return true;
        return living instanceof EntityVex && ((EntityVex) living).getOwner() != null
                && FFDRaidCaptains.isRaider(((EntityVex) living).getOwner());
    }

    public static boolean attackable(Entity entity) { return entity instanceof EntityLiving || entity instanceof EntityPlayer; }

    @Override
    public void setAttackTarget(EntityLivingBase target) {
        super.setAttackTarget(protectedTarget(raidSpawned, target) ? null : target);
        if (!world.isRemote) dataManager.set(VOICE_COMBAT, getAttackTarget() != null);
    }

    @Override protected boolean canDespawn() { return false; }
    @Override public boolean canBePushed() { return false; }
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) {
        super.addTrackingPlayer(player);
        Visuals.bossBar(player, bar.getUniqueId(), true, 0xD3D3D3, getEntityId());
        bar.addPlayer(player);
    }
    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) {
        super.removeTrackingPlayer(player);
        bar.removePlayer(player);
        Visuals.bossBar(player, bar.getUniqueId(), false, 0xD3D3D3, getEntityId());
    }
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!world.isRemote && getHealth() <= 0) { workers().removeAll(); broadcast(17); }
    }
    @Override
    public void setDead() {
        if (!world.isRemote) workers().removeAll();
        bar.setVisible(false);
        super.setDead();
    }
    @Override protected ResourceLocation getLootTable() { return new ResourceLocation(SnowsBosses.MOD_ID, "entities/mechasent"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_IRONGOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_IRONGOLEM_DEATH; }
    @Override protected void playStepSound(BlockPos pos, Block block) { playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 0.15F, 1); }

    private void start(int value) {
        dataManager.set(ACTION, value);
        dataManager.set(START, (int) world.getTotalWorldTime());
        if (value == 8) teleportBroadcastChecked = false;
        if (value != 0) previousAction = value;
    }

    private void finish() {
        int old = action();
        start(0);
        hide(false);
        if (old != 4) teleportCooldown = (old == 8 ? 160 : 100);
        if (old == 3) roarCooldown = 180;
    }

    private void hide(boolean hidden) {
        if (hidden() == hidden) return;
        dataManager.set(HIDDEN, hidden);
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        super.notifyDataManagerChange(key);
        if (HIDDEN.equals(key)) {
            double x = posX, y = posY, z = posZ;
            setSize(hidden() ? 0 : 1.9F, hidden() ? 0 : 6.3F);
            setPosition(x, y, z);
        }
    }

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && isEntityAlive()) {
            if (strongerTicks > 0) strongerTicks--;
            if (teleportCooldown > 0) teleportCooldown--;
            if (roarCooldown > 0) roarCooldown--;
            if (blockSoundCooldown > 0) blockSoundCooldown--;
            if (defenseCooldown > 0 && (action() == 0 || rand.nextBoolean())) defenseCooldown--;
            EntityLivingBase target = getAttackTarget();
            if (target != null && (!target.isEntityAlive() || protectedTarget(raidSpawned, target))) { setAttackTarget(null); target = null; }
            broadcastState(target);
            if (target != null) targetMemory = 4;
            else if (targetMemory > 0) { if (--targetMemory == 0) getNavigator().clearPath(); }
            else { roarCooldown = 140; teleportCooldown = 100; }
            if (breakArm) {
                if (guarding()) {
                    int oldArms = arms();
                    dataManager.set(ARMS, Math.max(0, oldArms - 1));
                    if (oldArms > arms()) workers().brokenArm();
                    start(6);
                    playSound(SoundEvents.ITEM_SHIELD_BREAK, 1, 1.1F);
                }
                breakArm = false;
            }
            workers().tick();
            if (action() == 0 && target != null) chooseAttack(target);
            if (action() != 0) attackTick();
            if (action() != 0 && action() != 4) {
                getNavigator().clearPath();
                setMoveForward(0); setMoveStrafing(0);
                motionX = motionZ = 0;
                motionY = Math.min(0, motionY);
            } else if (target != null && action() == 0) {
                getNavigator().tryMoveToEntityLiving(target, 1.3);
            }
        }
        super.onLivingUpdate();
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        bar.setPercent(MathHelper.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        bar.setName(getDisplayName());
        if (action() == 0 && getAttackTarget() != null) getLookHelper().setLookPositionWithEntity(getAttackTarget(), 30, 30);
        else if (action() != 0) {
            Vec3d look = getLookVec();
            getLookHelper().setLookPosition(posX + look.x * 8, posY + getEyeHeight() + look.y * 8, posZ + look.z * 8, 360, 360);
        }
    }

    private void face(EntityLivingBase target) {
        double dx = target.posX - posX, dz = target.posZ - posZ;
        rotationYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90;
        rotationPitch = (float) -Math.toDegrees(Math.atan2(target.posY + 1 - posY - getEyeHeight(), Math.hypot(dx, dz)));
        rotationYawHead = renderYawOffset = rotationYaw;
    }

    private void chooseAttack(EntityLivingBase target) {
        double distance = getDistance(target), dy = target.posY - posY;
        double padding = (width + target.width) / 2;
        int next = teleportCooldown <= 0 && arms() > 0 ? 8 : 0;
        if (roarCooldown <= 0 && distance < 7.0 + padding && Math.abs(dy) < 7.0) {
            next = 3; roarCooldown = 140;
        } else if (arms() > 0 && distance <= 4.5 + padding && dy > -2.0 && dy < 5.0
                || arms() == 0 && distance <= 2.0 + padding && dy > -1.0 && dy < 3.0) {
            if (arms() == 0) next = 4;
            else if (defenseCooldown <= 0) { next = 5; defenseCooldown = 320; }
            else {
                int[] options = new int[3]; int count = 0;
                options[count++] = 1;
                if (arms() >= 2 && distance >= 1.0 + target.width / 2 && distance <= 4.0 + target.width / 2) options[count++] = 2;
                if (heavyCooldown <= 0 && previousAction != 7) options[count++] = 7;
                next = options[rand.nextInt(count)];
                if (next == 7) { next = rand.nextBoolean() ? 7 : 8; heavyCooldown = 3; }
                else if (heavyCooldown > 0) heavyCooldown--;
            }
        }
        if (next != 0) { face(target); start(next); if (next != 4) teleportCooldown = 100; }
    }

    private boolean guarding() { return action() == 5 && actionAge() >= 6 && actionAge() <= 105; }

    private void attackTick() {
        int state = action(), age = actionAge();
        if (state == 8) {
            int first = 19, last = 29;
            if (age == 19 || age == 39) {
                playSound(SoundEvents.ENTITY_ENDERMEN_TELEPORT, 1, 1);
                Visuals.particles(world, "mechasent_teleport", posX, posY, posZ, 50, 0, 0);
            }
            if (age >= first && age <= last) {
                if (getAttackTarget() == null || !teleportBehind(getAttackTarget())) { finish(); return; }
            }
            hide(age >= 20 && age <= 37);
        }
        if (state == 3) {
            if (age == 18) playSound(MechasentRaids.ROAR, 1.8F, 0.9F);
            if (age >= 20 && age <= 60 && age % 10 == 0) {
                Vec3d look = getLookVec();
                Visuals.particles(world, "mechasent_particle_2", posX + look.x * 2, posY + height * (0.76 - (age - 21) * 0.0015), posZ + look.z * 2, 1, 0, 0);
            }
            if (age >= 26 && age <= 65) roar(age);
        } else if (state == 5 && age == 105) {
            playSound(MechasentRaids.ROAR, 1.8F, 1.2F);
            wave(1, posX, posZ, 0, 0, 15, n("defense.laserDamageMultiplier", 0.5));
        }
        int[] hits = {0,22,20,0,2,108,0,44,52};
        if (state != 3 && state != 6 && (age == hits[state]
                || state == 2 && age == 52)) strike(state);
        int[] lengths = {0,50,80,95,20,140,80,72,80};
        if (age > lengths[state]) finish();
    }

    private void strike(int state) {
        Vec3d look = getLookVec();
        String key = "attack" + state;
        if (state == 5) Visuals.particles(world, "mechasent_particle_2", posX + look.x * 0.25, posY + height * 0.9, posZ + look.z * 0.25, 1, 0, 0);
        double offset = (state == 4 || state == 5 ? 0 : state == 7 ? 4 : 3);
        double radius = (state == 2 ? 2.1 : state == 7 ? 3.8 : state == 4 ? 2 : state == 5 ? 4 : 2.7);
        double cx = posX + look.x * offset, cz = posZ + look.z * offset;
        double below = (state == 4 || state == 5 ? 1 : 2), above = (state == 4 ? 3 : state == 2 ? 4 : 5);
        double reach = radius + (state == 4 || state == 5 ? width / 2 : 0);
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(cx - reach, posY - below, cz - reach, cx + reach, posY + above, cz + reach))) {
            if (!attackable(victim) || victim == this || protectedTarget(raidSpawned, victim)) continue;
            if (state == 4 && victim != getAttackTarget()) continue;
            if (Math.hypot(victim.posX - cx, victim.posZ - cz) >= reach + victim.width / 2) continue;
            if ((state == 4 || state == 5) && !inFront(victim, (state == 5 ? 135 : 90))) continue;
            double multiplier = n(key + ".damageMultiplier", state == 2 ? 0.625 : state == 4 ? 0.4 : state == 7 ? 1.25 : 1);
            boolean blocking = state != 4 && victim.isActiveItemStackBlocking() && victim.getLookVec().dotProduct(getPositionVector().subtract(victim.getPositionVector()).normalize()) > 0;
            victim.attackEntityFrom(DamageSource.causeMobDamage(this), (float) (attackDamage() * multiplier));
            if (blocking && victim instanceof EntityPlayer) disableShield((EntityPlayer) victim);
            if (state != 4) push(victim, state == 5 ? posX : posX + look.x * 3, state == 5 ? posZ : posZ + look.z * 3,
                    (state == 2 ? 0 : state == 5 ? 2.2 : 0.5), (state == 5 ? 0.1 : 0.5));
        }
        if (state == 4 || state == 5) return;
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1, 0.8F);
        Visuals.particles(world, "mechasent_stone", cx, posY, cz, 36, radius, 0.2);
        if (state == 1 || state == 8) {
            int count = 15;
            for (int i = 0; i < count; i++) wave(0, cx, cz, Math.atan2(look.z, look.x) + Math.PI * 2 * (i + 1) / count,
                    0.3, 20, n(key + ".waveDamageMultiplier", state == 1 ? 0.375 : 0.5));
        }
        if (state == 7) wave(1, cx, cz, 0, 0, 20, n("heavy.laserDamageMultiplier", 0.5));
        if (state == 8) wave(2, cx, cz, Math.atan2(look.z, look.x), 0.65, 40, n("teleport.laserDamageMultiplier", 0.5));
    }

    private boolean inFront(EntityLivingBase victim, double degrees) {
        Vec3d look = getLookVec();
        double dx = victim.posX - posX, dz = victim.posZ - posZ;
        double length = Math.hypot(look.x, look.z) * Math.hypot(dx, dz);
        return length < 1.0E-7 || (look.x * dx + look.z * dz) / length >= Math.cos(Math.toRadians(degrees));
    }

    private void disableShield(EntityPlayer player) {
        ItemStack shield = player.getActiveItemStack();
        if (shield.getItem() == Items.SHIELD) {
            player.getCooldownTracker().setCooldown(shield.getItem(), 100);
            shield.damageItem(1, player);
        }
        player.stopActiveHand();
        world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_SHIELD_BREAK, player.getSoundCategory(), 1, 1);
    }

    private void roar(int age) {
        double range = 8.0, vertical = 7.0;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(range, vertical, range))) {
            if (!attackable(victim) || victim == this || protectedTarget(raidSpawned, victim) || Math.abs(victim.posY - posY) > vertical
                    || Math.hypot(victim.posX - posX, victim.posZ - posZ) >= range + victim.width / 2) continue;
            double oldX = victim.motionX, oldY = victim.motionY, oldZ = victim.motionZ;
            victim.attackEntityFrom(new EntityDamageSource("mechasent_roar", this).setDamageBypassesArmor(), (float) (attackDamage() * n("roar.damageMultiplier", 0.1)));
            victim.motionX = oldX; victim.motionY = oldY; victim.motionZ = oldZ;
            victim.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 1, false, false));
            victim.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, 60, 1, false, false));
            push(victim, posX, posZ, 0.05, 0);
        }
        if (age % 8 == 0) {
            int count = Math.max(1, age - 26 + 1);
            double radius = count * 0.3;
            for (int i = 0; i < count; i++) {
                double angle = Math.toRadians(rotationYaw) + 2 * Math.PI * i / count;
                laser(posX + Math.cos(angle) * radius, posY, posZ + Math.sin(angle) * radius, attackDamage() * n("roar.laserDamageMultiplier", 0.375));
            }
        }
    }

    private void push(EntityLivingBase victim, double cx, double cz, double horizontal, double vertical) {
        double factor = MathHelper.clamp(1 - victim.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue(), 0, 1);
        double dx = victim.posX - cx, dz = victim.posZ - cz, length = Math.hypot(dx, dz);
        victim.addVelocity(length > 1.0E-7 ? dx / length * horizontal * factor : 0, vertical * factor, length > 1.0E-7 ? dz / length * horizontal * factor : 0);
        victim.velocityChanged = true;
    }

    public double attackDamage() { return getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue(); }
    private void wave(int type, double x, double z, double angle, double speed, int lifetime, double multiplier) {
        world.spawnEntity(new MechasentWave(world, this, type, x, posY, z, angle, speed, lifetime, attackDamage() * multiplier));
    }
    public void laser(double x, double y, double z, double damage) { world.spawnEntity(new MechasentLaser(world, this, x, y, z, damage)); }

    public void debris(double x, double y, double z, int count, double spread) {
        if (world instanceof WorldServer) ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, y + 0.1, z, count, spread, 0.2, spread, 0.1,
                Block.getStateId(world.getBlockState(new BlockPos(x, y - 0.2, z))));
    }

    private boolean teleportBehind(EntityLivingBase target) {
        Vec3d look = target.getLookVec();
        double x = target.posX - look.x * 3.0, z = target.posZ - look.z * 3.0;
        for (int dy : new int[]{0,1,-1,2,-2,3,-3,4,-4}) {
            double y = Math.floor(target.posY) + dy;
            AxisAlignedBB box = new AxisAlignedBB(x - 0.95, y, z - 0.95, x + 0.95, y + 6.3, z + 0.95);
            if (y < 1 || box.maxY >= world.getActualHeight()
                    || !world.isAreaLoaded(new BlockPos(box.minX, y - 1, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))
                    || !world.getWorldBorder().contains(box) || !world.getCollisionBoxes(this, box).isEmpty()
                    || !world.checkNoEntityCollision(box, this)
                    || world.containsAnyLiquid(box) || world.getCollisionBoxes(this, box.offset(0, -0.08, 0)).isEmpty()) continue;
            setPositionAndUpdate(x, y, z);
            face(target);
            fallDistance = 0;
            if (!teleportBroadcastChecked) {
                teleportBroadcastChecked = true;
                Visuals.innerVoice(this, 5);
                if (rand.nextInt(4) == 0) broadcast(5);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.FALL || source == DamageSource.DROWN) return false;
        if (source == DamageSource.OUT_OF_WORLD || source.canHarmInCreative()) return super.attackEntityFrom(source, amount);
        if (guarding() && WorkerBeeEffects.marked(source.getTrueSource())) { workerLaserRing(); return false; }
        if (source.getImmediateSource() != null && source.getTrueSource() != null && amount >= 5.0
                && (source.isExplosion() || source.getImmediateSource() != source.getTrueSource())) breakArm = true;
        if (guarding() && !breakArm) {
            if (!world.isRemote && blockSoundCooldown == 0) {
                playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1, 1.2F);
                blockSoundCooldown = 10;
            }
            if (stronger()) heal((float) (amount * 0.2));
            return false;
        }
        if (hidden()) return false;
        double multiplier = workers().damageMultiplier();
        if (stronger() || action() != 6) multiplier -= arms() * (stronger() ? 0.225 : 0.175);
        amount *= Math.max(0, multiplier);
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setBoolean(RAID_TAG, raidSpawned);
        data.setInteger("MechasentArms", arms());
        data.setInteger("MechasentAction", action());
        data.setInteger("MechasentActionAge", actionAge());
        data.setInteger("MechasentTeleportCooldown", teleportCooldown);
        data.setInteger("MechasentRoarCooldown", roarCooldown);
        data.setInteger("MechasentDefenseCooldown", defenseCooldown);
        data.setInteger("MechasentHeavyCooldown", heavyCooldown);
        data.setInteger("MechasentPreviousAction", previousAction);
        data.setBoolean("MechasentBreakArm", breakArm);
        data.setInteger("MechasentStrongerTicks", strongerTicks);
        data.setInteger("MechasentBroadcastCooldown", broadcastCooldown);
        data.setInteger("MechasentBroadcastHealthBand", broadcastHealthBand);
        data.setBoolean("MechasentBroadcastSpawned", broadcastSpawned);
        data.setBoolean("MechasentBroadcastTarget", broadcastTarget);
        data.setBoolean("MechasentBroadcastStronger", broadcastStronger);
        data.setBoolean("MechasentTeleportBroadcastChecked", teleportBroadcastChecked);
        workers().write(data);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        raidSpawned = data.getBoolean(RAID_TAG) || getEntityData().getBoolean(RAID_TAG);
        getEntityData().setBoolean(RAID_TAG, raidSpawned);
        if (data.hasKey("MechasentArms")) dataManager.set(ARMS, MathHelper.clamp(data.getInteger("MechasentArms"), 0, 4));
        dataManager.set(ACTION, MathHelper.clamp(data.getInteger("MechasentAction"), 0, 8));
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("MechasentActionAge")));
        teleportCooldown = Math.max(0, data.getInteger("MechasentTeleportCooldown"));
        roarCooldown = Math.max(0, data.getInteger("MechasentRoarCooldown"));
        if (data.hasKey("MechasentDefenseCooldown")) defenseCooldown = Math.max(0, data.getInteger("MechasentDefenseCooldown"));
        heavyCooldown = Math.max(0, data.getInteger("MechasentHeavyCooldown"));
        previousAction = MathHelper.clamp(data.getInteger("MechasentPreviousAction"), 0, 8);
        breakArm = data.getBoolean("MechasentBreakArm");
        strongerTicks = Math.max(0, data.getInteger("MechasentStrongerTicks"));
        broadcastCooldown = Math.max(0, data.getInteger("MechasentBroadcastCooldown"));
        broadcastHealthBand = MathHelper.clamp(data.getInteger("MechasentBroadcastHealthBand"), 0, 2);
        broadcastSpawned = data.getBoolean("MechasentBroadcastSpawned") || data.hasUniqueId("UUID");
        broadcastTarget = data.getBoolean("MechasentBroadcastTarget");
        broadcastStronger = data.getBoolean("MechasentBroadcastStronger");
        teleportBroadcastChecked = data.getBoolean("MechasentTeleportBroadcastChecked");
        workers().read(data);
        hide(action() == 8 && actionAge() >= 20 && actionAge() <= 37);
    }
}
