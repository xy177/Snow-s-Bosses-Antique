package xy177.snowsbosses.content;

import net.minecraft.entity.EntityList;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.SnowsBosses;

public final class SpawnEgg {
    private SpawnEgg() {
    }

    public static void register(String name, int primaryColor, int secondaryColor) {
        EntityList.ENTITY_EGGS.put(new ResourceLocation(SnowsBosses.MOD_ID, name),
                new EntityList.EntityEggInfo(new ResourceLocation(SnowsBosses.MOD_ID, name), primaryColor, secondaryColor));
    }

    public static ItemStack stack(String name) {
        ItemStack stack = new ItemStack(Items.SPAWN_EGG);
        ItemMonsterPlacer.applyEntityIdToItemStack(stack, new ResourceLocation(SnowsBosses.MOD_ID, name));
        return stack;
    }
}
