package xy177.snowsbosses.client;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class HellstoneSkillParticle extends Particle {
    private static final Curves CURVES = curves();
    private static final ResourceLocation SPAWN_RING = new ResourceLocation("snows_bosses", "textures/particle/hellstone_spawn_ring.png");
    private static final ResourceLocation SLAM_RING = new ResourceLocation("snows_bosses", "textures/particle/hellstone_slam_ring.png");
    private final boolean sourceRing, slam;
    private final double radius;
    private final boolean summoning;
    private final double spin;

    public HellstoneSkillParticle(World world, String kind, double x, double y, double z) {
        super(world, x, y, z);
        summoning = kind.equals("hellstone_summon_ring");
        slam = kind.equals("hellstone_slam_ring");
        sourceRing = slam || kind.equals("hellstone_spawn_ring");
        radius = summoning || kind.equals("hellstone_slam_ring") ? 4 : kind.equals("hellstone_burst_ring") ? 3 : 1.2;
        particleMaxAge = summoning ? 72 : 26;
        spin = rand.nextDouble() * Math.PI * 2;
        motionX = motionY = motionZ = 0;
        canCollide = false;
    }
    @Override
    public void onUpdate() {
        prevPosX = posX; prevPosY = posY; prevPosZ = posZ;
        if (++particleAge >= particleMaxAge) { setExpired(); return; }
        if ((sourceRing || summoning) && particleAge % 3 == 0) {
            for (int index = 0; index < (summoning ? 6 : 4); index++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double distance = radius * rand.nextDouble();
                Minecraft.getMinecraft().effectRenderer.addEffect(new Spark(world,
                        posX + Math.cos(angle) * distance, posY + 0.15, posZ + Math.sin(angle) * distance,
                        Math.cos(angle) * 0.06, 0.08 + rand.nextDouble() * 0.10, Math.sin(angle) * 0.06));
            }
        }
        if (!sourceRing && !summoning && particleAge % 3 == 0) {
            double progress = Math.min(1, particleAge / (summoning ? 38.0 : 18.0));
            for (int index = 0; index < (summoning ? 4 : 3); index++) {
                double angle = rand.nextDouble() * Math.PI * 2;
                double distance = radius * progress * (0.65 + rand.nextDouble() * 0.35);
                Minecraft.getMinecraft().effectRenderer.addEffect(new HellstoneParticle(world, "hellstone_fire",
                        posX + Math.cos(angle) * distance, posY + 0.12, posZ + Math.sin(angle) * distance,
                        Math.cos(angle) * 0.035, 0.04 + rand.nextDouble() * 0.07, Math.sin(angle) * 0.035));
            }
        }
    }
    @Override
    public int getFXLayer() { return 3; }
    @Override
    public boolean shouldDisableDepth() { return false; }
    @Override
    public void renderParticle(BufferBuilder ignored, Entity camera, float partial, float rotationX, float rotationZ,
            float rotationYZ, float rotationXY, float rotationXZ) {
        if (summoning) return;
        if (sourceRing) { renderSourceRing(partial); return; }
        double age = particleAge + partial;
        double progress = Math.min(1, age / (summoning ? 38.0 : 18.0));
        double outer = 0.15 + (radius - 0.15) * (1 - Math.pow(1 - progress, 2));
        float alpha = (float) Math.min(1, age / 4) * (float) Math.pow(1 - age / particleMaxAge, 0.7);
        double x = posX - interpPosX, y = posY - interpPosY, z = posZ - interpPosZ;
        GlStateManager.disableTexture2D(); GlStateManager.disableLighting(); GlStateManager.disableCull();
        GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.depthMask(false);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        band(buffer, x, y, z, outer, 0.13 + outer * 0.04, age * 0.018 + spin, alpha * 0.65F, false);
        band(buffer, x, y + 0.018, z, outer * 0.82, 0.045, -age * 0.025 + spin, alpha, true);
        for (int index = 0; index < 12; index++) {
            double angle = spin + index * Math.PI / 6 + age * 0.012;
            double length = outer * (0.20 + 0.12 * Math.sin(index * 2.7));
            double near = outer - length;
            point(buffer, x, y + 0.025, z, near, angle - 0.018, 1, 0.25F, 0.015F, alpha * 0.65F);
            point(buffer, x, y + 0.025, z, outer, angle - 0.012, 1, 0.55F, 0.06F, alpha);
            point(buffer, x, y + 0.025, z, outer, angle + 0.012, 1, 0.55F, 0.06F, alpha);
            point(buffer, x, y + 0.025, z, near, angle + 0.018, 1, 0.25F, 0.015F, alpha * 0.65F);
        }
        tessellator.draw();
        GlStateManager.depthMask(true); GlStateManager.enableCull(); GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
    }
    private void renderSourceRing(float partial) {
        float progress = Math.min(1, (particleAge + partial) / particleMaxAge);
        float[] color = slam ? CURVES.slamColor : CURVES.spawnColor;
        float alpha = sample(color, 4, 3, progress) * emission(color, progress);
        double size = radius * sample(CURVES.spawnExpansion, 1, 0, progress);
        if (slam) size *= 1.5;
        double x = posX - interpPosX, y = posY - interpPosY, z = posZ - interpPosZ;
        GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.enableTexture2D();
        GlStateManager.enableBlend(); GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE); GlStateManager.depthMask(false);
        Minecraft.getMinecraft().getTextureManager().bindTexture(slam ? SLAM_RING : SPAWN_RING);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(x-size,y,z-size).tex(0,0).color(1F,0.55F,0.12F,alpha).endVertex();
        buffer.pos(x-size,y,z+size).tex(0,1).color(1F,0.55F,0.12F,alpha).endVertex();
        buffer.pos(x+size,y,z+size).tex(1,1).color(1F,0.55F,0.12F,alpha).endVertex();
        buffer.pos(x+size,y,z-size).tex(1,0).color(1F,0.55F,0.12F,alpha).endVertex();
        Tessellator.getInstance().draw();
        restore();
    }
    private static void restore() {
        GlStateManager.depthMask(true); GlStateManager.enableCull(); GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA); GlStateManager.disableBlend();
        GlStateManager.color(1,1,1,1);
    }
    private static float sample(float[] values, int width, int component, float progress) {
        float index = Math.max(0,Math.min(1,progress)) * (values.length / width - 1);
        int first = (int) index, last = Math.min(first+1,values.length/width-1);
        return values[first*width+component] + (values[last*width+component]-values[first*width+component])*(index-first);
    }
    private static float emission(float[] color, float progress) {
        float peak=0, current=0;
        for(int i=0;i<color.length;i+=4) peak=Math.max(peak,Math.max(color[i],Math.max(color[i+1],color[i+2])));
        for(int i=0;i<3;i++) current=Math.max(current,sample(color,4,i,progress));
        return (float)(Math.log1p(current)/Math.log1p(peak));
    }
    private static Curves curves() {
        try(Reader reader=new InputStreamReader(HellstoneSkillParticle.class.getResourceAsStream(
                "/assets/snows_bosses/particles/hellstone_skills.json"),StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader,Curves.class);
        } catch(Exception failure) { throw new IllegalStateException("Unable to load hellstone skill curves",failure); }
    }
    private static final class Curves { float[] spawnColor,spawnExpansion,slamColor,sparkColor,sparkSize; }
    private static final class Spark extends Particle {
        Spark(World world,double x,double y,double z,double dx,double dy,double dz) {
            super(world,x,y,z);motionX=dx;motionY=dy;motionZ=dz;particleGravity=0.03F;
            particleMaxAge=14+rand.nextInt(9);canCollide=false;
        }
        @Override public int getFXLayer() { return 3; }
        @Override public boolean shouldDisableDepth() { return false; }
        @Override public void renderParticle(BufferBuilder ignored,Entity camera,float partial,float rx,float rz,float ryz,float rxy,float rxz) {
            float age=Math.min(1,(particleAge+partial)/particleMaxAge);
            float w=sample(CURVES.sparkSize,2,0,age)*0.035F,h=sample(CURVES.sparkSize,2,1,age)*0.035F;
            float red=sample(CURVES.sparkColor,4,0,age),green=sample(CURVES.sparkColor,4,1,age),blue=sample(CURVES.sparkColor,4,2,age);
            float max=Math.max(red,Math.max(green,blue)),alpha=emission(CURVES.sparkColor,age)*sample(CURVES.sparkColor,4,3,age);
            red/=max;green/=max;blue/=max;
            double x=prevPosX+(posX-prevPosX)*partial-interpPosX,y=prevPosY+(posY-prevPosY)*partial-interpPosY,z=prevPosZ+(posZ-prevPosZ)*partial-interpPosZ;
            GlStateManager.disableTexture2D();GlStateManager.disableLighting();GlStateManager.disableCull();
            GlStateManager.enableBlend();GlStateManager.blendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE);GlStateManager.depthMask(false);
            BufferBuilder b=Tessellator.getInstance().getBuffer();b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
            b.pos(x-rx*w-rxy*h,y-rz*h,z-ryz*w-rxz*h).color(red,green,blue,alpha).endVertex();
            b.pos(x-rx*w+rxy*h,y+rz*h,z-ryz*w+rxz*h).color(red,green,blue,alpha).endVertex();
            b.pos(x+rx*w+rxy*h,y+rz*h,z+ryz*w+rxz*h).color(red,green,blue,alpha).endVertex();
            b.pos(x+rx*w-rxy*h,y-rz*h,z+ryz*w-rxz*h).color(red,green,blue,alpha).endVertex();
            Tessellator.getInstance().draw();restore();
        }
    }
    private void band(BufferBuilder buffer, double x, double y, double z, double radius, double width,
            double angle, float alpha, boolean broken) {
        for (int segment = 0; segment < 72; segment++) {
            if (broken && segment % 9 >= 6) continue;
            double a = angle + segment * Math.PI / 36, b = a + Math.PI / 36;
            point(buffer, x, y, z, radius - width, a, 1, 0.25F, 0.015F, alpha * 0.15F);
            point(buffer, x, y, z, radius + width, a, 1, 0.55F, 0.12F, alpha);
            point(buffer, x, y, z, radius + width, b, 1, 0.55F, 0.12F, alpha);
            point(buffer, x, y, z, radius - width, b, 1, 0.25F, 0.015F, alpha * 0.15F);
        }
    }
    private void point(BufferBuilder buffer, double x, double y, double z, double radius, double angle,
            float red, float green, float blue, float alpha) {
        buffer.pos(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius).color(red, green, blue, alpha).endVertex();
    }
}
