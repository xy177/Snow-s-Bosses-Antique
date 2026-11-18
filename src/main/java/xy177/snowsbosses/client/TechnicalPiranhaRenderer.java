package xy177.snowsbosses.client;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.GeoModelProvider;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.TechnicalPiranha;

public final class TechnicalPiranhaRenderer extends Render<TechnicalPiranha> implements IGeoRenderer<TechnicalPiranha> {
    private static final ResourceLocation GLOW = new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/deepwater_piranha_glow.png");
    private final Map<TechnicalPiranha, Model> entityModels = new WeakHashMap<>();
    private final Model fallbackModel = new Model();
    private Model activeModel;

    public TechnicalPiranhaRenderer(RenderManager manager) {
        super(manager);
        shadowSize = 0.0F;
    }

    private Model modelFor(TechnicalPiranha entity) {
        Model model = entityModels.get(entity);
        if (model == null) {
            model = new Model();
            entityModels.put(entity, model);
        }
        return model;
    }

    @Override
    public void doRender(TechnicalPiranha entity, double x, double y, double z, float yaw, float partialTicks) {
        Model model = modelFor(entity);
        activeModel = model;
        try {
            GeoModel geoModel = model.getModel(model.getModelLocation(entity));
            model.setLivingAnimations(entity, entity.getEntityId(),
                    new AnimationEvent<>(entity, 0.0F, 0.0F, partialTicks, false, Collections.emptyList()));
            float renderYaw = entity.prevRotationYaw + MathHelper.wrapDegrees(entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            float renderPitch = entity.prevRotationPitch + MathHelper.wrapDegrees(entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            float opacity = entity.getOpacity(partialTicks);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(180.0F - renderYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-renderPitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(TechnicalPiranha.MODEL_SCALE, TechnicalPiranha.MODEL_SCALE, TechnicalPiranha.MODEL_SCALE);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            bindEntityTexture(entity);
            render(geoModel, entity, partialTicks, 1.0F, 1.0F, 1.0F, opacity);
            if (entity.isGlowLayerEnabled() && opacity > 0.0F) {
                float oldX = OpenGlHelper.lastBrightnessX;
                float oldY = OpenGlHelper.lastBrightnessY;
                try {
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    GlStateManager.disableLighting();
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                    bindTexture(GLOW);
                    GeckoRenderFixes.renderOverlay(() -> render(geoModel, entity, partialTicks, 1.0F, 1.0F, 1.0F, opacity));
                } finally {
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, oldX, oldY);
                    GlStateManager.enableLighting();
                    GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                            GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                }
            }
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        } finally {
            activeModel = null;
        }
    }

    @Override
    protected ResourceLocation getEntityTexture(TechnicalPiranha entity) {
        return getTextureLocation(entity);
    }

    @Override
    public ResourceLocation getTextureLocation(TechnicalPiranha entity) {
        return new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/deepwater_piranha.png");
    }

    @Override
    public GeoModelProvider getGeoModelProvider() {
        return activeModel == null ? fallbackModel : activeModel;
    }

    @Override
    public Integer getUniqueID(TechnicalPiranha entity) {
        return entity.getEntityId();
    }

    private static final class Model extends AnimatedGeoModel<TechnicalPiranha> {
        @Override
        public GeoModel getModel(ResourceLocation location) {
            return GeckoRenderFixes.correctMirroredNormals(super.getModel(location));
        }

        @Override
        public ResourceLocation getModelLocation(TechnicalPiranha entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "geo/fish.geo.json");
        }

        @Override
        public ResourceLocation getTextureLocation(TechnicalPiranha entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/deepwater_piranha.png");
        }

        @Override
        public ResourceLocation getAnimationFileLocation(TechnicalPiranha entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "animations/fish.animation.json");
        }

        @Override
        public void setLivingAnimations(TechnicalPiranha entity, Integer id, AnimationEvent event) {
            super.setLivingAnimations(entity, id, event);
            IBone upper = getAnimationProcessor().getBone("bone");
            IBone lower = getAnimationProcessor().getBone("bone2");
            float amount = entity.getMouthOpen(event.getPartialTick());
            IBone head = getAnimationProcessor().getBone("head");
            if (head != null) {
                head.setRotationX(0);
                head.setRotationY(0);
                head.setRotationZ(0);
            }
            if (upper != null) upper.setRotationX((float) Math.toRadians(-5 + 30 * amount));
            if (lower != null) lower.setRotationX((float) Math.toRadians(5 - 40 * amount));
        }
    }
}
