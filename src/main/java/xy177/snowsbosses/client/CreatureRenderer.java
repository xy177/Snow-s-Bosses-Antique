package xy177.snowsbosses.client;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.RenderHurtColor;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.AnimatedCreature;
import xy177.snowsbosses.entity.Drowner;

public final class CreatureRenderer<T extends AnimatedCreature> extends GeoEntityRenderer<T> {
    private final Map<T, Model<T>> entityModels = new WeakHashMap<>();
    private Model<T> activeModel;

    public CreatureRenderer(RenderManager manager, float shadow) {
        super(manager, new Model<T>());
        shadowSize = shadow;
        addLayer(new Glow<>(this));
    }

    private Model<T> modelFor(T entity) {
        Model<T> model = entityModels.get(entity);
        if (model == null) {
            model = new Model<>();
            entityModels.put(entity, model);
        }
        return model;
    }

    @Override
    public Integer getUniqueID(T entity) {
        return entity.getEntityId();
    }

    @Override
    public software.bernie.geckolib3.model.provider.GeoModelProvider getGeoModelProvider() {
        return activeModel == null ? modelProvider : activeModel;
    }

    @Override
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        Model<T> entityModel = modelFor(entity);
        activeModel = entityModel;
        try {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            boolean shouldSit = entity.getRidingEntity() != null && entity.getRidingEntity().shouldRiderSit();
            EntityModelData entityModelData = new EntityModelData();
            entityModelData.isSitting = shouldSit;
            entityModelData.isChild = entity.isChild();

            float bodyYaw = lerpYaw(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float headYaw = lerpYaw(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float netHeadYaw = headYaw - bodyYaw;
            if (shouldSit && entity.getRidingEntity() instanceof EntityLivingBase) {
                EntityLivingBase riding = (EntityLivingBase) entity.getRidingEntity();
                bodyYaw = lerpYaw(riding.prevRenderYawOffset, riding.renderYawOffset, partialTicks);
                netHeadYaw = headYaw - bodyYaw;
                float wrapped = MathHelper.wrapDegrees(netHeadYaw);
                if (wrapped < -85.0F) wrapped = -85.0F;
                if (wrapped >= 85.0F) wrapped = 85.0F;
                bodyYaw = headYaw - wrapped;
                if (wrapped * wrapped > 2500.0F) bodyYaw += wrapped * 0.2F;
                netHeadYaw = headYaw - bodyYaw;
            }

            float headPitch = lerp(entity.prevRotationPitch, entity.rotationPitch, partialTicks);
            float age = handleRotationFloat(entity, partialTicks);
            applyRotations(entity, age, bodyYaw, partialTicks);

            float limbSwingAmount = 0.0F;
            float limbSwing = 0.0F;
            if (!shouldSit && entity.isEntityAlive()) {
                limbSwingAmount = lerp(entity.prevLimbSwingAmount, entity.limbSwingAmount, partialTicks);
                limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);
                if (entity.isChild()) limbSwing *= 3.0F;
                if (limbSwingAmount > 1.0F) limbSwingAmount = 1.0F;
            }
            entityModelData.headPitch = -headPitch;
            entityModelData.netHeadYaw = -netHeadYaw;

            AnimationEvent predicate = new AnimationEvent(entity, limbSwing, limbSwingAmount, partialTicks,
                    !(limbSwingAmount > -0.15F && limbSwingAmount < 0.15F),
                    Collections.singletonList(entityModelData));
            GeoModel model = entityModel.getModel(entityModel.getModelLocation(entity));
            entityModel.setLivingAnimations(entity, getUniqueID(entity), predicate);

            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0.01F, 0);
            Minecraft.getMinecraft().renderEngine.bindTexture(getEntityTexture(entity));
            Color renderColor = getRenderColor(entity, partialTicks);
            boolean hurt = setDoRenderBrightness(entity, partialTicks);
            if (!entity.isInvisibleToPlayer(Minecraft.getMinecraft().player)) {
                render(model, entity, partialTicks, renderColor.getRed() / 255.0F, renderColor.getBlue() / 255.0F,
                        renderColor.getGreen() / 255.0F, renderColor.getAlpha() / 255.0F);
            }
            for (GeoLayerRenderer<T> layerRenderer : layerRenderers) {
                layerRenderer.render(entity, limbSwing, limbSwingAmount, partialTicks, limbSwing, netHeadYaw,
                        headPitch, renderColor);
            }
            if (entity instanceof EntityLiving) {
                EntityLiving leashEntity = (EntityLiving) entity;
                if (leashEntity.getLeashHolder() != null) {
                    renderLeash(leashEntity, x, y, z, entityYaw, partialTicks);
                }
            }
            if (hurt) RenderHurtColor.unset();
            GlStateManager.popMatrix();
            GlStateManager.popMatrix();
        } finally {
            activeModel = null;
        }
    }

    @Override
    protected float getDeathMaxRotation(T entity) {
        return 0;
    }

    @Override
    public void renderEarly(T entity, float partialTicks, float red, float green, float blue, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        super.renderEarly(entity, partialTicks, red, green, blue, alpha);
    }

    private static ResourceLocation resource(String path) {
        return new ResourceLocation(SnowsBosses.MOD_ID, path);
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float lerpYaw(float start, float end, float amount) {
        return start + MathHelper.wrapDegrees(end - start) * amount;
    }

    private static final class Model<T extends AnimatedCreature> extends AnimatedGeoModel<T> {
        @Override
        public GeoModel getModel(ResourceLocation location) {
            return GeckoRenderFixes.correctMirroredNormals(super.getModel(location));
        }

        @Override
        public ResourceLocation getModelLocation(T entity) {
            return resource("geo/" + entity.modelName() + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return resource("textures/entities/" + entity.textureName() + ".png");
        }

        @Override
        public ResourceLocation getAnimationFileLocation(T entity) {
            return resource("animations/" + entity.modelName() + ".animation.json");
        }

        @Override
        public void setLivingAnimations(T entity, Integer id, AnimationEvent event) {
            super.setLivingAnimations(entity, id, event);
            IBone head = getAnimationProcessor().getBone("head");
            List<EntityModelData> data = event.getExtraDataOfType(EntityModelData.class);
            if (head != null && !data.isEmpty()) {
                head.setRotationX(data.get(0).headPitch * (float) Math.PI / 180);
                head.setRotationY(data.get(0).netHeadYaw * (float) Math.PI / 180);
            }
            if (entity instanceof Drowner) {
                IBone light = getAnimationProcessor().getBone("light");
                if (light != null) {
                    boolean hidden = !entity.animationName().equals("attack5") || !entity.isEntityAlive();
                    light.setHidden(hidden, hidden);
                }
                DrownerSwimming.apply(this, (Drowner) entity, event.getPartialTick());
            }
        }
    }

    private static final class Glow<T extends AnimatedCreature> extends GeoLayerRenderer<T> {
        private final CreatureRenderer<T> renderer;

        private Glow(CreatureRenderer<T> renderer) {
            super(renderer);
            this.renderer = renderer;
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }

        @Override
        public void render(T entity, float limbSwing, float amount, float partialTicks, float age,
                float yaw, float pitch, Color color) {
            String name = entity.species();
            String texture = name.equals("drowner") ? "drowner_glow_new"
                    : name.equals("tide_raiser") ? "tide_raiser_glow"
                    : name.equals("copper_golem") ? "copper_golem_glow"
                    : name.equals("deepwater_piranha") ? "deepwater_piranha_glow"
                    : name.equals("phantom_trident_entity") || name.equals("enchant_phantom_trident_entity") ? "phantom_trident_entity" : null;
            if (texture == null) return;
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.disableLighting();
            float x = OpenGlHelper.lastBrightnessX;
            float y = OpenGlHelper.lastBrightnessY;
            try {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                renderer.bindTexture(resource("textures/entities/" + texture + ".png"));
                Model<T> model = renderer.modelFor(entity);
                GeckoRenderFixes.renderOverlay(() -> renderer.render(
                        model.getModel(model.getModelLocation(entity)), entity, partialTicks, 1, 1, 1, 1));
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
                GlStateManager.enableLighting();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.disableBlend();
            }
        }
    }
}

