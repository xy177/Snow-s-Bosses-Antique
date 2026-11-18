package xy177.snowsbosses.content;

import com.google.common.collect.Multimap;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.client.CoralArmorModel;

public final class CoralArmor extends ItemArmor {
    private static final UUID RESISTANCE_ID = UUID.fromString("e5c6715a-d1fa-45eb-a5ad-9103dd9464cc");

    public CoralArmor(ArmorMaterial material, EntityEquipmentSlot slot) {
        super(material, 0, slot);
    }

    @Override
    public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
        if (armorType == EntityEquipmentSlot.LEGS) CoralEffects.armorTick(player);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
        String name = getRegistryName().getResourcePath();
        String texture = name.startsWith("seagolden") ? "seagolden_armor"
                : slot == EntityEquipmentSlot.HEAD ? (name.equals("regeneration_coral_helmet") ? "pink_coral_helmet"
                : name.equals("strength_coral_helmet") ? "red_coral_helmet"
                : name.equals("haste_coral_helmet") ? "yellow_coral_helmet" : "coral_helmet_layer") : "coral_armor_layer";
        return "snows_bosses:textures/entities/" + texture + ".png";
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack,
            EntityEquipmentSlot slot, ModelBiped model) {
        return CoralArmorModel.get(entity, stack, slot, model);
    }

    @Override
    public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
        Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
        if (slot == armorType) {
            UUID id = new UUID(RESISTANCE_ID.getMostSignificantBits(), RESISTANCE_ID.getLeastSignificantBits() + slot.getIndex());
            modifiers.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(id, "Coral armor resistance", Balance.number(getArmorMaterial() == ModItems.SEAGOLD_ARMOR
                            ? "armor.seagolden.knockbackResistance" : "armor.coral.knockbackResistance", 0), 0));
        }
        return modifiers;
    }
}
