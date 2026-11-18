package xy177.snowsbosses.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Drowner;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID, value = Side.CLIENT)
public final class CorruptedBeaconBeamRenderer {
    private static final ResourceLocation OUTER = new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/corrupted_beacon_outer.png");
    private static final ResourceLocation CORE = new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/corrupted_beacon_core.png");
    private static final ResourceLocation GLOW_PURPLE = new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/corrupted_beacon_glow_purple.png");
    private static final ResourceLocation GLOW_PINK = new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/corrupted_beacon_glow_pink.png");
    private static final ResourceLocation SPARK = new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/corrupted_beacon_spark.png");

    private static final Map<Drowner, BeamState> BEAMS = new WeakHashMap<>();
    private static World particleWorld;
    private static long particleTick = Long.MIN_VALUE;

    private CorruptedBeaconBeamRenderer() {
    }

    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public static void renderWorld(RenderWorldLastEvent event) {
        Minecraft client = Minecraft.getMinecraft();
        World world = client.world;
        if (particleWorld != world) {
            BEAMS.clear();
            particleTick = Long.MIN_VALUE;
            particleWorld = world;
        }
        if (world == null) return;
        Tessellator tessellator = Tessellator.getInstance();
        long tick = world.getTotalWorldTime();
        long interval = tick / 5L;
        boolean spawnParticles = particleTick != interval;
        particleTick = interval;
        float partialTicks = event.getPartialTicks();
        float phase = (tick + partialTicks) * 0.025F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(-client.getRenderManager().viewerPosX, -client.getRenderManager().viewerPosY,
                -client.getRenderManager().viewerPosZ);
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof Drowner)) continue;
            renderData(tessellator, (Drowner) entity, tick, partialTicks, phase, spawnParticles);
        }
        GlStateManager.popMatrix();
    }

    private static void renderData(Tessellator tessellator, Drowner drowner, long tick,
            float partialTicks, float phase, boolean spawnParticles) {
        String data = drowner.getBeamData();
        if (data == null || data.length() == 0) {
            BEAMS.remove(drowner);
            return;
        }
        BeamState state = BEAMS.get(drowner);
        if (state == null) {
            state = parse(data, tick);
            if (state == null) return;
            BEAMS.put(drowner, state);
        } else {
            if (state.tick != tick) {
                state.previous = state.current;
                state.tick = tick;
            }
            if (!data.equals(state.data)) {
                BeamState next = parse(data, tick);
                if (next == null) {
                    BEAMS.remove(drowner);
                    return;
                }
                if (next.action != state.action || next.current.length != state.current.length)
                    state.previous = next.current;
                state.data = data;
                state.action = next.action;
                state.current = next.current;
            }
        }
        for (int index = 0; index < state.current.length; index++) {
            Vec3d[] current = state.current[index];
            Vec3d[] previous = state.previous[index];
            boolean interpolate = previous[0].squareDistanceTo(current[0]) <= 4.0D
                    && previous[1].squareDistanceTo(current[1]) <= 16.0D;
            Vec3d start = interpolate ? interpolate(previous[0], current[0], partialTicks) : current[0];
            Vec3d end = interpolate ? interpolate(previous[1], current[1], partialTicks) : current[1];
            float width = state.action == 9 && index == 4
                    ? Drowner.TRACKING_BEAM_RENDER_WIDTH : Drowner.BEAM_RENDER_WIDTH;
            renderBeam(tessellator, start, end, width, phase + index * 0.08F,
                    state.action == 9 && index < 4);
            if (state.action != 9 || index == 4) {
                renderImpact(tessellator, start, end, phase + index * 0.37F,
                        state.action == 9 && index == 4 ? 1.15F : 0.85F);
                if (spawnParticles)
                    hitParticles(drowner.world, end, drowner.world.getBlockState(new BlockPos(end)).getMaterial() == Material.WATER);
                if (state.action == 9 && index == 4) {
                    renderImpact(tessellator, end, start, phase + index * 0.37F, 1.15F);
                    if (spawnParticles)
                        hitParticles(drowner.world, start, drowner.world.getBlockState(new BlockPos(start)).getMaterial() == Material.WATER);
                }
            }
        }
    }

    private static BeamState parse(String data, long tick) {
        String[] fields = data.split(";");
        if (fields.length < 2) return null;
        try {
            int action = Integer.parseInt(fields[0]);
            Vec3d[][] segments = new Vec3d[fields.length - 1][2];
            for (int index = 1; index < fields.length; index++) {
                String[] coordinates = fields[index].split(",");
                if (coordinates.length != 6) return null;
                double[] values = new double[6];
                for (int coordinate = 0; coordinate < values.length; coordinate++) {
                    values[coordinate] = Double.parseDouble(coordinates[coordinate]);
                    if (!Double.isFinite(values[coordinate])) return null;
                }
                segments[index - 1][0] = new Vec3d(values[0], values[1], values[2]);
                segments[index - 1][1] = new Vec3d(values[3], values[4], values[5]);
            }
            return new BeamState(data, action, segments, tick);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Vec3d interpolate(Vec3d previous, Vec3d current, float partialTicks) {
        return previous.add(current.subtract(previous).scale(partialTicks));
    }

    private static final class BeamState {
        private String data;
        private int action;
        private Vec3d[][] previous;
        private Vec3d[][] current;
        private long tick;

        private BeamState(String data, int action, Vec3d[][] segments, long tick) {
            this.data = data;
            this.action = action;
            previous = current = segments;
            this.tick = tick;
        }
    }

    public static void renderBeam(Tessellator tessellator, Vec3d start, Vec3d end, float width, float phase) {
        renderBeam(tessellator, start, end, width, phase, false);
    }

    private static void renderBeam(Tessellator tessellator, Vec3d start, Vec3d end, float width, float phase,
            boolean ignoreDepth) {
        if (tessellator == null || start == null || end == null) return;
        Vec3d axis = end.subtract(start);
        double length = axis.lengthVector();
        if (length < 0.001D || width <= 0.0F) return;
        axis = axis.scale(1.0D / length);
        Vec3d reference = Math.abs(axis.y) > 0.9D ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d side = axis.crossProduct(reference).normalize();
        Vec3d up = side.crossProduct(axis).normalize();
        double rotation = phase * Math.PI * 2.0D;
        Vec3d rotatedSide = side.scale(Math.cos(rotation)).add(up.scale(Math.sin(rotation)));
        Vec3d rotatedUp = up.scale(Math.cos(rotation)).subtract(side.scale(Math.sin(rotation)));
        float scroll = phase * 0.56F;
        boolean depthEnabled = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        if (ignoreDepth) GlStateManager.disableDepth();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int[] outerState = prepareTexture(OUTER);
        drawTaperedCross(tessellator, start, end, rotatedSide, rotatedUp, width * 1.36F, scroll, 0.58F);
        restoreTexture(outerState);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        int[] coreState = prepareTexture(CORE);
        Vec3d coreSide = rotatedSide.scale(Math.cos(0.24D)).add(rotatedUp.scale(Math.sin(0.24D)));
        Vec3d coreUp = rotatedUp.scale(Math.cos(0.24D)).subtract(rotatedSide.scale(Math.sin(0.24D)));
        drawTaperedCross(tessellator, start, end, coreSide, coreUp, width * 0.30F, scroll * 1.55F, 0.42F);
        restoreTexture(coreState);
        renderArcs(tessellator, start, end, rotatedSide, rotatedUp, width, phase);
        GlStateManager.depthMask(true);
        if (ignoreDepth) {
            if (depthEnabled) GlStateManager.enableDepth();
            else GlStateManager.disableDepth();
        }
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawTaperedCross(Tessellator tessellator, Vec3d start, Vec3d end, Vec3d side,
            Vec3d up, float width, float scroll, float alpha) {
        drawTaperedQuad(tessellator, start, end, side, width, scroll, alpha);
        drawTaperedQuad(tessellator, start, end, up, width, scroll + 0.25F, alpha * 0.88F);
    }

    private static void drawTaperedQuad(Tessellator tessellator, Vec3d start, Vec3d end, Vec3d normal,
            float width, float scroll, float alpha) {
        Vec3d path = end.subtract(start);
        double length = path.lengthVector();
        int count = Math.max(1, (int) Math.ceil(length / 0.8D));
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int index = 0; index < count; index++) {
            double from = index / (double) count;
            double to = (index + 1) / (double) count;
            Vec3d near = start.add(path.scale(from));
            Vec3d far = start.add(path.scale(to));
            double nearTaper = Math.min(1.0D, Math.min(0.48D + from * length / 0.5D, 0.65D + (1.0D - from) * length / 0.5D));
            double farTaper = Math.min(1.0D, Math.min(0.48D + to * length / 0.5D, 0.65D + (1.0D - to) * length / 0.5D));
            Vec3d nearOffset = normal.scale(width * 0.5D * nearTaper);
            Vec3d farOffset = normal.scale(width * 0.5D * farTaper);
            float u0 = (float) (from * length / 0.8D) + scroll;
            float u1 = (float) (to * length / 0.8D) + scroll;
            vertex(buffer, near.subtract(nearOffset), u0, 0, alpha);
            vertex(buffer, near.add(nearOffset), u0, 1, alpha);
            vertex(buffer, far.add(farOffset), u1, 1, alpha);
            vertex(buffer, far.subtract(farOffset), u1, 0, alpha);
        }
        tessellator.draw();
    }

    private static void renderArcs(Tessellator tessellator, Vec3d start, Vec3d end,
            Vec3d side, Vec3d up, float width, float phase) {
        Vec3d path = end.subtract(start);
        double length = path.lengthVector();
        int count = Math.max(2, (int) Math.ceil(length / 0.45D));
        GlStateManager.disableTexture2D();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int arc = 0; arc < 3; arc++) {
            Vec3d previous = start;
            for (int index = 1; index <= count; index++) {
                double progress = index / (double) count;
                double angle = arc * Math.PI * 2.0D / 3.0D + phase * 2.3D
                        + Math.sin(index * 2.7D + arc + phase * 4.0D) * 0.45D;
                double radius = width * (0.55D + 0.3D * Math.sin(index * 3.1D + phase * 5.0D + arc));
                radius *= Math.min(1.0D, Math.min(progress * length, (1.0D - progress) * length) / 0.4D);
                Vec3d point = start.add(path.scale(progress)).add(side.scale(Math.cos(angle) * radius))
                        .add(up.scale(Math.sin(angle) * radius));
                Vec3d edge = side.scale(width * 0.025D);
                float alpha = 0.14F + 0.06F * (float) Math.sin(phase * 17.0D + arc);
                buffer.pos(previous.x - edge.x, previous.y - edge.y, previous.z - edge.z).color(0.75F, 0.12F, 1.0F, alpha).endVertex();
                buffer.pos(previous.x + edge.x, previous.y + edge.y, previous.z + edge.z).color(0.75F, 0.12F, 1.0F, alpha).endVertex();
                buffer.pos(point.x + edge.x, point.y + edge.y, point.z + edge.z).color(0.75F, 0.12F, 1.0F, alpha).endVertex();
                buffer.pos(point.x - edge.x, point.y - edge.y, point.z - edge.z).color(0.75F, 0.12F, 1.0F, alpha).endVertex();
                previous = point;
            }
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private static void vertex(BufferBuilder buffer, Vec3d position, float u, float v, float alpha) {
        buffer.pos(position.x, position.y, position.z).tex(u, v).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
    }

    private static int[] prepareTexture(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        int[] state = new int[]{
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S),
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER),
                GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER)
        };
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return state;
    }

    private static void restoreTexture(int[] state) {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, state[0]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, state[1]);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, state[2]);
    }

    private static void renderImpact(Tessellator tessellator, Vec3d start, Vec3d position, float phase, float scale) {
        Minecraft client = Minecraft.getMinecraft();
        Entity view = client.getRenderViewEntity();
        if (view == null) return;
        Vec3d look = view.getLookVec();
        Vec3d worldUp = Math.abs(look.y) > 0.92D ? new Vec3d(1, 0, 0) : new Vec3d(0, 1, 0);
        Vec3d side = look.crossProduct(worldUp).normalize();
        Vec3d up = side.crossProduct(look).normalize();
        float pulse = 0.92F + 0.16F * (float) Math.sin(phase * Math.PI * 2.0D);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int[] purpleState = prepareTexture(GLOW_PURPLE);
        Vec3d viewLook = view.getLookVec();
        double cameraOffset = isEntityImpact(client.world, position) ? 0.72D : 0.12D;
        Vec3d impact = position.subtract(viewLook.scale(cameraOffset));
        if (client.world.getBlockState(new BlockPos(position)).getMaterial().blocksMovement()) {
            Vec3d ray = position.subtract(start);
            if (ray.lengthSquared() > 1.0E-6) impact = impact.subtract(ray.normalize().scale(0.08D));
        }
        drawBillboard(tessellator, impact, side, up, scale * pulse * 1.7F, 0.76F);
        restoreTexture(purpleState);
        int[] pinkState = prepareTexture(GLOW_PINK);
        drawBillboard(tessellator, impact, side, up, scale * pulse * 0.94F, 0.88F);
        restoreTexture(pinkState);
        int[] sparkState = prepareTexture(SPARK);
        for (int index = 0; index < 12; index++) {
            double cycle = phase * 4.0D + index * 0.61803398875D;
            double age = cycle - Math.floor(cycle);
            double angle = index * 2.39996322973D;
            double reach = scale * age * (0.35D + (index % 4) * 0.12D);
            Vec3d offset = side.scale(Math.cos(angle) * reach)
                    .add(up.scale(Math.sin(angle) * reach + age * age * scale * 0.45D));
            drawBillboard(tessellator, impact.add(offset), side, up,
                    scale * (float) (0.34D * (1.0D - age)), (float) Math.sin(age * Math.PI));
        }
        restoreTexture(sparkState);
        GlStateManager.depthMask(true);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableCull();
        GlStateManager.enableAlpha();
        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static boolean isEntityImpact(World world, Vec3d position) {
        if (world == null) return false;
        AxisAlignedBB probe = new AxisAlignedBB(position.x - 0.2D, position.y - 0.2D, position.z - 0.2D,
                position.x + 0.2D, position.y + 0.2D, position.z + 0.2D);
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, probe)) {
            AxisAlignedBB bounds = entity.getEntityBoundingBox().grow(0.2D);
            if (position.x >= bounds.minX && position.x <= bounds.maxX
                    && position.y >= bounds.minY && position.y <= bounds.maxY
                    && position.z >= bounds.minZ && position.z <= bounds.maxZ) return true;
        }
        return false;
    }

    private static void drawBillboard(Tessellator tessellator, Vec3d center, Vec3d side, Vec3d up,
            float size, float alpha) {
        Vec3d sideOffset = side.scale(size * 0.5D);
        Vec3d upOffset = up.scale(size * 0.5D);
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertex(buffer, center.subtract(sideOffset).subtract(upOffset), 0, 1, alpha);
        vertex(buffer, center.add(sideOffset).subtract(upOffset), 1, 1, alpha);
        vertex(buffer, center.add(sideOffset).add(upOffset), 1, 0, alpha);
        vertex(buffer, center.subtract(sideOffset).add(upOffset), 0, 0, alpha);
        tessellator.draw();
    }

    public static void hitParticles(World world, Vec3d position, boolean underwater) {
        if (world == null || !world.isRemote) return;
        for (int i = 0; i < 8; i++) {
            double dx = (world.rand.nextDouble() - 0.5D) * 0.34D;
            double dy = (world.rand.nextDouble() - 0.5D) * 0.34D;
            double dz = (world.rand.nextDouble() - 0.5D) * 0.34D;
            world.spawnParticle(EnumParticleTypes.PORTAL, position.x, position.y, position.z, dx, dy, dz);
        }
        for (int i = 0; i < 6; i++) {
            double dx = (world.rand.nextDouble() - 0.5D) * 0.28D;
            double dy = (world.rand.nextDouble() - 0.5D) * 0.28D;
            double dz = (world.rand.nextDouble() - 0.5D) * 0.28D;
            world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, position.x, position.y, position.z, dx, dy, dz);
        }
        EnumParticleTypes secondary = underwater ? EnumParticleTypes.WATER_BUBBLE : EnumParticleTypes.SMOKE_NORMAL;
        for (int i = 0; i < 3; i++) {
            double dx = (world.rand.nextDouble() - 0.5D) * 0.22D;
            double dy = underwater ? world.rand.nextDouble() * 0.16D : world.rand.nextDouble() * 0.1D;
            double dz = (world.rand.nextDouble() - 0.5D) * 0.22D;
            world.spawnParticle(secondary, position.x, position.y, position.z, dx, dy, dz);
        }
    }
}
