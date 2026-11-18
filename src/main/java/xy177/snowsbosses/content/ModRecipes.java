package xy177.snowsbosses.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import xy177.farmersfuturedelight.api.WorkstationRecipes;
import xy177.farmersfuturedelight.api.WorkstationRegistryEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class ModRecipes {
    private ModRecipes() {
    }

    @SubscribeEvent
    public static void smithing(WorkstationRegistryEvent event) {
        WorkstationRecipes.register(WorkstationRecipes.Type.BLASTING, new ResourceLocation(SnowsBosses.MOD_ID, "hellstone_ingot_smelt"),
                java.util.Collections.singletonList(Ingredient.fromItem(HellstoneContent.RAW)), new ItemStack(HellstoneContent.INGOT), 100, 1.0F);
        try (Reader reader = new InputStreamReader(ModRecipes.class.getResourceAsStream(
                "/assets/snows_bosses/smithing.json"), StandardCharsets.UTF_8)) {
            JsonContext context = new JsonContext(SnowsBosses.MOD_ID);
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonArray()) {
                JsonObject data = element.getAsJsonObject();
                String id = data.get("id").getAsString();
                Ingredient template = CraftingHelper.getIngredient(data.get("template"), context);
                Ingredient base = CraftingHelper.getIngredient(data.get("base"), context);
                Ingredient addition = CraftingHelper.getIngredient(data.get("addition"), context);
                if (data.getAsJsonObject("base").has("item")) {
                    JsonObject baseData = data.getAsJsonObject("base");
                    String baseName = baseData.get("item").getAsString();
                    Item baseItem = baseData.has("type") && baseData.get("type").getAsString().equals("snows_bosses:ffd_item")
                            ? HellstoneContent.ffd(baseName).getItem()
                            : ForgeRegistries.ITEMS.getValue(new ResourceLocation(baseName));
                    base = new Ingredient(new ItemStack(baseItem)) {
                        @Override
                        public boolean apply(@Nullable ItemStack stack) {
                            return stack != null && !stack.isEmpty() && baseItem != null && stack.getItem() == baseItem;
                        }

                        @Override
                        public boolean isSimple() {
                            return false;
                        }
                    };
                }
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(data.get("result").getAsString()));
                ItemStack result = new ItemStack(item);
                WorkstationRecipes.register(WorkstationRecipes.Type.SMITHING,
                        new ResourceLocation(SnowsBosses.MOD_ID, id), Arrays.asList(template, base, addition),
                        inputs -> WorkstationRecipes.copySmithingData(inputs.get(1), result), 1, 0.0F);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot register smithing recipes", exception);
        }
    }
}
