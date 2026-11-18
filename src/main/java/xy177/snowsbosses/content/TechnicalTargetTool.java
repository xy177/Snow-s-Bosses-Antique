package xy177.snowsbosses.content;

import java.util.ArrayList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.entity.TechnicalTarget;

public final class TechnicalTargetTool extends Item {
    public TechnicalTargetTool() { setMaxStackSize(1); }

    @Override
    public boolean hasEffect(ItemStack stack) { return true; }

    public void summon(World world, EntityPlayer player, boolean fixed) {
        RayTraceResult hit = rayTrace(world, player, false);
        Vec3d point = player.getPositionEyes(1).add(player.getLookVec().scale(4));
        BlockPos pos = hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK
                ? hit.getBlockPos().offset(hit.sideHit) : new BlockPos(point);
        TechnicalTarget target = new TechnicalTarget(world);
        target.setLocationAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.rotationYaw + 180, 0);
        target.setKnockbackImmune(fixed);
        if (world.getCollisionBoxes(target, target.getEntityBoundingBox()).isEmpty()) world.spawnEntity(target);
    }

    public static void clear(World world) {
        if (world.isRemote || world.getMinecraftServer() == null) return;
        for (WorldServer level : world.getMinecraftServer().worlds)
            for (Entity entity : new ArrayList<>(level.loadedEntityList))
                if (entity instanceof TechnicalTarget) entity.setDead();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (!world.isRemote) clear(world);
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) { return true; }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) { return false; }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) { return true; }
}
