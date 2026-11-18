package xy177.snowsbosses.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.ModItems;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID, value = Side.CLIENT)
public final class ClientRegistration {
    private ClientRegistration() {
    }

    @SubscribeEvent
    public static void attack(net.minecraftforge.client.event.MouseEvent event) {
        if (event.getButton() != 0 || !event.isButtonstate()) return;
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getMinecraft();
        if (client.player == null || client.currentScreen != null || client.player.isSpectator()
                || client.player.getHeldItemMainhand().getItem() != xy177.snowsbosses.content.HellstoneContent.MACHETE
                || client.objectMouseOver != null && client.objectMouseOver.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.ENTITY) return;
        net.minecraft.entity.Entity target = xy177.snowsbosses.network.HellstoneReach.pick(client.player);
        if (target != null) {
            event.setCanceled(true);
            xy177.snowsbosses.network.HellstoneReach.attack(target);
        }
    }

    @SubscribeEvent
    public static void targetTool(net.minecraftforge.client.event.MouseEvent event) {
        if (!event.isButtonstate() || event.getButton() != 0 && event.getButton() != 1) return;
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getMinecraft();
        if (client.player == null || client.currentScreen != null || client.player.isSpectator()
                || client.player.getHeldItemMainhand().getItem() != ModItems.TECHNICAL_TARGET_TOOL) return;
        event.setCanceled(true);
        xy177.snowsbosses.network.TechnicalTargetControl.use(event.getButton() == 1, client.player.isSneaking());
        client.player.swingArm(net.minecraft.util.EnumHand.MAIN_HAND);
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        for (Item item : ModItems.all()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
            if (item == ModItems.INFERNAL_EYE || item == ModItems.TIDE_EYE || item == ModItems.DROWNER_HOWL_EYE)
                item.setTileEntityItemStackRenderer(new ColoredGlintRenderer());
        }
    }

    @SubscribeEvent
    public static void particles(TextureStitchEvent.Pre event) {
        event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/mechasent_particle"));
        event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/mechasent_particle_2"));
        for (String name : new String[]{"hellstone_fire", "hellstone_fire_1", "hellstone_fire_2", "hellstone_explode",
                "hellstone_explode_1", "hellstone_explode_2", "hellstone_explode_3", "hellstone_explode_4",
                "hellstone_explode_5", "hellstone_explode_6", "hellstone_explode_7", "angry"})
            event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/" + name));
        event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/piranha_water"));
        for (int frame = 1; frame <= 6; frame++) {
            event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/trident_p_" + frame));
            event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/trident_red_" + frame));
        }
        for (int frame = 0; frame <= 10; frame++) {
            event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/soul_" + frame));
            if (frame < 8) event.getMap().registerSprite(new ResourceLocation(SnowsBosses.MOD_ID, "particle/spark_" + frame));
        }
    }
}
