package xy177.snowsbosses.compat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.gui.ingredients.CycleTimer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import xy177.farmersfuturedelight.compat.jei.FFDJeiRecipe;
import xy177.snowsbosses.content.MechasentContent;
import xy177.snowsbosses.content.MechasentModules;

@JEIPlugin
public final class MechasentJeiPlugin implements IModPlugin {
    private static final CycleTimer TIMER = new CycleTimer(0);
    private static final List<Integer> COMBINATIONS = Arrays.asList(0, 1, 2, 3);
    private static final List<ItemStack> BASES = new ArrayList<>();
    private static final List<ItemStack> ADDITIONS = new ArrayList<>();
    private static final List<ItemStack> RESULTS = new ArrayList<>();
    private static FFDJeiRecipe upgradeRecipe;
    private static IRecipeLayout lastLayout;
    private static int lastCombination = -1;

    @Override
    public void register(IModRegistry registry) {
        BASES.clear();
        ADDITIONS.clear();
        RESULTS.clear();
        for (MechasentModules addition : new MechasentModules[]{MechasentModules.PACEMAKER, MechasentModules.HELLSTONE_TOTEM}) {
            for (MechasentModules first : new MechasentModules[]{MechasentModules.PACEMAKER, MechasentModules.HELLSTONE_TOTEM}) {
                ItemStack base = MechasentModules.install(new ItemStack(MechasentContent.CHESTPLATE), first);
                BASES.add(base);
                ADDITIONS.add(addition.ingredient());
                RESULTS.add(MechasentModules.install(base, addition));
            }
        }
        upgradeRecipe = new FFDJeiRecipe();
        List<List<ItemStack>> inputs = ObfuscationReflectionHelper.getPrivateValue(FFDJeiRecipe.class, upgradeRecipe, "inputs");
        List<List<ItemStack>> outputs = ObfuscationReflectionHelper.getPrivateValue(FFDJeiRecipe.class, upgradeRecipe, "outputs");
        inputs.addAll(Arrays.asList(Collections.singletonList(new ItemStack(Items.EMERALD)), BASES, ADDITIONS));
        outputs.add(RESULTS);
        lastLayout = null;
        registry.addRecipes(Collections.singletonList(upgradeRecipe), "farmers_future_delight:smithing");
    }

    public static void synchronize(IRecipeLayout layout, IRecipeWrapper recipe) {
        if (recipe != upgradeRecipe) return;
        TIMER.onDraw();
        int combination = TIMER.getCycledItem(COMBINATIONS);
        if (layout == lastLayout && combination == lastCombination) return;
        IGuiItemStackGroup stacks = layout.getItemStacks();
        stacks.setOverrideDisplayFocus(null);
        stacks.set(1, BASES.get(combination));
        stacks.set(2, ADDITIONS.get(combination));
        stacks.set(3, RESULTS.get(combination));
        lastLayout = layout;
        lastCombination = combination;
    }
}
