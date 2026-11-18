package xy177.snowsbosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.DamageSource;
import net.minecraft.world.BossInfo;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.content.ModSounds;
import xy177.snowsbosses.network.Visuals;

public final class DeepwaterPiranha extends OceanBoss {
    public DeepwaterPiranha(World world) {
        super(world, BossInfo.Overlay.NOTCHED_6);
        setSize(1.5F, 1.2F);
        experienceValue = 50;
        setPathPriority(PathNodeType.WATER, 0);
        moveHelper = new SwimMovement();
    }

    @Override
    public String species() {
        return "deepwater_piranha";
    }

    @Override
    public String modelName() {
        return "fish";
    }

    @Override
    public String textureName() {
        return "deepwater_piranha";
    }

    @Override
    protected String idleAnimation(boolean moving) {
        return isInWater() ? "swim" : "idle";
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateSwimmer(this, world);
    }

    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, 10, false, false, null));
        targetTasks.addTask(2, new EntityAIHurtByTarget(this, false) {
            @Override
            public boolean shouldExecute() {
                return !isBeingRidden() && super.shouldExecute();
            }
        });
        tasks.addTask(3, new EntityAIWander(this, 1.0, 40) {
            @Override
            protected Vec3d getPosition() {
                return RandomPositionGenerator.findRandomTarget(DeepwaterPiranha.this,
                        10, 7);
            }
        });
    }

    @Override
    public double getMountedYOffset() {
        return super.getMountedYOffset() + -0.6;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public Entity getControllingPassenger() {
        return getPassengers().isEmpty() ? null : getPassengers().get(0);
    }

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || !isEntityAlive()) return;
        if (!isInWater()) addPotionEffect(new PotionEffect(MobEffects.RESISTANCE,
                5, 3, false, false));
        Entity passenger = getControllingPassenger();
        if (passenger instanceof EntityLiving && ((EntityLiving) passenger).getAttackTarget() != null
                && ((EntityLiving) passenger).getAttackTarget().isEntityAlive()) {
            setAttackTarget(((EntityLiving) passenger).getAttackTarget());
        }
        EntityLivingBase target = getAttackTarget();
        if (target != null && target.isEntityAlive()) {
            double stop = (passenger instanceof Drowner ? 10 : 1);
            if (getDistanceSq(target) > stop * stop) getNavigator().tryMoveToEntityLiving(target, 1.2);
            else getNavigator().clearPath();
            getLookHelper().setLookPositionWithEntity(target, 360, 360);
            if (action == 0) {
                if (getDistanceSq(target) <= Math.pow(2.6, 2)) begin(1, "attack");
                else if (!isInWater() && onGround) begin(2, "jump");
            }
            if (action == 1) {
                if (actionTick == 10) playSound(ModSounds.get("entity.evoker_fangs.attack"), 1, 1);
                if (actionTick == 15 && getDistanceSq(target) <= Math.pow(2.8, 2)) {
                    target.attackEntityFrom(DamageSource.causeMobDamage(this), (float) n("biteDamage", 18));
                    restrain(target);
                }
            } else if (action == 2) {
                if (actionTick == 11) {
                    playSound(ModSounds.get("entity.puffer_fish.blow_up"), 1, 1);
                    double spread = 10.0;
                    double dx = rand.nextDouble() * spread * 2 - spread;
                    double dz = rand.nextDouble() * spread * 2 - spread;
                    Vec3d look = new Vec3d(dx, 0, dz).normalize();
                    motionX = look.x * 0.5;
                    motionY = 0.5;
                    motionZ = look.z * 0.5;
                    velocityChanged = true;
                    rotationYaw = (float) (MathHelper.atan2(look.z, look.x) * 180 / Math.PI) - 90;
                } else if (actionTick > 14 && (onGround || isInWater())) {
                    Visuals.particles(world, "piranha_water", posX, posY, posZ,
                            30, 1.5, 0.1);
                    for (Entity entity : Combat.nearby(this, posX, posY, posZ, 2.5)) {
                        if (entity == passenger || isBossPartner(entity)
                                || !(entity instanceof EntityLiving || entity instanceof EntityPlayer)) continue;
                        entity.attackEntityFrom(DamageSource.causeMobDamage(this), (float) n("landingDamage", 12));
                        restrain(entity);
                    }
                    finish();
                }
            }
        }
        if (action != 0) {
            getNavigator().clearPath();
            if (action == 1 && actionTick >= 31
                    || target == null && actionTick >= 30
                    || actionTick >= 80) finish();
            else actionTick++;
        }
    }

    private void restrain(Entity target) {
        target.motionX *= 0.25;
        target.motionY *= 0.05;
        target.motionZ *= 0.25;
        target.velocityChanged = true;
        if (target instanceof EntityLivingBase) {
            int frozen = 80;
            target.getEntityData().setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, frozen);
            Visuals.freeze((EntityLivingBase) target, frozen);
        }
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isServerWorld() && isInWater()) {
            moveRelative(strafe, vertical, forward, (float) (0.02 * 3.0));
            move(MoverType.SELF, motionX, motionY, motionZ);
            motionX *= 0.8;
            motionY = motionY * 0.8 - 0.005;
            motionZ *= 0.8;
        } else super.travel(strafe, vertical, forward);
    }

    private final class SwimMovement extends EntityMoveHelper {
        private SwimMovement() {
            super(DeepwaterPiranha.this);
        }

        @Override
        public void onUpdateMoveHelper() {
            if (isInWater()) motionY += 0.005;
            if (action == Action.MOVE_TO && !getNavigator().noPath()) {
                double dx = posX - DeepwaterPiranha.this.posX;
                double dy = posY - DeepwaterPiranha.this.posY;
                double dz = posZ - DeepwaterPiranha.this.posZ;
                float yaw = (float) (MathHelper.atan2(dz, dx) * 180 / Math.PI) - 90;
                float movement = (float) (speed * getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                rotationYaw = limitAngle(rotationYaw, yaw, 10);
                renderYawOffset = rotationYawHead = rotationYaw;
                if (isInWater()) {
                    setAIMoveSpeed((float) getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                    float pitch = -(float) (MathHelper.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
                    rotationPitch = limitAngle(rotationPitch, MathHelper.clamp(pitch, -85, 85), 5);
                    moveForward = MathHelper.cos(rotationPitch * 0.017453292F) * movement;
                    moveVertical = (float) (movement * dy);
                } else setAIMoveSpeed(movement * 0.05F);
            } else {
                setAIMoveSpeed(0);
                moveVertical = moveForward = 0;
            }
        }
    }
}
