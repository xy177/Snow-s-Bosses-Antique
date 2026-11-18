package xy177.snowsbosses.content;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class HellstoneSleep {
    private static final String SPAWN = "SnowHellstoneBed";
    private static final Field SLEEPING = ReflectionHelper.findField(EntityPlayer.class, "sleeping", "field_71083_bS");
    private static final Field TIMER = ReflectionHelper.findField(EntityPlayer.class, "sleepTimer", "field_71076_b");
    private static final Method SHOULDERS = ReflectionHelper.findMethod(EntityPlayer.class,
            "spawnShoulderEntities", "func_192030_dh");

    private HellstoneSleep() {
    }

    private static boolean enabled(EntityPlayer player) {
        return player.world.provider.getDimension() == -1
                && player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem() == HellstoneContent.HELMET;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!enabled(player)) return;
        IBlockState state = event.getWorld().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BlockBed)) return;
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (player.world.isRemote) return;
        BlockPos head = state.getValue(BlockBed.PART) == BlockBed.EnumPartType.HEAD ? event.getPos()
                : event.getPos().offset(state.getValue(BlockBed.FACING));
        if (player.world.getBlockState(head).getBlock() != state.getBlock()) return;
        for (EntityPlayer other : player.world.playerEntities) {
            if (other.isPlayerSleeping() && head.equals(other.bedLocation)) {
                player.sendStatusMessage(new TextComponentTranslation("tile.bed.occupied"), true);
                return;
            }
        }
        EntityPlayer.SleepResult result = player.trySleep(head);
        if (result == EntityPlayer.SleepResult.OK) {
            state.getBlock().setBedOccupied(player.world, head, player, true);
        } else if (result == EntityPlayer.SleepResult.NOT_SAFE) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.notSafe"), true);
        } else if (result == EntityPlayer.SleepResult.TOO_FAR_AWAY) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.tooFarAway"), true);
        } else if (result == EntityPlayer.SleepResult.NOT_POSSIBLE_NOW) {
            player.sendStatusMessage(new TextComponentTranslation("tile.bed.noSleep"), true);
        }
    }

    private static boolean reachable(EntityPlayer player, BlockPos pos) {
        return Math.abs(player.posX - pos.getX() - 0.5) <= 3
                && Math.abs(player.posY - pos.getY()) <= 2
                && Math.abs(player.posZ - pos.getZ() - 0.5) <= 3;
    }

    private static boolean blocked(EntityPlayer player, BlockPos pos) {
        IBlockState state = player.world.getBlockState(pos);
        return state.getMaterial() != Material.GLASS && !(state.getBlock() instanceof BlockLeaves)
                && (state.causesSuffocation() || state.getBlock() == Blocks.FARMLAND || state.getBlock() == Blocks.GRASS_PATH);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void sleep(PlayerSleepInBedEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || !enabled(player) || event.getResultStatus() != null
                || !(player.world.getBlockState(event.getPos()).getBlock() instanceof BlockBed)) return;
        BlockPos pos = event.getPos();
        EnumFacing facing = player.world.getBlockState(pos).getValue(BlockBed.FACING);
        BlockPos foot = pos.offset(facing.getOpposite());
        if (!player.isEntityAlive() || player.isPlayerSleeping() || blocked(player, pos.up()) || blocked(player, foot.up())) {
            event.setResult(EntityPlayer.SleepResult.OTHER_PROBLEM);
            return;
        }
        if (!reachable(player, pos) && !reachable(player, foot)) {
            event.setResult(EntityPlayer.SleepResult.TOO_FAR_AWAY);
            return;
        }
        player.setSpawnPoint(pos, false);
        if (pos.equals(player.getBedLocation(-1)) && !player.isSpawnForced(-1)) {
            player.setSpawnDimension(-1);
            net.minecraft.nbt.NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            persisted.setLong(SPAWN, pos.toLong());
            player.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
        }
        if (!ForgeEventFactory.fireSleepingTimeCheck(player, pos)) {
            event.setResult(EntityPlayer.SleepResult.NOT_POSSIBLE_NOW);
            return;
        }
        if (!player.capabilities.isCreativeMode && !player.world.getEntitiesWithinAABB(EntityMob.class,
                new AxisAlignedBB(pos).grow(8, 5, 8), mob -> mob.isPreventingPlayerRest(player)).isEmpty()) {
            event.setResult(EntityPlayer.SleepResult.NOT_SAFE);
            return;
        }
        try {
            if (player.isRiding()) player.dismountRidingEntity();
            SHOULDERS.invoke(player);
            player.width = 0.2F;
            player.height = 0.2F;
            player.renderOffsetX = -1.8F * facing.getFrontOffsetX();
            player.renderOffsetZ = -1.8F * facing.getFrontOffsetZ();
            player.setPosition(pos.getX() + 0.5 + facing.getFrontOffsetX() * 0.4,
                    pos.getY() + 0.6875, pos.getZ() + 0.5 + facing.getFrontOffsetZ() * 0.4);
            SLEEPING.setBoolean(player, true);
            TIMER.setInt(player, 0);
            player.bedLocation = pos;
            player.motionX = 0;
            player.motionY = 0;
            player.motionZ = 0;
            player.world.updateAllPlayersSleepingFlag();
            event.setResult(EntityPlayer.SleepResult.OK);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot put player to sleep", exception);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void spawn(PlayerSetSpawnEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || player.getEntityData().getBoolean("SnowHellstoneRespawning")
                || !player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).hasKey(SPAWN)) return;
        if (player.dimension != -1 || event.isForced() || event.getNewSpawn() == null
                || !(player.world.getBlockState(event.getNewSpawn()).getBlock() instanceof BlockBed)) {
            player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).removeTag(SPAWN);
            player.setSpawnDimension(null);
        }
    }

    @SubscribeEvent
    public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG).hasKey(SPAWN)) return;
        player.setSpawnDimension(-1);
        if (event.isWasDeath()) player.getEntityData().setBoolean("SnowHellstoneRespawning", true);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.player instanceof EntityPlayerMP) || event.isEndConquered()) return;
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        player.getEntityData().removeTag("SnowHellstoneRespawning");
        net.minecraft.nbt.NBTTagCompound data = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!data.hasKey(SPAWN) || player.getSpawnDimension() != -1) return;
        BlockPos bed = BlockPos.fromLong(data.getLong(SPAWN));
        WorldServer world = player.getServer().getWorld(-1);
        BlockPos safe = world == null ? null : EntityPlayer.getBedSpawnLocation(world, bed, false);
        if (safe == null || !bed.equals(player.getBedLocation(-1))) {
            data.removeTag(SPAWN);
            player.setSpawnDimension(null);
            return;
        }
        if (player.dimension != -1) {
            player.getServer().getPlayerList().transferPlayerToDimension(player, -1,
                    (net.minecraftforge.common.util.ITeleporter) (destination, entity, yaw) ->
                            entity.setPositionAndRotation(safe.getX() + 0.5, safe.getY() + 0.1, safe.getZ() + 0.5, yaw, 0));
        }
        player.connection.setPlayerLocation(safe.getX() + 0.5, safe.getY() + 0.1, safe.getZ() + 0.5,
                player.rotationYaw, 0);
    }
}
