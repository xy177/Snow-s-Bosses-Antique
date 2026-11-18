package xy177.snowsbosses.client;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.HellstoneGolem;
import xy177.snowsbosses.entity.HellstoneMob;
import xy177.snowsbosses.entity.HellstoneImp;

public final class HellstoneRenderer<T extends HellstoneMob> extends RenderLiving<T> {
    private final String species;
    public HellstoneRenderer(RenderManager manager, String species) {
        super(manager, new Model(species), 0.5F); this.species = species;
        if (species.equals("hellstone_golem")) addLayer(new Glow());
    }
    @Override
    protected ResourceLocation getEntityTexture(T entity) { return resource("textures/entities/" + species + ".png"); }
    @Override
    protected void preRenderCallback(T entity, float partialTick) {
        if (entity instanceof HellstoneGolem) GlStateManager.scale(1.2F, 1.2F, 1.2F);
        else if (entity instanceof HellstoneImp && entity.action() == 3) {
            float pulse = 0.4F + 0.6F * (0.5F + 0.5F * (float) Math.sin((entity.actionAge() + partialTick) * 1.7));
            GlStateManager.color(1, pulse, pulse, 1);
        }
    }
    @Override
    public void doRender(T entity, double x, double y, double z, float yaw, float partialTick) {
        try {
            super.doRender(entity, x, y, z, yaw, partialTick);
            HellstoneChargeEffect.render(entity, x, y, z, partialTick);
        }
        finally { GlStateManager.color(1, 1, 1, 1); }
    }
    private static ResourceLocation resource(String path) { return new ResourceLocation(SnowsBosses.MOD_ID, path); }

    private final class Glow implements LayerRenderer<T> {
        @Override
        public void doRenderLayer(T entity, float limbSwing, float amount, float partialTick, float age, float yaw, float pitch, float scale) {
            boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING), blend = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int depth = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
            int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
            int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            try {
                for (int layer = 1; layer <= (((HellstoneGolem) entity).phase() == 2 ? 2 : 1); layer++) {
                    bindTexture(resource("textures/entities/hellstone_golem_glow" + layer + ".png"));
                    ((Model) getMainModel()).renderPose(scale);
                }
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
                GlStateManager.depthMask(depthMask);
                GlStateManager.depthFunc(depth);
                if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
                if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
                GlStateManager.tryBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            }
        }
        @Override public boolean shouldCombineTextures() { return false; }
    }

    private static final class Model extends ModelBase {
        private static final float GOLEM_WALK_SECONDS_PER_BLOCK = 0.36F;
        private final Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        private final Definition definition;
        private final Map<String, Animation> animations = new LinkedHashMap<>();
        private final Map<Entity, Float> chargePoseTimes = new WeakHashMap<>();
        private final Map<Entity, Walk> walks = new WeakHashMap<>();
        private final String species;
        Model(String species) {
            this.species = species;
            try (Reader model = new InputStreamReader(Model.class.getResourceAsStream("/assets/snows_bosses/models/entities/" + species + ".json"), StandardCharsets.UTF_8);
                    Reader frames = new InputStreamReader(Model.class.getResourceAsStream("/assets/snows_bosses/models/entities/" + species + ".keyframes.json"), StandardCharsets.UTF_8)) {
                Gson gson = new Gson(); definition = gson.fromJson(model, Definition.class);
                textureWidth = definition.textureSize[0]; textureHeight = definition.textureSize[1];
                for (Part part : definition.parts) {
                    ModelRenderer renderer = new ModelRenderer(this);
                    for (Cube cube : part.cubes) {
                        ModelRenderer mesh = new ModelRenderer(this, cube.uv[0], cube.uv[1]); mesh.mirror = cube.mirror;
                        mesh.addBox(cube.origin[0], cube.origin[1], cube.origin[2], cube.size[0], cube.size[1], cube.size[2], cube.inflate);
                        renderer.addChild(mesh);
                    }
                    parts.put(part.name, renderer);
                    if (part.parent != null) parts.get(part.parent).addChild(renderer);
                }
                com.google.gson.JsonObject entries = new com.google.gson.JsonParser().parse(frames).getAsJsonObject();
                for (Map.Entry<String, com.google.gson.JsonElement> entry : entries.entrySet()) animations.put(entry.getKey(), gson.fromJson(entry.getValue(), Animation.class));
            } catch (Exception exception) { throw new IllegalStateException("Cannot load hellstone model " + species, exception); }
        }
        @Override
        public void render(Entity entity, float limbSwing, float amount, float age, float yaw, float pitch, float scale) {
            setRotationAngles(limbSwing, amount, age, yaw, pitch, scale, entity);
            renderPose(scale);
        }
        private void renderPose(float scale) {
            GlStateManager.pushMatrix();
            try {
                for (Part part : definition.parts) if (part.parent == null) parts.get(part.name).render(scale);
            } finally { GlStateManager.popMatrix(); }
        }
        @Override
        public void setRotationAngles(float swing, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
            for (Part part : definition.parts) {
                ModelRenderer renderer = parts.get(part.name);
                renderer.setRotationPoint(part.pivot[0], part.pivot[1], part.pivot[2]);
                renderer.rotateAngleX = part.rotation[0]; renderer.rotateAngleY = part.rotation[1]; renderer.rotateAngleZ = part.rotation[2];
            }
            HellstoneMob mob = (HellstoneMob) entity;
            int action = mob.action();
            float partial = age - mob.ticksExisted;
            if (species.equals("hellstone_imp")) {
                if (action != 3) apply("idle", age / 20, 1);
                if (action != 3) apply("walk", swing * 0.05F * 1.6F, Math.min(amount * 3, 1));
                if (action == 1) apply("attack", (mob.actionAge() + partial) / 20, 1);
            } else {
                if (action == -1) apply("sleep", age / 20, 1);
                else if (action == -2) apply("sleep_end", (mob.actionAge() + partial) / 20, 1);
                else {
                    float walkTime = walkTime(entity, partial);
                    Walk walk = walks.get(entity);
                    float walkWeight = walk.previousWeight + (walk.weight - walk.previousWeight) * Math.max(0, Math.min(1, partial));
                    if (action < 6) apply("walk", walkTime, walkWeight);
                    float time = (mob.actionAge() + partial) / 20;
                    if (action == 7) chargePoseTimes.put(entity, time);
                    else if (action != 8) chargePoseTimes.remove(entity);
                    if (action == 6 && time < 0.2F) {
                        float blend = Math.max(0, Math.min(1, time / 0.2F));
                        blend = blend * blend * (3 - 2 * blend);
                        apply("walk", walkTime, walkWeight * (1 - blend));
                        apply("attack6", time, blend);
                    } else if (action == 8 && time < 0.15F) {
                        float blend = Math.max(0, time / 0.15F);
                        apply("attack7", chargePoseTimes.getOrDefault(entity, time), 1 - blend);
                        apply("attack8", time, blend);
                    } else if (action == 9) {
                        float blend = Math.max(0, Math.min(1, (time - 3.8F) / 0.5F));
                        apply("attack9", time, 1 - blend * blend * (3 - 2 * blend));
                    } else if (action == 10) {
                        apply("attack10", ((HellstoneGolem) entity).pillarAnimationTime(partial), 1);
                    } else if (action > 0) apply("attack" + action, time, 1);
                }
            }
            ModelRenderer head = parts.get("head");
            if (head != null && action >= 0 && action != 10) {
                head.rotateAngleX += Math.toRadians(pitch);
                head.rotateAngleY += Math.toRadians(yaw);
            }
        }
        private float walkTime(Entity entity, float partial) {
            Walk walk = walks.computeIfAbsent(entity, key -> new Walk());
            if (walk.tick != entity.ticksExisted) {
                walk.tick = entity.ticksExisted;
                walk.previousDistance = walk.distance;
                double dx = entity.posX - entity.prevPosX, dz = entity.posZ - entity.prevPosZ;
                double distance = Math.sqrt(dx * dx + dz * dz);
                walk.distance += distance;
                if (distance > 0.0001) walk.movedTick = walk.tick;
                walk.previousWeight = walk.weight;
                float target = walk.tick - walk.movedTick < 4 ? 1 : 0;
                walk.weight += (target - walk.weight) * 0.4F;
                if (Math.abs(target - walk.weight) < 0.001F) walk.weight = target;
            }
            return (float) ((walk.previousDistance + (walk.distance - walk.previousDistance)
                    * Math.max(0, Math.min(1, partial))) * GOLEM_WALK_SECONDS_PER_BLOCK);
        }
        private static final class Walk {
            int tick = Integer.MIN_VALUE, movedTick = -10;
            double distance, previousDistance;
            float weight, previousWeight;
        }
        private void apply(String name, float time, float weight) {
            Animation animation = animations.get(name);
            if (animation == null || weight <= 0) return;
            if (animation.loop && animation.length > 0) time %= animation.length;
            for (Channel channel : animation.channels) {
                ModelRenderer part = parts.get(channel.bone);
                if (part == null) continue;
                Key[] keys = channel.keys;
                int start = 0;
                while (start + 1 < keys.length && time >= keys[start + 1].time) start++;
                int end = Math.min(start + 1, keys.length - 1);
                float fraction = end == start ? 0 : Math.max(0, Math.min(1, (time - keys[start].time) / (keys[end].time - keys[start].time)));
                if (channel.target.equals("rotation_quaternion")) {
                    applyQuaternion(part, keys[start].vector, keys[end].vector, fraction, weight);
                    continue;
                }
                float[] vector = new float[3];
                for (int axis = 0; axis < 3; axis++) {
                    float a = keys[start].vector[axis], b = keys[end].vector[axis];
                    if (keys[end].smooth) {
                        float p = keys[Math.max(0, start - 1)].vector[axis], q = keys[Math.min(keys.length - 1, end + 1)].vector[axis];
                        float f2 = fraction * fraction, f3 = f2 * fraction;
                        vector[axis] = (2 * a + (-p + b) * fraction + (2 * p - 5 * a + 4 * b - q) * f2 + (-p + 3 * a - 3 * b + q) * f3) * 0.5F * weight;
                    } else vector[axis] = (a + (b - a) * fraction) * weight;
                }
                if (channel.target.equals("rotation")) {
                    part.rotateAngleX += Math.toRadians(vector[0]); part.rotateAngleY += Math.toRadians(vector[1]); part.rotateAngleZ += Math.toRadians(vector[2]);
                } else if (channel.target.equals("position")) {
                    part.rotationPointX += vector[0]; part.rotationPointY -= vector[1]; part.rotationPointZ += vector[2];
                }
            }
        }
        private void applyQuaternion(ModelRenderer part, float[] first, float[] second, float fraction, float weight) {
            double dot = 0;
            for (int axis = 0; axis < 4; axis++) dot += first[axis] * second[axis];
            double sign = dot < 0 ? -1 : 1;
            dot = Math.min(1, Math.abs(dot));
            double a = 1 - fraction, b = fraction;
            if (dot < 0.9995) {
                double angle = Math.acos(dot), sine = Math.sin(angle);
                a = Math.sin((1 - fraction) * angle) / sine;
                b = Math.sin(fraction * angle) / sine;
            }
            double[] quaternion = new double[4];
            double length = 0;
            for (int axis = 0; axis < 4; axis++) {
                quaternion[axis] = a * first[axis] + b * sign * second[axis];
                length += quaternion[axis] * quaternion[axis];
            }
            length = Math.sqrt(length);
            for (int axis = 0; axis < 4; axis++) quaternion[axis] /= length;
            if (weight < 1) {
                double angle = Math.acos(Math.min(1, Math.abs(quaternion[3])));
                double factor = angle < 0.000001 ? weight : Math.sin(angle * weight) / Math.sin(angle);
                double direction = quaternion[3] < 0 ? -1 : 1;
                for (int axis = 0; axis < 3; axis++) quaternion[axis] *= factor * direction;
                quaternion[3] = Math.cos(angle * weight);
            }
            double x = quaternion[0], y = quaternion[1], z = quaternion[2], w = quaternion[3];
            double pitch = Math.asin(Math.max(-1, Math.min(1, 2 * (w * y - z * x))));
            double roll = Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y));
            double yaw = Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z));
            if (Math.abs(Math.cos(pitch)) < 0.000001) {
                roll = 0;
                yaw = Math.atan2(2 * (w * z - x * y), 1 - 2 * (x * x + z * z));
            }
            part.rotateAngleX += (float) roll;
            part.rotateAngleY += (float) pitch;
            part.rotateAngleZ += (float) yaw;
        }
    }
    private static final class Definition { int[] textureSize; Part[] parts; }
    private static final class Part { String name, parent; float[] pivot, rotation; Cube[] cubes; }
    private static final class Cube { int[] uv, size; float[] origin; float inflate; boolean mirror; }
    private static final class Animation { float length; boolean loop; Channel[] channels; }
    private static final class Channel { String bone, target; Key[] keys; }
    private static final class Key { float time; float[] vector; boolean smooth; }
}
