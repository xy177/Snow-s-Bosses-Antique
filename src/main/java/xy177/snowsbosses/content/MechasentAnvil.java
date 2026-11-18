package xy177.snowsbosses.content;

import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID)
public final class MechasentAnvil {
    private MechasentAnvil(){ }
    public static boolean free(ContainerRepair container){
        ItemStack input=container.getSlot(0).getStack(),addition=container.getSlot(1).getStack(),result=container.getSlot(2).getStack();
        return input.getItem() instanceof MechasentContent.MechasentArmor && input.isItemDamaged()
                && addition.getItem()==Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_BLOCK)
                && !result.isEmpty() && result.getItem()==input.getItem() && result.getItemDamage()<input.getItemDamage() && container.maximumCost==0;
    }
    @SubscribeEvent public static void update(AnvilUpdateEvent event){
        ItemStack input=event.getLeft(),addition=event.getRight();
        if(!(input.getItem() instanceof MechasentContent.MechasentArmor) || !input.isItemDamaged()
                || addition.getItem()!=Item.getItemFromBlock(net.minecraft.init.Blocks.REDSTONE_BLOCK))return;
        int repair=Math.max(1,input.getMaxDamage()/4);
        int count=Math.min(addition.getCount(),(input.getItemDamage()+repair-1)/repair);
        ItemStack output=input.copy();output.setCount(1);output.setItemDamage(Math.max(0,input.getItemDamage()-count*repair));
        if(event.getName()!=null){if(event.getName().trim().isEmpty())output.clearCustomName();else output.setStackDisplayName(event.getName());}
        event.setOutput(output);event.setMaterialCost(count);event.setCost(0);
    }
}
