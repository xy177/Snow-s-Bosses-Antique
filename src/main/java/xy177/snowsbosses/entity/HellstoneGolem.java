package xy177.snowsbosses.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.BossInfo;
import net.minecraft.world.BossInfoServer;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.ForgeEventFactory;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.content.HellstoneEffects;
import xy177.snowsbosses.network.Visuals;

public final class HellstoneGolem extends HellstoneMob {
    private static final DataParameter<Integer> PHASE = EntityDataManager.createKey(HellstoneGolem.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> VOICE_COMBAT = EntityDataManager.createKey(HellstoneGolem.class, DataSerializers.BOOLEAN);
    private int rangedTicks;
    private int previousAttack;
    private int chargeCooldown;
    private int summonCooldown;
    private int pillarCooldown;
    public static final int PILLAR_BEND_TICKS = 28;
    public static final int PILLAR_WAVE_INTERVAL = 60;
    private static final int PILLAR_RECOVERY_TICKS = 28;
    private static final DataParameter<Integer> PILLAR_RELEASE = EntityDataManager.createKey(HellstoneGolem.class, DataSerializers.VARINT);
    private int pillarWaves;
    private double chargeStartX;
    private double chargeStartZ;
    private final Set<UUID> chargeVictims = new HashSet<>();
    private final Map<UUID, BlockPos> summonedImps = new LinkedHashMap<>();
    private final BossInfoServer bar;

    public HellstoneGolem(World world) {
        super(world); enablePersistence();
        bar = new BossInfoServer(new TextComponentTranslation("entity.snows_bosses.hellstone_golem.name"), BossInfo.Color.RED, BossInfo.Overlay.PROGRESS);
        setSize(2.2F, 2.6F);
        stepHeight = 1.1F; experienceValue = 20;
    }
    @Override
    public String species() { return "hellstone_golem"; }
    @Override
    protected void entityInit() { super.entityInit(); dataManager.register(PHASE, 1); dataManager.register(PILLAR_RELEASE, -1); dataManager.register(VOICE_COMBAT, false); }
    @Override
    public void setAttackTarget(EntityLivingBase target) {
        super.setAttackTarget(target);
        if (!world.isRemote) dataManager.set(VOICE_COMBAT, target != null && target.isEntityAlive());
    }
    public int innerVoiceState() {
        if (!isEntityAlive() || action() < 0 || !dataManager.get(VOICE_COMBAT)) return 0;
        return action() == 9 ? 2 : action() == 10 ? 3 : 1;
    }
    @Override
    protected void action(int value) {
        if (!world.isRemote && value != action()) {
            if (value == 9) pillarCooldown = (int) Math.min(Integer.MAX_VALUE, (long) pillarCooldown + 200);
            if (value == 10) summonCooldown = (int) Math.min(Integer.MAX_VALUE, (long) summonCooldown + 200);
        }
        if (value == 10) { pillarWaves = 0; dataManager.set(PILLAR_RELEASE, -1); }
        super.action(value);
    }
    public float pillarAnimationTime(float partialTicks) {
        float age = actionAge() + partialTicks;
        int released = dataManager.get(PILLAR_RELEASE);
        if (released >= 0) return 2.4666667F + Math.max(0, age - released) / 20.0F;
        float bend = PILLAR_BEND_TICKS / 20.0F;
        return age <= PILLAR_BEND_TICKS ? age / 20.0F : bend + (2.4666667F - bend)
                * Math.min(1, (age - PILLAR_BEND_TICKS) / (3.0F * PILLAR_WAVE_INTERVAL));
    }
    public int phase() { return dataManager.get(PHASE); }
    @Override
    protected boolean canDespawn() { return false; }
    @Override
    public boolean canBePushed() { return false; }
    @Override
    protected void collideWithEntity(Entity entity) {
        if (action() != 7 || !chargeVictims.contains(entity.getUniqueID())) super.collideWithEntity(entity);
    }
    @Override
    public void addTrackingPlayer(EntityPlayerMP player) { super.addTrackingPlayer(player); Visuals.bossBar(player, bar.getUniqueId(), true, 0xFF8C00, getEntityId()); bar.addPlayer(player); }
    @Override
    public void removeTrackingPlayer(EntityPlayerMP player) { super.removeTrackingPlayer(player); bar.removePlayer(player); Visuals.bossBar(player, bar.getUniqueId(), false, 0xFF8C00, getEntityId()); }
    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        bar.setPercent(Math.max(0, Math.min(1, getHealth() / getMaxHealth())));
        bar.setName(getDisplayName());
        if (action() <= 0 && getAttackTarget() != null) {
            getLookHelper().setLookPositionWithEntity(getAttackTarget(), 30, 30);
        } else if (action() > 0) {
            Vec3d direction = getLookVec();
            getLookHelper().setLookPosition(posX + direction.x * 8, posY + getEyeHeight() + direction.y * 8,
                    posZ + direction.z * 8, 360, 360);
        }
    }
    @Override
    public void setDead() {
        if (bar != null) bar.setVisible(false);
        if (!world.isRemote) {
            for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
                if (entity instanceof HellstoneImp && ((HellstoneImp) entity).summonedBy(this)) entity.setDead();
                if (entity instanceof HellstonePillar && ((HellstonePillar) entity).summonedBy(this)) entity.setDead();
                if (entity instanceof HellstonePillarShot && ((HellstonePillarShot) entity).summonedBy(this)) entity.setDead();
            }
        }
        super.setDead();
    }
    public boolean trackImp(HellstoneImp imp) {
        if (!summonedImps.containsKey(imp.getUniqueID()) && summonedImps.size() >= 6) return false;
        summonedImps.put(imp.getUniqueID(), imp.getPosition());
        return true;
    }
    public void releaseImp(UUID uuid) { summonedImps.remove(uuid); }
    private void updateSummons() {
        if (ticksExisted > 40 && ticksExisted % 20 == 0 && world instanceof WorldServer) {
            summonedImps.entrySet().removeIf(entry -> world.isBlockLoaded(entry.getValue())
                    && ((WorldServer) world).getEntityFromUuid(entry.getKey()) == null);
        }
    }
    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, false));
        targetTasks.addTask(2, new EntityAIHurtByTarget(this, false));
        tasks.addTask(4, new EntityAIWander(this, 1.0) {
            @Override public boolean shouldExecute() { return action() == 0 && getAttackTarget() == null && super.shouldExecute(); }
            @Override public boolean shouldContinueExecuting() { return action() == 0 && getAttackTarget() == null && super.shouldContinueExecuting(); }
        });
    }
    private boolean highTarget() { return getAttackTarget() != null && getAttackTarget().posY - posY >= 5.0; }

    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && isEntityAlive()) {
            if (chargeCooldown > 0) chargeCooldown--;
            if (summonCooldown > 0) summonCooldown--;
            if (pillarCooldown > 0) pillarCooldown--;
            updateSummons();
            extinguish(); removePotionEffect(MobEffects.POISON);
            getEntityData().setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, 0);
            if (isWet()) dataManager.set(PHASE, 1);
            else if (getHealth() <= getMaxHealth() * 0.5) dataManager.set(PHASE, 2);
            if (getAttackTarget() != null && (!getAttackTarget().isEntityAlive() || HellstoneEffects.allies(this, getAttackTarget()))) setAttackTarget(null);
            if (action() == -1) {
                double range = 8.0;
                if (getAttackTarget() != null || !world.getEntitiesWithinAABB(EntityPlayer.class, new AxisAlignedBB(posX - range, posY - range, posZ - range, posX + range, posY + range, posZ + range),
                        player -> !player.capabilities.isCreativeMode && !player.isSpectator()).isEmpty()) action(-2);
            }
            if (action() == -2) {
                if (actionAge() == 24) strike(-2);
                if (actionAge() >= 52) action(0);
            } else if (action() > 0) attackTick();
            else if (action() == 0 && getAttackTarget() != null) chooseAttack();
            if (action() != 0 && action() != 7 && !(action() == 5 && actionAge() >= 20)) {
                getNavigator().clearPath(); setMoveForward(0); setMoveStrafing(0); motionX = motionZ = 0;
                if (action() < 0) motionY = Math.min(motionY, 0);
            } else if (action() != 7 && getAttackTarget() != null) {
                getNavigator().tryMoveToEntityLiving(getAttackTarget(), 1.0);
                getLookHelper().setLookPositionWithEntity(getAttackTarget(), 30, 30);
            }
            absorbLava();
            if (ticksExisted % 2 == 0) Visuals.particles(world, "hellstone_fire", posX, posY + height / 2, posZ, phase(), width / 2, 0);
            if (highTarget() && ticksExisted % 5 == 0) Visuals.particles(world, "hellstone_angry", posX, posY + height * 1.15, posZ, 1, width / 3, 0);
        }
        super.onLivingUpdate();
    }

    private void chooseAttack() {
        EntityLivingBase target = getAttackTarget();
        double distance = getDistance(target) - (width + target.width) / 2;
        if (highTarget()) { faceTarget(); action(5); return; }
        if (distance < 12.0) rangedTicks = Math.min(100, rangedTicks + 1);
        double health = getHealth() / (double) getMaxHealth();
        String weightKey = health > 0.5 ? "weightAboveHalf" : health > 0.25 ? "weightAboveQuarter" : "weightAtOrBelowQuarter";
        int attackWeight = health > 0.5 ? 3 : health > 0.25 ? 2 : 1;
        int[] attacks = {1, 2, 3, 4, 6, 9, 10};
        int[] weights = new int[attacks.length];
        for (int index = 0; index < 4; index++) {
            int attack = attacks[index];
            if (distance < 3.2 ? attack == 1 || attack != previousAttack
                    : rangedTicks >= 100 && distance < 12.0 && attack == (distance < 6.0 ? 4 : 3))
                weights[index] = Math.max(0, (int) n("attack" + attack + "." + weightKey, attackWeight));
        }
        double centerDistance = getDistance(target);
        if (chargeCooldown == 0 && onGround && centerDistance >= 6 && centerDistance <= 16
                && Math.abs(target.posY - posY) < 2 && canEntityBeSeen(target)) weights[4] = Math.max(0, (int) n("chargeWeight", 2));
        if (summonCooldown == 0 && onGround && summonedImps.size() < 6)
            weights[5] = Math.max(0, (int) n(health > 0.5 ? "summonWeightAboveHalf" : "summonWeightAtOrBelowHalf", health > 0.5 ? 2 : 3));
        if (pillarCooldown == 0 && onGround && centerDistance >= 5.5 && centerDistance <= 16
                && Math.abs(target.posY - posY) < 2 && canEntityBeSeen(target)) {
            String pillarKey = health > 0.75 ? "pillarWeightAboveThreeQuarters" : health > 0.5 ? "pillarWeightAboveHalf"
                    : health > 0.25 ? "pillarWeightAboveQuarter" : "pillarWeightAtOrBelowQuarter";
            weights[6] = Math.max(0, (int) n(pillarKey, health > 0.75 ? 0 : health > 0.5 ? 1 : health > 0.25 ? 2 : 4));
        }
        long total = 0;
        for (int weight : weights) total += weight;
        if (total == 0) return;
        long choice = (long) (rand.nextDouble() * total);
        for (int index = 0; index < attacks.length; index++) {
            if ((choice -= weights[index]) < 0) {
                int next = attacks[index];
                previousAttack = next;
                faceTarget();
                action(next);
                if (next == 6) chargeVictims.clear();
                return;
            }
        }
    }

    private void faceTarget() {
        EntityLivingBase target = getAttackTarget();
        double x = target.posX - posX, z = target.posZ - posZ;
        rotationYaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90;
        rotationPitch = (float) -Math.toDegrees(Math.atan2(target.posY + 1 - posY - getEyeHeight(), Math.hypot(x,z)));
        renderYawOffset = rotationYawHead = rotationYaw;
    }

    private void attackTick() {
        int attack = action();
        int age = actionAge();
        if (attack >= 6) { skillTick(attack, age); return; }
        int[] hits = {0, 22, 18, 17, 23, 13};
        int[] durations = {0, 42, 52, 51, 59, 31};
        if (age == hits[attack]
                || attack == 2 && age == 39) strike(attack);
        if (attack == 4 && age >= 24 && age <= 44 && onGround) {
            waves(3, 3, 20, 0.4, rand.nextDouble() * Math.PI * 2, false, true);
        }
        if (age >= durations[attack]) {
            if (attack == 3 || attack == 4) rangedTicks = 0;
            action(0);
        }
    }

    private void skillTick(int attack, int age) {
        if (attack == 6) {
            if (getAttackTarget() != null && getAttackTarget().isEntityAlive()) faceTarget();
            if (age == 1) playSound(SoundEvents.ENTITY_IRONGOLEM_HURT, 1.2F, 0.65F);
            if (age % 4 == 0) Visuals.particles(world, "hellstone_fire", posX, posY + 1, posZ, 6, 0.6, 0.06);
            if (age >= 20) {
                chargeStartX = posX; chargeStartZ = posZ;
                rotationPitch = 0;
                action(7);
            }
        } else if (attack == 7) {
            Vec3d step = Vec3d.fromPitchYaw(0, rotationYaw).scale(0.7);
            AxisAlignedBB next = getEntityBoundingBox().offset(step);
            EntityLivingBase target = getAttackTarget();
            boolean reached = target != null && getDistanceSq(target) <= 4 + 1.0E-4;
            boolean limit = Math.hypot(posX - chargeStartX, posZ - chargeStartZ) >= 14 - 1.0E-4 || age > 20;
            boolean hitTarget = false;
            boolean blocked = !world.getCollisionBoxes(this, next).isEmpty();
            boolean unsupported = world.getCollisionBoxes(this, next.offset(0, -1.2, 0)).isEmpty();
            double x = unsupported || limit && !reached ? 0 : step.x, z = unsupported || limit && !reached ? 0 : step.z;
            if (blocked) {
                List<AxisAlignedBB> collisions = world.getCollisionBoxes(this, getEntityBoundingBox().expand(x, 0, z));
                for (AxisAlignedBB box : collisions) x = box.calculateXOffset(getEntityBoundingBox(), x);
                AxisAlignedBB shifted = getEntityBoundingBox().offset(x, 0, 0);
                for (AxisAlignedBB box : collisions) z = box.calculateZOffset(shifted, z);
            }
            for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class,
                    getEntityBoundingBox().expand(x, 0, z).grow(0.15))) {
                if (victim == this || HellstoneEffects.allies(this, victim) || !victim.isEntityAlive()
                        || blocked && world.rayTraceBlocks(getPositionVector().addVector(0, height * 0.5, 0),
                        victim.getPositionVector().addVector(0, victim.height * 0.5, 0), false, true, false) != null
                        || !chargeVictims.add(victim.getUniqueID())) continue;
                if (victim.attackEntityFrom(DamageSource.causeMobDamage(this), (float) n("chargeDamage", 7))) {
                    double resistance = victim.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
                    double factor = Math.max(0.5, 1 - resistance);
                    victim.addVelocity(step.x / 0.7 * factor, 0.75 * factor, step.z / 0.7 * factor);
                    victim.velocityChanged = true;
                    if (victim == target) hitTarget = true;
                }
            }
            if (hitTarget || limit || reached || blocked || unsupported) {
                motionX = motionZ = 0;
                action(8);
                return;
            }
            getNavigator().clearPath(); setMoveForward(0); setMoveStrafing(0);
            motionX = step.x; motionZ = step.z;
            if (age % 3 == 0) {
                Visuals.particles(world, "hellstone_fire", posX, posY + 0.25, posZ, 6, 0.65, 0.09);
                playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 0.9F, 0.7F);
            }
        } else if (attack == 8) {
            if (age == 12) {
                for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class,
                        getEntityBoundingBox().grow(4, 1.5, 4))) {
                    double x = victim.posX - posX, z = victim.posZ - posZ;
                    if (victim != this && !HellstoneEffects.allies(this, victim) && x * x + z * z <= 16)
                        victim.attackEntityFrom(DamageSource.causeMobDamage(this), (float) n("chargeSlamDamage", 14));
                }
                Visuals.particles(world, "hellstone_slam_ring", posX, posY + 0.08, posZ, 1, 0, 0);
                Visuals.particles(world, "hellstone_explode", posX, posY + 0.3, posZ, 8, 0.7, 0.05);
                playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.6F, 0.65F);
            }
            if (age >= 32) { chargeCooldown = 240; action(0); }
        } else if (attack == 9) {
            if (age == 10) {
                Visuals.particles(world, "hellstone_summon_ring", posX, posY + 0.08, posZ, 1, 0, 0);
                playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, 1.3F, 0.6F);
            }
            if (age >= 15 && age <= 50 && age % 5 == 0)
                Visuals.particles(world, "hellstone_fire", posX, posY + height * 0.7, posZ, 8, 0.8, 0.08);
            if (age == 50) summonImps();
            if (age >= 86) { summonCooldown = Math.max(1, (int) n("summonCooldown", 1500)); action(0); }
        } else if (attack == 10) {
            if (age == 1) playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, 1.4F, 0.55F);
            if (pillarWaves < 4 && age >= PILLAR_BEND_TICKS + pillarWaves * PILLAR_WAVE_INTERVAL) {
                spawnPillars();
                pillarWaves++;
                if (pillarWaves == 4) dataManager.set(PILLAR_RELEASE, age);
            }
            int released = dataManager.get(PILLAR_RELEASE);
            if (released >= 0 && age >= released + PILLAR_RECOVERY_TICKS) {
                pillarCooldown = Math.max(1, (int) n("summonCooldown", 1500));
                action(0);
            }
        }
    }

    private void spawnPillars() {
        int minimum = Math.max(1, Math.min(8, (int) n("pillarCountMin", 3)));
        int maximum = Math.max(minimum, Math.min(8, (int) n("pillarCountMax", 5)));
        int count = minimum + (maximum == minimum ? 0 : rand.nextInt(maximum - minimum + 1));
        double radius = Math.max(1, n("pillarRadius", 10));
        double separation = Math.min(3, radius / Math.sqrt(count));
        List<Vec3d> occupied = new ArrayList<>();
        for (HellstonePillar pillar : world.getEntitiesWithinAABB(HellstonePillar.class,
                getEntityBoundingBox().grow(radius + separation, 18, radius + separation), pillar -> !pillar.isDead))
            occupied.add(pillar.getPositionVector());
        int extent = (int) Math.ceil(radius);
        List<BlockPos> candidates = new ArrayList<>();
        for (int x = MathHelper.floor(posX) - extent; x <= MathHelper.floor(posX) + extent; x++) {
            for (int z = MathHelper.floor(posZ) - extent; z <= MathHelper.floor(posZ) + extent; z++) {
                double dx = x + 0.5 - posX, dz = z + 0.5 - posZ;
                double distance = dx * dx + dz * dz;
                if (distance > radius * radius || distance < 4) continue;
                BlockPos ground = pillarGround(x, posY + 8, z);
                if (ground != null) candidates.add(ground);
            }
        }
        Collections.shuffle(candidates, rand);
        double rotation = rand.nextDouble() * Math.PI * 2;
        int spawned = 0;
        for (int index = 0; index < count; index++) {
            BlockPos chosen = null;
            double bestDistance = -1;
            boolean bestSector = false;
            for (BlockPos candidate : candidates) {
                double x = candidate.getX() + 0.5, z = candidate.getZ() + 0.5;
                double nearest = Double.POSITIVE_INFINITY;
                for (Vec3d point : occupied) {
                    double dx = x - point.x, dz = z - point.z;
                    nearest = Math.min(nearest, dx * dx + dz * dz);
                }
                if (nearest < separation * separation) continue;
                double angle = (Math.atan2(z - posZ, x - posX) - rotation + Math.PI * 4) % (Math.PI * 2);
                boolean sector = (int) (angle * count / (Math.PI * 2)) == index;
                if (chosen == null || sector && !bestSector || sector == bestSector && nearest > bestDistance) {
                    chosen = candidate;
                    bestDistance = nearest;
                    bestSector = sector;
                }
            }
            if (chosen == null) break;
            candidates.remove(chosen);
            HellstonePillar pillar = new HellstonePillar(world, this, chosen.getX() + 0.5, chosen.getY(), chosen.getZ() + 0.5);
            if (world.spawnEntity(pillar)) {
                occupied.add(pillar.getPositionVector());
                spawned++;
            }
        }
        if (spawned > 0) playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 1.3F, 0.65F);
    }

    private BlockPos pillarGround(double x, double y, double z) {
        int start = MathHelper.floor(y);
        for (int offset = 0; offset <= 16; offset++) {
            BlockPos pos = new BlockPos(x, start - offset, z);
            if (!world.isBlockLoaded(pos) || !world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) continue;
            AxisAlignedBB box = new AxisAlignedBB(pos).expand(0, 1, 0);
            if (world.getCollisionBoxes(null, box).isEmpty() && !world.containsAnyLiquid(box)) return pos;
        }
        return null;
    }

    private void summonImps() {
        int count = Math.min(3, 6 - summonedImps.size());
        double start = rand.nextDouble() * Math.PI * 2;
        for (int index = 0; index < count; index++) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = start + index * Math.PI * 2 / Math.max(1, count) + (rand.nextDouble() - 0.5) * 0.6;
                double radius = 3 + rand.nextDouble() * 2;
                BlockPos pos = new BlockPos(posX + Math.cos(angle) * radius, posY + 2, posZ + Math.sin(angle) * radius);
                if (!world.isBlockLoaded(pos)) continue;
                for (int down = 0; down < 5 && !world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP); down++) pos = pos.down();
                if (!world.getBlockState(pos.down()).isSideSolid(world, pos.down(), net.minecraft.util.EnumFacing.UP)) continue;
                double spawnDistance = Math.hypot(pos.getX() + 0.5 - posX, pos.getZ() + 0.5 - posZ);
                if (spawnDistance < 3 || spawnDistance > 5) continue;
                HellstoneImp imp = new HellstoneImp(world);
                imp.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rand.nextFloat() * 360, 0);
                if (!world.getCollisionBoxes(imp, imp.getEntityBoundingBox()).isEmpty()
                        || world.containsAnyLiquid(imp.getEntityBoundingBox()) || !world.checkNoEntityCollision(imp.getEntityBoundingBox())) continue;
                imp.bindTo(this);
                imp.setAttackTarget(getAttackTarget());
                if (world.spawnEntity(imp)) {
                    trackImp(imp);
                    Visuals.particles(world, "hellstone_spawn_ring", imp.posX, imp.posY + 0.05, imp.posZ, 1, 0, 0);
                    Visuals.particles(world, "hellstone_fire", imp.posX, imp.posY + 0.35, imp.posZ, 16, 0.35, 0.09);
                }
                break;
            }
        }
        playSound(SoundEvents.BLOCK_LAVA_EXTINGUISH, 1.2F, 0.6F);
    }

    private void strike(int attack) {
        Vec3d look = getLookVec();
        String prefix = attack == -2 ? "wake" : "attack" + attack;
        double offset = (attack == 2 ? 2.7 : 2.3);
        double radius = (attack == 2 ? 1.4 : attack == 3 ? 1.7 : attack == 4 ? 2.2 : 2.9);
        double low = (attack == 2 ? 2 : attack == 5 ? 4 : 1);
        double high = (attack == 5 ? 6 : attack == 3 ? 5 : 3);
        double search = 6.0;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(posX - search, posY - low, posZ - search, posX + search, posY + high, posZ + search))) {
            if (victim == this || HellstoneEffects.allies(this, victim)) continue;
            double dx = victim.posX - (posX + look.x * offset), dz = victim.posZ - (posZ + look.z * offset);
            if (dx * dx + dz * dz >= Math.pow(radius + (width + victim.width) / 2, 2)) continue;
            if (attack == 3) {
                double direction = Math.hypot(look.x, look.z) * Math.hypot(victim.posX-posX,victim.posZ-posZ);
                if (direction > 0 && (look.x*(victim.posX-posX)+look.z*(victim.posZ-posZ))/direction < Math.cos(Math.toRadians(70.0))) continue;
            }
            victim.attackEntityFrom(DamageSource.causeMobDamage(this), (float) (getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue()
                    * n(prefix + ".damageMultiplier", attack == 2 || attack == 4 ? 0.8 : attack == 3 ? 1.2 : 1)));
            if (phase() == 2 || attack == -2) HellstoneEffects.burn(victim, 100);
            double resistance = victim.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
            double lift = attack == 2 ? 0.35 : attack == 5 ? 0.4 : 0.5;
            double factor = attack == -2 ? Math.max(0, 1 - resistance) : Math.max(0.5, 1 - resistance);
            if (attack == 3) victim.addVelocity(look.x * 1.0 * factor, 0.75 * factor, look.z * 1.0 * factor);
            else victim.addVelocity(0, lift * factor, 0);
            victim.velocityChanged = true;
        }
        playSound(attack == 3 ? SoundEvents.ENTITY_ZOMBIE_BREAK_DOOR_WOOD : SoundEvents.ENTITY_LIGHTNING_IMPACT, 1, 1);
        if (!onGround) return;
        Visuals.particles(world, "hellstone_fire", posX + look.x * offset, posY, posZ + look.z * offset, 30, 2.1, 0);
        if (attack == 3) bombFan();
        else if (attack != 4) {
            int count = attack == 2 || attack == 5 ? 12 : 18;
            waves(count, attack == -2 ? 2 : phase(), (attack == 2 ? 10 : attack == 5 ? 15 : 17),
                    (attack == 2 ? 0.55 : attack == 5 ? 0.4 : 0.35), Math.atan2(look.z, look.x), false, true);
        }
    }

    private void waves(int count, int type, int lifetime, double speed, double angle, boolean fan, boolean offset) {
        Vec3d look = getLookVec();
        for (int index = 0; index < count; index++) {
            double direction = angle + (fan ? (index - (count - 1) / 2.0) * Math.toRadians(24.0) : (index + 0.5) * Math.PI * 2 / count);
            double waveOffset = 2.7;
            if (action() != 2) waveOffset = 2.3;
            double x = posX + (fan ? Math.cos(direction) * 2.1 : offset ? look.x * waveOffset : 0);
            double z = posZ + (fan ? Math.sin(direction) * 2.1 : offset ? look.z * waveOffset : 0);
            world.spawnEntity(new HellstoneWave(world, this, x, posY, z, direction, speed, lifetime, type,
                    Math.ceil(getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * n("projectileDamageMultiplier", 0.8))));
        }
    }

    private void bombFan() {
        Vec3d look = getLookVec();
        int count = 5;
        for (int index = 0; index < count; index++) {
            double angle = Math.atan2(look.z, look.x) + (index - (count - 1) / 2.0) * Math.toRadians(24.0);
            for (int tier = 0; tier < 2; tier++) {
                HellstoneBomb bomb = new HellstoneBomb(world, this);
                bomb.setPosition(posX + Math.cos(angle) * 1.6 + (rand.nextDouble() - 0.5) * 0.6, posY, posZ + Math.sin(angle) * 1.6 + (rand.nextDouble() - 0.5) * 0.6);
                bomb.shoot(look.x + (rand.nextDouble() - 0.5) * 0.2, 1, look.z + (rand.nextDouble() - 0.5) * 0.2,
                        (float) (0.6 + (rand.nextDouble() - 0.5) * 0.2),
                        20.0F);
                bomb.damage = getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * n("projectileDamageMultiplier", 0.8);
                world.spawnEntity(bomb);
            }
        }
        waves(count, phase(), 20, 0.35, Math.atan2(look.z, look.x), true, false);
    }

    private void absorbLava() {
        if (!isInLava() || !ForgeEventFactory.getMobGriefingEvent(world, this)) return;
        int range = 4;
        int removed = 0;
        for (BlockPos pos : BlockPos.getAllInBox(getPosition().add(-range, -range, -range), getPosition().add(range, range, range))) {
            if (world.isBlockLoaded(pos) && world.getBlockState(pos).getMaterial() == Material.LAVA && world.setBlockToAir(pos)) removed++;
        }
        if (removed > 0) { heal((float) (removed * 1.0)); playSound(SoundEvents.ITEM_BUCKET_FILL_LAVA, 1, 1); }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (HellstoneEffects.allies(this, source.getTrueSource())) return false;
        if (source.isFireDamage() || source == DamageSource.FALL || source == DamageSource.DROWN || source == HellstoneEffects.DAMAGE) return false;
        if ((action() < 0 || highTarget()) && !source.canHarmInCreative()) return false;
        if (phase() == 2 && source.getTrueSource() != null) HellstoneEffects.burn(source.getTrueSource(), 100);
        Entity direct = source.getImmediateSource();
        if (direct instanceof net.minecraft.entity.EntityLiving || direct instanceof EntityPlayer) amount = (float) Math.min(amount * 0.25, 15.0);
        else {
            if (source.getTrueSource() != null && getDistanceSq(source.getTrueSource()) > Math.pow(22.0, 2)) return false;
            amount *= 0.8;
        }
        return super.attackEntityFrom(source, amount);
    }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_IRONGOLEM_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_IRONGOLEM_DEATH; }
    @Override
    protected void playStepSound(BlockPos pos, Block block) { playSound(SoundEvents.ENTITY_IRONGOLEM_STEP, 0.15F, 1); }
    @Override
    public void writeEntityToNBT(NBTTagCompound data) { super.writeEntityToNBT(data); data.setInteger("HellstonePhase", phase()); data.setInteger("RangedTicks", rangedTicks); data.setInteger("PreviousAttack", previousAttack);
        data.setInteger("ChargeCooldown", chargeCooldown); data.setInteger("SummonCooldown", summonCooldown); data.setInteger("PillarCooldown", pillarCooldown);
        data.setInteger("PillarWaves", pillarWaves); data.setInteger("PillarRelease", dataManager.get(PILLAR_RELEASE));
        data.setDouble("ChargeStartX", chargeStartX); data.setDouble("ChargeStartZ", chargeStartZ);
        NBTTagList imps = new NBTTagList();
        for (Map.Entry<UUID, BlockPos> entry : summonedImps.entrySet()) {
            NBTTagCompound imp = new NBTTagCompound(); imp.setUniqueId("Id", entry.getKey());
            imp.setLong("Pos", entry.getValue().toLong()); imps.appendTag(imp);
        }
        data.setTag("SummonedImps", imps);
        NBTTagList victims = new NBTTagList();
        for (UUID uuid : chargeVictims) { NBTTagCompound victim = new NBTTagCompound(); victim.setUniqueId("Id", uuid); victims.appendTag(victim); }
        data.setTag("ChargeVictims", victims);
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data); dataManager.set(PHASE, data.getInteger("HellstonePhase") == 2 ? 2 : 1);
        rangedTicks = Math.max(0, data.getInteger("RangedTicks")); previousAttack = data.getInteger("PreviousAttack");
        chargeCooldown = Math.max(0, data.getInteger("ChargeCooldown")); summonCooldown = Math.max(0, data.getInteger("SummonCooldown")); pillarCooldown = Math.max(0, data.getInteger("PillarCooldown"));
        pillarWaves = Math.max(0, Math.min(4, data.getInteger("PillarWaves")));
        dataManager.set(PILLAR_RELEASE, data.hasKey("PillarRelease") ? data.getInteger("PillarRelease") : -1);
        chargeStartX = data.getDouble("ChargeStartX"); chargeStartZ = data.getDouble("ChargeStartZ");
        summonedImps.clear();
        NBTTagList imps = data.getTagList("SummonedImps", 10);
        for (int index = 0; index < imps.tagCount(); index++) {
            NBTTagCompound imp = imps.getCompoundTagAt(index);
            if (imp.hasUniqueId("Id")) summonedImps.put(imp.getUniqueId("Id"), BlockPos.fromLong(imp.getLong("Pos")));
        }
        chargeVictims.clear();
        NBTTagList victims = data.getTagList("ChargeVictims", 10);
        for (int index = 0; index < victims.tagCount(); index++) {
            NBTTagCompound victim = victims.getCompoundTagAt(index);
            if (victim.hasUniqueId("Id")) chargeVictims.add(victim.getUniqueId("Id"));
        }
    }
}
