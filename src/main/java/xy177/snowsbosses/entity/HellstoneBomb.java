package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import xy177.snowsbosses.content.HellstoneEffects;
import xy177.snowsbosses.network.Visuals;

public final class HellstoneBomb extends EntityThrowable {
    public double damage;
    public HellstoneBomb(World world) { super(world); setSize(0.5F, 0.5F); isImmuneToFire = true; }
    public HellstoneBomb(World world, EntityLivingBase owner) { super(world, owner); setSize(0.5F, 0.5F); isImmuneToFire = true; }
    @Override
    protected float getGravityVelocity() { return 0.05F; }
    @Override
    protected void onImpact(RayTraceResult hit) {
        if (world.isRemote || hit.entityHit != null && (hit.entityHit == getThrower() || HellstoneEffects.allies(getThrower(), hit.entityHit))) return;
        double range = (hit.entityHit == null ? 2 : 0.9);
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(4))) {
            if (target instanceof HellstoneGolem || HellstoneEffects.allies(getThrower(), target)) continue;
            if (Math.abs(target.posY + target.height / 2 - posY) >= range + target.height / 2
                    || Math.hypot(target.posX - posX, target.posZ - posZ) >= range + target.width / 2) continue;
            HellstoneEffects.burn(target, 100);
            target.attackEntityFrom(new EntityDamageSourceIndirect("explosion", this, getThrower()).setExplosion(), (float) damage);
        }
        Visuals.particles(world, "hellstone_explode", posX, posY, posZ, 1, 0, 0);
        Visuals.particles(world, "hellstone_fire", posX, posY, posZ, 12, 0, 0.04);
        playSound(SoundEvents.ENTITY_LIGHTNING_IMPACT, 1, 1); setDead();
    }
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!world.isRemote && ticksExisted > 1200) setDead();
    }
    @Override public void writeEntityToNBT(NBTTagCompound data) { super.writeEntityToNBT(data); data.setDouble("Damage", damage); }
    @Override public void readEntityFromNBT(NBTTagCompound data) { super.readEntityFromNBT(data); damage = data.getDouble("Damage"); }
}
