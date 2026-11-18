package xy177.snowsbosses.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.BossInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

final class BossBars extends Gui {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/bars.png");
    private static final Map<UUID, Integer> BARS = new HashMap<>();

    static void register() {
        MinecraftForge.EVENT_BUS.register(new BossBars());
    }

    static void update(UUID id, boolean visible, int color) {
        if (visible && color >= 0) BARS.put(id, color);
        else BARS.remove(id);
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) BARS.clear();
    }

    @SubscribeEvent
    public void render(RenderGameOverlayEvent.BossInfo event) {
        BossInfo info = event.getBossInfo();
        Integer color = BARS.get(info.getUniqueId());
        if (color == null) return;
        event.setCanceled(true);
        Minecraft client = Minecraft.getMinecraft();
        int x = event.getX(), y = event.getY();
        int row = BossInfo.Color.WHITE.ordinal() * 10;
        int progress = (int) (info.getPercent() * 183.0F);
        client.getTextureManager().bindTexture(TEXTURE);
        GlStateManager.color((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F, 1.0F);
        drawTexturedModalRect(x, y, 0, row, 182, 5);
        if (progress > 0) drawTexturedModalRect(x, y, 0, row + 5, progress, 5);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (info.getOverlay() != BossInfo.Overlay.PROGRESS) {
            int overlay = 80 + (info.getOverlay().ordinal() - 1) * 10;
            drawTexturedModalRect(x, y, 0, overlay, 182, 5);
            if (progress > 0) drawTexturedModalRect(x, y, 0, overlay + 5, progress, 5);
        }
        String name = info.getName().getFormattedText();
        client.fontRenderer.drawStringWithShadow(name, x + 91 - client.fontRenderer.getStringWidth(name) / 2, y - 9, 0xFFFFFF);
    }
}
