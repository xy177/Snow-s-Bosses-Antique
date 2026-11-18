package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import xy177.snowsbosses.content.ModItems;

public final class ColoredGlintRenderer extends TileEntityItemStackRenderer {
    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    @Override
    public void renderByItem(ItemStack stack) {
        Item item = stack.getItem();
        IBakedModel model = ColoredGlintModel.base(item);
        if (model == null) return;
        render(model, stack, -1);
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(514);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        Minecraft.getMinecraft().getTextureManager().bindTexture(GLINT);
        GlStateManager.matrixMode(5890);
        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float first = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
        GlStateManager.translate(first, 0.0F, 0.0F);
        GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
        render(model, stack, color(item));
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float second = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
        GlStateManager.translate(-second, 0.0F, 0.0F);
        GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
        render(model, stack, color(item));
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(5888);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableLighting();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
    }

    private void render(IBakedModel model, ItemStack stack, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.ITEM);
        for (EnumFacing facing : EnumFacing.values()) renderQuads(buffer, model.getQuads(null, facing, 0L), color, stack);
        renderQuads(buffer, model.getQuads(null, null, 0L), color, stack);
        tessellator.draw();
    }

    private void renderQuads(BufferBuilder buffer, List<BakedQuad> quads, int color, ItemStack stack) {
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        if (renderItem != null && quads != null && !quads.isEmpty()) renderItem.renderQuads(buffer, quads, color, stack);
    }

    private static int color(Item item) {
        if (item == ModItems.TIDE_EYE) return 0xFF367EFF;
        if (item == ModItems.DROWNER_HOWL_EYE) return 0xFF22E0E8;
        return 0xFFFF4C18;
    }
}
