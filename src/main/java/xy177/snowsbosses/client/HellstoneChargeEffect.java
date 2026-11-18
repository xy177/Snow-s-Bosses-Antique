package xy177.snowsbosses.client;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.HellstoneGolem;
import xy177.snowsbosses.entity.HellstoneMob;

public final class HellstoneChargeEffect {
    private static final ResourceLocation TEXTURE = new ResourceLocation(SnowsBosses.MOD_ID, "textures/particle/hellstone_charge.png");
    private static final Mesh MESH = load();

    private HellstoneChargeEffect() { }

    private static Mesh load() {
        try (Reader reader = new InputStreamReader(HellstoneChargeEffect.class.getResourceAsStream(
                "/assets/snows_bosses/models/entities/hellstone_charge.json"), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Mesh.class);
        } catch (Exception exception) { throw new IllegalStateException("Cannot load hellstone charge effect", exception); }
    }

    public static void render(HellstoneMob entity, double x, double y, double z, float partialTick) {
        if (!(entity instanceof HellstoneGolem) || entity.action() != 7) return;
        float age = entity.actionAge() + partialTick;
        float centerY = entity.height * 0.80F;
        float progress = 0.16F + MathHelper.clamp(age / 20F, 0, 1) * 0.68F;
        float reveal = 0.55F + 0.45F * MathHelper.clamp(age / 2F, 0, 1);
        float yaw = entity.prevRotationYaw + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTick;
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        boolean textured = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
        FloatBuffer color = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y + centerY, z);
            GlStateManager.rotate(-yaw, 0, 1, 0);
            GlStateManager.translate(0, 0, entity.width * 0.35);
            GlStateManager.enableTexture2D();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
            GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.disableAlpha();
            GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.depthMask(false); GlStateManager.color(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            for (int layer = 0; layer < 2; layer++) {
                int direction = layer == 0 ? 1 : -1;
                float[] rgba = sample(MESH.colors[layer], progress);
                float brightness = (float) (1 - Math.exp(-rgba[0] / 45F));
                float size = entity.width * (1.10F + layer * 0.05F) / 7.798355F;
                GlStateManager.pushMatrix();
                try {
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, layer == 0 ? GL11.GL_ONE_MINUS_SRC_ALPHA : GL11.GL_ONE);
                    GlStateManager.scale(size, size, size * (0.68F + layer * 0.04F));
                    float rotation = direction * age * 9;
                    float sine = MathHelper.sin(rotation * 0.017453292F), cosine = MathHelper.cos(rotation * 0.017453292F);
                    GlStateManager.rotate(rotation, 0, 0, 1);
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder buffer = tessellator.getBuffer();
                    buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_TEX_COLOR);
                    for (int index : MESH.indices) {
                        float[] vertex = MESH.vertices[index];
                        float depth = MathHelper.clamp(vertex[2] / 7.798355F, 0, 1);
                        float edge = (float) Math.pow(1 - depth, 1.3);
                        float rim = MathHelper.clamp(depth / 0.40F, 0, 1);
                        rim = rim * rim * (3 - 2 * rim);
                        float ground = MathHelper.clamp((centerY + (vertex[0] * sine + vertex[1] * cosine) * size) / 0.50F, 0, 1);
                        ground = ground * ground * (3 - 2 * ground);
                        float flow = 0.80F + 0.20F * MathHelper.sin((float) Math.atan2(vertex[1], vertex[0]) * 6
                                + direction * age * 0.65F + depth * 14);
                        float opacity = rgba[3] * reveal * (layer == 0 ? 0.48F + edge * 0.50F : 0.08F + edge * 0.38F) * flow * rim * ground;
                        buffer.pos(vertex[0], vertex[1], vertex[2]).tex(vertex[3] + direction * age * 0.035,
                                vertex[4] + age * 0.018).color(brightness, brightness * (layer == 0 ? 0.08F + edge * 0.10F : 0.20F + edge * 0.16F),
                                brightness * (layer == 0 ? 0.015F : 0.025F + edge * 0.025F), opacity).endVertex();
                    }
                    tessellator.draw();
                } finally { GlStateManager.popMatrix(); }
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.depthMask(depthMask);
            if (textured) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
            if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
            if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
            GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            GlStateManager.bindTexture(texture);
            GlStateManager.color(color.get(0), color.get(1), color.get(2), color.get(3));
            GlStateManager.popMatrix();
            GlStateManager.setActiveTexture(activeTexture);
        }
    }

    private static float[] sample(float[] curve, float progress) {
        float position = progress * (curve.length / 4 - 1);
        int first = (int) position, second = Math.min(first + 1, curve.length / 4 - 1);
        float fraction = position - first;
        float[] result = new float[4];
        for (int component = 0; component < 4; component++) result[component] = curve[first * 4 + component]
                + (curve[second * 4 + component] - curve[first * 4 + component]) * fraction;
        return result;
    }

    private static final class Mesh { float[][] vertices; int[] indices; float[][] colors; }
}

