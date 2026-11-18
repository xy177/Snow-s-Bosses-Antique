package xy177.snowsbosses.content;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.Balance;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class ModItems {
    private static final List<Item> ITEMS = new ArrayList<>();
    public static final String CORAL_FRAGMENTS = "snowsBossesCoralFragment";
    public static final String CORAL_HELMETS = "snowsBossesCoralHelmet";
    public static final CreativeTabs TAB = new CreativeTabs("snows_bosses") {
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(CORAL_INGOT);
        }

        @Override
        public void displayAllRelevantItems(NonNullList<ItemStack> items) {
            super.displayAllRelevantItems(items);
            List<ItemStack> ordered = new ArrayList<>();
            for (Item item : equipmentOrder()) {
                for (Iterator<ItemStack> iterator = items.iterator(); iterator.hasNext();) {
                    ItemStack stack = iterator.next();
                    if (stack.getItem() == item) {
                        ordered.add(stack);
                        iterator.remove();
                    }
                }
            }
            int index = 0;
            while (index < items.size() && items.get(index).getItem() != HellstoneContent.INGOT) index++;
            items.addAll(index < items.size() ? index + 1 : items.size(), ordered);
        }
    };
    public static final Item TECHNICAL_TARGET_TOOL = add("technical_target_tool", new TechnicalTargetTool()).setCreativeTab(null);
    public static final Item CORAL_INGOT = add("coral_ingot", new Item());
    public static final Item PINK_CORAL_FRAG = add("pink_coral_frag", new Item());
    public static final Item RED_CORAL_FRAG = add("red_coral_frag", new Item());
    public static final Item BLUE_CORAL_FRAG = add("blue_coral_frag", new Item());
    public static final Item YELLOW_CORAL_FRAG = add("yellow_coral_frag", new Item());
    public static final Item BROKEN_DROWNED_SOUL = add("broken_drowned_soul", new Item());
    public static final Item DROWNED_SOUL = add("drowned_soul", new Item());
    public static final Item INFERNAL_EYE = add("infernal_eye", new StructureEyeItem("hellstone_ruin"));
    public static final Item TIDE_EYE = add("tide_raiser_eye", new StructureEyeItem("tide_raiser_tower"));
    public static final Item DROWNER_HOWL_EYE = add("drowner_howl_eye", new StructureEyeItem("the_ocean_shrine"));
    public static final Item CORAL_SMITHING_TEMPLATE = add("coral_smithing_template", new Item());
    public static final Item SEAGOLD_NUGGET = add("seagold_nugget", new Item());
    public static final Item SEAGOLD_INGOT = add("seagold_ingot", new Item());
    public static final Item CORAL_PEARL = add("coral_pearl", new Item());
    public static final Item.ToolMaterial CORAL = material("coral_sword", 4, 1300, 9.0F, 4.0F, 18);
    public static final Item.ToolMaterial ANCIENT = material("ancient_coral_spear", 3, 1000, 8.0F, 3.0F, 15);
    public static final Item.ToolMaterial REFINED = material("coral_spear", 4, 1800, 12.0F, 6.0F, 20);
    public static final ItemArmor.ArmorMaterial CORAL_ARMOR = armorMaterial("coral", 35, new int[]{3, 7, 8, 4}, 18);
    public static final ItemArmor.ArmorMaterial SEAGOLD_ARMOR = armorMaterial("seagolden", 40, new int[]{3, 7, 10, 5}, 20);
    public static final Item CORAL_SWORD = add("coral_sword",
            new CoralTools.Weapon("coral_sword", CORAL, 7.0, -2.4, 160, false, false));
    public static final Item CORAL_AXE = add("coral_axe", new CoralTools.Axe());
    public static final Item CORAL_PICKAXE = add("coral_pickaxe", new CoralTools.Pickaxe());
    public static final Item ANCIENT_CORAL_SPEAR = add("ancient_coral_spear",
            new CoralTools.Weapon("ancient_coral_spear", ANCIENT, 6.0, -2.5, 80, false, false));
    public static final Item CORAL_SPEAR = add("coral_spear",
            new CoralSpear(REFINED));
    public static final Item CORAL_SCYTHE = add("coral_scythe",
            new CoralTools.Weapon("coral_scythe", material("coral_scythe", 4, 1800, 12.0F, 12.0F, 20), 15.0, -2.8, 80, false, true));
    public static final Item CORAL_HELMET = armor("coral_helmet", EntityEquipmentSlot.HEAD, false);
    public static final Item CORAL_CHESTPLATE = armor("coral_chestplate", EntityEquipmentSlot.CHEST, false);
    public static final Item CORAL_LEGGINGS = armor("coral_leggings", EntityEquipmentSlot.LEGS, false);
    public static final Item CORAL_BOOTS = armor("coral_boots", EntityEquipmentSlot.FEET, false);
    public static final Item REGENERATION_CORAL_HELMET = armor("regeneration_coral_helmet", EntityEquipmentSlot.HEAD, false);
    public static final Item STRENGTH_CORAL_HELMET = armor("strength_coral_helmet", EntityEquipmentSlot.HEAD, false);
    public static final Item HASTE_CORAL_HELMET = armor("haste_coral_helmet", EntityEquipmentSlot.HEAD, false);
    public static final Item SEAGOLDEN_HELMET = armor("seagolden_helmet", EntityEquipmentSlot.HEAD, true);
    public static final Item SEAGOLDEN_CHESTPLATE = armor("seagolden_chestplate", EntityEquipmentSlot.CHEST, true);

    private ModItems() {
    }

    public static Item add(String name, Item item) {
        item.setRegistryName(SnowsBosses.MOD_ID, name);
        item.setUnlocalizedName(SnowsBosses.MOD_ID + "." + name);
        item.setCreativeTab(TAB);
        ITEMS.add(item);
        return item;
    }

    private static Item armor(String name, EntityEquipmentSlot slot, boolean seagold) {
        Item item = add(name, new CoralArmor(seagold ? SEAGOLD_ARMOR : CORAL_ARMOR, slot));
        String[] slots = {"boots", "leggings", "chestplate", "helmet"};
        String prefix = "armor." + (seagold ? "seagolden" : "coral") + ".";
        item.setMaxDamage(Math.max(1, (int) (Balance.ticks(prefix + slots[slot.getIndex()] + ".durability", item.getMaxDamage())
                * Balance.number(prefix + "durabilityMultiplier", seagold ? 40 : 35) / (seagold ? 40 : 35))));
        return item;
    }

    public static Item.ToolMaterial material(String name, int level, int durability,
            float efficiency, float damage, int enchantability) {
        String key = "items." + name + ".";
        boolean miningTool = name.equals("coral_axe") || name.equals("coral_pickaxe");
        Item.ToolMaterial material = EnumHelper.addToolMaterial("SNOWS_BOSSES_" + name.toUpperCase(java.util.Locale.ROOT),
                miningTool ? Balance.integer(key + "harvestLevel", level) : level, Balance.ticks(key + "durability", durability),
                miningTool ? (float) Balance.number(key + "miningSpeed", efficiency) : efficiency, damage,
                Balance.integer(key + "enchantability", enchantability));
        material.setRepairItem(new ItemStack(CORAL_INGOT));
        return material;
    }

    private static ItemArmor.ArmorMaterial armorMaterial(String name, int durability, int[] defense, int enchantability) {
        String key = "armor." + name + ".";
        String[] slots = {"boots", "leggings", "chestplate", "helmet"};
        for (int i = name.equals("seagolden") ? 2 : 0; i < slots.length; i++) defense[i] = Balance.integer(key + slots[i] + ".defense", defense[i]);
        return EnumHelper.addArmorMaterial("SNOWS_BOSSES_" + name.toUpperCase(java.util.Locale.ROOT),
                "snows_bosses:" + name, Balance.ticks(key + "durabilityMultiplier", durability), defense,
                Balance.integer(key + "enchantability", enchantability), SoundEvents.ITEM_ARMOR_EQUIP_IRON,
                (float) Balance.number(key + "toughness", 2.0));
    }

    public static List<Item> all() {
        return Collections.unmodifiableList(ITEMS);
    }

    private static Item blockItem(Block block) {
        return ITEMS.stream().filter(item -> item instanceof ItemBlock && ((ItemBlock) item).getBlock() == block)
                .findFirst().orElseThrow(IllegalStateException::new);
    }

    private static List<Item> equipmentOrder() {
        return Arrays.asList(blockItem(HellstoneContent.BLOCK), blockItem(HellstoneContent.RAW_BLOCK),
                HellstoneContent.SMITHING_TEMPLATE, HellstoneContent.TOTEM, HellstoneContent.MACHETE,
                HellstoneContent.HELMET, HellstoneContent.CHESTPLATE, HellstoneContent.LEGGINGS, HellstoneContent.BOOTS,
                MechasentContent.REMNANT, MechasentContent.SMITHING_TEMPLATE, MechasentContent.ARM,
                MechasentContent.HELMET, MechasentContent.CHESTPLATE, MechasentContent.LEGGINGS, MechasentContent.BOOTS);
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Item> event) {
        MechasentContent.initialize();
        List<Item> ordered = equipmentOrder();
        ITEMS.removeAll(ordered);
        ITEMS.addAll(ITEMS.indexOf(HellstoneContent.INGOT) + 1, ordered);
        CORAL_ARMOR.setRepairItem(new ItemStack(CORAL_INGOT));
        SEAGOLD_ARMOR.setRepairItem(new ItemStack(SEAGOLD_INGOT));
        event.getRegistry().registerAll(ITEMS.toArray(new Item[0]));
        for (Item item : new Item[]{PINK_CORAL_FRAG, RED_CORAL_FRAG, BLUE_CORAL_FRAG, YELLOW_CORAL_FRAG}) {
            OreDictionary.registerOre(CORAL_FRAGMENTS, item);
        }
        for (Item item : new Item[]{CORAL_HELMET, REGENERATION_CORAL_HELMET,
                STRENGTH_CORAL_HELMET, HASTE_CORAL_HELMET}) {
            OreDictionary.registerOre(CORAL_HELMETS, new ItemStack(item, 1, OreDictionary.WILDCARD_VALUE));
        }
    }
}
