package xy177.snowsbosses.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xy177.snowsbosses.entity.HellstonePillar;

public final class HellstonePillarRenderer extends Render<HellstonePillar> {
    private static final ResourceLocation SIDE = new ResourceLocation("snows_bosses", "textures/entities/hellstone_pillar_side.png");
    private static final ResourceLocation TOP = new ResourceLocation("snows_bosses", "textures/entities/hellstone_pillar_top.png");
    private static final ResourceLocation RING = new ResourceLocation("snows_bosses", "textures/particle/mechasent_particle_2.png");

    public HellstonePillarRenderer(RenderManager manager) { super(manager); shadowSize = 0; }

    @Override
    public void doRender(HellstonePillar entity, double x, double y, double z, float yaw, float partialTicks) {
        float visible = entity.emergence(partialTicks);
        if (visible <= 0) return;
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);
            GlStateManager.disableLighting();
            GlStateManager.disableBlend();
            GlStateManager.disableCull();
            double offset = (visible - 1) * 2;
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            bindTexture(SIDE);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (int level = 0; level < 2; level++) {
                double bottom = offset + level, top = bottom + 1;
                if (top <= 0) continue;
                double start = Math.max(0, bottom);
                float lowerV = (float) (1 - (start - bottom));
                face(buffer, 0.82F, 1, lowerV, 0, -0.5, start, 0.5, 0.5, start, 0.5, 0.5, top, 0.5, -0.5, top, 0.5);
                face(buffer, 0.82F, 1, lowerV, 0, 0.5, start, -0.5, -0.5, start, -0.5, -0.5, top, -0.5, 0.5, top, -0.5);
                face(buffer, 0.72F, 1, lowerV, 0, -0.5, start, -0.5, -0.5, start, 0.5, -0.5, top, 0.5, -0.5, top, -0.5);
                face(buffer, 0.9F, 1, lowerV, 0, 0.5, start, 0.5, 0.5, start, -0.5, 0.5, top, -0.5, 0.5, top, 0.5);
            }
            Tessellator.getInstance().draw();
            bindTexture(TOP);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            face(buffer, 1, 1, 1, 0, -0.5, offset + 2, 0.5, 0.5, offset + 2, 0.5, 0.5, offset + 2, -0.5, -0.5, offset + 2, -0.5);
            face(buffer, 0.55F, 1, 1, 0, -0.5, 0, -0.5, 0.5, 0, -0.5, 0.5, 0, 0.5, -0.5, 0, 0.5);
            Tessellator.getInstance().draw();
            float progress = entity.ringProgress(partialTicks);
            if (progress >= 0) {
                float eased = progress * progress * (3 - 2 * progress);
                double radius = 0.8 * eased;
                float alpha = 1 - progress * eased;
                bindTexture(RING);
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.depthMask(false);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (int level = 0; level < 2; level++) {
                    for (EnumFacing side : EnumFacing.HORIZONTALS) {
                        double cx = side.getFrontOffsetX(), cy = level + 0.5, cz = side.getFrontOffsetZ();
                        double ux = side.getFrontOffsetZ() * radius, uz = -side.getFrontOffsetX() * radius;
                        face(buffer, 1, alpha, 1, 0, cx - ux, cy - radius, cz - uz, cx + ux, cy - radius, cz + uz,
                                cx + ux, cy + radius, cz + uz, cx - ux, cy + radius, cz - uz);
                    }
                }
                Tessellator.getInstance().draw();
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.depthMask(depthMask);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    private static void face(BufferBuilder buffer, float shade, float alpha, float bottomV, float topV,
            double x0, double y0, double z0, double x1, double y1, double z1,
            double x2, double y2, double z2, double x3, double y3, double z3) {
        buffer.pos(x0, y0, z0).tex(0, bottomV).color(shade, shade, shade, alpha).endVertex();
        buffer.pos(x1, y1, z1).tex(1, bottomV).color(shade, shade, shade, alpha).endVertex();
        buffer.pos(x2, y2, z2).tex(1, topV).color(shade, shade, shade, alpha).endVertex();
        buffer.pos(x3, y3, z3).tex(0, topV).color(shade, shade, shade, alpha).endVertex();
    }

    @Override
    protected ResourceLocation getEntityTexture(HellstonePillar entity) { return SIDE; }
}
