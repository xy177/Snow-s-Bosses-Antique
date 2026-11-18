package xy177.snowsbosses.content;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.api.WorkstationRecipes;
import xy177.farmersfuturedelight.api.WorkstationRegistryEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public enum MechasentModules {
    NIGHT_VISION(EntityEquipmentSlot.HEAD, "golden_carrot"),
    GAZE_SHIELD(EntityEquipmentSlot.HEAD, "farmers_future_delight:carved_pumpkin"),
    RADAR(EntityEquipmentSlot.HEAD, "spectral_arrow"),
    FEEDING(EntityEquipmentSlot.HEAD, "bowl"),
    TRACKING(EntityEquipmentSlot.CHEST, "farmers_future_delight:lodestone"),
    PACEMAKER(EntityEquipmentSlot.CHEST, "totem_of_undying"),
    HELLSTONE_TOTEM(EntityEquipmentSlot.CHEST, "snows_bosses:hellstone_totem"),
    ARM_EXOSKELETON(EntityEquipmentSlot.CHEST, "snows_bosses:mechasent_remnant"),
    SAFE_WORK(EntityEquipmentSlot.LEGS, "farmers_future_delight:scaffolding"),
    FINAL_PROTOCOL(EntityEquipmentSlot.LEGS, "ender_pearl"),
    EARTH_HAMMER(EntityEquipmentSlot.LEGS, "piston"),
    METABOLISM(EntityEquipmentSlot.LEGS, "milk_bucket"),
    LEG_EXOSKELETON(EntityEquipmentSlot.FEET, "web"),
    LUCKY_HORSESHOE(EntityEquipmentSlot.FEET, "gold_ingot"),
    HERO_FOOTSTEPS(EntityEquipmentSlot.FEET, "writable_book"),
    WORLD_WALKER(EntityEquipmentSlot.FEET, "ice");

    public static final String SLOTS = "MechasentModules";
    public final EntityEquipmentSlot slot;
    private final String ingredient;

    MechasentModules(EntityEquipmentSlot slot, String ingredient) { this.slot = slot; this.ingredient = ingredient; }
    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean totem() { return this == PACEMAKER || this == HELLSTONE_TOTEM; }
    public String label(boolean upgraded) { return "module.snows_bosses." + id() + (upgraded ? ".upgraded" : ""); }

    public boolean worn(EntityLivingBase entity) { return has(entity.getItemStackFromSlot(slot), this); }

    public static MechasentModules get(ItemStack stack, int index) {
        if (stack.isEmpty() || !(stack.getItem() instanceof MechasentContent.MechasentArmor) || !stack.hasTagCompound()) return null;
        String id = stack.getTagCompound().getCompoundTag(SLOTS).getString(index == 0 ? "A" : "B");
        for (MechasentModules module : values()) if (module.id().equals(id)
                && module.slot == ((MechasentContent.MechasentArmor) stack.getItem()).equipmentSlot()) return module;
        return null;
    }

    public static boolean has(ItemStack stack, MechasentModules module) { return get(stack,0) == module || get(stack,1) == module; }
    public static boolean upgraded(ItemStack stack) {
        MechasentModules a = get(stack,0), b = get(stack,1);
        return a != null && b != null && a.totem() && b.totem();
    }

    public static NBTTagCompound slots(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        if (!stack.getTagCompound().hasKey(SLOTS,10)) stack.getTagCompound().setTag(SLOTS,new NBTTagCompound());
        return stack.getTagCompound().getCompoundTag(SLOTS);
    }

    public static boolean canInstall(ItemStack stack, MechasentModules module) {
        return !stack.isEmpty() && stack.getItem() instanceof MechasentContent.MechasentArmor
                && ((MechasentContent.MechasentArmor) stack.getItem()).equipmentSlot() == module.slot
                && (get(stack,0) == null || get(stack,1) == null) && (module.totem() || !has(stack,module));
    }

    public static ItemStack install(ItemStack stack, MechasentModules module) {
        if (!canInstall(stack,module)) return ItemStack.EMPTY;
        ItemStack result = stack.copy(); result.setCount(1);
        int index = get(result,0) == null ? 0 : 1;
        NBTTagCompound data = slots(result); data.setString(index == 0 ? "A" : "B",module.id());
        data.removeTag("Cooldown" + index);
        return result;
    }

    public static ItemStack clear(ItemStack stack) {
        ItemStack result = stack.copy(); result.setCount(1);
        if (result.hasTagCompound()) result.getTagCompound().removeTag(SLOTS);
        return result;
    }

    public static Item armor(EntityEquipmentSlot slot) {
        return slot == EntityEquipmentSlot.HEAD ? MechasentContent.HELMET : slot == EntityEquipmentSlot.CHEST ? MechasentContent.CHESTPLATE
                : slot == EntityEquipmentSlot.LEGS ? MechasentContent.LEGGINGS : MechasentContent.BOOTS;
    }

    private static Ingredient predicate(ItemStack example, Predicate<ItemStack> predicate) {
        return new Ingredient(example) {
            @Override public boolean apply(ItemStack stack) { return stack != null && !stack.isEmpty() && predicate.test(stack); }
            @Override public boolean isSimple() { return false; }
        };
    }

    public ItemStack ingredient() {
        ItemStack result = ingredient.indexOf(':') >= 0 && ingredient.startsWith("farmers_future_delight:")
                ? HellstoneContent.ffd(ingredient) : MechasentContent.item(ingredient.indexOf(':') < 0 ? "minecraft:"+ingredient : ingredient);
        if (result.isEmpty() && this == TRACKING) result = new ItemStack(net.minecraft.init.Items.COMPASS);
        if (result.isEmpty() && this == GAZE_SHIELD) result = new ItemStack(net.minecraft.init.Blocks.PUMPKIN);
        return result;
    }

    @SubscribeEvent
    public static void recipes(WorkstationRegistryEvent event) {
        for (MechasentModules module : values()) {
            ItemStack addition = module.ingredient();
            if (addition.isEmpty()) continue;
            ItemStack empty = new ItemStack(armor(module.slot));
            register(module.id()+"_a", predicate(empty,s -> canInstall(s,module)), Ingredient.fromStacks(addition),
                    inputs -> install(inputs.get(1),module));
        }
        for (EntityEquipmentSlot slot : new EntityEquipmentSlot[]{EntityEquipmentSlot.HEAD,EntityEquipmentSlot.CHEST,EntityEquipmentSlot.LEGS,EntityEquipmentSlot.FEET}) {
            ItemStack example = new ItemStack(armor(slot));
            for (MechasentModules module : values()) if (module.slot == slot) { example = install(example,module); break; }
            register("clear_"+slot.name().toLowerCase(java.util.Locale.ROOT),predicate(example,s -> s.getItem()==armor(slot) && (get(s,0)!=null || get(s,1)!=null)),
                    Ingredient.fromStacks(new ItemStack(net.minecraft.init.Blocks.SPONGE,1,1)), inputs -> clear(inputs.get(1)));
        }
    }

    private static void register(String id, Ingredient armor, Ingredient addition, WorkstationRecipes.Assembler assembler) {
        WorkstationRecipes.register(WorkstationRecipes.Type.SMITHING,new ResourceLocation(SnowsBosses.MOD_ID,"module_"+id),
                Arrays.asList(Ingredient.fromItem(net.minecraft.init.Items.EMERALD),armor,addition),assembler,1,0);
    }
}
