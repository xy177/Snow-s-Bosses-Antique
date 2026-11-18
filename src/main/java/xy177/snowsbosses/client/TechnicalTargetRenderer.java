package xy177.snowsbosses.client;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.entity.TechnicalTarget;

public final class TechnicalTargetRenderer extends RenderBiped<TechnicalTarget> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/steve.png");

    public TechnicalTargetRenderer(RenderManager manager) {
        super(manager, new ModelPlayer(0, false), 0.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(TechnicalTarget entity) {
        return TEXTURE;
    }
}
