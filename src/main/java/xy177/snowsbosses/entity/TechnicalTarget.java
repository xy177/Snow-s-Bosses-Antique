package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public final class TechnicalTarget extends EntityCreature {
    private static final DataParameter<Boolean> FIXED = EntityDataManager.createKey(TechnicalTarget.class, DataSerializers.BOOLEAN);
    public TechnicalTarget(World world) {
        super(world);
        setSize(0.6F, 1.8F);
        enablePersistence();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(FIXED, false);
    }

    public void setKnockbackImmune(boolean fixed) { dataManager.set(FIXED, fixed); }

    public boolean isKnockbackImmune() { return dataManager.get(FIXED); }

    @Override
    public boolean canBePushed() { return !isKnockbackImmune(); }

    @Override
    public void knockBack(net.minecraft.entity.Entity attacker, float strength, double x, double z) {
        if (!isKnockbackImmune()) super.knockBack(attacker, strength, x, z);
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (!isKnockbackImmune()) super.addVelocity(x, y, z);
    }

    @Override
    public void onLivingUpdate() {
        if (isKnockbackImmune()) {
            motionX = 0;
            motionZ = 0;
            if (motionY > 0) motionY = 0;
        }
        super.onLivingUpdate();
    }

    @Override
    protected void initEntityAI() { }

    @Override
    public void setHealth(float health) {
        super.setHealth(Float.isNaN(health) ? 1.0F : Math.max(1.0F, health));
    }

    @Override
    public void onDeath(DamageSource cause) {
        setHealth(getHealth());
        deathTime = 0;
    }

    @Override
    public void onKillCommand() {
        setDead();
    }

    @Override
    protected boolean canDespawn() { return false; }

    @Override
    protected boolean canDropLoot() { return false; }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("NoKnockback", isKnockbackImmune());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        setKnockbackImmune(compound.getBoolean("NoKnockback"));
        enablePersistence();
        setHealth(getHealth());
        deathTime = 0;
    }
}
