package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public final class MechasentWave extends Entity {
    private UUID ownerId;
    private boolean raid;
    private boolean playerOwned;
    private int type;
    private int age;
    private int lifetime = 20;
    private double dx;
    private double dz;
    private double damage;

    public MechasentWave(World world) { super(world); setSize(0.5F, 0.5F); }
    public MechasentWave(World world, Mechasent owner, int type, double x, double y, double z, double angle, double speed, int lifetime, double damage) {
        this(world);
        ownerId = owner.getUniqueID();
        raid = owner.raidSpawned();
        this.type = type; this.lifetime = lifetime; this.damage = damage;
        dx = Math.cos(angle) * speed; dz = Math.sin(angle) * speed;
        setPosition(x, y, z);
    }

    public MechasentWave(World world, net.minecraft.entity.player.EntityPlayer player, double damage) {
        this(world); ownerId=player.getUniqueID(); playerOwned=true; type=1; lifetime=15; this.damage=damage;
        setPosition(player.posX,player.posY,player.posZ);
    }

    @Override protected void entityInit() { }
    @Override public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        age++;
        if (type == 1) {
            if (age % 3 == 0) {
                int count = Math.max(1, (int) (age * 2.0));
                double radius = age * 0.5;
                for (int i = 0; i < count; i++) {
                    double angle = Math.PI * 2 * i / count;
                    spawnLaser(posX + Math.cos(angle) * radius, posY, posZ + Math.sin(angle) * radius);
                }
            }
        } else {
            double oldX = posX, oldY = posY, oldZ = posZ;
            setPosition(posX + dx, posY, posZ + dz);
            for (double depth : new double[]{0.8, 0.5, 0.1}) {
                if (!world.getBlockState(new BlockPos(posX, posY - depth, posZ)).isOpaqueCube()) { setPosition(posX, posY - depth, posZ); break; }
            }
            if (type == 2) spawnLaser(oldX, oldY, oldZ);
            else {
                Entity owner = ownerId == null ? null : ((WorldServer) world).getEntityFromUuid(ownerId);
                EntityLivingBase target = owner instanceof Mechasent ? ((Mechasent) owner).getAttackTarget() : null;
                if (owner instanceof Mechasent) ((Mechasent) owner).debris(oldX, oldY, oldZ, 7, 0.4);
                double radius = 0.6;
                if (target != null && !Mechasent.protectedTarget(raid, target)
                        && Math.abs(target.posY + target.height / 2 - oldY) < radius + target.height / 2
                        && Math.hypot(target.posX - oldX, target.posZ - oldZ) < radius + target.width / 2) {
                    target.attackEntityFrom(new EntityDamageSourceIndirect("mob", this, owner), (float) damage);
                }
            }
        }
        if (age >= Math.max(1, lifetime - (type == 1 ? 0 : 1)) || posY < -16) setDead();
    }

    private void spawnLaser(double x, double y, double z) {
        MechasentLaser laser=new MechasentLaser(world, ownerId, raid, x, y, z, damage);
        if(playerOwned) laser.forPlayer();
        world.spawnEntity(laser);
    }
    @Override protected void writeEntityToNBT(NBTTagCompound data) {
        if (ownerId != null) data.setUniqueId("Owner", ownerId);
        data.setBoolean("PlayerOwned",playerOwned); data.setBoolean(Mechasent.RAID_TAG, raid);
        data.setInteger("Type", type); data.setInteger("Age", age); data.setInteger("Lifetime", lifetime);
        data.setDouble("DirectionX", dx); data.setDouble("DirectionZ", dz); data.setDouble("Damage", damage);
    }
    @Override protected void readEntityFromNBT(NBTTagCompound data) {
        ownerId = data.hasUniqueId("Owner") ? data.getUniqueId("Owner") : null;
        playerOwned=data.getBoolean("PlayerOwned"); raid = data.getBoolean(Mechasent.RAID_TAG);
        type = Math.max(0, Math.min(2, data.getInteger("Type")));
        age = Math.max(0, data.getInteger("Age")); lifetime = Math.max(1, data.getInteger("Lifetime"));
        dx = data.getDouble("DirectionX"); dz = data.getDouble("DirectionZ"); damage = Math.max(0, data.getDouble("Damage"));
    }
}
