package xy177.snowsbosses.world;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.StructureBoundBoss;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class StructureDefeatData extends WorldSavedData {
    private static final String NAME = SnowsBosses.MOD_ID + "_defeated_structures";
    private final Map<String, Record> records = new HashMap<>();

    public StructureDefeatData() { super(NAME); }
    public StructureDefeatData(String name) { super(name); }

    public static String register(World world, String structure, BlockPos position) {
        StructureDefeatData data = get(world);
        String uuid = UUID.nameUUIDFromBytes((world.getSeed() + ":" + structure + ":"
                + position.getX() + ":" + position.getZ()).getBytes(StandardCharsets.UTF_8)).toString();
        if (!data.records.containsKey(uuid)) {
            data.records.put(uuid, new Record(uuid, structure, position, false));
            data.markDirty();
        }
        return uuid;
    }

    public static BlockPos nearest(World world, String structure, BlockPos origin, boolean skipDefeated) {
        Record nearest = null;
        double distance = Double.MAX_VALUE;
        for (Record record : get(world).records.values()) {
            if (!record.structure.equals(structure) || skipDefeated && record.defeated) continue;
            double dx = record.position.getX() - origin.getX();
            double dz = record.position.getZ() - origin.getZ();
            double value = dx * dx + dz * dz;
            if (value < distance) {
                distance = value;
                nearest = record;
            }
        }
        return nearest == null ? null : nearest.position;
    }

    public static BlockPos position(World world, String structure, BlockPos center) {
        for (Record record : get(world).records.values()) {
            if (record.structure.equals(structure) && record.position.getX() == center.getX()
                    && record.position.getZ() == center.getZ()) return record.position;
        }
        return null;
    }

    public static boolean isDefeated(World world, String structure, BlockPos position) {
        if (world.isRemote) return false;
        for (Record record : get(world).records.values()) {
            if (record.structure.equals(structure) && record.position.getX() == position.getX()
                    && record.position.getZ() == position.getZ()) return record.defeated;
        }
        return false;
    }

    private static StructureDefeatData get(World world) {
        MapStorage storage = world.getMapStorage();
        StructureDefeatData data = (StructureDefeatData) storage.getOrLoadData(StructureDefeatData.class, NAME);
        if (data == null) {
            data = new StructureDefeatData();
            storage.setData(NAME, data);
        }
        return data;
    }

    private static void record(LivingDeathEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof StructureBoundBoss) || entity.world.isRemote) return;
        StructureBoundBoss boss = (StructureBoundBoss) entity;
        StructureDefeatData data = get(entity.world);
        Record record = data.records.get(boss.structureUuid());
        if (record != null && record.structure.equals(boss.naturalStructure())) {
            if (!record.defeated) {
                record.defeated = true;
                data.markDirty();
            }
            return;
        }
        String structure = boss.naturalStructure();
        if (structure.isEmpty()) return;
        BlockPos position = StructureLocator.find(entity.world, entity.getPosition(), structure);
        if (position == null) return;
        double dx = position.getX() + 0.5 - entity.posX;
        double dz = position.getZ() + 0.5 - entity.posZ;
        if (dx * dx + dz * dz > 32 * 32) return;
        String uuid = register(entity.world, structure, position);
        record = data.records.get(uuid);
        record.defeated = true;
        data.markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        records.clear();
        NBTTagList entries = data.getTagList("Structures", 10);
        for (int index = 0; index < entries.tagCount(); index++) {
            NBTTagCompound entry = entries.getCompoundTagAt(index);
            String uuid = entry.getString("Uuid");
            if (uuid.isEmpty()) uuid = UUID.nameUUIDFromBytes((entry.getString("Structure") + ":"
                    + entry.getInteger("X") + ":" + entry.getInteger("Z")).getBytes(StandardCharsets.UTF_8)).toString();
            records.put(uuid, new Record(uuid, entry.getString("Structure"),
                    new BlockPos(entry.getInteger("X"), entry.getInteger("Y"), entry.getInteger("Z")),
                    entry.getBoolean("Defeated")));
        }
        NBTTagList old = data.getTagList("Defeated", 10);
        for (int index = 0; index < old.tagCount(); index++) {
            NBTTagCompound entry = old.getCompoundTagAt(index);
            String uuid = UUID.nameUUIDFromBytes((entry.getString("Structure") + ":"
                    + entry.getInteger("X") + ":" + entry.getInteger("Z")).getBytes(StandardCharsets.UTF_8)).toString();
            records.put(uuid, new Record(uuid, entry.getString("Structure"),
                    new BlockPos(entry.getInteger("X"), 0, entry.getInteger("Z")), true));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagList entries = new NBTTagList();
        for (Record record : records.values()) {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setString("Uuid", record.uuid);
            entry.setString("Structure", record.structure);
            entry.setInteger("X", record.position.getX());
            entry.setInteger("Y", record.position.getY());
            entry.setInteger("Z", record.position.getZ());
            entry.setBoolean("Defeated", record.defeated);
            entries.appendTag(entry);
        }
        data.setTag("Structures", entries);
        return data;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) { record(event); }

    private static final class Record {
        private final String uuid;
        private final String structure;
        private final BlockPos position;
        private boolean defeated;

        private Record(String uuid, String structure, BlockPos position, boolean defeated) {
            this.uuid = uuid;
            this.structure = structure;
            this.position = position.toImmutable();
            this.defeated = defeated;
        }
    }
}
