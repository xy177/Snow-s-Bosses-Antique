package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.Balance;

public final class HellstonePillar extends Entity {
    public static final int RISE_TICKS = 10;
    public static final int FIRE_DELAY = 10;
    public static final int RING_TICKS = 30;
    public static final int VOLLEY_INTERVAL = 40;
    public static final int VOLLEYS = 3;
    public static final int RETRACT_TICKS = 10;
    public static final int FIRST_SHOT = RISE_TICKS + FIRE_DELAY + RING_TICKS;
    public static final int RETRACT_START = FIRST_SHOT + (VOLLEYS - 1) * VOLLEY_INTERVAL;
    public static final int LIFETIME = RETRACT_START + RETRACT_TICKS;
    private static final DataParameter<Integer> START = EntityDataManager.createKey(HellstonePillar.class, DataSerializers.VARINT);
    private EntityLivingBase owner;
    private UUID ownerId;
    private int volleys;

    public HellstonePillar(World world) {
        super(world);
        setSize(1.0F, 2.0F);
        noClip = true;
        setNoGravity(true);
        isImmuneToFire = true;
    }

    public HellstonePillar(World world, EntityLivingBase owner, double x, double y, double z) {
        this(world);
        this.owner = owner;
        ownerId = owner.getUniqueID();
        setPosition(Math.floor(x) + 0.5, Math.floor(y), Math.floor(z) + 0.5);
    }

    public int age() { return Math.max(0, (int) world.getTotalWorldTime() - dataManager.get(START)); }
    public int volleys() { return volleys; }

    public float emergence(float partialTicks) {
        float time = age() + partialTicks;
        return Math.max(0, Math.min(Math.min(1, time / RISE_TICKS), (LIFETIME - time) / RETRACT_TICKS));
    }

    public float ringProgress(float partialTicks) {
        float time = age() + partialTicks - RISE_TICKS - FIRE_DELAY;
        if (time < 0 || time >= (VOLLEYS - 1) * VOLLEY_INTERVAL + RING_TICKS) return -1;
        float phase = time % VOLLEY_INTERVAL;
        return phase < RING_TICKS ? phase / RING_TICKS : -1;
    }

    public boolean summonedBy(EntityLivingBase entity) {
        return entity != null && ownerId != null && ownerId.equals(entity.getUniqueID());
    }

    @Override
    protected void entityInit() { dataManager.register(START, (int) world.getTotalWorldTime()); }
    @Override
    public boolean canBeCollidedWith() { return false; }
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) { return false; }
    @Override
    public boolean isEntityInvulnerable(DamageSource source) { return true; }
    @Override
    public boolean canBeAttackedWithItem() { return false; }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote) return;
        if (owner == null && ownerId != null && world instanceof WorldServer) {
            Entity found = ((WorldServer) world).getEntityFromUuid(ownerId);
            if (found instanceof EntityLivingBase) owner = (EntityLivingBase) found;
        }
        if (owner == null || !owner.isEntityAlive() || age() >= LIFETIME) {
            setDead();
            return;
        }
        if (age() == 1 || age() == RETRACT_START) playSound(SoundEvents.BLOCK_STONE_PLACE, 1.0F, 0.6F);
        if (volleys < VOLLEYS && age() >= FIRST_SHOT + volleys * VOLLEY_INTERVAL) {
            double damage = Balance.number("hellstone_golem.pillarProjectileDamage", 7);
            double acceleration = Balance.number("hellstone_golem.pillarProjectileAcceleration", 0.1);
            int lifetime = Balance.ticks("hellstone_golem.pillarProjectileLifetime", 40);
            int burn = Balance.ticks("hellstone_golem.pillarBurnDuration", 100);
            for (int level = 0; level < 2; level++) {
                for (EnumFacing side : EnumFacing.HORIZONTALS) {
                    world.spawnEntity(new HellstonePillarShot(world, owner,
                            posX + side.getFrontOffsetX() * (0.5 + HellstonePillarShot.SIZE * 0.5),
                            posY + level + 0.5 - HellstonePillarShot.SIZE * 0.5,
                            posZ + side.getFrontOffsetZ() * (0.5 + HellstonePillarShot.SIZE * 0.5),
                            Math.atan2(side.getFrontOffsetZ(), side.getFrontOffsetX()), acceleration, lifetime, damage, burn));
                }
            }
            volleys++;
            playSound(SoundEvents.ENTITY_BLAZE_SHOOT, 1.4F, 0.8F);
        }
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound data) {
        if (ownerId != null) data.setUniqueId("Owner", ownerId);
        data.setInteger("Age", age());
        data.setInteger("Volleys", volleys);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound data) {
        ownerId = data.hasUniqueId("Owner") ? data.getUniqueId("Owner") : null;
        dataManager.set(START, (int) world.getTotalWorldTime() - Math.max(0, data.getInteger("Age")));
        volleys = Math.max(0, Math.min(VOLLEYS, data.getInteger("Volleys")));
    }
}
