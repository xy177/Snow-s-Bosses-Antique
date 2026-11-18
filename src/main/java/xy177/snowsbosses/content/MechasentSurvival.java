package xy177.snowsbosses.content;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import xy177.snowsbosses.Balance;

public final class MechasentSurvival {
    private MechasentSurvival() { }

    public static boolean rescue(EntityPlayer entity, DamageSource source) {
        if (!(entity instanceof EntityPlayerMP) || entity.world.isRemote || entity.isDead
                || !MechasentModules.FINAL_PROTOCOL.worn(entity)) return false;
        EntityPlayerMP player = (EntityPlayerMP) entity;
        long now = MechasentEquipment.time(player);
        if (MechasentEquipment.state(player).getLong("ProtocolUntil") > now) return false;
        Destination destination = destination(player);
        if (destination == null) return false;
        MechasentEquipment.state(player).setLong("ProtocolUntil", now + Balance.integer("mechasent.modules.protocolCooldown", 3600));
        player.setHealth(player.getMaxHealth() * 0.5F);
        player.extinguish();
        player.fallDistance = 0;
        player.motionX = player.motionY = player.motionZ = 0;
        player.hurtResistantTime = player.maxHurtResistantTime;
        player.dismountRidingEntity();
        player.stopActiveHand();
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1, 1);
        if (player.dimension != destination.world.provider.getDimension()) {
            final BlockPos position = destination.pos;
            player.getServer().getPlayerList().transferPlayerToDimension(player, destination.world.provider.getDimension(), new Teleporter(destination.world) {
                @Override public void placeInPortal(net.minecraft.entity.Entity target, float yaw) {
                    target.setLocationAndAngles(position.getX() + 0.5, position.getY(), position.getZ() + 0.5, yaw, target.rotationPitch);
                }
            });
        }
        player.connection.setPlayerLocation(destination.pos.getX() + 0.5, destination.pos.getY(), destination.pos.getZ() + 0.5, player.rotationYaw, player.rotationPitch);
        player.motionX = player.motionY = player.motionZ = 0;
        player.fallDistance = 0;
        player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.PLAYERS, 1, 1);
        return true;
    }

    private static Destination destination(EntityPlayerMP player) {
        int dimension = player.getSpawnDimension();
        WorldServer world = player.getServer().getWorld(dimension);
        BlockPos bed = player.getBedLocation(dimension);
        if (world != null && bed != null && distant(player, world, bed)) {
            world.getChunkFromBlockCoords(bed);
            BlockPos spawn = EntityPlayer.getBedSpawnLocation(world, bed, player.isSpawnForced(dimension));
            if (spawn != null && distant(player, world, spawn) && safe(player, world, spawn)) return new Destination(world, spawn);
        }
        world = player.getServer().getWorld(0);
        if (world == null) return null;
        BlockPos spawn = world.getSpawnPoint();
        if (distant(player, world, spawn)) {
            BlockPos found = nearby(player, world, spawn);
            if (found != null) return new Destination(world, found);
        }
        double cx = player.dimension == world.provider.getDimension() ? player.posX : spawn.getX();
        double cz = player.dimension == world.provider.getDimension() ? player.posZ : spawn.getZ();
        for (int radius = 112; radius <= 240; radius += 32) {
            for (int direction = 0; direction < 16; direction++) {
                double angle = direction * Math.PI / 8;
                BlockPos center = new BlockPos(cx + Math.cos(angle) * radius, spawn.getY(), cz + Math.sin(angle) * radius);
                BlockPos found = nearby(player, world, center);
                if (found != null) return new Destination(world, found);
            }
        }
        return null;
    }

    private static BlockPos nearby(EntityPlayerMP player, WorldServer world, BlockPos center) {
        world.getChunkFromBlockCoords(center);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            BlockPos column = center.add(dx, 0, dz);
            if (!world.isBlockLoaded(column)) continue;
            BlockPos top = world.getTopSolidOrLiquidBlock(column);
            for (int dy : new int[]{0,1,-1,2}) {
                BlockPos position = top.add(0,dy,0);
                if (distant(player,world,position) && safe(player,world,position)) return position;
            }
        }
        return null;
    }

    private static boolean distant(EntityPlayer player, WorldServer world, BlockPos position) {
        return player.dimension != world.provider.getDimension() || player.getDistanceSqToCenter(position) >= 10000;
    }

    private static boolean safe(EntityPlayerMP player, WorldServer world, BlockPos position) {
        if (position.getY() < 1 || position.getY() + player.height >= world.getActualHeight()
                || !world.isAreaLoaded(position.down(),position.up(2))) return false;
        AxisAlignedBB box = new AxisAlignedBB(position.getX()+0.5-player.width/2,position.getY(),position.getZ()+0.5-player.width/2,
                position.getX()+0.5+player.width/2,position.getY()+player.height,position.getZ()+0.5+player.width/2);
        net.minecraft.block.Block floor = world.getBlockState(position.down()).getBlock();
        return floor != net.minecraft.init.Blocks.MAGMA && floor != net.minecraft.init.Blocks.CACTUS
                && world.getWorldBorder().contains(box) && world.getCollisionBoxes(player,box).isEmpty()
                && !world.containsAnyLiquid(box) && world.checkNoEntityCollision(box,player)
                && !world.getCollisionBoxes(player,box.offset(0,-0.1,0)).isEmpty()
                && world.getBlockState(position).getBlock()!=net.minecraft.init.Blocks.FIRE;
    }

    private static final class Destination {
        final WorldServer world;
        final BlockPos pos;
        Destination(WorldServer world, BlockPos pos) { this.world=world;this.pos=pos; }
    }
}
