package xy177.snowsbosses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class CoralArmorModel extends ModelBiped {
    private static final Map<String, CoralArmorModel> MODELS = new HashMap<>();

    private CoralArmorModel(String name) {
        super(0.0F);
        for (ModelRenderer part : new ModelRenderer[]{bipedHead, bipedHeadwear, bipedBody,
                bipedLeftArm, bipedRightArm, bipedLeftLeg, bipedRightLeg}) part.cubeList.clear();
        try (Reader reader = new InputStreamReader(CoralArmorModel.class.getResourceAsStream(
                "/assets/snows_bosses/models/armor/" + name + ".json"), StandardCharsets.UTF_8)) {
            JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
            textureWidth = data.getAsJsonArray("textureSize").get(0).getAsInt();
            textureHeight = data.getAsJsonArray("textureSize").get(1).getAsInt();
            Map<String, ModelRenderer> parts = new HashMap<>();
            for (JsonElement entry : data.getAsJsonArray("parts")) {
                JsonObject part = entry.getAsJsonObject();
                String partName = part.get("name").getAsString();
                ModelRenderer renderer = new ModelRenderer(this);
                JsonArray pivot = part.getAsJsonArray("pivot");
                renderer.setRotationPoint(f(pivot, 0), f(pivot, 1), f(pivot, 2));
                JsonArray rotation = part.getAsJsonArray("rotation");
                renderer.rotateAngleX = f(rotation, 0);
                renderer.rotateAngleY = f(rotation, 1);
                renderer.rotateAngleZ = f(rotation, 2);
                for (JsonElement box : part.getAsJsonArray("cubes")) {
                    JsonObject cube = box.getAsJsonObject();
                    ModelRenderer mesh = new ModelRenderer(this,
                            cube.getAsJsonArray("uv").get(0).getAsInt(), cube.getAsJsonArray("uv").get(1).getAsInt());
                    mesh.mirror = cube.get("mirror").getAsBoolean();
                    JsonArray origin = cube.getAsJsonArray("origin");
                    JsonArray size = cube.getAsJsonArray("size");
                    mesh.addBox(f(origin, 0), f(origin, 1), f(origin, 2),
                            size.get(0).getAsInt(), size.get(1).getAsInt(), size.get(2).getAsInt(), cube.get("inflate").getAsFloat());
                    renderer.addChild(mesh);
                }
                parts.put(partName, renderer);
                if (!part.get("parent").isJsonNull()) {
                    parts.get(part.get("parent").getAsString()).addChild(renderer);
                } else if (partName.equals("Head")) bipedHead = renderer;
                else if (partName.equals("Body")) bipedBody = renderer;
                else if (partName.equals("LeftArm")) bipedLeftArm = renderer;
                else if (partName.equals("RightArm")) bipedRightArm = renderer;
                else if (partName.startsWith("LeftLeg")) bipedLeftLeg = renderer;
                else if (partName.startsWith("RightLeg")) bipedRightLeg = renderer;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load coral armor model " + name, exception);
        }
        MoBendsArmorCompat.prepare(this);
    }

    private static float f(JsonArray values, int index) {
        return values.get(index).getAsFloat();
    }

    public static ModelBiped get(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped base) {
        String item = stack.getItem().getRegistryName().getResourcePath();
        String name = item.startsWith("seagolden") ? "seagolden_armor"
                : item.equals("coral_helmet") ? "coral_helmet"
                : slot == EntityEquipmentSlot.HEAD ? "another_coral_helmet" : item;
        CoralArmorModel model = MODELS.computeIfAbsent(name, CoralArmorModel::new);
        model.setModelAttributes(base);
        model.isSneak = entity.isSneaking();
        model.isRiding = entity.isRiding();
        model.isChild = entity.isChild();
        model.setVisible(false);
        model.bipedHead.showModel = slot == EntityEquipmentSlot.HEAD;
        model.bipedBody.showModel = slot == EntityEquipmentSlot.CHEST;
        model.bipedLeftArm.showModel = slot == EntityEquipmentSlot.CHEST;
        model.bipedRightArm.showModel = slot == EntityEquipmentSlot.CHEST;
        model.bipedLeftLeg.showModel = slot == EntityEquipmentSlot.LEGS || slot == EntityEquipmentSlot.FEET;
        model.bipedRightLeg.showModel = model.bipedLeftLeg.showModel;
        return model;
    }
}
