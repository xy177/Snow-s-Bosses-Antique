package xy177.snowsbosses;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import software.bernie.geckolib3.GeckoLib;
import xy177.snowsbosses.content.AdvancementCraftEvents;
import xy177.snowsbosses.content.ModEntities;
import xy177.snowsbosses.content.ModLoot;
import xy177.snowsbosses.network.Visuals;
import xy177.snowsbosses.world.OceanStructures;
import xy177.snowsbosses.world.LocateCommand;

@Mod(
    modid = SnowsBosses.MOD_ID,
    name = SnowsBosses.NAME,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:farmers_future_delight;required-after:geckolib3",
    useMetadata = true
)
public final class SnowsBosses {
    public static final String MOD_ID = "snows_bosses";
    public static final String NAME = "Snow's Bosses:Antique";
    @SidedProxy(clientSide = "xy177.snowsbosses.client.ClientProxy", serverSide = "xy177.snowsbosses.CommonProxy")
    public static CommonProxy PROXY;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new AdvancementCraftEvents());
        Balance.load(event.getSuggestedConfigurationFile());
        GeckoLib.initialize();
        Visuals.register();
        xy177.snowsbosses.network.MechasentControls.register();
        xy177.snowsbosses.network.HellstoneReach.register();
        xy177.snowsbosses.network.TechnicalTargetControl.register();
        ModEntities.register(this);
        ModLoot.register();
        PROXY.registerRenderers();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        GameRegistry.registerWorldGenerator(new OceanStructures(), 0);
        GameRegistry.registerWorldGenerator(new xy177.snowsbosses.world.HellstoneStructures(), 0);
        net.minecraftforge.fml.common.registry.EntityRegistry.addSpawn(
                xy177.snowsbosses.entity.HellstoneImp.class, 16,
                1,
                2,
                net.minecraft.entity.EnumCreatureType.MONSTER, net.minecraft.init.Biomes.HELL);
    }

    @Mod.EventHandler
    public void stopped(net.minecraftforge.fml.common.event.FMLServerStoppedEvent event) { xy177.snowsbosses.content.MechasentDeferred.clear(); xy177.snowsbosses.content.MechasentTracking.clear(); }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new LocateCommand());
    }
}
