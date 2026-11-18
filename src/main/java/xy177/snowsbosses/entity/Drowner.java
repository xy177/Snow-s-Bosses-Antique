package xy177.snowsbosses.entity;

import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.registry.FFDPotions;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.network.Visuals;
import xy177.snowsbosses.content.ModSounds;

public final class Drowner extends OceanBoss {
    public static final float BEAM_RENDER_WIDTH = 0.24F;
    public static final float TRACKING_BEAM_RENDER_WIDTH = 0.72F;
    public static final double BEAM_HIT_RADIUS = 0.24D;
    public static final double TRACKING_BEAM_HIT_RADIUS = 0.72D;
    private static final double BEAM_MAX_RANGE = 32.0D;
    private static final DataParameter<Boolean> SWIMMING = EntityDataManager.createKey(Drowner.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> BEAM_DATA = EntityDataManager.createKey(Drowner.class, DataSerializers.STRING);
    private final PathNavigate groundNavigator;
    private final PathNavigate swimNavigator;
    private float swimAmount;
    private float previousSwimAmount;
    private float swimCycle;
    private float previousSwimCycle;
    private int rangeTick;
    private int dashTick;
    private int meleeCount;
    private int regeneration;
    private final List<TechnicalPiranha> beamFish = new ArrayList<>();
    private EntityLivingBase beamTarget;
    private double beamTargetX;
    private double beamTargetY;
    private double beamTargetZ;
    private int beamDirection;
    private final Deque<Vec3d> beamTargetHistory = new ArrayDeque<>();
    private final List<Vec3d[]> beamSegments = new ArrayList<>();
    private final List<Vec3d> beamFishTargets = new ArrayList<>();
    private final List<Vec3d> beamPathStarts = new ArrayList<>();
    private final List<Vec3d> beamPathEnds = new ArrayList<>();
    private int beamDurationTicks;
    private double beamEndX;
    private double beamEndY;
    private double beamEndZ;
    private double beamLockX;
    private double beamLockY;
    private double beamLockZ;
    private Vec3d beamRotatingCenter;

    public Drowner(World world) {
        super(world, BossInfo.Overlay.NOTCHED_10);
        groundNavigator = navigator;
        ((PathNavigateGround) groundNavigator).setCanSwim(false);
        setPathPriority(PathNodeType.WATER, 0);
        swimNavigator = new PathNavigateSwimmer(this, world);
        moveHelper = new SwimMovement();
        setSize(0.6F, 1.8F);
        isImmuneToFire = true;
        experienceValue = 70;
    }

    @Override
    public String species() {
        return "drowner";
    }

    @Override
    public String textureName() {
        return "drowner_new";
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SWIMMING, false);
        dataManager.register(BEAM_DATA, "");
    }

    public float getSwimAmount(float partialTicks) {
        if (isRiding() || !isEntityAlive() || hasActionAnimation()) return 0;
        return previousSwimAmount + (swimAmount - previousSwimAmount) * partialTicks;
    }

    public float getSwimCycle(float partialTicks) {
        return previousSwimCycle + (swimCycle - previousSwimCycle) * partialTicks;
    }

    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, 10, false, false, null));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 10, false, false, player -> !isCreativePlayer(player)));
        targetTasks.addTask(3, new EntityAIHurtByTarget(this, false));
        tasks.addTask(4, new EntityAIWander(this, 1.0, 120) {
            @Override
            public boolean shouldExecute() {
                return getAttackTarget() == null && super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return getAttackTarget() == null && super.shouldContinueExecuting();
            }
        });
        tasks.addTask(5, new EntityAISwimming(this) {
            @Override
            public boolean shouldExecute() {
                return !isInWater() && super.shouldExecute();
            }
        });
        tasks.addTask(6, new EntityAILookIdle(this) {
            @Override
            public boolean shouldExecute() {
                return getAttackTarget() == null && super.shouldExecute();
            }
            @Override
            public boolean shouldContinueExecuting() {
                return getAttackTarget() == null && super.shouldContinueExecuting();
            }
        });
    }

    @Override
    protected void updateAITasks() {
        super.updateAITasks();
        EntityLivingBase target = getAttackTarget();
        if (isCreativePlayer(target)) {
            setAttackTarget(null);
            return;
        }
        if (target != null && target.isEntityAlive()) look(target, false);
    }

    static boolean isCreativePlayer(Entity target) {
        if (!(target instanceof EntityPlayer)) return false;
        EntityPlayer player = (EntityPlayer) target;
        return player.isCreative() || player.capabilities.disableDamage || player.isSpectator();
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public PathNavigate getNavigator() {
        Entity vehicle = getRidingEntity();
        return vehicle instanceof DeepwaterPiranha ? ((DeepwaterPiranha) vehicle).getNavigator() : super.getNavigator();
    }

    @Override
    public EntityMoveHelper getMoveHelper() {
        Entity vehicle = getRidingEntity();
        return vehicle instanceof DeepwaterPiranha ? ((DeepwaterPiranha) vehicle).getMoveHelper() : super.getMoveHelper();
    }

    @Override
    protected String idleAnimation(boolean moving) {
        return moving ? "walk" : "idle2";
    }

    @Override
    public void onLivingUpdate() {
        if (world.isRemote) {
            previousSwimAmount = swimAmount;
            previousSwimCycle = swimCycle;
            if (isRiding() || !isEntityAlive() || hasActionAnimation()) {
                previousSwimAmount = swimAmount = 0;
            } else {
                float step = 1.0F / 10;
                swimAmount = MathHelper.clamp(swimAmount + (dataManager.get(SWIMMING) ? step : -step), 0, 1);
            }
            if (swimAmount > 0) swimCycle++;
        } else {
            boolean swimming = shouldSwim();
            PathNavigate next = swimming ? swimNavigator : groundNavigator;
            if (navigator != next) {
                navigator.clearPath();
                navigator = next;
                moveVertical = moveForward = 0;
                setJumping(false);
            }
        }
        if (isBeamAction()) {
            motionX = motionY = motionZ = 0;
            velocityChanged = true;
        }
        super.onLivingUpdate();
        if (world.isRemote || !isEntityAlive()) return;
        if (!isRiding() && !isBeamAction()) {
            double radius = 2.0;
            List<DeepwaterPiranha> fish = world.getEntitiesWithinAABB(DeepwaterPiranha.class,
                    new AxisAlignedBB(getPositionVector(), getPositionVector()).grow(radius));
            DeepwaterPiranha closest = null;
            for (DeepwaterPiranha candidate : fish) {
                if (candidate.isEntityAlive() && (closest == null || getDistanceSq(candidate) < getDistanceSq(closest))) closest = candidate;
            }
            if (closest != null) startRiding(closest);
        } else {
            resistance(1);
        }
        if (regeneration >= 20) {
            heal(1.0F);
            regeneration = 0;
        }
        boolean wet = isInWater() || world.isRainingAt(getPosition()) || world.isRainingAt(new net.minecraft.util.math.BlockPos(posX, posY + height, posZ));
        if (wet && FFDPotions.effectiveDolphinsGrace() != null) {
            addPotionEffect(new PotionEffect(FFDPotions.effectiveDolphinsGrace(), 5, 0, false, false));
        }
        int rangedCooldown = isRiding() ? 80 : 50;
        if (rangeTick < rangedCooldown) rangeTick++;
        if (isRiding() && rangeTick < 45) rangeTick++;
        if (dashTick < 120) dashTick++;
        EntityLivingBase target = getAttackTarget();
        if (isBeamAction()) {
            updateBeamAction();
            dataManager.set(SWIMMING, false);
            return;
        }
        if (target != null && target.isEntityAlive()) {
            double distance = getDistanceSq(target);
            double protectedRange = (isRiding() ? 36 : 24);
            if (distance > protectedRange * protectedRange) resistance(4);
            if (distance > Math.pow(1.0, 2)) {
                getNavigator().tryMoveToEntityLiving(target, 1.2);
                look(target, false);
            } else getNavigator().clearPath();
            if (action == 0) select(target, distance);
            if (isBeamAction()) {
                motionX = motionY = motionZ = 0;
                setPosition(beamLockX, beamLockY, beamLockZ);
                updateBeamAction();
                dataManager.set(SWIMMING, false);
                return;
            }
            if (action != 0) attack(target);
        }
        if (action != 0) {
            getNavigator().clearPath();
            int duration = new int[]{0, 20, 18, 22, 15, 70, 65}[action];
            if (actionTick >= duration) finish();
            else actionTick++;
        }
        if (wet && regeneration < 20) regeneration++;
        dataManager.set(SWIMMING, shouldSwim() && !hasFloorWithinThreeBlocks() && action == 0 && !onGround && !navigator.noPath());
    }

    private void resistance(int amplifier) {
        addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 5, amplifier, false, false));
    }

    private boolean shouldSwim() {
        EntityLivingBase target = getAttackTarget();
        return !isRiding() && isInWater()
                && target != null && target.isEntityAlive() && target.isInWater();
    }

    private boolean hasFloorWithinThreeBlocks() {
        AxisAlignedBB feet = getEntityBoundingBox();
        AxisAlignedBB below = new AxisAlignedBB(feet.minX, feet.minY - 3 - 1.0E-6, feet.minZ, feet.maxX, feet.minY + 1.0E-6, feet.maxZ);
        List<AxisAlignedBB> collisions = new java.util.ArrayList<>();
        for (BlockPos position : BlockPos.getAllInBox(new BlockPos(below.minX, below.minY, below.minZ),
                new BlockPos(below.maxX, feet.minY, below.maxZ))) {
            net.minecraft.block.state.IBlockState state = world.getBlockState(position);
            state.addCollisionBoxToList(world, position, below, collisions, this, false);
        }
        for (AxisAlignedBB box : collisions) if (box.maxY <= feet.minY + 1.0E-6 && box.maxY >= feet.minY - 3) return true;
        return false;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isServerWorld() && isBeamAction()) {
            motionX = motionY = motionZ = 0;
            return;
        }
        if (isServerWorld() && !isRiding() && navigator == swimNavigator && isInWater()) {
            moveRelative(strafe, vertical, forward, 0.025F);
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.9;
            motionY *= 0.9;
            motionZ *= 0.9;
        } else super.travel(strafe, vertical, forward);
    }

    private final class SwimMovement extends EntityMoveHelper {
        private SwimMovement() {
            super(Drowner.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (!shouldSwim()) {
                if (!isRiding() && isInWater() && !onGround) motionY -= 0.008;
                super.onUpdateMoveHelper();
                return;
            }
            EntityLivingBase target = getAttackTarget();
            if (target.posY > Drowner.this.posY) motionY += 0.002;
            if (action != Action.MOVE_TO || navigator.noPath()) {
                setAIMoveSpeed(0);
                return;
            }
            double x = posX - Drowner.this.posX;
            double y = posY - Drowner.this.posY;
            double z = posZ - Drowner.this.posZ;
            double distance = Math.sqrt(x * x + y * y + z * z);
            if (distance < 1.0E-6) {
                setAIMoveSpeed(0);
                return;
            }
            if (x * x + z * z > 1.0E-6) {
                float yaw = (float) (MathHelper.atan2(z, x) * 180 / Math.PI) - 90;
                rotationYaw = limitAngle(rotationYaw, yaw, 90);
                renderYawOffset = rotationYaw;
            }
            float desired = (float) (speed * getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue()
                    * 2.5);
            setAIMoveSpeed(getAIMoveSpeed() + (desired - getAIMoveSpeed()) * 0.125F);
            motionY += getAIMoveSpeed() * y / distance * 0.1;
            motionX += getAIMoveSpeed() * x * 0.005;
            motionZ += getAIMoveSpeed() * z * 0.005;
        }
    }

    private void look(EntityLivingBase target, boolean aim) {
        getLookHelper().setLookPosition(target.posX, target.posY + (aim ? target.height * 0.75 : 0), target.posZ, 360, 360);
    }

    private void select(EntityLivingBase target, double distance) {
        boolean half = isHalfHealthCombat();
        double meleeRange = 3.0;
        if (!isRiding() && distance <= meleeRange * meleeRange) {
            if (rangeTick >= 50) {
                if (rand.nextInt(3) == 0) chooseBeamAction(target);
                rangeTick = 0;
            }
            if (action == 0) {
                boolean spin = half && meleeCount >= 3;
                begin(spin ? 5 : 1, spin ? "attack5" : "attack1");
                if (spin) meleeCount = 0;
                else meleeCount = Math.min(meleeCount + 1, 3);
                look(target, false);
            }
        } else if (rangeTick >= (isRiding() ? 70 : 50)) {
            if (half && !isRiding() && dashTick >= 120
                    && distance > meleeRange * meleeRange && distance < Math.pow(7.0, 2)
                    && posY - target.posY > -0.7) {
                begin(6, "attack6");
                rangeTick = dashTick = 0;
                look(target, false);
            } else if (!isRiding()) {
                if (chooseBeamAction(target)) rangeTick = 0;
                else begin(half ? 4 : 2, half ? "attack4" : "attack2");
            } else if (rangeTick == 80 && rand.nextDouble() < 0.5) {
                begin(3, "attack3");
                rangeTick = 0;
            } else {
                begin(half ? 4 : 2, half ? "attack4" : "attack2");
                int repeat = (int) (half ? 5 : 3);
                if (rangeTick - 80 < repeat - 1) rangeTick++;
                else rangeTick = 0;
            }
            look(target, false);
        }
    }

    private boolean isBeamAction() {
        return action >= 7 && action <= 10;
    }

    public boolean hasActiveBeam() {
        return isBeamAction();
    }

    private boolean chooseBeamAction(EntityLivingBase target) {
        boolean targetInBeamRange = target.getDistanceSq(this) <= BEAM_MAX_RANGE * BEAM_MAX_RANGE;
        int[] actions = {2, 3, 4, 7, 8, 9, 10};
        int[] weights = {
                Math.max(0, (int) n("normalShotWeight", 4)),
                Math.max(0, (int) n("pillarWeight", 4)),
                Math.max(0, (int) n("redShotWeight", 4)),
                targetInBeamRange ? Math.max(0, (int) n("horizontalSweepWeight", 2)) : 0,
                targetInBeamRange ? Math.max(0, (int) n("verticalSweepWeight", 2)) : 0,
                targetInBeamRange ? Math.max(0, (int) n("trackingBeamWeight", 1)) : 0,
                targetInBeamRange ? Math.max(0, (int) n("rotatingBeamWeight", 1)) : 0
        };
        int total = 0;
        for (int i = 0; i < actions.length; i++) {
            if (weights[i] > 0) total += weights[i];
        }
        if (total <= 0) return false;
        int choice = rand.nextInt(total);
        for (int i = 0; i < actions.length; i++) {
            int actionId = actions[i];
            int weight = weights[i];
            if (weight <= 0) continue;
            choice -= weight;
            if (choice < 0) {
                if (actionId >= 7) beginBeam(actionId, target);
                else begin(actionId, actionId == 3 ? "attack3" : "attack" + actionId);
                return true;
            }
        }
        return false;
    }

    private Vec3d beamForward(EntityLivingBase target) {
        Vec3d direction = new Vec3d(target.posX - posX, 0, target.posZ - posZ);
        if (direction.lengthSquared() < 1.0E-6) {
            Vec3d look = getLookVec();
            direction = new Vec3d(look.x, 0, look.z);
        }
        return direction.normalize();
    }

    private List<Vec3d> beamFormation(int actionId, EntityLivingBase target) {
        List<Vec3d> points = new ArrayList<>();
        Vec3d forward = beamForward(target);
        Vec3d right = new Vec3d(-forward.z, 0, forward.x);
        Vec3d center = new Vec3d(posX, posY + height * 0.5, posZ);
        if (actionId == 7) points.add(center.add(forward.scale(2)));
        else if (actionId == 8) {
            Vec3d formation = new Vec3d(posX, posY + height + 2, posZ);
            for (int i = -1; i <= 1; i++) points.add(formation.add(right.scale(i * 3)));
        } else if (actionId == 9) {
            Vec3d formation = center.add(new Vec3d(0, 0.75, 0));
            for (int side : new int[]{-1, 1}) for (int vertical : new int[]{-1, 1})
                points.add(formation.add(right.scale(side * height * 0.5))
                        .add(new Vec3d(0, vertical * height * 0.5, 0)));
        } else {
            for (int i = 0; i < 4; i++) {
                double angle = Math.PI * 0.5 * i;
                points.add(center.add(new Vec3d(Math.cos(angle), 0, Math.sin(angle))));
            }
        }
        return points;
    }

    private List<Vec3d> findBeamFormation(int actionId, EntityLivingBase target) {
        List<Vec3d> requested = beamFormation(actionId, target);
        for (int radius = 0; radius <= 6; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != radius) continue;
                        List<Vec3d> candidate = new ArrayList<>();
                        for (Vec3d point : requested) candidate.add(point.add(new Vec3d(dx, dy, dz)));
                        if (canPlaceBeamFish(candidate, target, actionId)) return candidate;
                    }
                }
            }
        }
        return requested;
    }

    private boolean canPlaceBeamFish(List<Vec3d> positions, EntityLivingBase target, int actionId) {
        if (actionId != 10) {
            Vec3d forward = beamForward(target);
            for (Vec3d position : positions) {
                Vec3d start = position.subtract(forward.scale(3));
                for (int step = 0; step <= 6; step++) {
                    Vec3d sample = start.add(position.subtract(start).scale(step / 6.0));
                    if (!safeBeamFishPoint(sample)) return false;
                }
            }
        }
        if (actionId == 10) {
            Vec3d center = new Vec3d(0, 0, 0);
            for (Vec3d position : positions) center = center.add(position);
            center = center.scale(1.0 / positions.size());
            for (int step = 0; step < 16; step++) {
                double angle = Math.PI * 2 * step / 16.0;
                if (!safeBeamFishPoint(center.add(new Vec3d(Math.cos(angle), 0, Math.sin(angle))))) return false;
            }
        }
        return true;
    }

    private boolean safeBeamFishPoint(Vec3d point) {
        BlockPos origin = new BlockPos(point);
        if (point.y < 0 || point.y + 1.2 >= world.getActualHeight() || !world.isBlockLoaded(origin, false)
                || !world.getWorldBorder().contains(origin)) return false;
        AxisAlignedBB box = new AxisAlignedBB(point.x - 0.7, point.y - 0.55, point.z - 0.7,
                point.x + 0.7, point.y + 0.65, point.z + 0.7);
        List<AxisAlignedBB> collisions = new ArrayList<>();
        BlockPos min = new BlockPos(MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ));
        BlockPos max = new BlockPos(MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ));
        for (BlockPos position : BlockPos.getAllInBox(min, max)) {
            if (!world.isBlockLoaded(position, false)) return false;
            world.getBlockState(position).addCollisionBoxToList(world, position, box, collisions, this, false);
            if (!collisions.isEmpty()) return false;
        }
        return true;
    }

    private void beginBeam(int next, EntityLivingBase target) {
        begin(next, "attack" + next);
        beamLockX = posX;
        beamLockY = posY;
        beamLockZ = posZ;
        beamTarget = target;
        beamTargetX = target.posX;
        beamTargetY = target.posY + target.height * 0.5;
        beamTargetZ = target.posZ;
        beamDirection = rand.nextBoolean() ? 1 : -1;
        beamTargetHistory.clear();
        beamSegments.clear();
        beamFishTargets.clear();
        beamPathStarts.clear();
        beamPathEnds.clear();
        clearBeamFish();
        Vec3d forward = beamForward(target);
        Vec3d right = new Vec3d(-forward.z, 0, forward.x);
        Vec3d center = new Vec3d(posX, posY + height * 0.5, posZ);
        List<Vec3d> fishPositions = findBeamFormation(next, target);
        if (next == 7) {
            Vec3d fish = fishPositions.get(0);
            beamFishTargets.add(fish);
            Vec3d lock = new Vec3d(beamTargetX, beamTargetY, beamTargetZ);
            beamPathStarts.add(lock.subtract(right.scale(8)));
            beamPathEnds.add(lock.add(right.scale(8)));
            beamDurationTicks = 30 + (int) Math.ceil(16.0 / 16.0 * 20.0);
        } else if (next == 8) {
            for (int i = -1; i <= 1; i++) {
                Vec3d fish = fishPositions.get(i + 1);
                beamFishTargets.add(fish);
                Vec3d start = fish.add(forward.scale(2)).add(new Vec3d(0, -4, 0));
                Vec3d lock = new Vec3d(beamTargetX, beamTargetY, beamTargetZ);
                Vec3d path = lock.subtract(start);
                double length = path.lengthVector();
                Vec3d end = length < 1.0E-6 ? lock : lock.add(path.scale(8.0 / length));
                beamPathStarts.add(start);
                beamPathEnds.add(end);
            }
            double longest = 0;
            for (int i = 0; i < beamPathStarts.size(); i++) longest = Math.max(longest, beamPathStarts.get(i).distanceTo(beamPathEnds.get(i)));
            beamDurationTicks = 30 + Math.max(1, (int) Math.ceil(longest / 10.0 * 20.0));
        } else if (next == 9) {
            beamFishTargets.addAll(fishPositions);
            Vec3d converge = center.add(forward.scale(2));
            for (Vec3d fish : beamFishTargets) {
                beamPathStarts.add(fish);
                beamPathEnds.add(converge);
            }
            beamPathStarts.add(converge);
            beamPathEnds.add(new Vec3d(beamTargetX, beamTargetY, beamTargetZ));
            beamDurationTicks = 370;
        } else {
            beamFishTargets.addAll(fishPositions);
            beamRotatingCenter = new Vec3d(0, 0, 0);
            for (Vec3d position : fishPositions) beamRotatingCenter = beamRotatingCenter.add(position);
            beamRotatingCenter = beamRotatingCenter.scale(1.0 / fishPositions.size());
            beamDurationTicks = 270;
        }
        for (int i = 0; i < beamFishTargets.size(); i++) addBeamFish(beamFishTargets.get(i), forward);
    }

    private void addBeamFish(Vec3d target, Vec3d forward) {
        TechnicalPiranha fish = new TechnicalPiranha(world);
        Vec3d start = action == 10 ? target : target.subtract(forward.scale(3));
        fish.setPosition(start.x, start.y, start.z);
        fish.setCaster(this);
        fish.faceDirection(action == 10 ? new Vec3d(target.x - beamLockX, 0, target.z - beamLockZ) : forward);
        fish.prevRotationYaw = fish.rotationYaw;
        fish.prevRotationPitch = fish.rotationPitch;
        fish.setOpacity(0);
        fish.setGlow(false);
        world.spawnEntity(fish);
        beamFish.add(fish);
    }

    private void clearBeamFish() {
        for (TechnicalPiranha fish : beamFish) if (!fish.isDead) fish.setDead();
        beamFish.clear();
    }

    public List<Vec3d[]> getBeamSegments() { return beamSegments; }
    public String getBeamData() { return dataManager.get(BEAM_DATA); }

    private void updateBeamAction() {
        if (!isEntityAlive()) {
            clearBeamFish();
            dataManager.set(BEAM_DATA, "");
            finish();
            return;
        }
        if (beamFish.isEmpty()) {
            dataManager.set(BEAM_DATA, "");
            finish();
            return;
        }
        beamSegments.clear();
        int age = actionTick;
        setPosition(beamLockX, beamLockY, beamLockZ);
        motionX = motionY = motionZ = 0;
        boolean shortTiming = action == 7 || action == 8;
        int fadeInTicks = shortTiming ? 10 : 20;
        int mouthStart = shortTiming ? fadeInTicks : 40;
        int activeStart = shortTiming ? mouthStart + 10 : 50;
        int fadeOutTicks = shortTiming ? 10 : 20;
        float opacity = age < fadeInTicks ? (age + 1) / (float) fadeInTicks : age >= beamDurationTicks - fadeOutTicks ? MathHelper.clamp((beamDurationTicks - age) / (float) fadeOutTicks, 0, 1) : 1.0F;
        boolean active = age >= activeStart && age < beamDurationTicks - fadeOutTicks;
        if (action != 10 && age > 0 && age <= 20) {
            Vec3d forward = new Vec3d(beamTargetX - beamLockX, 0, beamTargetZ - beamLockZ);
            if (forward.lengthSquared() < 1.0E-6) forward = getLookVec();
            forward = new Vec3d(forward.x, 0, forward.z).normalize();
            for (int i = 0; i < beamFish.size(); i++) {
                Vec3d destination = beamFishTargets.get(i);
                Vec3d start = destination.subtract(forward.scale(3));
                double progress = age / 20.0;
                Vec3d point = start.add(destination.subtract(start).scale(progress));
                beamFish.get(i).setPosition(point.x, point.y, point.z);
            }
        }
        for (TechnicalPiranha fish : beamFish) {
            fish.setOpacity(opacity);
            fish.setGlow(age >= fadeInTicks && age < beamDurationTicks - fadeOutTicks);
            fish.setMouthOpen(age < mouthStart ? 0 : age < activeStart ? (age - mouthStart) / (float) (activeStart - mouthStart) : age < beamDurationTicks - fadeOutTicks ? 1 : MathHelper.clamp((beamDurationTicks - age) / (float) fadeOutTicks, 0, 1));
        }
        EntityLivingBase target = beamTarget != null && beamTarget.isEntityAlive() ? beamTarget : null;
        if (target != null) {
            beamTargetX = target.posX;
            beamTargetY = target.posY + target.height * 0.5;
            beamTargetZ = target.posZ;
        }
        beamTargetHistory.addLast(new Vec3d(beamTargetX, beamTargetY, beamTargetZ));
        while (beamTargetHistory.size() > 31) beamTargetHistory.removeFirst();
        if (age > 0) {
            if (action == 7) updateHorizontalBeam(active);
            else if (action == 8) updateVerticalBeam(active);
            else if (action == 9) updateTrackingBeam(active);
            else updateRotatingBeam(active);
        }
        syncBeamData();
        actionTick++;
        if (actionTick >= beamDurationTicks) {
            clearBeamFish();
            dataManager.set(BEAM_DATA, "");
            finish();
        }
    }

    private void syncBeamData() {
        if (beamSegments.isEmpty()) { dataManager.set(BEAM_DATA, ""); return; }
        StringBuilder value = new StringBuilder().append(action);
        for (Vec3d[] segment : beamSegments) {
            value.append(";");
            for (int i = 0; i < 2; i++) {
                Vec3d point = segment[i];
                value.append(Float.toString((float) point.x)).append(",").append(Float.toString((float) point.y)).append(",").append(Float.toString((float) point.z));
                if (i == 0) value.append(",");
            }
        }
        dataManager.set(BEAM_DATA, value.toString());
    }

    private Vec3d beamSource(TechnicalPiranha fish) {
        return fish.getMouthPosition();
    }

    private void updateHorizontalBeam(boolean active) {
        if (beamFish.isEmpty() || beamPathStarts.isEmpty()) return;
        double progress = MathHelper.clamp((actionTick - 20) / 20.0 * 16.0, 0, 16);
        if (beamDirection < 0) progress = 16 - progress;
        Vec3d end = beamPathStarts.get(0).add(beamPathEnds.get(0).subtract(beamPathStarts.get(0)).scale(progress / 16.0));
        TechnicalPiranha fish = beamFish.get(0);
        faceBeamFish(fish, end);
        if (!active) return;
        Vec3d source = beamSource(fish);
        beamDamage(source, end.subtract(source).normalize(), n("horizontalBeamDamage", 5), 0, source.distanceTo(end));
    }

    private void updateVerticalBeam(boolean active) {
        if (beamFish.isEmpty()) return;
        for (int i = 0; i < beamFish.size(); i++) {
            Vec3d start = beamPathStarts.get(i);
            Vec3d end = beamPathEnds.get(i);
            double length = start.distanceTo(end);
            double travel = Math.min(length, Math.max(0, (actionTick - 20) / 20.0 * 10.0));
            Vec3d point = start.add(end.subtract(start).scale(length < 1.0E-6 ? 0 : travel / length));
            TechnicalPiranha fish = beamFish.get(i);
            faceBeamFish(fish, point);
            if (!active) continue;
            Vec3d source = beamSource(fish);
            beamDamage(source, point.subtract(source).normalize(), n("verticalBeamDamage", 5), 1, source.distanceTo(point));
        }
    }

    private void updateTrackingBeam(boolean active) {
        if (beamFish.size() < 4 || beamPathStarts.size() < 5) return;
        Vec3d converge = beamPathEnds.get(0);
        for (TechnicalPiranha fish : beamFish) faceBeamFish(fish, converge);
        if (!active) return;
        for (int i = 0; i < 4; i++) beamSegments.add(new Vec3d[]{beamSource(beamFish.get(i)), converge});
        Vec3d aim = beamTargetHistory.size() > 30 ? beamTargetHistory.peekFirst() : new Vec3d(beamTargetX, beamTargetY, beamTargetZ);
        double sourceLength = 0;
        for (int i = 0; i < 4; i++) sourceLength = Math.max(sourceLength, beamSource(beamFish.get(i)).distanceTo(converge));
        double maxDistance = Math.max(0, BEAM_MAX_RANGE - sourceLength);
        beamDamage(converge, aim.subtract(converge).normalize(), n("trackingBeamDamage", 3), 2, maxDistance);
    }

    private void faceBeamFish(TechnicalPiranha fish, Vec3d target) {
        float previousYaw = fish.rotationYaw;
        float previousPitch = fish.rotationPitch;
        for (int i = 0; i < 2; i++) fish.faceDirection(target.subtract(fish.getMouthPosition()));
        fish.prevRotationYaw = previousYaw;
        fish.prevRotationPitch = previousPitch;
    }

    private void faceBeamFishDirection(TechnicalPiranha fish, Vec3d direction) {
        float previousYaw = fish.rotationYaw;
        float previousPitch = fish.rotationPitch;
        fish.faceDirection(direction);
        fish.prevRotationYaw = previousYaw;
        fish.prevRotationPitch = previousPitch;
    }

    private void updateRotatingBeam(boolean active) {
        Vec3d center = beamRotatingCenter;
        double phase = Math.max(0, actionTick - 50) * Math.toRadians(49.4) / 20.0 * beamDirection;
        for (int i = 0; i < beamFish.size(); i++) {
            double angle = phase + Math.PI * 0.5 * i;
            Vec3d radial = new Vec3d(Math.cos(angle), 0, Math.sin(angle));
            Vec3d fishPoint = center.add(radial);
            TechnicalPiranha fish = beamFish.get(i);
            if (actionTick >= 20) fish.setPosition(fishPoint.x, fishPoint.y, fishPoint.z);
            faceBeamFishDirection(fish, radial);
            if (active) beamDamage(beamSource(fish), radial, n("rotatingBeamDamage", 4), 3, BEAM_MAX_RANGE);
        }
    }

    private void beamDamage(Vec3d source, Vec3d direction, double amount, int knockback, double maxDistance) {
        if (direction.lengthSquared() < 1.0E-6 || maxDistance <= 0) return;
        Vec3d end = source.add(direction.normalize().scale(Math.min(BEAM_MAX_RANGE, maxDistance)));
        RayTraceResult block = world.rayTraceBlocks(source, end, false, true, false);
        if (block != null) end = block.hitVec;
        EntityLivingBase hit = null;
        double closest = source.squareDistanceTo(end);
        double radius = action == 9 ? TRACKING_BEAM_HIT_RADIUS : BEAM_HIT_RADIUS;
        AxisAlignedBB box = new AxisAlignedBB(source, end).grow(radius);
        for (EntityLivingBase candidate : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (candidate == this || isBeamAlly(candidate)) continue;
            RayTraceResult intercept = candidate.getEntityBoundingBox().grow(radius).calculateIntercept(source, end);
            if (intercept != null && source.squareDistanceTo(intercept.hitVec) < closest) {
                closest = source.squareDistanceTo(intercept.hitVec);
                hit = candidate;
                end = intercept.hitVec;
            }
        }
        beamSegments.add(new Vec3d[]{source, end});
        if (hit == null) return;
        if (hit.isActiveItemStackBlocking() && hit.getLookVec().dotProduct(source.subtract(hit.getPositionVector()).normalize()) > 0.15) {
            world.setEntityState(hit, (byte) 29);
            return;
        }
        boolean damaged = hit.attackEntityFrom(new EntityDamageSourceIndirect("drowner_beam", this, this).setProjectile(), (float) amount);
        if (!damaged) return;
        int frozen = Math.min(140, hit.getEntityData().getInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG) + 47);
        hit.getEntityData().setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, frozen);
        Visuals.freeze(hit, frozen);
        if (knockback == 1) hit.knockBack(this, 0.28F, posX - hit.posX, posZ - hit.posZ);
        else if (knockback == 2) hit.knockBack(this, 0.4F, posX - hit.posX, posZ - hit.posZ);
        else if (knockback == 3) {
            double tangent = Math.atan2(source.z - beamLockZ, source.x - beamLockX) + Math.PI * 0.5 * beamDirection;
            hit.knockBack(this, 0.2F, -Math.cos(tangent), -Math.sin(tangent));
        }
    }

    private boolean isBeamAlly(EntityLivingBase candidate) {
        if (candidate == this || isBossPartner(candidate) || isOnSameTeam(candidate)) return true;
        if (candidate instanceof net.minecraft.entity.IEntityOwnable) {
            Entity owner = ((net.minecraft.entity.IEntityOwnable) candidate).getOwner();
            return owner == this || owner != null && (isOnSameTeam(owner) || owner.isOnSameTeam(this));
        }
        return false;
    }

    private float damage(Entity target, String key, double base) {
        return (float) (n(key, base) + Combat.depthBonus(target) * n("depthStriderDamagePerLevel", 1));
    }

    private void attack(EntityLivingBase target) {
        if (isCreativePlayer(target)) {
            setAttackTarget(null);
            return;
        }
        if (action == 1 && actionTick == 7) {
            playSound(ModSounds.get("item.trident.hit"), 1, 1);
            look(target, false);
            if (getDistanceSq(target) <= Math.pow(3.5, 2)) {
                meleeDamage(target, damage(target, "meleeDamage", 12));
            }
        } else if ((action == 2 && actionTick == 11)
                || (action == 4 && actionTick == 8)) {
            look(target, true);
            playSound(ModSounds.get("item.trident.throw"), 1, 1);
            PhantomShot shot = action == 4 ? new PhantomShot.Red(world, this) : new PhantomShot(world, this);
            shot.setPosition(posX, posY + getEyeHeight() - 0.1, posZ);
            Vec3d direction = new Vec3d(target.posX - posX, target.posY + target.height * 0.75 - (posY + getEyeHeight()), target.posZ - posZ).normalize();
            shot.shoot(direction.x, direction.y, direction.z, 1.4F, 0);
            shot.setDamage(damage(target, action == 4 ? "redShotDamage" : "normalShotDamage", action == 4 ? 7 : 8));
            shot.setKnockbackStrength(0);
            world.spawnEntity(shot);
        } else if (action == 3) {
            int start = 10;
            if (actionTick == start) {
                Combat.pillar(this, target.posX, target.posY, target.posZ, false);
                playSound(ModSounds.get("entity.evoker.prepare_summon"), 1, 1);
            }
            if (actionTick >= start && actionTick <= 20) {
                int steps = 30;
                double spacing = 0.1;
                for (int i = 0; i < 3; i++) {
                    double x = target.posX + (rand.nextInt(steps * 2 + 1) - steps) * spacing;
                    double z = target.posZ + (rand.nextInt(steps * 2 + 1) - steps) * spacing;
                    Combat.pillar(this, x, target.posY, z, false);
                }
            }
        } else if (action == 5 && actionTick >= 10
                && actionTick % 10 == 0
                && actionTick / 10 <= 4) {
            playSound(ModSounds.get("entity.player.attack.sweep"), 1, 1);
            for (Entity candidate : Combat.nearby(this, posX, posY, posZ, 2.5)) {
                if (!validAreaTarget(candidate) || candidate instanceof PhantomPillar
                        || getDistanceSq(candidate) > Math.pow(3.5, 2)) continue;
                if (meleeDamage(candidate, damage(candidate, "spinDamage", 8)) && isEntityAlive()) heal(1.0F);
            }
        } else if (action == 6) {
            int first = 18;
            int interval = 15;
            int dash = actionTick < first ? -1 : (actionTick - first) / interval;
            int offset = actionTick < first ? -1 : (actionTick - first) % interval;
            if (dash >= 0 && dash < 3) {
                if (offset == 0) {
                    playSound(ModSounds.get("entity.player.attack.sweep"), 1, 1);
                    Vec3d direction = target.getPositionVector().subtract(getPositionVector()).normalize();
                    double speed = 1.65;
                    motionX = direction.x * speed;
                    motionY = direction.y * speed;
                    motionZ = direction.z * speed;
                    velocityChanged = true;
                    look(target, false);
                } else if (offset <= 5) {
                    for (Entity candidate : Combat.nearby(this, posX, posY, posZ, 1.0)) {
                        if (!validAreaTarget(candidate) || candidate instanceof PhantomPillar && !((PhantomPillar) candidate).enchanted()
                                || getDistanceSq(candidate) > Math.pow(2.0, 2)) continue;
                        meleeDamage(candidate, damage(candidate, "dashDamage", 7));
                    }
                }
            }
        }
    }

    public boolean isHalfHealthCombat() {
        return getHealth() <= getMaxHealth() * 0.5F
                || !isRiding() && Balance.flag("drowner.alwaysHalfHealthCombat", false);
    }

    private boolean meleeDamage(Entity target, float amount) {
        return target.attackEntityFrom(DamageSource.causeMobDamage(this), amount);
    }

    private boolean validAreaTarget(Entity entity) {
        return !isBossPartner(entity) && (entity instanceof EntityLiving || entity instanceof EntityPlayer || entity == getAttackTarget());
    }

    @Override
    public void onDeath(DamageSource cause) {
        clearBeamFish();
        dataManager.set(BEAM_DATA, "");
        if (isBeamAction()) finish();
        super.onDeath(cause);
    }

    @Override
    public void knockBack(Entity attacker, float strength, double x, double z) {
        if (!isBeamAction()) super.knockBack(attacker, strength, x, z);
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (!isBeamAction()) super.addVelocity(x, y, z);
    }

    @Override
    public void setDead() {
        clearBeamFish();
        dataManager.set(BEAM_DATA, "");
        if (isBeamAction()) finish();
        super.setDead();
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setInteger("RangeTick", rangeTick);
        data.setInteger("DashTick", dashTick);
        data.setInteger("MeleeCount", meleeCount);
        data.setInteger("Regeneration", regeneration);
        data.setDouble("BeamTargetX", beamTargetX);
        data.setDouble("BeamTargetY", beamTargetY);
        data.setDouble("BeamTargetZ", beamTargetZ);
        data.setInteger("BeamDirection", beamDirection);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        rangeTick = data.getInteger("RangeTick");
        dashTick = data.getInteger("DashTick");
        meleeCount = data.getInteger("MeleeCount");
        regeneration = data.getInteger("Regeneration");
        beamTargetX = data.getDouble("BeamTargetX");
        beamTargetY = data.getDouble("BeamTargetY");
        beamTargetZ = data.getDouble("BeamTargetZ");
        beamDirection = data.getInteger("BeamDirection");
        if (data.getInteger("Action") >= 7) {
            action = 0;
            actionTick = 0;
            animate("");
            dataManager.set(BEAM_DATA, "");
        }
    }
}
