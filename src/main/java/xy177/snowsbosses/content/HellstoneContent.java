package xy177.snowsbosses.content;

import com.google.common.collect.Multimap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.crafting.IIngredientFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.client.HellstoneArmorModel;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class HellstoneContent {
    public static final SoundEvent MUSIC = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "hellstone_golem_music"))
            .setRegistryName(SnowsBosses.MOD_ID, "hellstone_golem_music");
    public static final Item RAW = ModItems.add("raw_hellstone", new Fireproof());
    public static final Item NUGGET = ModItems.add("hellstone_nugget", new Fireproof());
    public static final Item INGOT = ModItems.add("hellstone_ingot", new Fireproof());
    public static final Item TOTEM = ModItems.add("hellstone_totem", new Fireproof().setMaxStackSize(1));
    public static final Item MACHETE = ModItems.add("hellstone_machete", new Machete());
    public static final Item SMITHING_TEMPLATE = ModItems.add("hellstone_smithing_template", new Item());
    public static final Item HELMET = ModItems.add("hellstone_helmet", new Armor(EntityEquipmentSlot.HEAD));
    public static final Item CHESTPLATE = ModItems.add("hellstone_chestplate_chestplate", new Armor(EntityEquipmentSlot.CHEST));
    public static final Item LEGGINGS = ModItems.add("hellstone_leggings", new Armor(EntityEquipmentSlot.LEGS));
    public static final Item BOOTS = ModItems.add("hellstone_boots", new Armor(EntityEquipmentSlot.FEET));
    public static final Block BLOCK = new HotBlock("hellstone_block");
    public static final Block RAW_BLOCK = new HotBlock("raw_hellstone_block");

    private HellstoneContent() {
    }

    public static ItemStack ffd(String name) {
        ResourceLocation id = new ResourceLocation(name.indexOf(':') < 0 ? "farmers_future_delight:" + name : name);
        Item local = FFDItems.localBlockItem(id);
        if (id.getResourcePath().equals("netherite_ingot")) local = FFDItems.NETHERITE_INGOT;
        if (id.getResourcePath().equals("netherite_scrap")) local = FFDItems.NETHERITE_SCRAP;
        if (id.getResourcePath().equals("netherite_sword")) local = FFDItems.NETHERITE_SWORD;
        if (id.getResourcePath().equals("netherite_helmet")) local = FFDItems.NETHERITE_HELMET;
        if (id.getResourcePath().equals("netherite_chestplate")) local = FFDItems.NETHERITE_CHESTPLATE;
        if (id.getResourcePath().equals("netherite_leggings")) local = FFDItems.NETHERITE_LEGGINGS;
        if (id.getResourcePath().equals("netherite_boots")) local = FFDItems.NETHERITE_BOOTS;
        if (local == null) local = Item.REGISTRY.getObject(id);
        return local == null ? ItemStack.EMPTY : FFDItems.effectiveStack(local);
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(MUSIC);
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> event) {
        for (Block block : new Block[]{BLOCK, RAW_BLOCK}) {
            event.getRegistry().register(block);
            ModItems.add(block.getRegistryName().getResourcePath(), new net.minecraft.item.ItemBlock(block));
        }
    }

    public static final class FfdIngredientFactory implements IIngredientFactory {
        @Override
        public Ingredient parse(JsonContext context, JsonObject data) {
            ItemStack stack = ffd(data.get("item").getAsString());
            return stack.isEmpty() ? new Ingredient(new ItemStack[0]) {
                @Override public boolean apply(ItemStack input) { return false; }
                @Override public boolean isSimple() { return false; }
            } : Ingredient.fromStacks(stack);
        }
    }

    public static final class EffectiveItem extends LootFunction {
        private final String item;
        public EffectiveItem(LootCondition[] conditions, String item) { super(conditions); this.item = item; }
        @Override
        public ItemStack apply(ItemStack stack, Random random, LootContext context) {
            ItemStack resolved = ffd(item);
            if (!resolved.isEmpty()) resolved.setCount(stack.getCount());
            return resolved;
        }
        public static final class Serializer extends LootFunction.Serializer<EffectiveItem> {
            public Serializer() { super(new ResourceLocation(SnowsBosses.MOD_ID, "effective_ffd_item"), EffectiveItem.class); }
            @Override
            public void serialize(JsonObject data, EffectiveItem function, JsonSerializationContext context) { data.addProperty("item", function.item); }
            @Override
            public EffectiveItem deserialize(JsonObject data, JsonDeserializationContext context, LootCondition[] conditions) {
                return new EffectiveItem(conditions, data.get("item").getAsString());
            }
        }
    }

    public static class Fireproof extends Item {
        @Override
        public boolean hasCustomEntity(ItemStack stack) { return true; }
        @Override
        public Entity createEntity(World world, Entity original, ItemStack stack) { return new CoralTools.FireproofItem(world, original, stack); }
        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
            if (this == TOTEM) lines.add(I18n.translateToLocal(getUnlocalizedName() + ".description_0"));
        }
    }

    public static final class HotBlock extends Block {
        HotBlock(String name) {
            super(Material.ROCK);
            setRegistryName(SnowsBosses.MOD_ID, name);
            setUnlocalizedName(SnowsBosses.MOD_ID + "." + name);
            setCreativeTab(ModItems.TAB);
            setSoundType(SoundType.STONE);
            setHardness((float) Balance.number("hellstone.blocks.hardness", 20));
            setResistance((float) (Balance.number("hellstone.blocks.explosionResistance", 1200) * 5 / 3));
            setLightLevel(1);
            setHarvestLevel("pickaxe", Balance.integer("hellstone.blocks.harvestLevel", 3));
        }
        @Override
        public void onEntityWalk(World world, BlockPos pos, Entity entity) {
            if (!world.isRemote && entity instanceof EntityLivingBase) HellstoneEffects.blockContact((EntityLivingBase) entity);
            super.onEntityWalk(world, pos, entity);
        }
        @Override
        public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, net.minecraft.world.IBlockAccess access, BlockPos pos, net.minecraft.block.state.IBlockState state, int fortune) {
            if (!(access instanceof net.minecraft.world.WorldServer)) return;
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) access;
            drops.addAll(world.getLootTableManager().getLootTableFromLocation(new ResourceLocation(SnowsBosses.MOD_ID, "blocks/" + getRegistryName().getResourcePath()))
                    .generateLootForPools(world.rand, new LootContext.Builder(world).build()));
        }
    }

    public static final class Machete extends ItemSword {
        public Machete() {
            super(EnumHelper.addToolMaterial("SNOWS_BOSSES_HELLSTONE", 4, Balance.ticks("hellstone.machete.durability", 2000),
                    0, 0, Balance.integer("hellstone.machete.enchantability", 22)));
        }
        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
            if (slot == EntityEquipmentSlot.MAINHAND) {
                map.removeAll(SharedMonsterAttributes.ATTACK_DAMAGE.getName());
                map.removeAll(SharedMonsterAttributes.ATTACK_SPEED.getName());
                map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", Balance.number("hellstone.machete.attackDamage", 9), 0));
                map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", Balance.number("hellstone.machete.attackSpeed", 1.1) - 4, 0));
            }
            return map;
        }
        @Override
        public boolean getIsRepairable(ItemStack stack, ItemStack repair) { return repair.getItem() == INGOT; }
        @Override
        public boolean hasCustomEntity(ItemStack stack) { return true; }
        @Override
        public Entity createEntity(World world, Entity original, ItemStack stack) { return new CoralTools.FireproofItem(world, original, stack); }
        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) { lines.add(I18n.translateToLocal(getUnlocalizedName() + ".description_0")); }
    }

    public static final class Armor extends ItemArmor {
        private static final String[] PARTS = {"boots", "leggings", "chestplate", "helmet"};
        private static final int[] DEFENSE = {4, 7, 9, 4};
        private static final int[] DURABILITY = {500, 600, 650, 450};
        private static final UUID[] RESISTANCE = {
                UUID.fromString("179b208f-6f17-434a-ab69-1f0762825bc8"),
                UUID.fromString("663ad374-76ce-4b41-a011-aab8281a21fb"),
                UUID.fromString("e13e7c71-4e06-4860-9807-124f6420f44b"),
                UUID.fromString("2dcb681b-0b0d-4a36-a195-4e8055899771")};

        Armor(EntityEquipmentSlot slot) {
            super(material(slot), 0, slot);
            setMaxDamage(Balance.ticks(key(slot) + "durability", DURABILITY[slot.getIndex()]));
        }

        private static String key(EntityEquipmentSlot slot) {
            return "hellstone." + PARTS[slot.getIndex()] + ".";
        }

        private static ArmorMaterial material(EntityEquipmentSlot slot) {
            int[] defense = new int[4];
            defense[slot.getIndex()] = Balance.integer(key(slot) + "armor", DEFENSE[slot.getIndex()]);
            return EnumHelper.addArmorMaterial("SNOWS_BOSSES_HELLSTONE_" + slot.name(), "snows_bosses:hellstone", 40,
                    defense, Balance.integer(key(slot) + "enchantability", 23),
                    xy177.farmersfuturedelight.common.registry.FFDSounds.NETHERITE_ARMOR_EQUIP,
                    (float) Balance.number(key(slot) + "toughness", 2));
        }

        @Override
        public boolean onEntityItemUpdate(net.minecraft.entity.item.EntityItem entity) {
            ItemStack stack = entity.getItem();
            if (!entity.world.isRemote && stack.isItemDamaged() && entity.isInLava()
                    && entity.ticksExisted % HellstoneEffects.t("chestplate.lavaInterval", 10) == 0) {
                stack.setItemDamage(Math.max(0, stack.getItemDamage() - HellstoneEffects.t("chestplate.lavaRepair", 1)));
            }
            return false;
        }
        @Override
        public boolean getIsRepairable(ItemStack stack, ItemStack repair) { return false; }
        @Override
        public Multimap<String, AttributeModifier> getItemAttributeModifiers(EntityEquipmentSlot slot) {
            Multimap<String, AttributeModifier> map = super.getItemAttributeModifiers(slot);
            if (slot == armorType) map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(),
                    new AttributeModifier(RESISTANCE[slot.getIndex()], "Hellstone armor resistance",
                            Balance.number(key(slot) + "knockbackResistance", 0.2), 0));
            return map;
        }
        @Override
        public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type) { return "snows_bosses:textures/entities/hellstone_armor.png"; }
        @SideOnly(Side.CLIENT)
        @Override
        public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped base) { return HellstoneArmorModel.get(slot); }
        @Override
        public boolean hasCustomEntity(ItemStack stack) { return true; }
        @Override
        public Entity createEntity(World world, Entity original, ItemStack stack) { return new CoralTools.FireproofItem(world, original, stack); }
        @SideOnly(Side.CLIENT)
        @Override
        public void addInformation(ItemStack stack, World world, List<String> lines, ITooltipFlag flag) {
            lines.add(I18n.translateToLocal("item.snows_bosses.hellstone_chestplate_chestplate.description_1"));
            if (armorType == EntityEquipmentSlot.CHEST) lines.add(I18n.translateToLocal("item.snows_bosses.hellstone_chestplate_chestplate.description_2"));
            if (armorType != EntityEquipmentSlot.CHEST) lines.add(I18n.translateToLocal(getUnlocalizedName() + ".description_0"));
        }
    }
}
