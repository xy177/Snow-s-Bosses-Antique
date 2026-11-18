package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.HellstoneEffects;
import xy177.snowsbosses.content.MechasentRaids;

public final class WorkerBeeBeam extends Entity {
    private static final DataParameter<Integer> START = EntityDataManager.createKey(WorkerBeeBeam.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> DURATION = EntityDataManager.createKey(WorkerBeeBeam.class, DataSerializers.VARINT);
    private UUID casterId;
    private UUID playerId;
    private boolean raid;
    private int interval = 20;
    private double damage = 8;

    public WorkerBeeBeam(World world) { super(world); setSize(1.5F, 1); setNoGravity(true); }
    public WorkerBeeBeam(World world, Entity caster, boolean raid, double x, double y, double z) {
        this(world); casterId = caster.getUniqueID(); this.raid = raid; setPosition(x, y, z);
        if(caster instanceof MechasentWorkerBee && ((MechasentWorkerBee)caster).isPlayerBound()
                && ((MechasentWorkerBee)caster).playerOwner()!=null)playerId=((MechasentWorkerBee)caster).playerOwner().getUniqueID();
        damage = MechasentWorkerBee.n("beamDamage", 8); interval = MechasentWorkerBee.seconds("beamDamageInterval", 1);
        dataManager.set(START, (int) world.getTotalWorldTime());
        dataManager.set(DURATION, MechasentWorkerBee.seconds("beamDuration", 15));
    }
    @Override protected void entityInit() { dataManager.register(START, 0); dataManager.register(DURATION, 300); }
    public int age() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    public int duration() { return dataManager.get(DURATION); }
    @Override public AxisAlignedBB getRenderBoundingBox() { return new AxisAlignedBB(posX - 5.1, posY, posZ - 5.1, posX + 5.1, world.getActualHeight(), posZ + 5.1); }
    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) {
            if (ticksExisted == 1) SnowsBosses.PROXY.beamSound(this);
            return;
        }
        if (age() > duration()) { setDead(); return; }
        Entity caster = casterId == null ? null : ((WorldServer) world).getEntityFromUuid(casterId);
        net.minecraft.entity.player.EntityPlayer player=playerId==null?null:world.getPlayerEntityByUUID(playerId);
        if(playerId!=null && (player==null || !player.isEntityAlive())){setDead();return;}
        boolean pulse = age() > 0 && age() % interval == 0;
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(posX - 5, posY - 1, posZ - 5, posX + 5, world.getActualHeight(), posZ + 5))) {
            double distance = Math.hypot(target.posX - posX, target.posZ - posZ);
            if (distance >= 5 || !Mechasent.attackable(target) || (playerId!=null ? xy177.snowsbosses.content.MechasentEquipment.allies(player,target) : Mechasent.protectedTarget(raid, target))
                    || caster != null && caster.isOnSameTeam(target)) continue;
            if (target.getEntityData().getInteger(HellstoneEffects.BURN) <= 0) HellstoneEffects.burn(target, 60);
            if (pulse) {
                int resistance = target.hurtResistantTime;
                target.hurtResistantTime = 0;
                target.attackEntityFrom(new EntityDamageSourceIndirect("worker_bee_beam", this, playerId!=null?player:caster), (float) (damage * (1 - distance / 5)));
                target.hurtResistantTime = Math.max(resistance, target.hurtResistantTime);
            }
        }
    }
    @Override public void setDead() {
        if (!world.isRemote && !isDead && !isSilent()) world.playSound(null, posX, posY, posZ, MechasentRaids.BEAM_END, SoundCategory.HOSTILE, 1, 1);
        super.setDead();
    }
    @Override protected void writeEntityToNBT(NBTTagCompound data) {
        if (casterId != null) data.setUniqueId("Caster", casterId);
        if (playerId != null) data.setUniqueId("PlayerOwner", playerId);
        data.setBoolean("Raid", raid); data.setDouble("Damage", damage);
        data.setInteger("Interval", interval); data.setInteger("Age", age()); data.setInteger("Duration", duration());
    }
    @Override protected void readEntityFromNBT(NBTTagCompound data) {
        casterId = data.hasUniqueId("Caster") ? data.getUniqueId("Caster") : null;
        playerId=data.hasUniqueId("PlayerOwner")?data.getUniqueId("PlayerOwner"):null;
        raid = data.getBoolean("Raid"); damage = Math.max(0, data.getDouble("Damage")); interval = Math.max(1, data.getInteger("Interval"));
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("Age")));
        dataManager.set(DURATION, Math.max(1, data.getInteger("Duration")));
    }
}
