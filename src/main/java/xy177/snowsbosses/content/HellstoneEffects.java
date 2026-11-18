package xy177.snowsbosses.content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.HellstoneGolem;
import xy177.snowsbosses.entity.HellstoneImp;
import xy177.snowsbosses.network.Visuals;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class HellstoneEffects {
    public static final String BURN = "malevolentBurn";
    private static final String MACHETE_ATTACK = "HellstoneMacheteAttack";
    public static final DamageSource DAMAGE = new DamageSource("malevolent_burn").setDamageBypassesArmor().setDamageIsAbsolute();

    private HellstoneEffects() {
    }

    public static double n(String key, double fallback) { return Balance.number("hellstone." + key, fallback); }
    public static int t(String key, int fallback) { return Balance.ticks("hellstone." + key, fallback); }

    public static boolean allies(Entity a, Entity b) {
        return a != null && b != null && ((a instanceof HellstoneGolem || a instanceof HellstoneImp)
                && (b instanceof HellstoneGolem || b instanceof HellstoneImp));
    }

    public static void burn(Entity target, int duration) {
        if (!(target instanceof EntityLivingBase) || target instanceof HellstoneGolem
                || target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isCreativeMode) return;
        int applied=Math.max(duration,target.getEntityData().getInteger(BURN));
        if(MechasentModules.METABOLISM.worn((EntityLivingBase)target) && !MechasentModuleEffects.blacklisted("snows_bosses:malevolent_burn"))
            applied=Math.min(applied,Balance.integer("mechasent.modules.debuffDuration",60));
        target.getEntityData().setInteger(BURN, applied);
    }

    public static float burnDamage(EntityLivingBase entity, double fraction, boolean resistance) {
        double floor = n("burn.minimumDamage", 2);
        double amount = resistance && entity.isPotionActive(MobEffects.FIRE_RESISTANCE)
                ? floor : Math.max(floor, entity.getHealth() * fraction);
        PotionEffect effect = entity.getActivePotionEffect(MobEffects.RESISTANCE);
        amount *= effect == null ? 1 : Math.max(0, 1 - (effect.getAmplifier() + 1) * 0.2);
        double cap = n("burn.nonPlayerDamageCap", 20);
        return (float) (!(entity instanceof EntityPlayer) && cap > 0 ? Math.min(amount, cap) : amount);
    }

    public static void blockContact(EntityLivingBase entity) {
        if (entity instanceof HellstoneGolem) return;
        burn(entity, t("blocks.burnDuration", 80));
        entity.attackEntityFrom(DAMAGE, burnDamage(entity, n("blocks.healthFraction", 0.1), true));
    }

    private static void ignite(Entity target) {
        ignite(target, 1, t("equipment.burnDuration", 30));
    }

    private static void ignite(Entity target, int pieces, int duration) {
        if (target.world.rand.nextDouble() < Math.min(1, pieces * n("equipment.burnChance", 0.1))
                && target.getEntityData().getInteger(BURN) <= n("equipment.burnThreshold", 20)) {
            burn(target, duration);
        } else target.setFire(t("equipment.fireSeconds", 10));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void attack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) return;
        player.getEntityData().removeTag(MACHETE_ATTACK);
        if (event.isCanceled() || player.getHeldItemMainhand().getItem() != HellstoneContent.MACHETE
                || player.getCooledAttackStrength(0) < n("machete.minimumCharge", 0.9)) return;
        NBTTagCompound attack = new NBTTagCompound();
        attack.setInteger("Target", event.getTarget().getEntityId());
        attack.setInteger("Tick", player.ticksExisted);
        player.getEntityData().setTag(MACHETE_ATTACK, attack);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        Entity attacker = event.getSource().getTrueSource();
        if (HellstoneNetherProtection.passiveContact(attacker, victim)) { event.setCanceled(true); return; }
        if (allies(victim, attacker)) { event.setCanceled(true); return; }
        if (victim.world.isRemote || event.getAmount() <= 0 || victim.isEntityInvulnerable(event.getSource())) return;
        int pieces = 0;
        for (ItemStack armor : victim.getArmorInventoryList()) if (armor.getItem() instanceof HellstoneContent.Armor) pieces++;
        if (attacker != null && pieces > 0) ignite(attacker, pieces, pieces == 1 ? t("equipment.burnDuration", 30)
                : t("armor.burnDuration" + pieces, pieces == 2 ? 40 : pieces == 3 ? 50 : 30));
        if (attacker instanceof EntityPlayer && event.getSource().getImmediateSource() == attacker
                && ((EntityPlayer) attacker).getHeldItemMainhand().getItem() == HellstoneContent.MACHETE) {
            NBTTagCompound attack = attacker.getEntityData().getCompoundTag(MACHETE_ATTACK);
            attacker.getEntityData().removeTag(MACHETE_ATTACK);
            if (attack.hasKey("Target") && attack.getInteger("Target") == victim.getEntityId()
                    && attack.getInteger("Tick") == attacker.ticksExisted) ignite(victim);
        }
    }

    @SubscribeEvent
    public static void knockedBack(LivingKnockBackEvent event) {
        Entity attacker = event.getAttacker();
        if (attacker instanceof HellstoneImp && ((HellstoneImp) attacker).suppressesMeleeKnockback()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote) return;
        int remaining = entity.getEntityData().getInteger(BURN);
        if (remaining > 0) {
            Visuals.particles(entity.world, "hellstone_fire", entity.posX, entity.posY + entity.height / 2, entity.posZ, 1, entity.width / 2, 0);
            entity.getEntityData().setInteger(BURN, --remaining);
            if (remaining % t("burn.interval", 20) == 0) entity.attackEntityFrom(DAMAGE, burnDamage(entity, n("burn.healthFraction", 0.07), true));
        }
        boolean chestplate = entity.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem() == HellstoneContent.CHESTPLATE;
        if (chestplate && entity.getEntityData().getInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG) > 0) {
            entity.getEntityData().setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, 0);
        }
        if (entity.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem() == HellstoneContent.LEGGINGS) {
            entity.extinguish();
            entity.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, t("chestplate.fireResistanceDuration", 11), 0, false, false));
        }
        boolean equipped = false;
        for (ItemStack armor : entity.getArmorInventoryList()) if (armor.getItem() instanceof HellstoneContent.Armor) equipped = true;
        if (!equipped) {
            if (entity.getEntityData().hasKey("hellstoneChestplate")) entity.getEntityData().removeTag("hellstoneChestplate");
            return;
        }
        int count = entity.isInLava() ? entity.getEntityData().getInteger("hellstoneChestplate") + 1 : 0;
        if (count >= t("chestplate.lavaInterval", 10)) {
            for (ItemStack armor : entity.getArmorInventoryList()) {
                if (armor.getItem() instanceof HellstoneContent.Armor && armor.isItemDamaged()) {
                    armor.setItemDamage(Math.max(0, armor.getItemDamage() - t("chestplate.lavaRepair", 1)));
                }
            }
            if (chestplate) entity.heal((float) n("chestplate.lavaHealing", 1));
            count = 0;
        }
        entity.getEntityData().setInteger("hellstoneChestplate", count);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || event.getSource().canHarmInCreative()) return;
        ItemStack totem = ItemStack.EMPTY;
        for (EnumHand hand : EnumHand.values()) if (entity.getHeldItem(hand).getItem() == HellstoneContent.TOTEM) {
            totem = entity.getHeldItem(hand); break;
        }
        if (totem.isEmpty()) return;
        event.setCanceled(true);
        totem.shrink(1);
        activateTotem(entity);
    }

    public static void activateTotem(EntityLivingBase entity) {
        entity.setHealth((float) n("totem.restoredHealth", 1));
        entity.clearActivePotions();
        entity.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, t("totem.resistanceDuration", 60), (int) n("totem.resistanceAmplifier", 3), false, false));
        entity.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE, t("totem.fireResistanceDuration", 2400), (int) n("totem.fireResistanceAmplifier", 0)));
        entity.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, t("totem.regenerationDuration", 1200), (int) n("totem.regenerationAmplifier", 3)));
        double range = n("totem.range", 5);
        for (EntityLivingBase nearby : entity.world.getEntitiesWithinAABB(EntityLivingBase.class, entity.getEntityBoundingBox().grow(range))) {
            if (nearby == entity) continue;
            if (nearby instanceof IMob || entity instanceof EntityLiving && ((EntityLiving) entity).getAttackTarget() != null
                    && nearby instanceof EntityLiving && ((EntityLiving) nearby).getAttackTarget() == entity) burn(nearby, t("totem.burnDuration", 100));
        }
        Visuals.totem(entity);
        Visuals.particles(entity.world, "hellstone_fire", entity.posX, entity.posY + entity.height / 2, entity.posZ, 24, entity.width / 2, 0);
    }
}
