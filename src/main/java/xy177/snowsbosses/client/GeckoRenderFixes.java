package xy177.snowsbosses.client;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumFacing;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;

final class GeckoRenderFixes {
    private static final Set<GeoModel> FIXED_MODELS = Collections.newSetFromMap(new WeakHashMap<GeoModel, Boolean>());

    private GeckoRenderFixes() {
    }

    static synchronized GeoModel correctMirroredNormals(GeoModel model) {
        if (FIXED_MODELS.add(model)) {
            for (GeoBone bone : model.topLevelBones) correctBone(bone);
        }
        return model;
    }

    static void renderOverlay(Runnable render) {
        GlStateManager.enablePolygonOffset();
        GlStateManager.doPolygonOffset(-1.0F, -1.0F);
        try {
            render.run();
        } finally {
            GlStateManager.doPolygonOffset(0.0F, 0.0F);
            GlStateManager.disablePolygonOffset();
        }
    }

    private static void correctBone(GeoBone bone) {
        boolean mirrored = Boolean.TRUE.equals(bone.mirror);
        for (GeoCube cube : bone.childCubes) {
            if (mirrored || Boolean.TRUE.equals(cube.mirror)) correctCubeNormals(cube);
        }
        for (GeoBone child : bone.childBones) correctBone(child);
    }

    private static void correctCubeNormals(GeoCube cube) {
        for (int i = 0; i < cube.quads.length; i++) {
            GeoQuad quad = cube.quads[i];
            if (quad == null || quad.normal.getX() == 0) continue;
            EnumFacing flipped = quad.normal.getX() < 0 ? EnumFacing.EAST : EnumFacing.WEST;
            GeoVertex[] vertices = quad.vertices;
            GeoQuad corrected = new GeoQuad(vertices.clone(), 0, 0, 0, 0, 1, 1, Boolean.FALSE, flipped);
            corrected.vertices = vertices;
            cube.quads[i] = corrected;
        }
    }
}
