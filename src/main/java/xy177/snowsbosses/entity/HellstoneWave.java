package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.content.HellstoneEffects;
import xy177.snowsbosses.network.Visuals;

public final class HellstoneWave extends Entity {
    private EntityLivingBase owner;
    private UUID ownerId;
    private double dx, dz, damage;
    private int lifetime, type;

    public HellstoneWave(World world) { super(world); setSize(0.25F, 0.25F); noClip = true; isImmuneToFire = true; }
    public HellstoneWave(World world, EntityLivingBase owner, double x, double y, double z, double direction, double speed, int lifetime, int type, double damage) {
        this(world); this.owner = owner; ownerId = owner.getUniqueID();
        setPosition(x, y, z); dx = Math.cos(direction) * speed; dz = Math.sin(direction) * speed;
        this.lifetime = lifetime; this.type = type; this.damage = damage;
    }
    @Override protected void entityInit() { }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isInRangeToRenderDist(double distance) { return false; }
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        if (--lifetime <= 1) { setDead(); return; }
        if (owner == null && ownerId != null && world instanceof WorldServer) {
            Entity found = ((WorldServer) world).getEntityFromUuid(ownerId);
            if (found instanceof EntityLivingBase) owner = (EntityLivingBase) found;
        }
        double y = posY;
        for (double drop : new double[]{0.8, 0.5, 0.1}) {
            if (!world.getBlockState(new BlockPos(posX, y - drop, posZ)).isFullCube()) { y -= drop; break; }
        }
        setPosition(posX + dx, y, posZ + dz);
        if (type < 3 && world instanceof WorldServer) {
            BlockPos under = new BlockPos(posX, posY - 0.2, posZ);
            ((WorldServer) world).spawnParticle(EnumParticleTypes.BLOCK_DUST, posX, posY + 0.1, posZ, 7, 0.4, 0, 0.4, 0,
                    net.minecraft.block.Block.getStateId(world.getBlockState(under)));
        }
        if (type > 1) Visuals.particles(world, "hellstone_fire", posX, posY, posZ, type == 3 ? 2 : 1, 0.4, 0);
        if (owner == null) return;
        double range = 0.6;
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(4))) {
            if (target == owner || HellstoneEffects.allies(owner, target)) continue;
            if (Math.abs(target.posY + target.height / 2 - posY) >= range + target.height / 2
                    || Math.hypot(target.posX - posX, target.posZ - posZ) >= range + target.width / 2) continue;
            if (type < 3) target.attackEntityFrom(new EntityDamageSourceIndirect("mob", this, owner), (float) damage);
            if (type == 2) HellstoneEffects.burn(target, 100);
            if (type == 3) {
                HellstoneEffects.burn(target, 200);
                target.attackEntityFrom(HellstoneEffects.DAMAGE, HellstoneEffects.burnDamage(target, HellstoneEffects.n("wave.flameHealthFraction", 0.07), false));
            }
        }
    }
    @Override
    protected void writeEntityToNBT(NBTTagCompound data) {
        if (ownerId != null) data.setUniqueId("Owner", ownerId);
        data.setDouble("DX", dx); data.setDouble("DZ", dz); data.setDouble("Damage", damage);
        data.setInteger("Lifetime", lifetime); data.setInteger("Type", type);
    }
    @Override
    protected void readEntityFromNBT(NBTTagCompound data) {
        ownerId = data.hasUniqueId("Owner") ? data.getUniqueId("Owner") : null;
        dx = data.getDouble("DX"); dz = data.getDouble("DZ"); damage = data.getDouble("Damage");
        lifetime = data.getInteger("Lifetime"); type = data.getInteger("Type");
    }
}
