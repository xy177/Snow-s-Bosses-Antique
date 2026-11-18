package xy177.snowsbosses.compat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import xy177.snowsbosses.network.Visuals;
import xy177.snowsbosses.entity.Mechasent;

public final class MechasentInnerVoice {
    private static final String ENTITY = "snows_bosses:mechasent";
    private static final String BASE = "snows_bosses_mechasent";
    private static final Deque<Event> pending = new ArrayDeque<>();
    private static List<String> events = Collections.emptyList();
    private static boolean managed;
    private static int mainColor = 0xD2DEE9;
    private static int accentColor = 0xFC4632;
    private static double range = 64;
    private static World eventWorld;
    private static Event active;
    private static long sequence;

    private MechasentInnerVoice() { }

    public static void register() {
        if (Loader.isModLoaded("inner_voice_antique")) MinecraftForge.EVENT_BUS.register(new MechasentInnerVoice());
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (event.getWorld() == eventWorld) world(null);
    }

    static void install(Path directory) throws IOException {
        for (Path file : InnerVoiceCompat.configs(directory))
            if (ENTITY.equals(InnerVoiceCompat.readConfig(file).get("entity"))) return;
        for (String suffix : new String[]{"_combat.txt", "_events.txt", ".toml"})
            InnerVoiceCompat.copy(directory, BASE + suffix);
    }

    static void reload(Path directory) {
        managed = false;
        events = Collections.emptyList();
        try {
            for (Path file : InnerVoiceCompat.configs(directory)) {
                Map<String, String> values = InnerVoiceCompat.readConfig(file);
                if (!ENTITY.equals(values.get("entity"))) continue;
                String actions = values.get("snowsBossesActions");
                managed = Boolean.parseBoolean(values.get("enabled")) && (actions == null
                        ? (BASE + "_combat.txt").equals(values.get("texts")) : Boolean.parseBoolean(actions));
                mainColor = color(values.get("mainColor"), 0xD2DEE9);
                accentColor = color(values.get("accentColor"), 0xFC4632);
                range = 64;
                try { range = Math.max(1, Math.min(512, Double.parseDouble(values.get("range")))); }
                catch (NumberFormatException | NullPointerException ignored) { }
                if (!Double.isFinite(range)) range = 64;
                events = managed ? InnerVoiceCompat.texts(directory, values.getOrDefault("eventTexts", BASE + "_events.txt"))
                        : Collections.emptyList();
            }
        } catch (IOException exception) {
            LogManager.getLogger("snows_bosses").warn("Could not read Mechasent Inner Voice profile", exception);
        }
        if (!managed) { pending.clear(); active = null; }
    }

    public static boolean managed() { return managed; }

    public static long delay(long original, Minecraft minecraft, int entityId) {
        return managed && minecraft != null && minecraft.world != null
                && minecraft.world.getEntityByID(entityId) instanceof Mechasent ? original * 2 : original;
    }

    public static void receive(Minecraft minecraft, Visuals.Message message) {
        if (!Loader.isModLoaded("inner_voice_antique") || !managed || minecraft.world == null
                || minecraft.player == null || message.count < 1 || message.count > events.size()) return;
        world(minecraft.world);
        if (message.count == 17 || message.count == 8) {
            pending.removeIf(event -> event.entityId == message.entityId);
            if (active != null && active.entityId == message.entityId) active = null;
        }
        String text = events.get(message.count - 1);
        if (text.isEmpty()) return;
        Event event = new Event(message, text, ++sequence, minecraft.world.getTotalWorldTime());
        if (message.count == 17) pending.addFirst(event);
        else pending.addLast(event);
        while (pending.size() > 8) { if (message.count == 17) pending.removeLast(); else pending.removeFirst(); }
    }

    public static Event event(Minecraft minecraft) {
        if (minecraft == null || minecraft.world == null || minecraft.player == null) {
            world(null);
            return null;
        }
        world(minecraft.world);
        if (!managed) return null;
        long now = minecraft.world.getTotalWorldTime();
        if (active != null && (now - active.started) * 50 >= active.duration) active = null;
        while (active == null && !pending.isEmpty()) {
            Event next = pending.removeFirst();
            if (now - next.received > 600) continue;
            if (minecraft.player.getDistanceSq(next.x, next.y, next.z) > range * range) continue;
            next.started = now;
            active = next;
        }
        if (active == null) return null;
        Entity entity = minecraft.world.getEntityByID(active.entityId);
        if (entity != null) { active.x = entity.posX; active.y = entity.posY + entity.height; active.z = entity.posZ; }
        if (minecraft.player.getDistanceSq(active.x, active.y, active.z) > range * range) {
            active = null;
            return null;
        }
        active.mainColor = mainColor;
        active.accentColor = accentColor;
        active.elapsed = Math.max(0, (now - active.started) * 50);
        return active;
    }

    private static void world(World world) {
        if (eventWorld == world) return;
        eventWorld = world;
        pending.clear();
        active = null;
    }

    private static int color(String value, int fallback) {
        if (value == null) return fallback;
        String hex = value.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.startsWith("0x") || hex.startsWith("0X")) hex = hex.substring(2);
        try { return Integer.parseInt(hex, 16) & 0xFFFFFF; }
        catch (NumberFormatException ignored) { return fallback; }
    }

    public static final class Event {
        public final String key;
        public final List<String> lines;
        public final int entityId;
        public int mainColor;
        public int accentColor;
        public double x, y, z;
        public final long duration;
        public long elapsed;
        private final long received;
        private long started;

        private Event(Visuals.Message message, String text, long sequence, long now) {
            key = "subtitle:entity:" + message.entityId + ":mechasent:" + sequence;
            lines = Collections.singletonList(text);
            entityId = message.entityId;
            x = message.x; y = message.y; z = message.z;
            duration = Math.max(4000, text.length() * 42L + 2000);
            received = now;
        }
    }
}
