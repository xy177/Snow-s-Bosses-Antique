package xy177.snowsbosses.content;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import xy177.snowsbosses.SnowsBosses;

public final class AdvancementCraftEvents {
    @SubscribeEvent
    public void crafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.crafting.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        Item item = event.crafting.getItem();
        String advancement;
        if (item == HellstoneContent.MACHETE || item == HellstoneContent.HELMET || item == HellstoneContent.CHESTPLATE
                || item == HellstoneContent.LEGGINGS || item == HellstoneContent.BOOTS) {
            advancement = "hellstone_upgrade";
        } else if (item == MechasentContent.ARM || item == MechasentContent.HELMET || item == MechasentContent.CHESTPLATE
                || item == MechasentContent.LEGGINGS || item == MechasentContent.BOOTS) {
            advancement = "mechasent_upgrade";
        } else {
            return;
        }
        Advancement target = player.getServerWorld().getAdvancementManager()
                .getAdvancement(new ResourceLocation(SnowsBosses.MOD_ID, advancement));
        if (target == null) return;
        AdvancementProgress progress = player.getAdvancements().getProgress(target);
        for (String criterion : progress.getRemaningCriteria()) {
            player.getAdvancements().grantCriterion(target, criterion);
        }
    }
}
