package xy177.snowsbosses.world;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;
import xy177.farmersfuturedelight.api.WaterloggedBlockApi;
import xy177.farmersfuturedelight.common.registry.FFDItems;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.ModBlocks;
import xy177.snowsbosses.entity.StructureBoundBoss;

public final class OceanStructures implements IWorldGenerator {
    private final Map<String, Template> templates = new HashMap<>();

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world,
            IChunkGenerator generator, IChunkProvider provider) {
        if (world.provider.getDimension() != Balance.integer("structures.dimension", 0)
                || !world.getWorldInfo().isMapFeaturesEnabled()) return;
        place(world, chunkX, chunkZ, "the_ocean_shrine", true, 58, 34, 1512744094);
        place(world, chunkX, chunkZ, "tide_raiser_tower", false, 54, 32, 787430955);
    }

    private void place(World world, int chunkX, int chunkZ, String name, boolean deep,
            int originalSpacing, int originalSeparation, int originalSalt) {
        String key = "structures." + name + ".";
        if (!Balance.flag(key + "enabled", true)) return;
        int spacing = Math.max(1, Balance.integer(key + "spacing", originalSpacing));
        int separation = Math.min(spacing - 1, Balance.integer(key + "separation", originalSeparation));
        int regionX = Math.floorDiv(chunkX, spacing);
        int regionZ = Math.floorDiv(chunkZ, spacing);
        Random random = new Random((long) regionX * 341873128712L + (long) regionZ * 132897987541L
                + world.getSeed() + originalSalt);
        if (chunkX != regionX * spacing + random.nextInt(spacing - separation)
                || chunkZ != regionZ * spacing + random.nextInt(spacing - separation)) return;
        BlockPos center = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
        Biome biome = world.getBiome(center);
        if (!BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) return;
        if (deep && biome.getBaseHeight() > -1.75f) return;
        int y = world.getHeight(center).getY() - 1;
        if (deep) {
            while (y > 1 && (world.getBlockState(center.up(y)).getMaterial() == Material.WATER
                    || world.isAirBlock(center.up(y)))) y--;
        }
        Template template = template(name);
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        BlockPos size = template.transformedSize(rotation);
        if (y < 1 || y + size.getY() >= world.getHeight()) return;
        BlockPos corner = center.add(-size.getX() / 2, y, -size.getZ() / 2);
        BlockPos origin = template.getZeroPositionWithTransform(corner, Mirror.NONE, rotation);
        PlacementSettings settings = new PlacementSettings().setIgnoreEntities(false).setRotation(rotation)
                .setReplacedBlock(Blocks.STRUCTURE_VOID).setRandom(random);
        template.addBlocksToWorld(world, origin, settings, 2);
        BlockPos structurePosition = center.up(y);
        String uuid = StructureDefeatData.register(world, name, structurePosition);
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class,
                new AxisAlignedBB(structurePosition).grow(64))) {
            if (entity instanceof StructureBoundBoss
                    && name.equals(((StructureBoundBoss) entity).naturalStructure())) {
                ((StructureBoundBoss) entity).setStructureUuid(uuid);
            }
        }
    }

    public Template template(String name) {
        return templates.computeIfAbsent(name, this::load);
    }

    private Template load(String name) {
        try (InputStream input = OceanStructures.class.getResourceAsStream("/assets/snows_bosses/structures/" + name + ".nbt")) {
            NBTTagCompound data = CompressedStreamTools.readCompressed(input);
            NBTTagList palette = data.getTagList("palette", 10);
            for (int index = 0; index < palette.tagCount(); index++) {
                palette.set(index, NBTUtil.writeBlockState(new NBTTagCompound(), translate(palette.getCompoundTagAt(index))));
            }
            for (int index = 0; index < data.getTagList("blocks", 10).tagCount(); index++) {
                NBTTagCompound entry = data.getTagList("blocks", 10).getCompoundTagAt(index);
                if (!entry.hasKey("nbt", 10)) continue;
                NBTTagCompound tile = entry.getCompoundTag("nbt");
                String id = tile.getString("id");
                if (id.equals("call_of_drowner:ocean_altar")) tile.setString("id", SnowsBosses.MOD_ID + ":ocean_altar");
                if (tile.hasKey("LootTable")) tile.setString("LootTable", tile.getString("LootTable").replace("call_of_drowner:", "snows_bosses:"));
            }
            NBTTagList entities = data.getTagList("entities", 10);
            for (int index = 0; index < entities.tagCount(); index++) {
                NBTTagCompound entity = entities.getCompoundTagAt(index).getCompoundTag("nbt");
                String id = entity.getString("id").replace("call_of_drowner:", "snows_bosses:");
                entity.setString("id", id);
                if (id.equals("snows_bosses:tide_raiser")) entity.setString("SnowNaturalStructure", "tide_raiser_tower");
                for (String key : new String[]{"UUID", "UUIDMost", "UUIDLeast", "Attributes", "Health", "Brain", "ForgeData"}) entity.removeTag(key);
            }
            data.setInteger("DataVersion", 1343);
            Template template = new Template();
            template.read(data);
            return template;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load ocean structure " + name, exception);
        }
    }

    private static IBlockState translate(NBTTagCompound data) {
        String name = data.getString("Name");
        NBTTagCompound properties = data.getCompoundTag("Properties");
        IBlockState state;
        if (name.equals("call_of_drowner:ocean_altar")) state = ModBlocks.OCEAN_ALTAR.getDefaultState();
        else if (name.equals("minecraft:dark_prismarine")) state = Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.DARK);
        else if (name.equals("minecraft:prismarine_bricks")) state = Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.BRICKS);
        else if (name.equals("minecraft:mangrove_door")) state = Blocks.ACACIA_DOOR.getDefaultState();
        else if (name.equals("minecraft:stone_bricks")) state = Blocks.STONEBRICK.getStateFromMeta(0);
        else if (name.equals("minecraft:mossy_stone_bricks")) state = Blocks.STONEBRICK.getStateFromMeta(1);
        else if (name.equals("minecraft:cracked_stone_bricks")) state = Blocks.STONEBRICK.getStateFromMeta(2);
        else if (name.equals("minecraft:grass_block")) state = Blocks.GRASS.getDefaultState();
        else {
            ResourceLocation id = new ResourceLocation(name);
            if (Block.REGISTRY.containsKey(id)) state = Block.REGISTRY.getObject(id).getDefaultState();
            else {
                String path = id.getResourcePath();
                if (properties.getString("type").equals("double") && path.endsWith("_slab")) path = path.substring(0, path.length() - 5) + "_double_slab";
                Block block = FFDItems.localBlock(new ResourceLocation("farmers_future_delight", path));
                state = block == null ? null : FFDItems.effectiveBlockState(block);
                if (state == null) throw new IllegalArgumentException("Unavailable structure block " + name);
            }
        }
        for (String key : properties.getKeySet()) {
            String value = properties.getString(key);
            if (key.equals("waterlogged")) continue;
            String targetKey = key.equals("type") && state.getBlock() instanceof BlockSlab ? "half" : key;
            if (targetKey.equals("half") && value.equals("double")) continue;
            if (targetKey.equals("half") && state.getBlock() instanceof BlockDoor) value = value.equals("upper") ? "upper" : "lower";
            IProperty<?> property = state.getBlock().getBlockState().getProperty(targetKey);
            if (property == null) continue;
            if (property.getValueClass() == Boolean.class
                    && (value.equals("low") || value.equals("tall") || value.equals("none"))) value = value.equals("none") ? "false" : "true";
            state = property(state, property, value);
        }
        if (!properties.getString("waterlogged").equals("true")) return state;
        IBlockState waterlogged = WaterloggedBlockApi.withWaterlogged(state, true);
        if (waterlogged == null) throw new IllegalArgumentException("Unavailable waterlogged structure state " + name);
        return waterlogged;
    }

    private static <T extends Comparable<T>> IBlockState property(IBlockState state, IProperty<T> property, String value) {
        com.google.common.base.Optional<T> parsed = property.parseValue(value);
        if (!parsed.isPresent()) throw new IllegalArgumentException("Invalid structure state " + property.getName() + "=" + value);
        return state.withProperty(property, parsed.get());
    }
}
