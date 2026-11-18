package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

public final class MechasentLaser extends Entity {
    private static final DataParameter<Integer> START = EntityDataManager.createKey(MechasentLaser.class, DataSerializers.VARINT);
    private UUID ownerId;
    private boolean raid;
    private boolean playerOwned;
    private int damage;

    public MechasentLaser(World world) { super(world); setSize(0.5F, 0.6F); }
    public MechasentLaser(World world, Mechasent owner, double x, double y, double z, double damage) {
        this(world, owner.getUniqueID(), owner.raidSpawned(), x, y, z, damage);
    }
    public MechasentLaser(World world, UUID owner, boolean raid, double x, double y, double z, double damage) {
        this(world); ownerId = owner; this.raid = raid; this.damage = (int) damage; setPosition(x, y, z);
        dataManager.set(START, (int) world.getTotalWorldTime());
    }
    public MechasentLaser forPlayer() { playerOwned=true; return this; }
    @Override protected void entityInit() { dataManager.register(START, 0); }
    public int age() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    @Override public boolean attackEntityFrom(DamageSource source, float damage) { return false; }
    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        move(MoverType.SELF, 0, -10, 0);
        fallDistance = 0;
        int age = age();
        if (age >= 18 || posY < -16) { setDead(); return; }
        if (age < 6 || age > 15) return;
        net.minecraft.entity.player.EntityPlayer player=playerOwned ? world.getPlayerEntityByUUID(ownerId) : null;
        if(playerOwned && (player==null || !player.isEntityAlive())){setDead();return;}
        double radius = 0.3;
        double below = 1.0, above = 2.0;
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(posX - radius, posY - below, posZ - radius, posX + radius, posY + above, posZ + radius))) {
            if (!Mechasent.attackable(victim) || !victim.isEntityAlive() || (playerOwned
                    ? xy177.snowsbosses.content.MechasentEquipment.allies(player,victim) || victim instanceof net.minecraft.entity.player.EntityPlayer
                        && (((net.minecraft.entity.player.EntityPlayer) victim).isSpectator() || ((net.minecraft.entity.player.EntityPlayer) victim).capabilities.isCreativeMode)
                    : Mechasent.protectedTarget(raid, victim))) continue;
            if (Math.hypot(victim.posX - posX, victim.posZ - posZ) < radius + victim.width / 2) {
                victim.attackEntityFrom(new EntityDamageSourceIndirect("mob", this, playerOwned ? player : ownerId == null ? null : ((net.minecraft.world.WorldServer)world).getEntityFromUuid(ownerId)), (float) damage);
            }
        }
    }
    @Override protected void writeEntityToNBT(NBTTagCompound data) {
        if (ownerId != null) data.setUniqueId("Owner", ownerId);
        data.setBoolean("PlayerOwned",playerOwned); data.setBoolean(Mechasent.RAID_TAG, raid); data.setDouble("Damage", damage); data.setInteger("Age", age());
    }
    @Override protected void readEntityFromNBT(NBTTagCompound data) {
        ownerId = data.hasUniqueId("Owner") ? data.getUniqueId("Owner") : null;
        playerOwned=data.getBoolean("PlayerOwned"); raid = data.getBoolean(Mechasent.RAID_TAG); damage = Math.max(0, data.getInteger("Damage"));
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("Age")));
    }
}
