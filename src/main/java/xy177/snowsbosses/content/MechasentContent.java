package xy177.snowsbosses.content;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ActionResult;
import net.minecraft.item.EnumAction;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.client.MechasentArmorModel;

public final class MechasentContent {
    public static final Item REMNANT = ModItems.add("mechasent_remnant", new Item());
    public static final Item SMITHING_TEMPLATE = ModItems.add("mechasent_smithing_template", new Item());
    public static final Item ARM = ModItems.add("mechasent_arm", new MechasentArm());
    public static final Item HELMET = ModItems.add("mechasent_helmet", new MechasentArmor(EntityEquipmentSlot.HEAD));
    public static final Item CHESTPLATE = ModItems.add("mechasent_chestplate", new MechasentArmor(EntityEquipmentSlot.CHEST));
    public static final Item LEGGINGS = ModItems.add("mechasent_leggings", new MechasentArmor(EntityEquipmentSlot.LEGS));
    public static final Item BOOTS = ModItems.add("mechasent_boots", new MechasentArmor(EntityEquipmentSlot.FEET));

    private MechasentContent() {
    }

    public static void initialize() {
    }

    public static int armDurability() {
        ItemStack netherite = HellstoneContent.ffd("farmers_future_delight:netherite_sword");
        int base = netherite.isEmpty() ? 2031 : netherite.getMaxDamage();
        int step = Math.max(1, Balance.integer("mechasent.arm.durabilityStep", 25));
        double scaled = base * Balance.number("mechasent.arm.durabilityMultiplier", 0.85);
        return Math.max(step, (int) Math.round(scaled / step) * step);
    }

    public static ItemStack item(String id) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation(id));
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static final class MechasentArm extends ItemSword {
        MechasentArm() {
            super(net.minecraft.item.Item.ToolMaterial.DIAMOND);
            setMaxDamage(armDurability());
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> modifiers = super.getItemAttributeModifiers(slot);
            if (slot == EntityEquipmentSlot.MAINHAND) {
                modifiers.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
                modifiers.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
                modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(),
                        new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", Balance.number("mechasent.arm.attackDamage", 11) - 1, 0));
                modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                        new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", Balance.number("mechasent.arm.attackSpeed", 1.6) - 4, 0));
            }
            return modifiers;
        }

        @Override
        public boolean getIsRepairable(ItemStack stack, ItemStack repair) {
            return repair.getItem() == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_BLOCK);
        }

        @Override
        public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
            if (!MechasentEquipment.armUsable(player, player.getHeldItem(hand))) return new ActionResult<>(EnumActionResult.FAIL, player.getHeldItem(hand));
            player.setActiveHand(hand);
            return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
        }

        @Override
        public EnumAction getItemUseAction(ItemStack stack) { return EnumAction.BLOCK; }

        @Override
        public int getMaxItemUseDuration(ItemStack stack) { return 72000; }

        @Override
        public int getItemEnchantability() { return Balance.integer("mechasent.arm.enchantability",15); }

        @Override
        public boolean isShield(ItemStack stack, EntityLivingBase entity) { return true; }

        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
            for(int i=1;i<=4;i++)lines.add(net.minecraft.util.text.TextFormatting.GREEN
                    +net.minecraft.util.text.translation.I18n.translateToLocal("tooltip.snows_bosses.mechasent_arm."+i));
        }

        @Override
        public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
            MechasentEquipment.hit(stack,target,attacker);
            return super.hitEntity(stack,target,attacker);
        }

    }

    public static final class MechasentArmor extends ItemArmor {
        private static final String[] PARTS = {"boots", "leggings", "chestplate", "helmet"};
        private static final int[] DEFENSE = {3, 6, 8, 3};
        private static final int[] DURABILITY = {400, 475, 500, 350};
        private static final UUID[] RESISTANCE = {
                UUID.fromString("93b1d31c-bcc7-4c1e-8474-8f8900dbf9f2"),
                UUID.fromString("e1b4de3f-2cd0-4ec8-80a2-04bcbcf7f3b9"),
                UUID.fromString("8ce66d9f-2af5-48dc-bc4d-63ff0e4bc10c"),
                UUID.fromString("86d2b4c3-cb52-4b66-b00b-1fa82c67d42e")};
        private final EntityEquipmentSlot slot;

        MechasentArmor(EntityEquipmentSlot slot) {
            super(material(slot), 0, slot);
            this.slot = slot;
            setMaxDamage(Balance.integer("mechasent." + PARTS[slot.getIndex()] + ".durability", DURABILITY[slot.getIndex()]));
        }

        private static ItemArmor.ArmorMaterial material(EntityEquipmentSlot slot) {
            int[] defense = new int[4];
            defense[slot.getIndex()] = Balance.integer("mechasent." + PARTS[slot.getIndex()] + ".armor", DEFENSE[slot.getIndex()]);
            return net.minecraftforge.common.util.EnumHelper.addArmorMaterial("SNOWS_BOSSES_MECHASENT_" + slot.name(),
                    "snows_bosses:mechasent", 1,
                    defense, Balance.integer("mechasent." + PARTS[slot.getIndex()] + ".enchantability", 15),
                    net.minecraft.init.SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                    (float) Balance.number("mechasent." + PARTS[slot.getIndex()] + ".toughness", 4));
        }

        public EntityEquipmentSlot equipmentSlot() { return slot; }

        @Override
        public boolean getIsRepairable(ItemStack stack, ItemStack repair) {
            return repair.getItem() == Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_BLOCK);
        }

        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
            if (slot == armorType) map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(RESISTANCE[slot.getIndex()], "Mechasent armor resistance",
                            Balance.number("mechasent." + PARTS[slot.getIndex()] + ".knockbackResistance", 0.1), 0));
            return map;
        }

        @Override
        public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) {
            return "snows_bosses:textures/models/armor/mechasent.png";
        }

        @SideOnly(Side.CLIENT)
        @Override
        public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped model) {
            return MechasentArmorModel.get(slot);
        }

        @Override
        public boolean onEntityItemUpdate(EntityItem entity) {
            return false;
        }

        @Override
        public boolean hasCustomEntity(ItemStack stack) { return true; }

        @Override
        public Entity createEntity(World world, Entity original, ItemStack stack) { return new CoralTools.FireproofItem(world, original, stack); }

        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
            MechasentEquipment.addInformation(stack, lines);
        }
    }
}
