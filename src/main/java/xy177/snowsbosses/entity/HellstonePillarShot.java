package xy177.snowsbosses.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.content.HellstoneEffects;

public final class HellstonePillarShot extends Entity {
    public static final float SIZE = 1.0F / 3.0F;
    private EntityLivingBase owner;
    private UUID ownerId;
    private double damage = 7;
    private double accelerationX;
    private double accelerationZ;
    private int burnTicks = 100;
    private int lifetime = 40;
    private int age;
    private final List<Vec3d> trail = new ArrayList<>();

    public HellstonePillarShot(World world) {
        super(world);
        setSize(SIZE, SIZE);
        noClip = true;
        setNoGravity(true);
        isImmuneToFire = true;
    }

    public HellstonePillarShot(World world, EntityLivingBase owner, double x, double y, double z,
            double direction, double acceleration, int lifetime, double damage, int burnTicks) {
        this(world);
        this.owner = owner;
        ownerId = owner.getUniqueID();
        accelerationX = Math.cos(direction) * acceleration;
        accelerationZ = Math.sin(direction) * acceleration;
        this.lifetime = lifetime;
        this.damage = damage;
        this.burnTicks = burnTicks;
        setPosition(x, y, z);
    }

    public List<Vec3d> trail() { return trail; }

    public boolean summonedBy(EntityLivingBase entity) {
        return entity != null && ownerId != null && ownerId.equals(entity.getUniqueID());
    }

    @Override
    protected void entityInit() { }
    @Override
    public boolean canBeCollidedWith() { return false; }
    @Override
    public boolean attackEntityFrom(net.minecraft.util.DamageSource source, float amount) { return false; }
    @Override
    public boolean isEntityInvulnerable(net.minecraft.util.DamageSource source) { return true; }
    @Override
    public boolean canBeAttackedWithItem() { return false; }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            Vec3d point = new Vec3d(posX, posY + height * 0.5, posZ);
            if (trail.isEmpty() || trail.get(0).squareDistanceTo(point) > 0.000001) {
                trail.add(0, point);
                if (trail.size() > 6) trail.remove(trail.size() - 1);
            }
            return;
        }
        age++;
        if (owner == null && ownerId != null && world instanceof WorldServer) {
            Entity found = ((WorldServer) world).getEntityFromUuid(ownerId);
            if (found instanceof EntityLivingBase) owner = (EntityLivingBase) found;
        }
        if (owner == null || !owner.isEntityAlive() || age > lifetime) {
            setDead();
            return;
        }
        Vec3d start = new Vec3d(posX, posY + height * 0.5, posZ);
        Vec3d end = start.addVector(motionX, motionY, motionZ);
        double radius = SIZE * 0.5;
        AxisAlignedBB sweep = getEntityBoundingBox().expand(motionX, motionY, motionZ).grow(0.00001);
        Vec3d impact = null;
        EntityLivingBase victim = null;
        double nearest = Double.MAX_VALUE;
        for (AxisAlignedBB obstacle : world.getCollisionBoxes(this, sweep)) {
            AxisAlignedBB expanded = obstacle.grow(radius);
            RayTraceResult hit = expanded.calculateIntercept(start, end);
            Vec3d point = expanded.contains(start) ? start : hit == null ? null : hit.hitVec;
            if (point != null && start.squareDistanceTo(point) < nearest) {
                impact = point;
                nearest = start.squareDistanceTo(point);
            }
        }
        for (EntityLivingBase candidate : world.getEntitiesWithinAABB(EntityLivingBase.class, sweep)) {
            if (candidate == owner || !candidate.isEntityAlive() || HellstoneEffects.allies(owner, candidate)
                    || candidate instanceof EntityPlayer && (((EntityPlayer) candidate).isSpectator()
                    || ((EntityPlayer) candidate).capabilities.isCreativeMode)) continue;
            AxisAlignedBB box = candidate.getEntityBoundingBox().grow(radius);
            RayTraceResult hit = box.calculateIntercept(start, end);
            Vec3d point = box.contains(start) ? start : hit == null ? null : hit.hitVec;
            if (point != null && start.squareDistanceTo(point) < nearest) {
                victim = candidate;
                impact = point;
                nearest = start.squareDistanceTo(point);
            }
        }
        if (impact != null) {
            setPosition(impact.x, impact.y - height * 0.5, impact.z);
            if (victim != null) {
                victim.attackEntityFrom(new EntityDamageSourceIndirect("hellstone", this, owner).setProjectile(), (float) damage);
                HellstoneEffects.burn(victim, burnTicks);
            }
            setDead();
            return;
        }
        setPosition(end.x, end.y - height * 0.5, end.z);
        motionX = (motionX + accelerationX) * 0.95F;
        motionY *= 0.95F;
        motionZ = (motionZ + accelerationZ) * 0.95F;
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound data) {
        if (ownerId != null) data.setUniqueId("Owner", ownerId);
        data.setDouble("Damage", damage);
        data.setDouble("AccelerationX", accelerationX);
        data.setDouble("AccelerationZ", accelerationZ);
        data.setInteger("BurnTicks", burnTicks);
        data.setInteger("Lifetime", lifetime);
        data.setInteger("Age", age);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound data) {
        ownerId = data.hasUniqueId("Owner") ? data.getUniqueId("Owner") : null;
        damage = data.getDouble("Damage");
        accelerationX = data.getDouble("AccelerationX");
        accelerationZ = data.getDouble("AccelerationZ");
        burnTicks = data.hasKey("BurnTicks") ? Math.max(1, data.getInteger("BurnTicks")) : 100;
        lifetime = Math.max(1, data.getInteger("Lifetime"));
        age = Math.max(0, data.getInteger("Age"));
    }
}
