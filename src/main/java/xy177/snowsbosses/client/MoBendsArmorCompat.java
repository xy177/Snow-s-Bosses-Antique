package xy177.snowsbosses.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class MoBendsArmorCompat {
    private MoBendsArmorCompat() {
    }

    static void prepare(ModelBiped model) {
        if (!Loader.isModLoaded("mobends")) return;
        split(model, model.bipedRightArm, 4);
        split(model, model.bipedLeftArm, 4);
        split(model, model.bipedRightLeg, 6);
        split(model, model.bipedLeftLeg, 6);
    }

    private static void split(ModelBiped model, ModelRenderer part, float joint) {
        List<TexturedQuad> upper = new ArrayList<>();
        List<TexturedQuad> lower = new ArrayList<>();
        Matrix4d identity = new Matrix4d();
        identity.setIdentity();
        collect(part, identity, joint, upper, lower);
        part.cubeList.clear();
        if (part.childModels != null) part.childModels.clear();
        attach(model, part, upper, 0);
        attach(model, part, lower, joint);
    }

    private static void collect(ModelRenderer part, Matrix4d transform, float joint,
            List<TexturedQuad> upper, List<TexturedQuad> lower) {
        for (ModelBox box : part.cubeList) {
            TexturedQuad[] faces = box instanceof Geometry ? ((Geometry) box).quads() : vanillaQuads(box);
            for (TexturedQuad face : faces) {
                if (face == null) continue;
                List<PositionTextureVertex> vertices = new ArrayList<>(4);
                boolean above = false;
                boolean below = false;
                for (PositionTextureVertex vertex : face.vertexPositions) {
                    Point3d point = new Point3d(vertex.vector3D.x, vertex.vector3D.y, vertex.vector3D.z);
                    transform.transform(point);
                    vertices.add(new PositionTextureVertex(new Vec3d(point.x, point.y, point.z),
                            vertex.texturePositionX, vertex.texturePositionY));
                    above |= point.y < joint;
                    below |= point.y > joint;
                }
                if (!below) append(upper, vertices, 0);
                else if (!above) append(lower, vertices, joint);
                else {
                    append(upper, clip(vertices, joint, false), 0);
                    append(lower, clip(vertices, joint, true), joint);
                }
            }
        }
        if (part.childModels == null) return;
        for (ModelRenderer child : part.childModels) {
            if (child.isHidden || !child.showModel) continue;
            Matrix4d local = new Matrix4d();
            local.setIdentity();
            local.m03 = child.rotationPointX + 16 * child.offsetX;
            local.m13 = child.rotationPointY + 16 * child.offsetY;
            local.m23 = child.rotationPointZ + 16 * child.offsetZ;
            Matrix4d rotation = new Matrix4d();
            rotation.rotZ(child.rotateAngleZ);
            local.mul(rotation);
            rotation.rotY(child.rotateAngleY);
            local.mul(rotation);
            rotation.rotX(child.rotateAngleX);
            local.mul(rotation);
            Matrix4d combined = new Matrix4d(transform);
            combined.mul(local);
            collect(child, combined, joint, upper, lower);
        }
    }

    private static List<PositionTextureVertex> clip(List<PositionTextureVertex> vertices, float joint, boolean lower) {
        List<PositionTextureVertex> clipped = new ArrayList<>(5);
        PositionTextureVertex previous = vertices.get(vertices.size() - 1);
        boolean previousInside = lower ? previous.vector3D.y >= joint : previous.vector3D.y <= joint;
        for (PositionTextureVertex current : vertices) {
            boolean inside = lower ? current.vector3D.y >= joint : current.vector3D.y <= joint;
            if (inside != previousInside) {
                double fraction = (joint - previous.vector3D.y) / (current.vector3D.y - previous.vector3D.y);
                Vec3d point = previous.vector3D.add(current.vector3D.subtract(previous.vector3D).scale(fraction));
                clipped.add(new PositionTextureVertex(point,
                        (float) (previous.texturePositionX + (current.texturePositionX - previous.texturePositionX) * fraction),
                        (float) (previous.texturePositionY + (current.texturePositionY - previous.texturePositionY) * fraction)));
            }
            if (inside) clipped.add(current);
            previous = current;
            previousInside = inside;
        }
        return clipped;
    }

    private static void append(List<TexturedQuad> faces, List<PositionTextureVertex> polygon, float offset) {
        List<PositionTextureVertex> points = new ArrayList<>(polygon.size());
        for (PositionTextureVertex vertex : polygon) {
            if (points.isEmpty() || vertex.vector3D.squareDistanceTo(points.get(points.size() - 1).vector3D) > 1.0E-12) {
                points.add(vertex);
            }
        }
        if (points.size() > 1 && points.get(0).vector3D.squareDistanceTo(points.get(points.size() - 1).vector3D) <= 1.0E-12) {
            points.remove(points.size() - 1);
        }
        if (points.size() < 3) return;
        PositionTextureVertex[] vertices = new PositionTextureVertex[points.size()];
        for (int i = 0; i < vertices.length; i++) {
            PositionTextureVertex vertex = points.get(i);
            vertices[i] = new PositionTextureVertex(vertex.vector3D.addVector(0, -offset, 0),
                    vertex.texturePositionX, vertex.texturePositionY);
        }
        if (vertices.length == 4) {
            faces.add(new TexturedQuad(vertices));
        } else {
            for (int i = 1; i < vertices.length - 1; i++) {
                faces.add(new TexturedQuad(new PositionTextureVertex[]{vertices[0], vertices[i], vertices[i + 1], vertices[i + 1]}));
            }
        }
    }

    private static void attach(ModelBiped model, ModelRenderer part, List<TexturedQuad> faces, float offset) {
        if (faces.isEmpty()) return;
        ModelRenderer mesh = new ModelRenderer(model);
        mesh.setRotationPoint(0, offset, 0);
        mesh.cubeList.add(new QuadBox(mesh, faces));
        part.addChild(mesh);
    }

    private static TexturedQuad[] vanillaQuads(ModelBox box) {
        try {
            return (TexturedQuad[]) VanillaQuads.FIELD.get(box);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read armor geometry", exception);
        }
    }

    interface Geometry {
        TexturedQuad[] quads();
    }

    private static final class VanillaQuads {
        private static final Field FIELD = ReflectionHelper.findField(ModelBox.class, "quadList", "field_78254_i");
    }

    private static final class QuadBox extends ModelBox implements Geometry {
        private final TexturedQuad[] faces;

        private QuadBox(ModelRenderer renderer, List<TexturedQuad> faces) {
            super(renderer, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            this.faces = faces.toArray(new TexturedQuad[0]);
        }

        @Override
        public TexturedQuad[] quads() {
            return faces;
        }

        @Override
        public void render(BufferBuilder buffer, float scale) {
            for (TexturedQuad face : faces) face.draw(buffer, scale);
        }
    }
}
