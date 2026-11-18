package xy177.snowsbosses.world;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.block.material.Material;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class StructureLocator {
    private static final ArrayDeque<Request> REQUESTS = new ArrayDeque<>();
    private static final Map<ICommandSender, Request> PENDING = new HashMap<>();
    private static final Map<World, Map<String, CandidateCache>> CANDIDATES = new WeakHashMap<>();

    private static CandidateCache cache(World world, String name) {
        Map<String, CandidateCache> byName = CANDIDATES.computeIfAbsent(world, key -> new HashMap<>());
        return byName.computeIfAbsent(name, key -> new CandidateCache());
    }

    private static final class CandidateCache extends LinkedHashMap<Long, Candidate> {
        private static final long serialVersionUID = 1L;
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Candidate> eldest) {
            return size() > 4096;
        }
    }

    private static final class Candidate {
        private final BlockPos center;
        private final int y;
        private final boolean valid;

        private Candidate(BlockPos center, int y, boolean valid) {
            this.center = center;
            this.y = y;
            this.valid = valid;
        }
    }

    private StructureLocator() {
    }

    public static BlockPos find(World world, BlockPos origin, String input) {
        Search search = new Search(world, origin, input, false);
        while (!search.advance(256)) {
        }
        return search.result();
    }

    public static boolean request(ICommandSender sender, String input, boolean skipDefeated,
            Consumer<BlockPos> callback) {
        if (PENDING.containsKey(sender)) return false;
        Request request = new Request(sender, new Search(sender.getEntityWorld(), sender.getPosition(),
                input, skipDefeated), callback);
        PENDING.put(sender, request);
        REQUESTS.addLast(request);
        return true;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long start = System.nanoTime();
        for (int count = 0; count < 64 && !REQUESTS.isEmpty(); count++) {
            Request request = REQUESTS.removeFirst();
            if (!request.active()) {
                PENDING.remove(request.sender);
            } else if (request.search.advance(1)) {
                PENDING.remove(request.sender);
                request.callback.accept(request.search.result());
            } else {
                REQUESTS.addLast(request);
            }
            if (System.nanoTime() - start >= 2000000L) break;
        }
    }

    @SubscribeEvent
    public static void unload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        CANDIDATES.remove(event.getWorld());
        REQUESTS.removeIf(request -> {
            if (request.search.world != event.getWorld()) return false;
            PENDING.remove(request.sender);
            return true;
        });
    }

    public static String normalize(String input) {
        String name = input.toLowerCase(Locale.ROOT);
        if (name.startsWith("snows_bosses:")) name = name.substring("snows_bosses:".length());
        if (name.equals("ocean_shrine") || name.equals("the_ocean_shrine")) return "the_ocean_shrine";
        if (name.equals("tide_raiser") || name.equals("tide_raiser_tower")) return "tide_raiser_tower";
        if (name.equals("hellstone_golem_ruin") || name.equals("the_hellstone_golem_ruin") || name.equals("hellstone_ruin")) return "hellstone_ruin";
        return null;
    }

    private static final class Request {
        private final ICommandSender sender;
        private final Search search;
        private final Consumer<BlockPos> callback;

        private Request(ICommandSender sender, Search search, Consumer<BlockPos> callback) {
            this.sender = sender;
            this.search = search;
            this.callback = callback;
        }

        private boolean active() {
            if (sender.getEntityWorld() != search.world) return false;
            if (!(sender instanceof EntityPlayerMP)) return true;
            EntityPlayerMP player = (EntityPlayerMP) sender;
            return !player.isDead && player.connection.netManager.isChannelOpen();
        }
    }

    public static final class Search {
        private final World world;
        private final BlockPos origin;
        private final String name;
        private final boolean skipDefeated;
        private final boolean hellstone;
        private final int spacing;
        private final int spread;
        private final int regionX;
        private final int regionZ;
        private final int radiusLimit;
        private final int salt;
        private final CandidateCache candidates;
        private int radius;
        private int offset;
        private boolean finished;
        private BlockPos best;
        private double bestDistance = Double.MAX_VALUE;

        public Search(World world, BlockPos origin, String input, boolean skipDefeated) {
            this.world = world;
            this.origin = origin.toImmutable();
            name = normalize(input);
            hellstone = "hellstone_ruin".equals(name);
            boolean shrine = "the_ocean_shrine".equals(name);
            this.skipDefeated = skipDefeated && !shrine;
            String key = hellstone ? "hellstone_ruin." : "structures." + name + ".";
            int defaultSpacing = hellstone ? 52 : shrine ? 58 : 54;
            int defaultSeparation = hellstone ? 44 : shrine ? 34 : 32;
            spacing = name == null ? 1 : Balance.ticks(key + "spacing", defaultSpacing);
            spread = name == null ? 1 : spacing - Math.min(spacing - 1,
                    Balance.integer(key + "separation", defaultSeparation));
            regionX = Math.floorDiv(Math.floorDiv(origin.getX(), 16), spacing);
            regionZ = Math.floorDiv(Math.floorDiv(origin.getZ(), 16), spacing);
            radiusLimit = Balance.integer("structures.searchRegionRadius", 256);
            salt = hellstone ? 1502233420 : shrine ? 1512744094 : 787430955;
            candidates = name == null ? null : cache(world, name);
            finished = name == null || !world.getWorldInfo().isMapFeaturesEnabled()
                    || world.provider.getDimension() != Balance.integer(hellstone ? "hellstone_ruin.dimension" : "structures.dimension", hellstone ? -1 : 0)
                    || !Balance.flag(key + "enabled", true);
            BlockPos known = name == null ? null : StructureDefeatData.nearest(world, name, this.origin, this.skipDefeated);
            if (known != null) {
                double dx = known.getX() - this.origin.getX();
                double dz = known.getZ() - this.origin.getZ();
                best = known;
                bestDistance = dx * dx + dz * dz;
            }
        }

        public boolean advance(int candidates) {
            for (int count = 0; count < candidates && !finished; count++) {
                int x = regionX;
                int z = regionZ;
                if (radius > 0) {
                    int edge = offset / (2 * radius);
                    int step = offset % (2 * radius);
                    x += edge == 0 ? -radius + step : edge == 1 ? radius : edge == 2 ? radius - step : -radius;
                    z += edge == 0 ? -radius : edge == 1 ? -radius + step : edge == 2 ? radius : radius - step;
                }
                examine(x, z);
                if (++offset == (radius == 0 ? 1 : radius * 8)) {
                    offset = 0;
                    finished = radius >= radiusLimit || best != null && outsideDistance() >= Math.sqrt(bestDistance);
                    radius++;
                }
            }
            return finished;
        }

        private void examine(int x, int z) {
            long key = ((long) x << 32) ^ (z & 0xffffffffL);
            Candidate candidate = candidates.get(key);
            if (candidate == null) {
                Random random = new Random((long) x * 341873128712L + (long) z * 132897987541L
                        + world.getSeed() + salt);
                int candidateX = x * spacing + random.nextInt(spread);
                int candidateZ = z * spacing + random.nextInt(spread);
                BlockPos center = new BlockPos(candidateX * 16 + 8, 0, candidateZ * 16 + 8);
                Biome biome = world.getBiomeProvider().getBiome(center);
                boolean valid = BiomeDictionary.hasType(biome, hellstone ? BiomeDictionary.Type.NETHER : BiomeDictionary.Type.OCEAN)
                        && (!name.equals("the_ocean_shrine") || biome.getBaseHeight() <= -1.75f);
                int y = world.getSeaLevel();
                if (hellstone) {
                    int min = Math.min(world.getActualHeight() - 7, Balance.integer("hellstone_ruin.minimumY", 40));
                    int max = Math.max(min, Math.min(world.getActualHeight() - 7, Balance.integer("hellstone_ruin.maximumY", 72)));
                    y = min + random.nextInt(max - min + 1);
                }
                candidate = new Candidate(center, y, valid);
                candidates.put(key, candidate);
            }
            if (!candidate.valid) return;
            BlockPos center = candidate.center;
            double dx = (double) center.getX() - origin.getX();
            double dz = (double) center.getZ() - origin.getZ();
            double distance = dx * dx + dz * dz;
            if (distance >= bestDistance) return;
            if (skipDefeated && StructureDefeatData.isDefeated(world, name, center)) return;
            if (hellstone) {
                world.getChunkFromBlockCoords(center);
                BlockPos actual = StructureDefeatData.position(world, name, center);
                if (actual == null) {
                    net.minecraft.world.gen.structure.template.Template template = new HellstoneStructures().template();
                    if (template == null) return;
                    int ceiling = world.getActualHeight() - template.getSize().getY() - 1;
                    int min = Math.max(1, Math.min(ceiling, Balance.integer("hellstone_ruin.minimumY", 40)));
                    int max = Math.max(min, Math.min(ceiling, Balance.integer("hellstone_ruin.maximumY", 72)));
                    actual = HellstoneStructures.groundedBase(world, center, template.getSize(), min, max,
                            Math.max(min, Math.min(max, candidate.y)));
                }
                if (actual == null) return;
                best = actual;
                bestDistance = distance;
                return;
            }
            best = center.up(candidate.y);
            bestDistance = distance;
        }

        private double outsideDistance() {
            double blocks = spacing * 16.0;
            double lastOffset = (spread - 1) * 16.0 + 8;
            double left = origin.getX() - ((regionX - radius - 1) * blocks + lastOffset);
            double right = (regionX + radius + 1) * blocks + 8 - origin.getX();
            double north = origin.getZ() - ((regionZ - radius - 1) * blocks + lastOffset);
            double south = (regionZ + radius + 1) * blocks + 8 - origin.getZ();
            return Math.min(Math.min(left, right), Math.min(north, south));
        }

        public BlockPos result() {
            if (best == null || hellstone || !world.isBlockLoaded(best)) return best;
            int y = world.getHeight(best).getY() - 1;
            if (name.equals("the_ocean_shrine")) {
                BlockPos column = new BlockPos(best.getX(), 0, best.getZ());
                while (y > 1 && (world.getBlockState(column.up(y)).getMaterial() == Material.WATER
                        || world.isAirBlock(column.up(y)))) y--;
            }
            return y < 1 || y >= world.getHeight() ? best : new BlockPos(best.getX(), y, best.getZ());
        }
    }
}
