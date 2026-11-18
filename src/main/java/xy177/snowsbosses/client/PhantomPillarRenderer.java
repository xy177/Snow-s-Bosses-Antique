package xy177.snowsbosses.client;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.renderers.geo.GeoProjectilesRenderer;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.PhantomPillar;

public final class PhantomPillarRenderer extends GeoProjectilesRenderer<PhantomPillar> {
    private final Map<PhantomPillar, Model> entityModels = new WeakHashMap<>();
    private final Model fallbackModel;
    private Model activeModel;

    public PhantomPillarRenderer(RenderManager manager) {
        this(manager, new Model());
    }

    private PhantomPillarRenderer(RenderManager manager, Model model) {
        super(manager, model);
        fallbackModel = model;
        shadowSize = 0;
    }

    private Model modelFor(PhantomPillar entity) {
        Model model = entityModels.get(entity);
        if (model == null) {
            model = new Model();
            entityModels.put(entity, model);
        }
        return model;
    }

    @Override
    public void doRender(PhantomPillar entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity.isInvisibleToPlayer(Minecraft.getMinecraft().player)) return;
        Model model = modelFor(entity);
        activeModel = model;
        try {
            GeoModel geoModel = model.getModel(model.getModelLocation(entity));
            model.setLivingAnimations(entity, getUniqueID(entity),
                    new AnimationEvent<>(entity, 0, 0, partialTicks, false, Collections.emptyList()));
            float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
            float lightX = OpenGlHelper.lastBrightnessX;
            float lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + 0.01, z);
            GlStateManager.rotate(180.0F - yaw, 0, 1, 0);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            try {
                bindEntityTexture(entity);
                render(geoModel, entity, partialTicks, 1, 1, 1, 1);
                GlStateManager.disableLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GeckoRenderFixes.renderOverlay(() -> render(geoModel, entity, partialTicks, 1, 1, 1, 1));
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
                GlStateManager.enableLighting();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
            }
        } finally {
            activeModel = null;
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PhantomPillar entity) {
        return new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/phantom_trident_entity.png");
    }

    @Override
    public ResourceLocation getEntityTexture(PhantomPillar entity) {
        return getTextureLocation(entity);
    }

    @Override
    public software.bernie.geckolib3.model.provider.GeoModelProvider getGeoModelProvider() {
        return activeModel == null ? fallbackModel : activeModel;
    }

    @Override
    public Integer getUniqueID(PhantomPillar entity) {
        return entity.getEntityId();
    }

    private static final class Model extends AnimatedGeoModel<PhantomPillar> {
        @Override
        public GeoModel getModel(ResourceLocation location) {
            return GeckoRenderFixes.correctMirroredNormals(super.getModel(location));
        }

        @Override
        public void setLivingAnimations(PhantomPillar entity, Integer id, AnimationEvent event) {
            super.setLivingAnimations(entity, id, event);
        }

        @Override
        public ResourceLocation getModelLocation(PhantomPillar entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "geo/" + entity.species() + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureLocation(PhantomPillar entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/phantom_trident_entity.png");
        }

        @Override
        public ResourceLocation getAnimationFileLocation(PhantomPillar entity) {
            return new ResourceLocation(SnowsBosses.MOD_ID, "animations/" + entity.species() + ".animation.json");
        }
    }
}
