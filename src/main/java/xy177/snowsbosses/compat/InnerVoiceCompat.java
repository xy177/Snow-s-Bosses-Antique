package xy177.snowsbosses.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import xy177.snowsbosses.entity.Mechasent;
import net.minecraftforge.fml.common.Loader;
import org.apache.logging.log4j.LogManager;
import xy177.snowsbosses.entity.HellstoneGolem;

public final class InnerVoiceCompat {
    private static final String GOLEM = "snows_bosses:hellstone_golem";
    private static final String BASE = "snows_bosses_hellstone_golem";
    private static Profile profile;

    private InnerVoiceCompat() { }

    public static void install() {
        if (!Loader.isModLoaded("inner_voice_antique")) return;
        Path directory = directory();
        try {
            Files.createDirectories(directory);
            MechasentInnerVoice.install(directory);
            for (Path file : configs(directory)) {
                Map<String, String> values = readConfig(file);
                if (GOLEM.equals(values.get("entity"))) {
                    if (Boolean.parseBoolean(values.get("enabled"))
                            && (BASE + "_combat.txt").equals(values.get("texts"))
                            && !"false".equalsIgnoreCase(values.get("snowsBossesActions"))) {
                        String colors = "";
                        if (!values.containsKey("mainColor")) colors += "mainColor = \"D17C40\"\n";
                        if (!values.containsKey("accentColor")) colors += "accentColor = \"F2A85C\"\n";
                        if (!colors.isEmpty()) Files.write(file, (colors + new String(
                                Files.readAllBytes(file), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
                    }
                    return;
                }
            }
            copy(directory, BASE + "_combat.txt");
            copy(directory, BASE + "_summon_imps.txt");
            copy(directory, BASE + "_summon_totems.txt");
            copy(directory, BASE + ".toml");
        } catch (IOException exception) {
            LogManager.getLogger("snows_bosses").warn("Could not install default Inner Voice profile", exception);
        }
    }

    public static void reload() {
        Profile loaded = null;
        Path directory = directory();
        try {
            for (Path file : configs(directory)) {
                Map<String, String> values = readConfig(file);
                if (!GOLEM.equals(values.get("entity"))) continue;
                String actions = values.get("snowsBossesActions");
                boolean managed = actions == null ? (BASE + "_combat.txt").equals(values.get("texts"))
                        : Boolean.parseBoolean(actions);
                loaded = Boolean.parseBoolean(values.get("enabled")) && managed
                        ? new Profile(texts(directory, values.getOrDefault("summonImpsTexts", BASE + "_summon_imps.txt")),
                                texts(directory, values.getOrDefault("summonTotemsTexts", BASE + "_summon_totems.txt"))) : null;
            }
        } catch (IOException exception) {
            LogManager.getLogger("snows_bosses").warn("Could not read Inner Voice action profile", exception);
        }
        profile = loaded;
        MechasentInnerVoice.reload(directory);
    }

    public static ResourceLocation entityKey(Entity entity) {
        if (MechasentInnerVoice.managed() && entity instanceof Mechasent
                && !((Mechasent) entity).innerVoiceCombat()) return null;
        if (profile != null && entity instanceof HellstoneGolem
                && ((HellstoneGolem) entity).innerVoiceState() == 0) return null;
        return EntityList.getKey(entity);
    }

    public static boolean immediate(World world, int entityId) {
        if (world == null) return false;
        Entity entity = world.getEntityByID(entityId);
        if (MechasentInnerVoice.managed() && entity instanceof Mechasent) return ((Mechasent) entity).innerVoiceCombat();
        if (profile == null) return false;
        return entity instanceof HellstoneGolem && ((HellstoneGolem) entity).innerVoiceState() > 0;
    }

    public static List<String> lines(List<String> original, EntityPlayerSP player, int entityId) {
        Profile current = profile;
        if (player == null) return original;
        Entity entity = player.world.getEntityByID(entityId);
        if (MechasentInnerVoice.managed() && entity instanceof Mechasent)
            return ((Mechasent) entity).innerVoiceCombat() ? original : Collections.emptyList();
        if (current == null) return original;
        if (!(entity instanceof HellstoneGolem)) return original;
        int state = ((HellstoneGolem) entity).innerVoiceState();
        if (state == 0) return Collections.emptyList();
        List<String> selected = state == 2 ? current.imps : state == 3 ? current.totems : original;
        return selected.isEmpty() ? original : selected;
    }

    private static Path directory() {
        return Loader.instance().getConfigDir().toPath().resolve("inner_voice_antique").resolve("entities");
    }

    static List<Path> configs(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        if (Files.isDirectory(directory)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
                for (Path entry : entries) {
                    if (Files.isRegularFile(entry)
                            && entry.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".toml"))
                        files.add(entry);
                }
            }
        }
        Collections.sort(files);
        return files;
    }

    static Map<String, String> readConfig(Path file) throws IOException {
        Map<String, String> values = new HashMap<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.startsWith("[")) break;
            int equals = line.indexOf('=');
            if (equals < 1 || line.startsWith("#")) continue;
            String value = line.substring(equals + 1).trim();
            if (value.startsWith("\"")) {
                int end = value.lastIndexOf('"');
                if (end > 0) value = value.substring(1, end);
            } else {
                int comment = value.indexOf('#');
                if (comment >= 0) value = value.substring(0, comment).trim();
            }
            values.put(line.substring(0, equals).trim(), value);
        }
        return values;
    }

    static List<String> texts(Path directory, String name) throws IOException {
        if (name == null || name.trim().isEmpty()) return Collections.emptyList();
        Path root = directory.toAbsolutePath().normalize();
        Path file = root.resolve(name.replace('\\', '/')).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) return Collections.emptyList();
        List<String> lines = new ArrayList<>();
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (!line.isEmpty() && !line.startsWith("#")) lines.add(line);
        }
        return Collections.unmodifiableList(lines);
    }

    static void copy(Path directory, String name) throws IOException {
        Path target = directory.resolve(name);
        if (Files.exists(target)) return;
        try (InputStream source = InnerVoiceCompat.class.getResourceAsStream("/assets/snows_bosses/inner_voice/" + name)) {
            if (source == null) throw new IOException("Missing default voice resource " + name);
            Files.copy(source, target);
        }
    }

    private static final class Profile {
        private final List<String> imps;
        private final List<String> totems;
        private Profile(List<String> imps, List<String> totems) { this.imps = imps; this.totems = totems; }
    }
}
