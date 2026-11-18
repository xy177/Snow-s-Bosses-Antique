package xy177.snowsbosses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class MechasentArmorModel extends ModelBiped {
    private static final Map<EntityEquipmentSlot, MechasentArmorModel> MODELS = new EnumMap<>(EntityEquipmentSlot.class);
    private ModelBiped wearer;

    private MechasentArmorModel(EntityEquipmentSlot slot) {
        super(0);
        textureWidth = textureHeight = 64;
        for (ModelRenderer part : parts()) part.cubeList.clear();
        String wanted = slot == EntityEquipmentSlot.HEAD ? "helmet" : slot == EntityEquipmentSlot.CHEST ? "chestplate"
                : slot == EntityEquipmentSlot.LEGS ? "leggings" : "boots";
        try (InputStreamReader reader = new InputStreamReader(MechasentArmorModel.class.getResourceAsStream(
                "/assets/snows_bosses/models/armor/mechasent_armor.json"), StandardCharsets.UTF_8)) {
            for (JsonElement element : new JsonParser().parse(reader).getAsJsonObject().getAsJsonArray("cubes")) {
                JsonObject data = element.getAsJsonObject();
                if (!wanted.equals(data.get("slot").getAsString())) continue;
                ModelRenderer mesh = new ModelRenderer(this);
                mesh.cubeList.add(new FaceBox(mesh, data));
                part(data.get("part").getAsString()).addChild(mesh);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load mechasent armor model", exception);
        }
        MoBendsArmorCompat.prepare(this);
    }

    public static ModelBiped get(EntityEquipmentSlot slot) { return MODELS.computeIfAbsent(slot, MechasentArmorModel::new); }

    @Override
    public void setModelAttributes(ModelBase model) {
        super.setModelAttributes(model);
        wearer = model instanceof ModelBiped && model != this ? (ModelBiped) model : null;
    }

    @Override
    public void setRotationAngles(float swing, float amount, float age, float yaw, float pitch, float scale, Entity entity) {
        if (wearer == null) { super.setRotationAngles(swing, amount, age, yaw, pitch, scale, entity); return; }
        ModelRenderer[] own = parts();
        ModelRenderer[] source = {wearer.bipedHead, wearer.bipedHeadwear, wearer.bipedBody,
                wearer.bipedRightArm, wearer.bipedLeftArm, wearer.bipedRightLeg, wearer.bipedLeftLeg};
        for (int i = 0; i < own.length; i++) copyModelAngles(source[i], own[i]);
    }

    private ModelRenderer[] parts() { return new ModelRenderer[]{bipedHead, bipedHeadwear, bipedBody,
            bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg}; }

    private ModelRenderer part(String name) {
        switch (name) {
            case "head": return bipedHead;
            case "body": return bipedBody;
            case "left_arm": return bipedLeftArm;
            case "right_arm": return bipedRightArm;
            case "left_leg": return bipedLeftLeg;
            case "right_leg": return bipedRightLeg;
            default: throw new IllegalArgumentException(name);
        }
    }

    private static float f(JsonArray array, int i) { return array.get(i).getAsFloat(); }

    private static final class FaceBox extends ModelBox implements MoBendsArmorCompat.Geometry {
        private final List<TexturedQuad> faces = new ArrayList<>();

        FaceBox(ModelRenderer renderer, JsonObject data) {
            super(renderer, 0, 0, 0, 0, 0, 1, 1, 1, 0);
            JsonArray origin = data.getAsJsonArray("origin"), size = data.getAsJsonArray("size");
            float inflate = data.get("inflate").getAsFloat();
            float x0 = f(origin, 0) - inflate, y0 = f(origin, 1) - inflate, z0 = f(origin, 2) - inflate;
            float x1 = f(origin, 0) + f(size, 0) + inflate, y1 = f(origin, 1) + f(size, 1) + inflate,
                    z1 = f(origin, 2) + f(size, 2) + inflate;
            PositionTextureVertex[] v = {v(x0,y0,z0),v(x1,y0,z0),v(x1,y1,z0),v(x0,y1,z0),
                    v(x0,y0,z1),v(x1,y0,z1),v(x1,y1,z1),v(x0,y1,z1)};
            String[] names = {"west","east","up","down","north","south"};
            int[][] indices = {{5,1,2,6},{0,4,7,3},{5,4,0,1},{2,3,7,6},{1,0,3,2},{4,5,6,7}};
            JsonObject uv = data.getAsJsonObject("uv");
            for (int i = 0; i < names.length; i++) {
                JsonArray coords = uv.getAsJsonArray(names[i]);
                if (f(coords,0) == f(coords,2) || f(coords,1) == f(coords,3)) continue;
                int[] idx = indices[i];
                faces.add(new TexturedQuad(new PositionTextureVertex[]{v[idx[0]],v[idx[1]],v[idx[2]],v[idx[3]]},
                        coords.get(0).getAsInt(), coords.get(1).getAsInt(), coords.get(2).getAsInt(), coords.get(3).getAsInt(),64,64));
            }
        }

        private static PositionTextureVertex v(float x, float y, float z) { return new PositionTextureVertex(x,y,z,0,0); }
        @Override public TexturedQuad[] quads() { return faces.toArray(new TexturedQuad[0]); }
        @Override public void render(BufferBuilder buffer, float scale) { for (TexturedQuad face : faces) face.draw(buffer,scale); }
    }
}
