package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class WorkerBeeLaser extends Entity {
    private static final DataParameter<Integer> START = EntityDataManager.createKey(WorkerBeeLaser.class, DataSerializers.VARINT);
    private UUID casterId;
    private UUID playerId;
    private boolean raid;
    private boolean burst;
    private double damage;

    public WorkerBeeLaser(World world) { super(world); setSize(0.2F, 0.2F); setNoGravity(true); }
    public WorkerBeeLaser(World world, MechasentWorkerBee caster, EntityLivingBase target, boolean burst) {
        this(world); casterId = caster.getUniqueID(); raid = caster.raidAlly(); this.burst = burst;
        if(caster.isPlayerBound() && caster.playerOwner()!=null)playerId=caster.playerOwner().getUniqueID();
        damage = MechasentWorkerBee.n(burst ? "burstDamage" : "laserDamage", burst ? 3 : 6);
        Vec3d source = caster.getPositionEyes(1).add(caster.getLookVec().scale(0.55));
        setPosition(source.x, source.y, source.z);
        Vec3d velocity = target.getPositionEyes(1).subtract(source).normalize().scale(0.9);
        motionX = velocity.x; motionY = velocity.y; motionZ = velocity.z;
        dataManager.set(START, (int) world.getTotalWorldTime()); orient();
    }
    @Override protected void entityInit() { dataManager.register(START, 0); }
    public int age() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    private Entity caster() { return casterId == null ? null : ((WorldServer) world).getEntityFromUuid(casterId); }
    private void orient() {
        rotationYaw = (float) Math.toDegrees(Math.atan2(-motionX, motionZ));
        rotationPitch = (float) -Math.toDegrees(Math.atan2(motionY, Math.hypot(motionX, motionZ)));
    }
    @Override public void onUpdate() {
        super.onUpdate(); orient();
        if (world.isRemote) { setPosition(posX + motionX, posY + motionY, posZ + motionZ); return; }
        if (age() > 80 || posY < 0 || posY > world.getActualHeight()) { setDead(); return; }
        Vec3d from = getPositionVector(), to = from.addVector(motionX, motionY, motionZ);
        RayTraceResult block = world.rayTraceBlocks(from, to, false, true, false);
        Vec3d end = block == null ? to : block.hitVec;
        EntityLivingBase hit = null; double closest = from.squareDistanceTo(end);
        Entity caster = caster();
        net.minecraft.entity.player.EntityPlayer player=playerId==null?null:world.getPlayerEntityByUUID(playerId);
        if(playerId!=null && (player==null || !player.isEntityAlive())){setDead();return;}
        for (EntityLivingBase candidate : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().expand(motionX, motionY, motionZ).grow(0.5))) {
            if (!Mechasent.attackable(candidate) || candidate == caster || (playerId!=null ? xy177.snowsbosses.content.MechasentEquipment.allies(player,candidate) : Mechasent.protectedTarget(raid, candidate))
                    || caster != null && caster.isOnSameTeam(candidate)) continue;
            RayTraceResult intercept = candidate.getEntityBoundingBox().grow(0.15).calculateIntercept(from, end);
            if (intercept != null && from.squareDistanceTo(intercept.hitVec) <= closest) {
                closest = from.squareDistanceTo(intercept.hitVec); hit = candidate;
            }
        }
        if (hit != null) {
            double oldX = hit.motionX, oldY = hit.motionY, oldZ = hit.motionZ;
            int resistance = hit.hurtResistantTime;
            if (burst) hit.hurtResistantTime = 0;
            boolean damaged = hit.attackEntityFrom(new EntityDamageSourceIndirect("worker_bee_laser", this, playerId!=null?player:caster).setProjectile(), (float) damage);
            if (burst) hit.hurtResistantTime = Math.max(resistance, hit.hurtResistantTime);
            if (burst) {
                hit.motionX = oldX; hit.motionY = oldY; hit.motionZ = oldZ; hit.velocityChanged = true;
                if (damaged) hit.setFire(4);
            } else if (damaged) {
                double length = Math.hypot(motionX, motionZ);
                double factor = Math.max(0, 1 - hit.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue());
                hit.motionX = oldX + (length > 1.0E-7 ? motionX / length * 0.65 * factor : 0);
                hit.motionZ = oldZ + (length > 1.0E-7 ? motionZ / length * 0.65 * factor : 0);
                hit.motionY = oldY + 0.12 * factor; hit.velocityChanged = true;
            }
            setDead();
        } else if (block != null) setDead();
        else setPosition(to.x, to.y, to.z);
    }
    @Override protected void writeEntityToNBT(NBTTagCompound data) {
        if (casterId != null) data.setUniqueId("Caster", casterId);
        if (playerId != null) data.setUniqueId("PlayerOwner", playerId);
        data.setBoolean("Raid", raid); data.setBoolean("Burst", burst);
        data.setDouble("Damage", damage); data.setInteger("Age", age());
    }
    @Override protected void readEntityFromNBT(NBTTagCompound data) {
        casterId = data.hasUniqueId("Caster") ? data.getUniqueId("Caster") : null;
        playerId=data.hasUniqueId("PlayerOwner")?data.getUniqueId("PlayerOwner"):null;
        raid = data.getBoolean("Raid"); burst = data.getBoolean("Burst"); damage = Math.max(0, data.getDouble("Damage"));
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("Age")));
    }
}
