package xy177.snowsbosses.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.PhantomShot;

public final class TridentRenderer<T extends PhantomShot> extends Render<T> {
    private final ResourceLocation texture;
    private final ModelRenderer model;

    public TridentRenderer(RenderManager manager, boolean red) {
        super(manager);
        texture = new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/phantom_trident" + (red ? "3" : "") + ".png");
        ModelBase base = new ModelBase() {};
        base.textureWidth = 128;
        base.textureHeight = 64;
        model = new ModelRenderer(base);
        model.setRotationPoint(0, 24, 0);
        model.setTextureOffset(64, 47).addBox(-0.5F, -9, -0.5F, 1, 16, 1);
        model.setTextureOffset(68, 62).addBox(-2.5F, -10, -0.5F, 5, 1, 1);
        model.setTextureOffset(84, 60).addBox(1.5F, -13, -0.5F, 1, 3, 1);
        model.setTextureOffset(80, 59).addBox(-0.5F, -14, -0.5F, 1, 4, 1);
        model.setTextureOffset(84, 60).addBox(-2.5F, -13, -0.5F, 1, 3, 1);
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTicks) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90, 0, 1, 0);
        GlStateManager.rotate(90 + entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks, 0, 0, 1);
        bindEntityTexture(entity);
        model.render(0.0625F);
        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return texture;
    }
}
