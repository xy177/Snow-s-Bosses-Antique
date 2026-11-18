package xy177.snowsbosses.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.ModItems;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID, value = Side.CLIENT)
public final class ColoredGlintModel implements IBakedModel {
    private static final Map<Item, IBakedModel> BASE_MODELS = new HashMap<>();
    private final IBakedModel base;

    private ColoredGlintModel(IBakedModel base) {
        this.base = base;
    }

    public static IBakedModel base(Item item) {
        return BASE_MODELS.get(item);
    }

    @SubscribeEvent
    public static void bake(ModelBakeEvent event) {
        for (Item item : new Item[]{ModItems.INFERNAL_EYE, ModItems.TIDE_EYE, ModItems.DROWNER_HOWL_EYE}) {
            ModelResourceLocation location = new ModelResourceLocation(item.getRegistryName(), "inventory");
            IBakedModel model = event.getModelRegistry().getObject(location);
            BASE_MODELS.put(item, model);
            event.getModelRegistry().putObject(location, new ColoredGlintModel(model));
        }
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return base.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return base.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return base.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return base.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return base.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return base.getOverrides();
    }

    @Override
    public Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        Pair<? extends IBakedModel, javax.vecmath.Matrix4f> perspective = base.handlePerspective(type);
        return Pair.of(this, perspective.getRight());
    }
}
