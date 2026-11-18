package xy177.snowsbosses.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
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
public final class HellstoneArmorModel extends ModelBiped {
    private static final Map<EntityEquipmentSlot, HellstoneArmorModel> MODELS = new EnumMap<>(EntityEquipmentSlot.class);
    private ModelBiped wearer;

    private HellstoneArmorModel(EntityEquipmentSlot slot) {
        super(0.0F);
        for (ModelRenderer part : parts()) part.cubeList.clear();
        textureWidth = 128;
        textureHeight = 128;
        try (Reader reader = new InputStreamReader(HellstoneArmorModel.class.getResourceAsStream(
                "/assets/snows_bosses/models/armor/hellstone_armor.json"), StandardCharsets.UTF_8)) {
            JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
            for (JsonElement element : data.getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                String name = cube.get("part").getAsString();
                boolean leg = name.endsWith("_leg");
                if (slot == EntityEquipmentSlot.HEAD ? !name.equals("head")
                        : slot == EntityEquipmentSlot.CHEST ? name.equals("head") || leg : !leg) continue;
                JsonArray uv = cube.getAsJsonArray("uv");
                JsonArray origin = cube.getAsJsonArray("origin");
                JsonArray size = cube.getAsJsonArray("size");
                ModelRenderer mesh = new ModelRenderer(this, uv.get(0).getAsInt(), uv.get(1).getAsInt());
                JsonArray pivot = cube.getAsJsonArray("pivot");
                mesh.setRotationPoint(f(pivot, 0), f(pivot, 1), f(pivot, 2));
                JsonArray rotation = cube.getAsJsonArray("rotation");
                mesh.rotateAngleX = (float) Math.toRadians(f(rotation, 0));
                mesh.rotateAngleY = (float) Math.toRadians(f(rotation, 1));
                mesh.rotateAngleZ = (float) Math.toRadians(f(rotation, 2));
                float inflate = cube.get("inflate").getAsFloat();
                if (leg) {
                    mesh.cubeList.add(new LegBox(mesh, uv.get(0).getAsInt(), uv.get(1).getAsInt(),
                            inflate + 0.26F, slot == EntityEquipmentSlot.FEET));
                } else {
                    float clearance = size.get(2).getAsInt() == 0 ? 0.0F
                            : name.equals("head") ? size.get(0).getAsInt() == 8 ? 0.51F : 0.0F : 0.26F;
                    mesh.addBox(f(origin, 0), f(origin, 1), f(origin, 2), size.get(0).getAsInt(),
                            size.get(1).getAsInt(), size.get(2).getAsInt(), inflate + clearance);
                }
                part(name).addChild(mesh);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load hellstone armor model", exception);
        }
        MoBendsArmorCompat.prepare(this);
    }

    public static ModelBiped get(EntityEquipmentSlot slot) {
        return MODELS.computeIfAbsent(slot, HellstoneArmorModel::new);
    }

    @Override
    public void setModelAttributes(ModelBase model) {
        super.setModelAttributes(model);
        wearer = model instanceof ModelBiped && model != this ? (ModelBiped) model : null;
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch, float scale, Entity entity) {
        if (wearer == null) {
            super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
            return;
        }
        ModelRenderer[] own = parts();
        ModelRenderer[] source = {wearer.bipedHead, wearer.bipedHeadwear, wearer.bipedBody,
                wearer.bipedRightArm, wearer.bipedLeftArm, wearer.bipedRightLeg, wearer.bipedLeftLeg};
        for (int i = 0; i < own.length; i++) copyModelAngles(source[i], own[i]);
    }

    private ModelRenderer[] parts() {
        return new ModelRenderer[]{bipedHead, bipedHeadwear, bipedBody,
                bipedRightArm, bipedLeftArm, bipedRightLeg, bipedLeftLeg};
    }

    private ModelRenderer part(String name) {
        switch (name) {
            case "head": return bipedHead;
            case "body": return bipedBody;
            case "right_arm": return bipedRightArm;
            case "left_arm": return bipedLeftArm;
            case "right_leg": return bipedRightLeg;
            case "left_leg": return bipedLeftLeg;
            default: throw new IllegalArgumentException("Unknown armor part " + name);
        }
    }

    private static float f(JsonArray values, int index) {
        return values.get(index).getAsFloat();
    }

    private static final class LegBox extends ModelBox implements MoBendsArmorCompat.Geometry {
        private final TexturedQuad[] faces;

        private LegBox(ModelRenderer renderer, int u, int v, float inflate, boolean boots) {
            super(renderer, u, v, -2, 0, -2, 4, 12, 4, inflate);
            float x0 = -2 - inflate;
            float x1 = 2 + inflate;
            float y0 = boots ? 8 + inflate / 3 : -inflate;
            float y1 = boots ? 12 + inflate : 8 + inflate / 3;
            float z0 = -2 - inflate;
            float z1 = 2 + inflate;
            PositionTextureVertex a = new PositionTextureVertex(x0, y0, z0, 0, 0);
            PositionTextureVertex b = new PositionTextureVertex(x1, y0, z0, 0, 0);
            PositionTextureVertex c = new PositionTextureVertex(x1, y1, z0, 0, 0);
            PositionTextureVertex d = new PositionTextureVertex(x0, y1, z0, 0, 0);
            PositionTextureVertex e = new PositionTextureVertex(x0, y0, z1, 0, 0);
            PositionTextureVertex f = new PositionTextureVertex(x1, y0, z1, 0, 0);
            PositionTextureVertex g = new PositionTextureVertex(x1, y1, z1, 0, 0);
            PositionTextureVertex h = new PositionTextureVertex(x0, y1, z1, 0, 0);
            int top = v + 4 + (boots ? 8 : 0);
            int bottom = v + 4 + (boots ? 12 : 8);
            faces = new TexturedQuad[]{
                    new TexturedQuad(new PositionTextureVertex[]{f, b, c, g}, u + 8, top, u + 12, bottom, 128, 128),
                    new TexturedQuad(new PositionTextureVertex[]{a, e, h, d}, u, top, u + 4, bottom, 128, 128),
                    boots ? new TexturedQuad(new PositionTextureVertex[]{c, d, h, g}, u + 8, v + 4, u + 12, v, 128, 128)
                            : new TexturedQuad(new PositionTextureVertex[]{f, e, a, b}, u + 4, v, u + 8, v + 4, 128, 128),
                    new TexturedQuad(new PositionTextureVertex[]{b, a, d, c}, u + 4, top, u + 8, bottom, 128, 128),
                    new TexturedQuad(new PositionTextureVertex[]{e, f, g, h}, u + 12, top, u + 16, bottom, 128, 128)
            };
        }

        @Override
        public TexturedQuad[] quads() {
            return faces;
        }

        @Override
        public void render(BufferBuilder renderer, float scale) {
            for (TexturedQuad face : faces) face.draw(renderer, scale);
        }
    }
}
