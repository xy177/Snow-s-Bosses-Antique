package xy177.snowsbosses.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Mechasent;
import xy177.snowsbosses.entity.MechasentLaser;

public final class MechasentRenderer extends RenderLiving<Mechasent> {
    public MechasentRenderer(RenderManager manager) { super(manager, new Model("mechasent"), 0.9F); addLayer(new Glow()); }
    private static ResourceLocation resource(String path) { return new ResourceLocation(SnowsBosses.MOD_ID, path); }
    private ResourceLocation texture(Mechasent entity, boolean glow) {
        return resource("textures/entities/mechasent" + (entity.arms() == 0 ? "" : "_" + entity.arms()) + (glow ? "_glow" : "") + ".png");
    }
    @Override protected ResourceLocation getEntityTexture(Mechasent entity) { return texture(entity, false); }
    @Override public void doRender(Mechasent entity, double x, double y, double z, float yaw, float partial) {
        if (entity.hidden()) return;
        GlStateManager.disableCull();
        super.doRender(entity, x, y, z, yaw, partial);
        GlStateManager.enableCull();
    }

    private final class Glow implements LayerRenderer<Mechasent> {
        @Override public void doRenderLayer(Mechasent entity, float swing, float amount, float partial, float age, float yaw, float pitch, float scale) {
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            bindTexture(texture(entity, true));
            getMainModel().render(entity, swing, amount, age, yaw, pitch, scale);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        }
        @Override public boolean shouldCombineTextures() { return false; }
    }

    public static final class Laser extends Render<MechasentLaser> {
        private final Model model = new Model("mechasent_laser");
        public Laser(RenderManager manager) { super(manager); }
        @Override protected ResourceLocation getEntityTexture(MechasentLaser entity) { return resource("textures/entities/mechasent_laser.png"); }
        @Override public void doRender(MechasentLaser entity, double x, double y, double z, float yaw, float partial) {
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + 1.501, z);
            GlStateManager.scale(-1, -1, 1);
            GlStateManager.disableCull();
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            bindEntityTexture(entity);
            model.render(entity, 0, 0, entity.age() + partial, 0, 0, 0.0625F);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            model.render(entity, 0, 0, entity.age() + partial, 0, 0, 0.0625F);
            GlStateManager.disableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
        }
    }

    static final class Model extends ModelBase {
        private static final String[] ACTIONS = {"", "attack1_smash", "attack2_smash2", "attack3_roar", "attack4_normalhit", "attack5_defense", "attack6_defensebreak", "attack7_bigsmash", "attack8_teleportsmash"};
        private final Map<String, PartRenderer> parts = new LinkedHashMap<>();
        private final Map<String, Animation> animations = new LinkedHashMap<>();
        private final Definition definition;

        Model(String species) {
            String path = "/assets/snows_bosses/models/entities/" + species;
            try (Reader model = new InputStreamReader(Model.class.getResourceAsStream(path + ".json"), StandardCharsets.UTF_8);
                    Reader frames = new InputStreamReader(Model.class.getResourceAsStream(path + ".keyframes.json"), StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                definition = gson.fromJson(model, Definition.class);
                textureWidth = definition.textureSize[0]; textureHeight = definition.textureSize[1];
                for (Part part : definition.parts) {
                    PartRenderer renderer = new PartRenderer(this);
                    for (Cube cube : part.cubes) {
                        ModelRenderer mesh = new ModelRenderer(this, cube.uv[0], cube.uv[1]);
                        mesh.mirror = cube.mirror;
                        mesh.addBox(cube.origin[0], cube.origin[1], cube.origin[2], cube.size[0], cube.size[1], cube.size[2], cube.inflate);
                        renderer.addChild(mesh);
                    }
                    parts.put(part.name, renderer);
                    if (part.parent != null) parts.get(part.parent).addChild(renderer);
                }
                for (Map.Entry<String, JsonElement> entry : new JsonParser().parse(frames).getAsJsonObject().entrySet()) animations.put(entry.getKey(), gson.fromJson(entry.getValue(), Animation.class));
            } catch (Exception exception) { throw new IllegalStateException("Cannot load model " + species, exception); }
        }

        @Override public void render(Entity entity, float swing, float amount, float age, float yaw, float pitch, float scale) {
            for (Part part : definition.parts) {
                PartRenderer renderer = parts.get(part.name);
                renderer.setRotationPoint(part.pivot[0], part.pivot[1], part.pivot[2]);
                renderer.rotateAngleX = part.rotation[0]; renderer.rotateAngleY = part.rotation[1]; renderer.rotateAngleZ = part.rotation[2];
                renderer.sx = renderer.sy = renderer.sz = 1;
            }
            if (entity instanceof Mechasent) {
                Mechasent mob = (Mechasent) entity;
                apply("idle", age / 20, 1);
                apply("walk", swing * 0.05F * 1.7F, Math.min(amount * 2, 1));
                if (mob.action() != 0) apply(ACTIONS[mob.action()], (mob.actionAge() + age - mob.ticksExisted) / 20, 1);
                parts.get("head2").rotateAngleY = (float) Math.toRadians(yaw);
                parts.get("head2").rotateAngleX = (float) Math.toRadians(pitch);
            } else apply("summon", age / 20, 1);
            for (Part part : definition.parts) if (part.parent == null) parts.get(part.name).render(scale);
        }

        private void apply(String name, float time, float weight) {
            Animation animation = animations.get(name);
            if (animation == null || weight <= 0) return;
            if (animation.loop && animation.length > 0) time %= animation.length;
            for (Channel channel : animation.channels) {
                PartRenderer part = parts.get(channel.bone);
                Key[] keys = channel.keys;
                int start = 0;
                while (start + 1 < keys.length && time >= keys[start + 1].time) start++;
                int end = Math.min(start + 1, keys.length - 1);
                float fraction = end == start ? 0 : Math.max(0, Math.min(1, (time - keys[start].time) / (keys[end].time - keys[start].time)));
                float[] vector = new float[3];
                for (int axis = 0; axis < 3; axis++) {
                    float a = keys[start].vector[axis], b = keys[end].vector[axis];
                    if (keys[end].smooth) {
                        float p = keys[Math.max(0, start - 1)].vector[axis], q = keys[Math.min(keys.length - 1, end + 1)].vector[axis];
                        float f2 = fraction * fraction, f3 = f2 * fraction;
                        vector[axis] = (2 * a + (-p + b) * fraction + (2 * p - 5 * a + 4 * b - q) * f2 + (-p + 3 * a - 3 * b + q) * f3) * 0.5F;
                    } else vector[axis] = a + (b - a) * fraction;
                }
                if (channel.target.equals("rotation")) {
                    part.rotateAngleX += Math.toRadians(vector[0]) * weight; part.rotateAngleY += Math.toRadians(vector[1]) * weight; part.rotateAngleZ += Math.toRadians(vector[2]) * weight;
                } else if (channel.target.equals("position")) {
                    part.rotationPointX += vector[0] * weight; part.rotationPointY -= vector[1] * weight; part.rotationPointZ += vector[2] * weight;
                } else {
                    part.sx += (vector[0] - 1) * weight; part.sy += (vector[1] - 1) * weight; part.sz += (vector[2] - 1) * weight;
                }
            }
        }
    }

    private static final class PartRenderer extends ModelRenderer {
        float sx = 1, sy = 1, sz = 1;
        PartRenderer(ModelBase model) { super(model); }
        @Override public void render(float scale) {
            if (sx == 0 || sy == 0 || sz == 0) return;
            if (sx == 1 && sy == 1 && sz == 1) { super.render(scale); return; }
            float x = rotationPointX, y = rotationPointY, z = rotationPointZ;
            float rx = rotateAngleX, ry = rotateAngleY, rz = rotateAngleZ;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x * scale, y * scale, z * scale);
            if (rz != 0) GlStateManager.rotate((float) Math.toDegrees(rz), 0, 0, 1);
            if (ry != 0) GlStateManager.rotate((float) Math.toDegrees(ry), 0, 1, 0);
            if (rx != 0) GlStateManager.rotate((float) Math.toDegrees(rx), 1, 0, 0);
            GlStateManager.scale(sx, sy, sz);
            rotationPointX = rotationPointY = rotationPointZ = rotateAngleX = rotateAngleY = rotateAngleZ = 0;
            super.render(scale);
            rotationPointX = x; rotationPointY = y; rotationPointZ = z;
            rotateAngleX = rx; rotateAngleY = ry; rotateAngleZ = rz;
            GlStateManager.popMatrix();
        }
    }

    private static final class Definition { int[] textureSize; Part[] parts; }
    private static final class Part { String name, parent; float[] pivot, rotation; Cube[] cubes; }
    private static final class Cube { int[] uv, size; float[] origin; float inflate; boolean mirror; }
    private static final class Animation { float length; boolean loop; Channel[] channels; }
    private static final class Channel { String bone, target; Key[] keys; }
    private static final class Key { float time; float[] vector; boolean smooth; }
}
