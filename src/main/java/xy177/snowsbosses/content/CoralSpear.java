package xy177.snowsbosses.content;

import com.google.common.collect.Multimap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import xy177.farmersfuturedelight.common.entity.EntityTrident;
import xy177.farmersfuturedelight.common.item.ItemTrident;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.entity.CoralSpearShot;

public final class CoralSpear extends ItemTrident {
    private final int enchantability;
    private final double damage;
    private final double speed;
    private final int lungDuration;

    public CoralSpear(Item.ToolMaterial material) {
        setMaxDamage(material.getMaxUses());
        enchantability = material.getEnchantability();
        damage = Balance.number("items.coral_spear.attackDamage", 9.0);
        speed = Balance.number("items.coral_spear.attackSpeed", -2.5);
        lungDuration = Balance.integer("effects.spearLungDuration", 160);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
            modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                    new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", damage, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", speed, 0));
        }
        return modifiers;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        boolean result = super.hitEntity(stack, target, attacker);
        CoralEffects.hit(target, attacker, lungDuration, false, true);
        return result;
    }

    @Override
    public int getItemEnchantability() {
        return enchantability;
    }

    @Override
    public boolean getIsRepairable(ItemStack stack, ItemStack repair) {
        return repair.getItem() == ModItems.CORAL_INGOT || super.getIsRepairable(stack, repair);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(World world, Entity location, ItemStack stack) {
        return new CoralTools.FireproofItem(world, location, stack);
    }

    public static boolean isTrident(boolean original, Item item) {
        return original || item instanceof CoralSpear;
    }

    public static boolean isTrident(Item item, Item original) {
        return item == original || item instanceof CoralSpear;
    }

    public static boolean channelingWeather(boolean thundering, EntityTrident projectile) {
        return thundering || projectile instanceof CoralSpearShot;
    }

    public static int returnLevel(int loyalty, EntityTrident projectile) {
        return projectile instanceof CoralSpearShot ? loyalty + 3 : loyalty;
    }

    public static EntityTrident projectile(World world, EntityLivingBase thrower, ItemStack stack) {
        return stack.getItem() instanceof CoralSpear
                ? new CoralSpearShot(world, thrower, stack) : new EntityTrident(world, thrower, stack);
    }
}
