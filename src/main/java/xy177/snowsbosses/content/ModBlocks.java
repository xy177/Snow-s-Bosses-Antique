package xy177.snowsbosses.content;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import xy177.farmersfuturedelight.api.WaterloggedBlockApi;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class ModBlocks {
    public static final OceanAltar OCEAN_ALTAR = new OceanAltar();

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<Block> event) {
        OCEAN_ALTAR.setRegistryName(SnowsBosses.MOD_ID, "ocean_altar");
        OCEAN_ALTAR.setUnlocalizedName(SnowsBosses.MOD_ID + ".ocean_altar");
        OCEAN_ALTAR.setCreativeTab(ModItems.TAB);
        event.getRegistry().register(OCEAN_ALTAR);
        ModItems.add("ocean_altar", new ItemBlock(OCEAN_ALTAR));
        GameRegistry.registerTileEntity(OceanAltar.AltarTile.class, OCEAN_ALTAR.getRegistryName());
        WaterloggedBlockApi.registerRenderedBlock(OCEAN_ALTAR);
    }
}
