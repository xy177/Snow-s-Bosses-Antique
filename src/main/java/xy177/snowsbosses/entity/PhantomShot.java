package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.network.Visuals;

public class PhantomShot extends EntityArrow {
    private java.util.UUID casterId;
    private boolean fixed;
    private double fixedX;
    private double fixedY;
    private double fixedZ;
    private int age;

    public PhantomShot(World world) {
        super(world);
        setSilent(true);
        pickupStatus = PickupStatus.DISALLOWED;
    }

    public PhantomShot(World world, EntityLivingBase caster) {
        super(world, caster);
        setSilent(true);
        pickupStatus = PickupStatus.DISALLOWED;
        casterId = caster.getUniqueID();
    }

    protected boolean red() {
        return false;
    }

    @Override
    public void onUpdate() {
        if (!world.isRemote && shootingEntity == null && casterId != null && world instanceof net.minecraft.world.WorldServer) {
            shootingEntity = ((net.minecraft.world.WorldServer) world).getEntityFromUuid(casterId);
        }
        super.onUpdate();
        age++;
        if (!fixed) {
            fixedX = motionX;
            fixedY = motionY;
            fixedZ = motionZ;
            fixed = true;
            setNoGravity(true);
        } else {
            motionX = fixedX;
            motionY = fixedY;
            motionZ = fixedZ;
        }
        if (!world.isRemote && !isDead && (inGround || age >= 52)) expire();
    }

    @Override
    protected void onHit(RayTraceResult hit) {
        if (shootingEntity instanceof OceanBoss && ((OceanBoss) shootingEntity).isBossPartner(hit.entityHit)) return;
        super.onHit(hit);
        if (world.isRemote) return;
        if (red() && hit.entityHit instanceof EntityLivingBase
                && ((EntityLivingBase) hit.entityHit).getHealth() > 0 && shootingEntity instanceof EntityLivingBase) {
            ((EntityLivingBase) shootingEntity).heal((float) 5.0);
        }
        expire();
    }

    @Override
    protected void arrowHit(EntityLivingBase target) {
        super.arrowHit(target);
        target.setArrowCountInEntity(Math.max(0, target.getArrowCountInEntity() - 1));
    }

    private void expire() {
        Visuals.particles(world, red() ? "trident_red" : "trident_p", posX, posY, posZ,
                1, 0, 0);
        setDead();
    }

    @Override
    protected ItemStack getArrowStack() {
        return FFDItems.effectiveStack(FFDItems.TRIDENT);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setBoolean("FixedMotion", fixed);
        data.setDouble("FixedX", fixedX);
        data.setDouble("FixedY", fixedY);
        data.setDouble("FixedZ", fixedZ);
        data.setInteger("Age", age);
        if (casterId != null) data.setUniqueId("Caster", casterId);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        fixed = data.getBoolean("FixedMotion");
        fixedX = data.getDouble("FixedX");
        fixedY = data.getDouble("FixedY");
        fixedZ = data.getDouble("FixedZ");
        age = data.getInteger("Age");
        if (data.hasUniqueId("Caster")) casterId = data.getUniqueId("Caster");
    }

    public static final class Red extends PhantomShot {
        public Red(World world) {
            super(world);
        }

        public Red(World world, EntityLivingBase caster) {
            super(world, caster);
        }

        @Override
        protected boolean red() {
            return true;
        }
    }
}
