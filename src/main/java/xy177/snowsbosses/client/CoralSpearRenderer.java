package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.entity.CoralSpearShot;

public final class CoralSpearRenderer extends Render<CoralSpearShot> {
    public CoralSpearRenderer(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(CoralSpearShot entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90, 0, 1, 0);
        GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks - 45, 0, 0, 1);
        GlStateManager.scale(1.7F, 1.7F, 0.85F);
        bindEntityTexture(entity);
        Minecraft.getMinecraft().getRenderItem().renderItem(entity.getTridentStack(), ItemCameraTransforms.TransformType.NONE);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(CoralSpearShot entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
