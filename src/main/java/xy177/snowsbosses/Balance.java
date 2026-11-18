package xy177.snowsbosses;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.ConfigCategory;

public final class Balance {
    private static Configuration configuration;
    private static final Map<String, Double> NUMBERS = new HashMap<>();
    private static final Map<String, Boolean> FLAGS = new HashMap<>();
    private static final Map<String, String> TEXT = new HashMap<>();

    private Balance() {
    }

    public static void load(File file) {
        configuration = new Configuration(file);
        configuration.load();
        try (Reader reader = new InputStreamReader(Balance.class.getResourceAsStream(
                "/assets/snows_bosses/balance.json"), StandardCharsets.UTF_8)) {
            JsonObject defaults = new JsonParser().parse(reader).getAsJsonObject();
            Set<String> known = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
                String key = entry.getKey();
                known.add(key);
                String[] parts = split(key);
                JsonObject data = entry.getValue().getAsJsonObject();
                String description = data.get("zh").getAsString() + "\n" + data.get("en").getAsString();
                if (data.get("default").getAsJsonPrimitive().isString()) {
                    TEXT.put(key,configuration.get(parts[0],parts[1],data.get("default").getAsString(),description).getString());
                } else if (data.get("default").getAsJsonPrimitive().isBoolean()) {
                    FLAGS.put(key, configuration.get(parts[0], parts[1],
                            data.get("default").getAsBoolean(), description).getBoolean());
                } else {
                    double fallback = data.get("default").getAsDouble();
                    double min = data.has("min") ? data.get("min").getAsDouble() : 0.0;
                    double max = data.has("max") ? data.get("max").getAsDouble() : Double.MAX_VALUE;
                    double value = configuration.get(parts[0], parts[1], fallback,
                            description, min, max).getDouble();
                    NUMBERS.put(key, Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback);
                }
            }
            for (String categoryName : new HashSet<>(configuration.getCategoryNames())) {
                ConfigCategory category = configuration.getCategory(categoryName);
                for (String property : new HashSet<>(category.keySet())) {
                    if (!known.contains(categoryName + "." + property)) category.remove(property);
                }
                if (category.isEmpty()) configuration.removeCategory(category);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load balance configuration", exception);
        }
        save();
    }

    public static String text(String key) {
        String value=TEXT.get(key);
        if(value==null)throw new IllegalArgumentException("Unknown text configuration: "+key);
        return value;
    }

    public static double number(String key, double value) {
        Double cached = NUMBERS.get(key);
        if (cached != null) return cached;
        throw new IllegalArgumentException("Unknown numeric configuration: " + key);
    }

    public static int integer(String key, int value) {
        return (int) Math.min(Integer.MAX_VALUE, number(key, value));
    }

    public static int ticks(String key, int value) {
        return Math.max(1, integer(key, value));
    }

    public static boolean flag(String key, boolean value) {
        Boolean cached = FLAGS.get(key);
        if (cached != null) return cached;
        throw new IllegalArgumentException("Unknown boolean configuration: " + key);
    }

    private static String[] split(String key) {
        int dot = key.indexOf('.');
        return new String[]{key.substring(0, dot), key.substring(dot + 1)};
    }

    private static void save() {
        if (configuration.hasChanged()) configuration.save();
    }
}
