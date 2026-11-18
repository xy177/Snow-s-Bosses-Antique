package xy177.snowsbosses.world;

import java.io.InputStream;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Mirror;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.entity.StructureBoundBoss;

public final class HellstoneStructures implements IWorldGenerator {
    private Template template;
    private boolean unavailable;
    @Override
    public void generate(Random ignored, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!Balance.flag("hellstone_ruin.enabled", true) || world.provider.getDimension() != Balance.integer("hellstone_ruin.dimension", -1)
                || !world.getWorldInfo().isMapFeaturesEnabled()) return;
        int spacing = Balance.ticks("hellstone_ruin.spacing", 52);
        int separation = Math.min(spacing - 1, Balance.integer("hellstone_ruin.separation", 44));
        int regionX = Math.floorDiv(chunkX, spacing), regionZ = Math.floorDiv(chunkZ, spacing);
        Random random = new Random(regionX * 341873128712L + regionZ * 132897987541L + world.getSeed() + 1502233420L);
        if (chunkX != regionX * spacing + random.nextInt(spacing - separation)
                || chunkZ != regionZ * spacing + random.nextInt(spacing - separation)) return;
        BlockPos center = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
        if (!BiomeDictionary.hasType(world.getBiome(center), BiomeDictionary.Type.NETHER)) return;
        Template structure = template();
        if (structure == null) return;
        int ceiling = world.getActualHeight() - structure.getSize().getY() - 1;
        int min = Math.max(1, Math.min(ceiling, Balance.integer("hellstone_ruin.minimumY", 40)));
        int max = Math.max(min, Math.min(ceiling, Balance.integer("hellstone_ruin.maximumY", 72)));
        int y = min + random.nextInt(max - min + 1);
        Rotation rotation = Rotation.values()[random.nextInt(4)];
        BlockPos size = structure.transformedSize(rotation);
        BlockPos base = groundedBase(world, center, size, min, max, y);
        if (base == null) return;
        y = base.getY();
        support(world, base, size, true);
        BlockPos origin = structure.getZeroPositionWithTransform(center.add(-size.getX() / 2, y, -size.getZ() / 2), Mirror.NONE, rotation);
        structure.addBlocksToWorld(world, origin, new PlacementSettings().setRotation(rotation).setIgnoreEntities(false)
                .setReplacedBlock(Blocks.STRUCTURE_BLOCK).setRandom(random), 2);
        BlockPos structurePosition = center.up(y);
        String uuid = StructureDefeatData.register(world, "hellstone_ruin", structurePosition);
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(structurePosition).grow(64))) {
            if (entity instanceof StructureBoundBoss
                    && "hellstone_ruin".equals(((StructureBoundBoss) entity).naturalStructure())) {
                ((StructureBoundBoss) entity).setStructureUuid(uuid);
            }
        }
    }

    public static BlockPos groundedBase(World world, BlockPos center, BlockPos size, int min, int max, int preferred) {
        for (int distance = 0; distance <= max - min; distance++) {
            for (int direction = 0; direction < (distance == 0 ? 1 : 2); direction++) {
                int y = preferred + (direction == 0 ? -distance : distance);
                if (y < min || y > max) continue;
                BlockPos base = new BlockPos(center.getX(), y, center.getZ());
                IBlockState ground = world.getBlockState(base.down());
                if (!ground.isSideSolid(world, base.down(), EnumFacing.UP)) continue;
                boolean clear = true;
                for (int height = 0; height < size.getY() - 1; height++) {
                    IBlockState state = world.getBlockState(base.up(height));
                    if (!state.getMaterial().isReplaceable() || state.getMaterial().isLiquid()) {
                        clear = false;
                        break;
                    }
                }
                if (clear && support(world, base, size, false)) return base;
            }
        }
        return null;
    }

    private static boolean support(World world, BlockPos base, BlockPos size, boolean place) {
        for (int x = -size.getX() / 2; x < size.getX() - size.getX() / 2; x++) {
            for (int z = -size.getZ() / 2; z < size.getZ() - size.getZ() / 2; z++) {
                BlockPos column = base.add(x, -1, z);
                int depth = 0;
                while (depth < size.getY() && column.getY() - depth > 0) {
                    BlockPos pos = column.down(depth);
                    IBlockState state = world.getBlockState(pos);
                    if (state.isSideSolid(world, pos, EnumFacing.UP)) break;
                    if (state.getMaterial().isLiquid() || !state.getMaterial().isReplaceable()) return false;
                    depth++;
                }
                if (depth == size.getY() || column.getY() - depth <= 0) return false;
                if (place) for (int height = 0; height < depth; height++) {
                    world.setBlockState(column.down(height), Blocks.NETHERRACK.getDefaultState(), 2);
                }
            }
        }
        return true;
    }

    public Template template() {
        if (template != null || unavailable) return template;
        try (InputStream input = HellstoneStructures.class.getResourceAsStream("/assets/snows_bosses/structures/the_hellstone_golem_ruin.nbt")) {
            NBTTagCompound data = CompressedStreamTools.readCompressed(input);
            NBTTagList palette = data.getTagList("palette", 10);
            for (int i = 0; i < palette.tagCount(); i++) {
                NBTTagCompound source = palette.getCompoundTagAt(i);
                IBlockState state = state(source);
                if (state == null) {
                    unavailable = true;
                    org.apache.logging.log4j.LogManager.getLogger("snows_bosses").warn("Hellstone ruins disabled: unavailable dependency block {}", source.getString("Name"));
                    return null;
                }
                palette.set(i, NBTUtil.writeBlockState(new NBTTagCompound(), state));
            }
            NBTTagList entities = data.getTagList("entities", 10);
            for (int i = 0; i < entities.tagCount(); i++) {
                NBTTagCompound entity = entities.getCompoundTagAt(i).getCompoundTag("nbt");
                String id = entity.getString("id").replace("hellstone_golem:", "snows_bosses:");
                entity.setString("id", id);
                if (id.equals("snows_bosses:hellstone_golem")) entity.setString("SnowNaturalStructure", "hellstone_ruin");
                for (String key : new String[]{"UUID", "UUIDMost", "UUIDLeast", "Attributes", "Health", "Brain", "ForgeData"}) entity.removeTag(key);
            }
            data.setInteger("DataVersion", 1343);
            template = new Template(); template.read(data); return template;
        } catch (Exception exception) { throw new IllegalStateException("Cannot load hellstone ruin", exception); }
    }
    private static IBlockState state(NBTTagCompound data) {
        ResourceLocation id = new ResourceLocation(data.getString("Name"));
        IBlockState state;
        if (id.toString().equals("minecraft:magma_block")) state = Blocks.MAGMA.getDefaultState();
        else if (Block.REGISTRY.containsKey(id)) state = Block.REGISTRY.getObject(id).getDefaultState();
        else {
            String path = id.getResourcePath().equals("chain") ? "iron_chain" : id.getResourcePath();
            Block local = FFDItems.localBlock(new ResourceLocation("farmers_future_delight", path));
            state = local == null ? null : FFDItems.effectiveBlockState(local);
        }
        if (state == null) return null;
        NBTTagCompound properties = data.getCompoundTag("Properties");
        for (String key : properties.getKeySet()) {
            if (key.equals("waterlogged")) continue;
            IProperty<?> property = state.getBlock().getBlockState().getProperty(key);
            if (property == null) continue;
            String value = properties.getString(key);
            if (property.getValueClass() == Boolean.class && (value.equals("none") || value.equals("low") || value.equals("tall"))) value = value.equals("none") ? "false" : "true";
            state = property(state, property, value);
        }
        return state;
    }
    private static <T extends Comparable<T>> IBlockState property(IBlockState state, IProperty<T> property, String value) {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        if (!parsed.isPresent()) throw new IllegalArgumentException("Invalid ruin property " + property.getName() + "=" + value);
        return state.withProperty(property, parsed.get());
    }
}
