package xy177.snowsbosses.content;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import xy177.farmersfuturedelight.api.IWaterloggableBlock;
import xy177.farmersfuturedelight.api.WaterloggedBlockApi;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.entity.DeepwaterPiranha;
import xy177.snowsbosses.entity.Drowner;
import xy177.snowsbosses.network.Visuals;

public final class OceanAltar extends Block implements IWaterloggableBlock {
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");
    private static final AxisAlignedBB[] BOXES = {
            box(3, 0, 3, 13, 2, 13), box(5, 2, 5, 11, 8, 11), box(2, 8, 2, 14, 10, 14),
            box(1, 7, 1, 4, 11, 4), box(1, 7, 12, 4, 11, 15), box(12, 7, 12, 15, 11, 15),
            box(12, 7, 1, 15, 11, 4), box(6, 7, 6, 10, 11, 10)};

    public OceanAltar() {
        super(Material.ROCK);
        setDefaultState(blockState.getBaseState().withProperty(WATERLOGGED, false));
        setHardness(-1);
        setResistance(3600000);
        setLightLevel(1.0F);
        setLightOpacity(0);
        setSoundType(SoundType.STONE);
    }

    private static AxisAlignedBB box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    private static boolean hasClearSpace(World world, BlockPos pos) {
        for (BlockPos target : BlockPos.getAllInBox(pos.add(-1, 1, -1), pos.add(1, 3, 1))) {
            IBlockState existing = world.getBlockState(target);
            Block block = existing.getBlock();
            if (!world.isAirBlock(target) && block != Blocks.WATER && block != Blocks.FLOWING_WATER) return false;
        }
        return true;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack soul = player.getHeldItemMainhand();
        int cost = Balance.integer("altar.soulCost", 1);
        if (soul.getItem() != ModItems.DROWNED_SOUL) return true;
        if (world.isRemote) return true;
        if (!hasClearSpace(world, pos)) {
            player.sendStatusMessage(new TextComponentTranslation("message.snows_bosses.altar_blocked"), true);
            return true;
        }
        if (soul.getCount() < cost) {
            player.sendStatusMessage(new TextComponentTranslation("message.snows_bosses.altar_soul_cost", cost), true);
            return true;
        }
        AltarTile altar = world.getTileEntity(pos) instanceof AltarTile ? (AltarTile) world.getTileEntity(pos) : null;
        if (altar != null && altar.isSummoning()) {
            player.sendStatusMessage(new TextComponentTranslation("message.snows_bosses.altar_pending"), true);
            return true;
        }
        for (BlockPos target : BlockPos.getAllInBox(pos.add(-1, 1, -1), pos.add(1, 3, 1))) {
            world.setBlockState(target, Blocks.WATER.getDefaultState(), 3);
        }
        soul.shrink(cost);
        if (Balance.flag("altar.delayedSummoning", true) && altar != null) {
            altar.beginSummoning(cost);
            return true;
        }
        summon(world, pos);
        return true;
    }

    private static void summon(World world, BlockPos pos) {
        DeepwaterPiranha fish = new DeepwaterPiranha(world);
        Drowner boss = new Drowner(world);
        fish.setLocationAndAngles(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, 0);
        boss.setLocationAndAngles(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 0, 0);
        world.spawnEntity(fish);
        world.spawnEntity(boss);
        world.notifyNeighborsOfStateChange(pos, ModBlocks.OCEAN_ALTAR, false);
        world.playSound(null, pos, ModSounds.get("item.trident.thunder"), SoundCategory.BLOCKS, 1, 1);
        Visuals.particles(world, "soul", pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5,
                70, 0, 0.2);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty<?>[]{WATERLOGGED},
                WaterloggedBlockApi.extendedProperties());
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return WaterloggedBlockApi.getExtendedState(state, world, pos);
    }

    @Override
    public boolean isWaterloggedState(IBlockState state) {
        return state.getValue(WATERLOGGED);
    }

    @Override
    public IBlockState setWaterloggedState(IBlockState state, boolean waterlogged) {
        return state.withProperty(WATERLOGGED, waterlogged);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(WATERLOGGED) ? 1 : 0;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(WATERLOGGED, (meta & 1) != 0);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
            float x, float y, float z, int meta, EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(WATERLOGGED, WaterloggedBlockApi.isWaterSource(world, pos));
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        WaterloggedBlockApi.onBlockAdded(world, pos, this);
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos from) {
        WaterloggedBlockApi.onNeighborChanged(world, pos, this);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        WaterloggedBlockApi.updateTick(world, pos, state);
    }

    @Override
    public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB query,
            List<AxisAlignedBB> result, Entity entity, boolean actual) {
        for (AxisAlignedBB box : BOXES) addCollisionBoxToList(pos, query, result, box);
    }

    @Override
    public RayTraceResult collisionRayTrace(IBlockState state, World world, BlockPos pos, Vec3d start, Vec3d end) {
        RayTraceResult nearest = null;
        for (AxisAlignedBB box : BOXES) {
            RayTraceResult hit = rayTrace(pos, start, end, box);
            if (hit != null && (nearest == null || start.squareDistanceTo(hit.hitVec) < start.squareDistanceTo(nearest.hitVec))) nearest = hit;
        }
        return nearest;
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return box(1, 0, 1, 15, 11, 15);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new AltarTile();
    }

    @Override
    public void getDrops(net.minecraft.util.NonNullList<ItemStack> drops, IBlockAccess access, BlockPos pos, IBlockState state, int fortune) {
        if (access instanceof net.minecraft.world.WorldServer) {
            net.minecraft.world.WorldServer world = (net.minecraft.world.WorldServer) access;
            drops.addAll(world.getLootTableManager().getLootTableFromLocation(new net.minecraft.util.ResourceLocation(
                    "snows_bosses:blocks/ocean_altar")).generateLootForPools(world.rand,
                    new net.minecraft.world.storage.loot.LootContext.Builder(world).build()));
        } else super.getDrops(drops, access, pos, state, fortune);
    }

    public static final class AltarTile extends TileEntity implements ITickable {
        private final ItemStackHandler inventory = new ItemStackHandler(0);
        private int summoningTicks;
        private int summoningCost;
        private boolean visualStarted;

        private void beginSummoning(int cost) {
            summoningTicks = 140;
            summoningCost = cost;
            visualStarted = false;
            markDirty();
        }

        private boolean isSummoning() {
            return summoningTicks > 0;
        }

        @Override
        public void update() {
            if (world == null || world.isRemote || !isSummoning()) return;
            if (!visualStarted) {
                Visuals.shake(world, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5, summoningTicks);
                visualStarted = true;
            }
            if (!hasClearSpace(world, pos)) {
                cancel();
                return;
            }
            if (summoningTicks % 4 == 0) {
                Visuals.particles(world, "soul", pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5,
                        2, 2.0, 0.01);
            }
            summoningTicks--;
            markDirty();
            if (summoningTicks == 0) {
                summon(world, pos);
                Visuals.stopShake(world, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5);
                summoningCost = 0;
                markDirty();
            }
        }

        private void cancel() {
            if (!isSummoning()) return;
            if (world != null && !world.isRemote) {
                world.spawnEntity(new EntityItem(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        new ItemStack(ModItems.DROWNED_SOUL, summoningCost)));
                Visuals.stopShake(world, pos.getX() + 0.5, pos.getY() + 2.5, pos.getZ() + 0.5);
            }
            summoningTicks = 0;
            summoningCost = 0;
            visualStarted = false;
            markDirty();
        }

        @Override
        public void invalidate() {
            cancel();
            super.invalidate();
        }

        @Override
        public void onChunkUnload() {
            cancel();
            super.onChunkUnload();
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound data) {
            super.writeToNBT(data);
            data.setInteger("SummoningTicks", summoningTicks);
            data.setInteger("SummoningCost", summoningCost);
            return data;
        }

        @Override
        public void readFromNBT(NBTTagCompound data) {
            super.readFromNBT(data);
            summoningTicks = Math.max(0, data.getInteger("SummoningTicks"));
            summoningCost = Math.max(0, data.getInteger("SummoningCost"));
            visualStarted = false;
        }

        @Override
        public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
            return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
        }

        @Override
        public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
            return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
                    ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inventory) : super.getCapability(capability, facing);
        }
    }
}
