package xy177.snowsbosses.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xy177.snowsbosses.entity.HellstonePillarShot;

public final class HellstonePillarShotRenderer extends Render<HellstonePillarShot> {
    private static final ResourceLocation TRAIL = new ResourceLocation("snows_bosses", "textures/effects/hellstone_ember_trail.png");
    private static final ResourceLocation SPARK = new ResourceLocation("snows_bosses", "textures/effects/hellstone_ember_spark.png");
    private static final double TRAIL_LENGTH = 1.8;

    public HellstonePillarShotRenderer(RenderManager manager) { super(manager); shadowSize = 0; }

    @Override
    public void doRender(HellstonePillarShot entity, double x, double y, double z, float yaw, float partialTicks) {
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int shadeModel = GL11.glGetInteger(GL11.GL_SHADE_MODEL);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y + entity.height * 0.5, z);
            GlStateManager.disableLighting();
            GlStateManager.enableTexture2D();
            GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.scale(HellstonePillarShot.SIZE, HellstonePillarShot.SIZE, HellstonePillarShot.SIZE);
                GlStateManager.translate(-0.5, -0.5, -0.5);
                IBlockState state = Blocks.MAGMA.getDefaultState();
                Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightnessColor(
                        state, Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state), 1, 1, 1, 1);
            } finally {
                GlStateManager.popMatrix();
            }
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(false);
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            bindTexture(TRAIL);
            double eyeHeight = Minecraft.getMinecraft().getRenderViewEntity() == null ? 0
                    : Minecraft.getMinecraft().getRenderViewEntity().getEyeHeight();
            renderTrail(entity, partialTicks, new Vec3d(-x, eyeHeight - y - entity.height * 0.5, -z));
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.depthMask(depthMask);
            GlStateManager.shadeModel(shadeModel);
            GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GlStateManager.bindTexture(textureId);
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            GlStateManager.color(1, 1, 1, 1);
            GlStateManager.popMatrix();
        }
    }

    private void renderTrail(HellstonePillarShot entity, float partialTicks, Vec3d view) {
        Vec3d center = new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + entity.height * 0.5,
                entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks);
        List<Vec3d> history = entity.trail();
        if (history.isEmpty()) return;
        Vec3d forward = new Vec3d(entity.posX, entity.posY + entity.height * 0.5, entity.posZ)
                .subtract(history.get(history.size() - 1));
        List<Vec3d> points = new ArrayList<>();
        points.add(Vec3d.ZERO);
        Vec3d previous = center;
        double length = 0;
        for (Vec3d point : history) {
            Vec3d delta = point.subtract(previous);
            double distance = Math.sqrt(delta.lengthSquared());
            if (distance < 0.0001 || length == 0 && delta.dotProduct(forward) > 0) continue;
            double span = Math.min(distance, TRAIL_LENGTH - length);
            int steps = Math.max(1, (int) Math.ceil(span / 0.04));
            for (int step = 1; step <= steps; step++) {
                points.add(previous.add(delta.scale(span * step / (distance * steps))).subtract(center));
            }
            length += span;
            if (length >= TRAIL_LENGTH) break;
            previous = point;
        }
        if (length < 0.01) return;
        Vec3d tangent = points.get(points.size() - 1).normalize();
        Vec3d side = tangent.crossProduct(view).normalize();
        if (side.lengthSquared() < 0.01) side = tangent.crossProduct(new Vec3d(0, 1, 0)).normalize();
        Vec3d lift = tangent.crossProduct(side).normalize();
        float age = entity.ticksExisted + partialTicks;
        float seed = entity.getEntityId() * 0.73F;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int layer = 0; layer < 2; layer++) {
            double distance = 0;
            for (int index = 1; index < points.size(); index++) {
                Vec3d start = points.get(index - 1), end = points.get(index);
                float startProgress = (float) (distance / length);
                distance += start.distanceTo(end);
                float endProgress = (float) Math.min(1, distance / length);
                trailVertex(buffer, start, side, lift, startProgress, 0, layer, age, seed);
                trailVertex(buffer, end, side, lift, endProgress, 0, layer, age, seed);
                trailVertex(buffer, end, side, lift, endProgress, 1, layer, age, seed);
                trailVertex(buffer, start, side, lift, startProgress, 1, layer, age, seed);
            }
        }
        Tessellator.getInstance().draw();
        bindTexture(SPARK);
        Vec3d right = view.crossProduct(new Vec3d(0, 1, 0)).normalize();
        if (right.lengthSquared() < 0.01) right = new Vec3d(1, 0, 0);
        Vec3d up = right.crossProduct(view).normalize();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int index = 0; index < 5; index++) {
            double phase = age * 0.13 + seed * 0.21 + index * 0.2;
            float progress = (float) (phase - Math.floor(phase));
            double cursor = progress * (points.size() - 1);
            int from = Math.min(points.size() - 2, (int) cursor);
            Vec3d point = points.get(from).add(points.get(from + 1).subtract(points.get(from)).scale(cursor - from));
            double drift = progress * progress;
            point = point.add(side.scale(Math.sin(seed + index * 2.4) * drift * 0.2))
                    .addVector(0, drift * (0.04 + 0.1 * Math.sin(seed + index)), 0);
            double size = 0.009 + 0.015 * (1 - progress);
            float opacity = (float) (0.7 * Math.pow(1 - progress, 1.3) * Math.min(1, progress * 8));
            sparkVertex(buffer, point, right, up, -size, -size, 0, 1, opacity);
            sparkVertex(buffer, point, right, up, size, -size, 1, 1, opacity);
            sparkVertex(buffer, point, right, up, size, size, 1, 0, opacity);
            sparkVertex(buffer, point, right, up, -size, size, 0, 0, opacity);
        }
        Tessellator.getInstance().draw();
    }

    private static void trailVertex(BufferBuilder buffer, Vec3d point, Vec3d side, Vec3d lift,
            float progress, int edge, int layer, float age, float seed) {
        float strength = 1 - progress;
        double flow = progress * 21 - age * 0.9 + seed;
        double pulse = 1 + progress * (0.24 * Math.sin(flow) + 0.13 * Math.sin(flow * 1.73 + layer));
        double width = HellstonePillarShot.SIZE * (layer == 0 ? 0.68 : 0.5) * Math.pow(strength, 0.7) * pulse;
        double spread = 0.105 * progress;
        double lateral = Math.sin(progress * 11 - age * 0.8 + seed + layer * 0.35) * spread;
        double curl = Math.cos(progress * 16 - age * 0.63 + seed) * spread * 0.7;
        Vec3d position = point.add(side.scale((edge == 0 ? -width : width) + lateral)).add(lift.scale(curl));
        float green = layer == 0 ? 0.16F + strength * 0.24F : 0.45F + strength * 0.35F;
        float flicker = 0.83F + 0.17F * (float) Math.sin(progress * 25 - age * 1.2 + seed);
        float alpha = (layer == 0 ? 0.52F : 0.9F) * (float) Math.pow(strength, 0.9) * flicker;
        buffer.pos(position.x, position.y, position.z).tex(progress * 1.6 - age * 0.075 + seed, edge)
                .color(1, green, layer == 0 ? 0.015F : 0.09F, alpha).endVertex();
    }

    private static void sparkVertex(BufferBuilder buffer, Vec3d point, Vec3d right, Vec3d up,
            double x, double y, double u, double v, float alpha) {
        Vec3d position = point.add(right.scale(x)).add(up.scale(y));
        buffer.pos(position.x, position.y, position.z).tex(u, v).color(1, 0.5F, 0.06F, alpha).endVertex();
    }

    @Override
    protected ResourceLocation getEntityTexture(HellstonePillarShot entity) { return TextureMap.LOCATION_BLOCKS_TEXTURE; }
}
