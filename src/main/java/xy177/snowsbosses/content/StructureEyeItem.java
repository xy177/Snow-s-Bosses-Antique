package xy177.snowsbosses.content;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.init.SoundEvents;
import net.minecraft.world.World;
import xy177.snowsbosses.entity.StructureEyeEntity;
import xy177.snowsbosses.world.StructureLocator;
import xy177.snowsbosses.Balance;

public final class StructureEyeItem extends Item {
    private final String structure;

    public StructureEyeItem(String structure) {
        this.structure = structure;
        setMaxStackSize(16);
    }

    public String structure() {
        return structure;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.RARE;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (player.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<>(EnumActionResult.FAIL, stack);
        }
        player.getCooldownTracker().setCooldown(this, Balance.ticks("structure_eye.cooldown", 20));
        if (!world.isRemote) {
            Vec3d look = player.getLookVec();
            StructureEyeEntity eye = new StructureEyeEntity(world, player.posX,
                    player.posY + player.height / 2.0F, player.posZ);
            eye.setStructure(structure);
            eye.moveTowards(new BlockPos(player.posX + look.x * 16.0D,
                    player.posY + player.height / 2.0D + look.y * 16.0D,
                    player.posZ + look.z * 16.0D));
            StructureLocator.request(player, structure, true, target -> {
                if (target != null && eye.isEntityAlive()) eye.moveTowards(target);
            });
            if (world.spawnEntity(eye)) {
                world.playSound(null, player.posX, player.posY, player.posZ,
                        SoundEvents.ENTITY_ENDEREYE_LAUNCH, SoundCategory.NEUTRAL, 0.5F,
                        0.4F / (Item.itemRand.nextFloat() * 0.4F + 0.8F));
                world.playEvent(null, 1003, player.getPosition(), 0);
            }
        }
        player.addStat(net.minecraft.stats.StatList.getObjectUseStats(this));
        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }
}
