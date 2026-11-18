package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.item.ItemStack;
import xy177.snowsbosses.content.ModItems;
import xy177.snowsbosses.entity.StructureEyeEntity;

public final class StructureEyeRenderer extends RenderSnowball<StructureEyeEntity> {
    public StructureEyeRenderer(RenderManager manager) {
        super(manager, ModItems.INFERNAL_EYE, Minecraft.getMinecraft().getRenderItem());
    }

    @Override
    public ItemStack getStackToRender(StructureEyeEntity entity) {
        if (entity.structure().equals("tide_raiser_tower")) return new ItemStack(ModItems.TIDE_EYE);
        if (entity.structure().equals("the_ocean_shrine")) return new ItemStack(ModItems.DROWNER_HOWL_EYE);
        return new ItemStack(ModItems.INFERNAL_EYE);
    }
}
