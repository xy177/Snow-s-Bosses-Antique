package xy177.snowsbosses.entity;

import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import xy177.snowsbosses.Balance;

public final class StructureEyeEntity extends EntityEnderEye {
    private static final DataParameter<String> STRUCTURE = EntityDataManager.createKey(StructureEyeEntity.class, DataSerializers.STRING);
    private double targetX;
    private double targetY;
    private double targetZ;
    private int despawnTimer;

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(STRUCTURE, "");
    }

    public StructureEyeEntity(World world) {
        super(world);
        setSize(0.25F, 0.25F);
    }

    public StructureEyeEntity(World world, double x, double y, double z) {
        this(world);
        setPosition(x, y, z);
    }

    public void setStructure(String structure) {
        dataManager.set(STRUCTURE, structure);
    }

    public String structure() {
        return dataManager.get(STRUCTURE);
    }

    public void moveTowards(BlockPos position) {
        double dx = position.getX() - posX;
        double dz = position.getZ() - posZ;
        float horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        if (horizontal > 12.0F) {
            targetX = posX + dx / horizontal * 12.0D;
            targetZ = posZ + dz / horizontal * 12.0D;
            targetY = posY + 8.0D;
        } else {
            targetX = position.getX();
            targetY = position.getY();
            targetZ = position.getZ();
        }
        despawnTimer = 0;
    }

    @Override
    public void onUpdate() {
        lastTickPosX = posX;
        lastTickPosY = posY;
        lastTickPosZ = posZ;
        onEntityUpdate();
        posX += motionX;
        posY += motionY;
        posZ += motionZ;
        float horizontal = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
        rotationYaw = (float) (MathHelper.atan2(motionX, motionZ) * (180.0D / Math.PI));
        rotationPitch = (float) (MathHelper.atan2(motionY, horizontal) * (180.0D / Math.PI));
        while (rotationPitch - prevRotationPitch < -180.0F) prevRotationPitch -= 360.0F;
        while (rotationPitch - prevRotationPitch >= 180.0F) prevRotationPitch += 360.0F;
        while (rotationYaw - prevRotationYaw < -180.0F) prevRotationYaw -= 360.0F;
        while (rotationYaw - prevRotationYaw >= 180.0F) prevRotationYaw += 360.0F;
        rotationPitch = prevRotationPitch + (rotationPitch - prevRotationPitch) * 0.2F;
        rotationYaw = prevRotationYaw + (rotationYaw - prevRotationYaw) * 0.2F;
        if (!world.isRemote) {
            double dx = targetX - posX;
            double dz = targetZ - posZ;
            float distance = MathHelper.sqrt(dx * dx + dz * dz);
            float currentSpeed = MathHelper.sqrt(motionX * motionX + motionZ * motionZ);
            double speed = currentSpeed + (distance - currentSpeed) * 0.0025D;
            if (distance < 1.0F) {
                speed *= 0.8D;
                motionY *= 0.8D;
            }
            double angle = MathHelper.atan2(dz, dx);
            motionX = Math.cos(angle) * speed;
            motionZ = Math.sin(angle) * speed;
            motionY += ((posY < targetY ? 1.0D : -1.0D) - motionY) * 0.015D;
            setPosition(posX, posY, posZ);
            if (++despawnTimer > Balance.ticks("structure_eye.lifetime", 80)) {
                playSound(SoundEvents.ENTITY_ENDEREYE_DEATH, 1.0F, 1.0F);
                world.playEvent(2003, new BlockPos(this), 0);
                setDead();
            }
        }
        if (isInWater()) {
            for (int index = 0; index < 4; index++) world.spawnParticle(EnumParticleTypes.WATER_BUBBLE,
                    posX - motionX * 0.25D, posY - motionY * 0.25D, posZ - motionZ * 0.25D,
                    motionX, motionY, motionZ);
        } else {
            world.spawnParticle(EnumParticleTypes.PORTAL, posX - motionX * 0.25D + rand.nextDouble() * 0.6D - 0.3D,
                    posY - motionY * 0.25D - 0.5D, posZ - motionZ * 0.25D + rand.nextDouble() * 0.6D - 0.3D,
                    motionX, motionY, motionZ);
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        data.setDouble("TargetX", targetX);
        data.setDouble("TargetY", targetY);
        data.setDouble("TargetZ", targetZ);
        data.setInteger("DespawnTimer", despawnTimer);
        data.setString("Structure", structure());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        targetX = data.getDouble("TargetX");
        targetY = data.getDouble("TargetY");
        targetZ = data.getDouble("TargetZ");
        despawnTimer = data.getInteger("DespawnTimer");
        setStructure(data.getString("Structure"));
    }
}
