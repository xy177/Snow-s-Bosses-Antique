package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public final class TechnicalPiranha extends Entity implements IAnimatable {
    public static final float MODEL_SCALE = 0.35F;
    private static final DataParameter<Float> OPACITY = EntityDataManager.createKey(TechnicalPiranha.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> MOUTH = EntityDataManager.createKey(TechnicalPiranha.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> SWIMMING = EntityDataManager.createKey(TechnicalPiranha.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> CASTER_ID = EntityDataManager.createKey(TechnicalPiranha.class, DataSerializers.VARINT);
    private final AnimationFactory factory = new AnimationFactory(this);
    private UUID casterUuid;
    private int casterEntityId = -1;
    private float previousOpacity;
    private float previousMouth;

    public TechnicalPiranha(World world) {
        super(world);
        setSize(1.5F, 1.2F);
        setNoGravity(true);
    }

    @Override
    protected void entityInit() {
        dataManager.register(OPACITY, 0.0F);
        dataManager.register(MOUTH, 0.0F);
        dataManager.register(SWIMMING, false);
        dataManager.register(CASTER_ID, -1);
    }

    public void setCaster(Drowner caster) {
        casterUuid = caster == null ? null : caster.getUniqueID();
        casterEntityId = caster == null ? -1 : caster.getEntityId();
        dataManager.set(CASTER_ID, casterEntityId);
    }

    public Drowner getCaster() {
        Entity entity = world.getEntityByID(dataManager.get(CASTER_ID));
        if (!(entity instanceof Drowner) || casterUuid != null && !casterUuid.equals(entity.getUniqueID())) {
            entity = world instanceof WorldServer && casterUuid != null ? ((WorldServer) world).getEntityFromUuid(casterUuid) : null;
        }
        return entity instanceof Drowner ? (Drowner) entity : null;
    }

    public void setOpacity(float opacity) {
        dataManager.set(OPACITY, MathHelper.clamp(opacity, 0.0F, 1.0F));
    }

    public float getOpacity() {
        return dataManager.get(OPACITY);
    }

    public float getOpacity(float partialTicks) {
        return previousOpacity + (getOpacity() - previousOpacity) * partialTicks;
    }

    public void setMouthOpen(float amount) {
        dataManager.set(MOUTH, MathHelper.clamp(amount, 0.0F, 1.0F));
    }

    public float getMouthOpen() {
        return dataManager.get(MOUTH);
    }

    public float getMouthOpen(float partialTicks) {
        return previousMouth + (getMouthOpen() - previousMouth) * partialTicks;
    }

    public void setGlow(boolean glow) {
        setGlowing(glow);
    }

    public boolean isGlowLayerEnabled() {
        return isGlowing();
    }

    public void setSwimmingAnimation(boolean swimming) {
        dataManager.set(SWIMMING, swimming);
    }

    public boolean isSwimmingAnimation() {
        return dataManager.get(SWIMMING);
    }

    public Vec3d getMouthPosition() {
        Vec3d forward = getLookVec();
        double yaw = Math.toRadians(rotationYaw);
        double pitch = Math.toRadians(rotationPitch);
        Vec3d up = new Vec3d(-Math.sin(yaw) * Math.sin(pitch), Math.cos(pitch), Math.cos(yaw) * Math.sin(pitch));
        return getPositionVector().add(forward.scale(22.0D / 16.0D * MODEL_SCALE)).add(up.scale(6.5D / 16.0D * MODEL_SCALE));
    }

    public void face(Vec3d target) {
        faceDirection(target.subtract(getPositionVector()));
    }

    public void faceDirection(Vec3d direction) {
        if (direction.lengthSquared() < 1.0E-6) return;
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        rotationYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        rotationPitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
    }

    @Override
    public void onUpdate() {
        previousOpacity = getOpacity();
        previousMouth = getMouthOpen();
        super.onUpdate();
        motionX = motionY = motionZ = 0.0D;
        fallDistance = 0.0F;
        if (!world.isRemote) {
            Drowner caster = getCaster();
            if (caster == null || !caster.isEntityAlive() || !caster.hasActiveBeam()) setDead();
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return true;
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound data) {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound data) {
        casterUuid = data.hasUniqueId("Caster") ? data.getUniqueId("Caster") : null;
        casterEntityId = data.getInteger("CasterEntityId");
        dataManager.set(CASTER_ID, casterEntityId);
        setOpacity(data.getFloat("Opacity"));
        setMouthOpen(data.getFloat("Mouth"));
        setGlow(data.getBoolean("Glow"));
        setSwimmingAnimation(data.getBoolean("Swimming"));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound data) {
        if (casterUuid != null) data.setUniqueId("Caster", casterUuid);
        data.setInteger("CasterEntityId", casterEntityId);
        data.setFloat("Opacity", getOpacity());
        data.setFloat("Mouth", getMouthOpen());
        data.setBoolean("Glow", isGlowLayerEnabled());
        data.setBoolean("Swimming", isSwimmingAnimation());
    }

    private <T extends IAnimatable> PlayState animation(AnimationEvent<T> event) {
        TechnicalPiranha fish = (TechnicalPiranha) event.getAnimatable();
        event.getController().setAnimation(new AnimationBuilder().addAnimation(fish.isSwimmingAnimation() ? "swim" : "idle", true));
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "main", 1, this::animation));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }
}
