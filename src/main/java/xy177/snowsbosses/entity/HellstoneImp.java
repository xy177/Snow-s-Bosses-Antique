package xy177.snowsbosses.entity;

import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.network.Visuals;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xy177.snowsbosses.content.HellstoneEffects;

public final class HellstoneImp extends HellstoneMob {
    private UUID summoner;
    private BlockPos summonerPosition;
    private int summonedAge;
    private int pursuitTicks;
    private boolean meleeAttacking;

    public boolean summonedBy(HellstoneGolem golem) { return golem.getUniqueID().equals(summoner); }
    public boolean suppressesMeleeKnockback() { return summoner != null && meleeAttacking; }
    public void bindTo(HellstoneGolem golem) {
        summoner = golem.getUniqueID(); summonerPosition = golem.getPosition(); enablePersistence();
    }
    private HellstoneGolem summoner() {
        Entity entity = summoner != null && world instanceof WorldServer ? ((WorldServer) world).getEntityFromUuid(summoner) : null;
        return entity instanceof HellstoneGolem ? (HellstoneGolem) entity : null;
    }
    @Override
    public boolean isAIDisabled() { return action() == 3 || super.isAIDisabled(); }
    @Override
    public void setDead() {
        HellstoneGolem golem = summoner();
        if (golem != null) golem.releaseImp(getUniqueID());
        super.setDead();
    }
    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        if (summoner != null) {
            data.setUniqueId("HellstoneSummoner", summoner);
            data.setLong("HellstoneSummonerPosition", summonerPosition.toLong());
            data.setInteger("SummonedAge", summonedAge); data.setInteger("PursuitTicks", pursuitTicks);
        }
    }
    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        if (data.hasUniqueId("HellstoneSummoner")) {
            summoner = data.getUniqueId("HellstoneSummoner");
            summonerPosition = BlockPos.fromLong(data.getLong("HellstoneSummonerPosition"));
            summonedAge = Math.max(0, data.getInteger("SummonedAge"));
            pursuitTicks = Math.max(0, data.getInteger("PursuitTicks"));
            enablePersistence();
        }
    }
    public HellstoneImp(World world) {
        super(world);
        setSize(0.5F, 1.1F);
        stepHeight = 0.6F; experienceValue = 5;
    }
    @Override
    public String species() { return "hellstone_imp"; }
    @Override
    protected void initEntityAI() {
        tasks.addTask(3, new EntityAIAttackMelee(this, 1.2, false));
        tasks.addTask(4, new EntityAIWander(this, 1.0));
        tasks.addTask(6, new EntityAILookIdle(this));
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityPlayer.class, false));
        targetTasks.addTask(2, new EntityAINearestAttackableTarget<>(this, EntityVillager.class, false));
        targetTasks.addTask(5, new EntityAIHurtByTarget(this, false));
    }
    @Override
    public EnumCreatureAttribute getCreatureAttribute() { return EnumCreatureAttribute.UNDEAD; }
    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (action() >= 2 || HellstoneEffects.allies(this, target)) return false;
        action(1);
        target.setFire(4);
        meleeAttacking = true;
        try { return super.attackEntityAsMob(target); }
        finally { meleeAttacking = false; }
    }
    @Override
    public void onLivingUpdate() {
        if (!world.isRemote && isEntityAlive() && summoner != null) {
            HellstoneGolem golem = summoner();
            if (golem != null) {
                if (!golem.isEntityAlive()) { setDead(); return; }
                summonerPosition = golem.getPosition();
                if (!golem.trackImp(this)) { setDead(); return; }
            } else if (ticksExisted > 40 && world.isBlockLoaded(summonerPosition)) { setDead(); return; }
            summonedAge++;
            if (action() < 2 && summonedAge >= Math.ceil(n("selfDestructDelay", 10) * 20)) {
                EntityPlayer player = nearbyPlayer();
                if (player == null) startFuse();
                else { setAttackTarget(player); action(2); }
            }
            if (action() == 2) {
                EntityPlayer player = nearbyPlayer();
                if (player == null || getDistanceSq(player) <= 4 || pursuitTicks++ >= 80) startFuse();
                else {
                    setAttackTarget(player);
                    if (pursuitTicks % 10 == 1) getNavigator().tryMoveToEntityLiving(player, 1.2);
                }
            }
            if (action() == 3) {
                getNavigator().clearPath(); setMoveForward(0); setMoveStrafing(0); motionX = motionZ = 0;
                if (actionAge() % 4 == 0) Visuals.particles(world, "hellstone_fire", posX, posY + height / 2, posZ, 3, 0.2, 0.04);
                if (actionAge() >= 20) { detonate(); return; }
            }
        }
        super.onLivingUpdate();
        if (!world.isRemote && action() == 1 && actionAge() > 5) action(0);
    }
    private EntityPlayer nearbyPlayer() {
        EntityPlayer nearest = null;
        double distance = 144;
        for (EntityPlayer player : world.playerEntities) {
            if (!player.isEntityAlive() || player.capabilities.isCreativeMode || player.isSpectator() || HellstoneEffects.allies(this, player)) continue;
            double next = getDistanceSq(player);
            if (next <= distance) { nearest = player; distance = next; }
        }
        return nearest;
    }
    private void startFuse() {
        action(3);
        getNavigator().clearPath();
        playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1, 0.8F);
    }
    private void detonate() {
        for (EntityLivingBase victim : world.getEntitiesWithinAABB(EntityLivingBase.class, getEntityBoundingBox().grow(3))) {
            if (victim == this || HellstoneEffects.allies(this, victim)) continue;
            double distance = getDistance(victim);
            if (distance < 3 && victim.attackEntityFrom(DamageSource.causeMobDamage(this),
                    (float) (n("selfDestructDamage", 8) * (1 - distance / 3)))) HellstoneEffects.burn(victim, 160);
        }
        Visuals.particles(world, "hellstone_burst_ring", posX, posY + 0.05, posZ, 1, 0, 0);
        Visuals.particles(world, "hellstone_explode", posX, posY + 0.5, posZ, 10, 0.45, 0.08);
        playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, 1.3F, 0.9F);
        setDead();
    }
    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isFireDamage() || source == DamageSource.WITHER || "witherSkull".equals(source.getDamageType())
                || HellstoneEffects.allies(this, source.getTrueSource())) return false;
        if (summoner != null && (source.isProjectile() || source.isMagicDamage())) amount *= 2;
        return super.attackEntityFrom(source, amount);
    }
    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT; }
    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.ENTITY_WITHER_SKELETON_HURT; }
    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.ENTITY_WITHER_SKELETON_DEATH; }
    @Override
    protected void playStepSound(BlockPos pos, Block block) { playSound(SoundEvents.ENTITY_WITHER_SKELETON_STEP, 0.15F, 1); }
}
