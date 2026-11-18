package xy177.snowsbosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.content.CoralEffects;
import xy177.snowsbosses.content.ModSounds;
import xy177.snowsbosses.network.Visuals;

public final class CopperGolem extends AnimatedCreature {
    private int waterTick;

    public CopperGolem(World world) {
        super(world);
        setSize(1.1F, 2.2F);
        experienceValue = 0;
    }

    @Override
    public String species() {
        return "copper_golem";
    }

    @Override
    protected void initEntityAI() {
        targetTasks.addTask(1, new EntityAINearestAttackableTarget<>(this, EntityLiving.class,
                10, false, false, target -> target instanceof IMob));
        targetTasks.addTask(2, new EntityAIHurtByTarget(this, false));
        EntityAIWanderAvoidWater wander = new EntityAIWanderAvoidWater(this, 1.0);
        wander.setExecutionChance(120);
        tasks.addTask(3, wander);
        tasks.addTask(4, new EntityAILookIdle(this));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (world.isRemote || !isEntityAlive()) return;
        boolean damaged = getHealth() <= getMaxHealth() * 0.5;
        if (damaged && textureName().equals("copper_golem")) playSound(ModSounds.get("entity.iron_golem.damage"), 1, 1);
        texture(damaged ? "copper_golem_destory_1" : "copper_golem");
        EntityLivingBase target = getAttackTarget();
        if (target != null && target.isEntityAlive()) {
            getNavigator().tryMoveToEntityLiving(target, 1.0);
            if (action == 0 && getDistanceSq(target) <= Math.pow(2.5, 2)) {
                begin(1, rand.nextBoolean() ? "attack1" : "attack2");
                getLookHelper().setLookPositionWithEntity(target, 360, 360);
            }
            if (action != 0 && actionTick == 10) {
                playSound(SoundEvents.ENTITY_IRONGOLEM_ATTACK, 1, 1);
                double reach = 2.75;
                net.minecraft.util.math.Vec3d look = getLookVec();
                for (Entity candidate : Combat.nearby(this, posX + look.x * reach * 0.5,
                        posY + look.y * reach * 0.5, posZ + look.z * reach * 0.5, reach * 1.25 / 2)) {
                    if (candidate != target || getDistanceSq(target) > reach * reach) continue;
                    boolean lightning = world.isThundering() && rand.nextDouble() < 0.5;
                    float damage = lightning ? (float) n("lightningAttackDamage", 14)
                            : (float) randomDamage(world.isThundering() ? "thunder" : "normal", 9, world.isThundering() ? 13 : 14);
                    target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
                    if (lightning) world.addWeatherEffect(new EntityLightningBolt(world,
                            Math.floor(target.posX) + 0.5, Math.floor(target.posY), Math.floor(target.posZ) + 0.5, true));
                }
            }
        }
        if (action != 0) {
            getNavigator().clearPath();
            if (actionTick >= 18) finish();
            else actionTick++;
        }
        if (waterTick < 10) {
            waterTick++;
            if (action == 0) animate("");
        } else if (isInWater()) {
            waterTick = 0;
            if (action == 0) animate("water_hurt");
            attackEntityFrom(DamageSource.MAGIC, 1.0F);
            getNavigator().clearPath();
            Visuals.particles(world, "electric_spark", posX, posY + 1.2, posZ,
                    5, 0.6, 0);
            for (Entity candidate : Combat.nearby(this, posX, posY, posZ, 6.0)) {
                if (!(candidate instanceof EntityLiving || candidate instanceof EntityPlayer)
                        || candidate instanceof PhantomPillar || !candidate.isInWater()
                        || getDistanceSq(candidate) > Math.pow(12.0, 2)) continue;
                candidate.attackEntityFrom(DamageSource.MAGIC, (float) n("dischargeDamage", 1));
                Visuals.particles(world, "electric_spark", candidate.posX, candidate.posY, candidate.posZ,
                        4, 0.5, 0);
            }
        }
    }

    private double randomDamage(String prefix, int min, int max) {
        int low = (int) n(prefix + "DamageMin", min);
        int high = Math.max(low, (int) n(prefix + "DamageMax", max));
        return low + rand.nextInt(high - low + 1);
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (getHealth() < getMaxHealth() && stack.getCount() >= 1
                && (stack.getItem() == FFDItems.effectiveItem(FFDItems.COPPER_INGOT)
                || CoralEffects.inGroup(stack, "ingotCopper"))) {
            if (!world.isRemote) {
                heal(20.0F);
                stack.shrink(1);
                playSound(ModSounds.get("entity.iron_golem.repair"), 1, 1);
            }
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return source != DamageSource.FALL && source != DamageSource.DROWN && source != DamageSource.LIGHTNING_BOLT
                && super.attackEntityFrom(source, amount);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound data) {
        super.writeEntityToNBT(data);
        data.setInteger("WaterTick", waterTick);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound data) {
        super.readEntityFromNBT(data);
        waterTick = data.getInteger("WaterTick");
    }
}
