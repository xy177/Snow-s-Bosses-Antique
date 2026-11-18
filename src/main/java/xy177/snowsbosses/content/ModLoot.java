package xy177.snowsbosses.content;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootTableList;
import xy177.snowsbosses.SnowsBosses;

public final class ModLoot {
    private static final String[] TABLES = {
            "blocks/ocean_altar",
            "chests/tide_raiser_tower",
            "entities/copper_golem",
            "entities/deepwater_piranha",
            "entities/drowner",
            "entities/tide_raiser",
            "entities/hellstone_golem",
            "entities/hellstone_imp",
            "entities/mechasent",
            "blocks/hellstone_block",
            "blocks/raw_hellstone_block"
    };

    private ModLoot() {
    }

    public static void register() {
        net.minecraft.world.storage.loot.functions.LootFunctionManager.registerFunction(new HellstoneContent.EffectiveItem.Serializer());
        for (String table : TABLES) LootTableList.register(new ResourceLocation(SnowsBosses.MOD_ID, table));
    }
}
