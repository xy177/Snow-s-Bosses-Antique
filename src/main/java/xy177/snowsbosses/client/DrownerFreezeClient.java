package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.DrownerCold;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID, value = Side.CLIENT)
public final class DrownerFreezeClient {
    private DrownerFreezeClient() {
    }

    public static void setCold(int entityId, int ticks) {
        EntityPlayer player = player(entityId);
        if (player != null) player.getEntityData().setInteger(DrownerCold.COLD_TAG, ticks);
    }

    public static void start(int entityId, int ticks) {
        EntityPlayer player = player(entityId);
        if (player != null) player.getEntityData().setInteger(DrownerCold.FREEZE_TICKS_TAG, ticks);
    }

    public static void thaw(int entityId) {
        EntityPlayer player = player(entityId);
        if (player != null) {
            player.getEntityData().setInteger(DrownerCold.COLD_TAG, 0);
            player.getEntityData().setInteger(DrownerCold.FREEZE_TICKS_TAG, 0);
        }
    }

    private static EntityPlayer player(int entityId) {
        if (Minecraft.getMinecraft().world == null) return null;
        net.minecraft.entity.Entity entity = Minecraft.getMinecraft().world.getEntityByID(entityId);
        return entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
    }

    private static boolean frozen(EntityPlayer player) {
        return player != null && player.getEntityData().getInteger(DrownerCold.FREEZE_TICKS_TAG) > 0;
    }

    @SubscribeEvent
    public static void openGui(GuiOpenEvent event) {
        if (frozen(Minecraft.getMinecraft().player) && event.getGui() instanceof GuiContainer) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void input(InputUpdateEvent event) {
        if (!frozen(event.getEntityPlayer())) return;
        event.getMovementInput().moveForward = 0;
        event.getMovementInput().moveStrafe = 0;
        event.getMovementInput().jump = false;
        event.getMovementInput().sneak = false;
    }

    @SubscribeEvent
    public static void mouse(MouseEvent event) {
        if (frozen(Minecraft.getMinecraft().player)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void key(InputEvent.KeyInputEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        if (frozen(client.player)) releaseKeys(client);
    }

    private static void releaseKeys(Minecraft client) {
        release(client.gameSettings.keyBindAttack);
        release(client.gameSettings.keyBindUseItem);
        release(client.gameSettings.keyBindForward);
        release(client.gameSettings.keyBindBack);
        release(client.gameSettings.keyBindLeft);
        release(client.gameSettings.keyBindRight);
        release(client.gameSettings.keyBindJump);
        release(client.gameSettings.keyBindSneak);
        release(client.gameSettings.keyBindSprint);
        release(client.gameSettings.keyBindInventory);
        release(client.gameSettings.keyBindDrop);
        release(client.gameSettings.keyBindSwapHands);
        release(client.gameSettings.keyBindPickBlock);
        release(client.gameSettings.keyBindLoadToolbar);
        release(client.gameSettings.keyBindSaveToolbar);
        for (KeyBinding binding : client.gameSettings.keyBindsHotbar) release(binding);
    }

    private static void release(KeyBinding binding) {
        KeyBinding.setKeyBindState(binding.getKeyCode(), false);
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null) return;
        if (frozen(client.player)) releaseKeys(client);
        for (EntityPlayer player : client.world.playerEntities) {
            if (frozen(player)) player.getEntityData().setInteger(DrownerCold.COLD_TAG, 140);
            int ticks = player.getEntityData().getInteger(DrownerCold.FREEZE_TICKS_TAG);
            if (ticks > 0) player.getEntityData().setInteger(DrownerCold.FREEZE_TICKS_TAG, ticks - 1);
        }
        if (frozen(client.player) && client.currentScreen instanceof GuiContainer) client.displayGuiScreen((GuiScreen) null);
    }

    @SubscribeEvent
    public static void renderIce(RenderWorldLastEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        if (client.world == null || client.getRenderViewEntity() == null) return;
        TextureAtlasSprite sprite = client.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.ICE.getDefaultState());
        client.getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (EntityPlayer player : client.world.playerEntities) {
            if (!frozen(player)) continue;
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks()
                    - client.getRenderManager().viewerPosX;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.getPartialTicks()
                    - client.getRenderManager().viewerPosY;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks()
                    - client.getRenderManager().viewerPosZ;
            drawIce(buffer, sprite, x, y, z);
        }
        Tessellator.getInstance().draw();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawIce(BufferBuilder buffer, TextureAtlasSprite sprite, double x, double y, double z) {
        double x0 = x - 0.55, x1 = x + 0.55;
        double y0 = y, y1 = y + 2.0;
        double z0 = z - 0.55, z1 = z + 0.55;
        float u0 = sprite.getMinU(), u1 = sprite.getMaxU();
        float v0 = sprite.getMinV(), v1 = sprite.getMaxV();
        iceFace(buffer, x0, y0, z1, u0, v1, x1, y0, z1, u1, v1, x1, y1, z1, u1, v0, x0, y1, z1, u0, v0);
        iceFace(buffer, x1, y0, z0, u0, v1, x0, y0, z0, u1, v1, x0, y1, z0, u1, v0, x1, y1, z0, u0, v0);
        iceFace(buffer, x0, y0, z0, u0, v1, x0, y0, z1, u1, v1, x0, y1, z1, u1, v0, x0, y1, z0, u0, v0);
        iceFace(buffer, x1, y0, z1, u0, v1, x1, y0, z0, u1, v1, x1, y1, z0, u1, v0, x1, y1, z1, u0, v0);
        iceFace(buffer, x0, y1, z1, u0, v1, x1, y1, z1, u1, v1, x1, y1, z0, u1, v0, x0, y1, z0, u0, v0);
        iceFace(buffer, x0, y0, z0, u0, v1, x1, y0, z0, u1, v1, x1, y0, z1, u1, v0, x0, y0, z1, u0, v0);
    }

    private static void iceFace(BufferBuilder buffer,
            double x0, double y0, double z0, float u0, float v0,
            double x1, double y1, double z1, float u1, float v1,
            double x2, double y2, double z2, float u2, float v2,
            double x3, double y3, double z3, float u3, float v3) {
        iceVertex(buffer, x0, y0, z0, u0, v0);
        iceVertex(buffer, x1, y1, z1, u1, v1);
        iceVertex(buffer, x2, y2, z2, u2, v2);
        iceVertex(buffer, x3, y3, z3, u3, v3);
    }

    private static void iceVertex(BufferBuilder buffer, double x, double y, double z, float u, float v) {
        buffer.pos(x, y, z).tex(u, v).color(180, 225, 255, 112).endVertex();
    }
}
