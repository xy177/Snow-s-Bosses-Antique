package xy177.snowsbosses.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.MechasentWorkerBee;
import xy177.snowsbosses.entity.WorkerBeeBeam;
import xy177.snowsbosses.entity.WorkerBeeLaser;

public final class WorkerBeeRenderer extends RenderLiving<MechasentWorkerBee> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/mechasent_worker_bee.png");
    private static final ResourceLocation RING = new ResourceLocation(SnowsBosses.MOD_ID, "textures/particle/mechasent_particle_2.png");
    public WorkerBeeRenderer(RenderManager manager) { super(manager, new Model(), 0.25F); }
    @Override protected ResourceLocation getEntityTexture(MechasentWorkerBee entity) { return TEXTURE; }
    @Override public boolean shouldRender(MechasentWorkerBee bee, ICamera camera, double x, double y, double z) {
        EntityLivingBase target = bee.lockedTarget();
        return super.shouldRender(bee, camera, x, y, z) || target != null && bee.action() >= 3
                && camera.isBoundingBoxInFrustum(bee.getEntityBoundingBox().union(target.getEntityBoundingBox()));
    }
    @Override public void doRender(MechasentWorkerBee bee, double x, double y, double z, float yaw, float partial) {
        GlStateManager.disableCull();
        super.doRender(bee, x, y, z, yaw, partial);
        GlStateManager.enableCull();
        if (bee.sleeping() || renderOutlines) return;
        EntityLivingBase target = bee.lockedTarget();
        Vec3d start = new Vec3d(x, y + bee.getEyeHeight(), z);
        if (bee.action() >= 3 && target != null) {
            Vec3d end = interpolated(target, partial).subtract(interpolated(bee, partial))
                    .addVector(x, y + (bee.action() == 4 || bee.action() == 6 ? target.height * 0.7 : target.getEyeHeight()), z);
            int color = bee.action() == 4 ? 0xB832F2 : bee.action() == 6 ? 0x2AFF52 : 0xFF2020;
            line(start, end, color);
        }
        float age = bee.actionAge() + partial;
        int period = bee.action() == 2 ? 10 : 30;
        if (bee.action() == 5 || (bee.action() == 1 || bee.action() == 2) && age < 30) {
            float progress = age % period / period;
            float eased = progress * progress * (3 - 2 * progress);
            Vec3d forward = bee.getLook(partial);
            Vec3d right = forward.crossProduct(new Vec3d(0, 1, 0)).normalize();
            if (right.lengthSquared() < 0.01) right = new Vec3d(1, 0, 0);
            Vec3d up = right.crossProduct(forward).normalize();
            Vec3d center = start.add(forward.scale(0.8));
            bindTexture(RING);
            texturedPlane(center, right.scale(0.8 * eased), up.scale(0.8 * eased), 1 - progress * eased);
        }
    }
    private static Vec3d interpolated(Entity entity, float partial) {
        return new Vec3d(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partial,
                entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partial, entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partial);
    }
    private static void line(Vec3d start, Vec3d end, int color) {
        GlStateManager.disableTexture2D(); GlStateManager.disableLighting(); GlStateManager.enableBlend(); GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA); GL11.glLineWidth(2);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer(); buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(start.x, start.y, start.z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 220).endVertex();
        buffer.pos(end.x, end.y, end.z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 220).endVertex();
        Tessellator.getInstance().draw(); GL11.glLineWidth(1);
        GlStateManager.depthMask(true); GlStateManager.disableBlend(); GlStateManager.enableLighting(); GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
    private static void texturedPlane(Vec3d center, Vec3d right, Vec3d up, float alpha) {
        GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.enableBlend(); GlStateManager.depthMask(false);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer(); buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vertex(buffer, center.subtract(right).subtract(up), 0, 1, alpha);
        vertex(buffer, center.add(right).subtract(up), 1, 1, alpha);
        vertex(buffer, center.add(right).add(up), 1, 0, alpha);
        vertex(buffer, center.subtract(right).add(up), 0, 0, alpha);
        Tessellator.getInstance().draw();
        GlStateManager.depthMask(true); GlStateManager.disableBlend(); GlStateManager.enableCull(); GlStateManager.enableLighting();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }
    private static void vertex(BufferBuilder buffer, Vec3d p, double u, double v, float alpha) { buffer.pos(p.x, p.y, p.z).tex(u, v).color(1, 1, 1, alpha).endVertex(); }

    private static final class Model extends ModelBase {
        private final ModelRenderer body, left, right;
        Model() {
            textureWidth = textureHeight = 64;
            body = new ModelRenderer(this, 0, 0); body.setRotationPoint(0, 20, 0); body.addBox(-3.5F, -3, -5, 7, 7, 10);
            left = new ModelRenderer(this, 19, 1); left.setRotationPoint(-1.5F, -3, -3); left.addBox(-9, 0, 0, 9, 0, 6, 0.001F);
            right = new ModelRenderer(this, 19, 1); right.mirror = true; right.setRotationPoint(1.5F, -3, -3); right.addBox(0, 0, 0, 9, 0, 6, 0.001F);
            body.addChild(left); body.addChild(right);
        }
        @Override public void render(Entity entity, float swing, float amount, float age, float yaw, float pitch, float scale) {
            boolean sleeping = ((MechasentWorkerBee) entity).sleeping();
            float cycle = age / 3 % 1;
            float angle = sleeping ? 0 : (float) Math.toRadians(27) * (cycle <= 0.5 ? 1 - cycle * 4 : cycle * 4 - 3);
            left.rotateAngleZ = -angle; right.rotateAngleZ = angle; body.rotateAngleX = sleeping ? 0 : (float) Math.toRadians(pitch);
            if (sleeping) GlStateManager.color(0.18F, 0.18F, 0.18F, 1);
            body.render(scale); GlStateManager.color(1, 1, 1, 1);
        }
    }

    public static final class Laser extends Render<WorkerBeeLaser> {
        private final MechasentRenderer.Model model = new MechasentRenderer.Model("mechasent_laser");
        public Laser(RenderManager manager) { super(manager); }
        @Override protected ResourceLocation getEntityTexture(WorkerBeeLaser entity) { return new ResourceLocation(SnowsBosses.MOD_ID, "textures/entities/mechasent_laser.png"); }
        @Override public void doRender(WorkerBeeLaser entity, double x, double y, double z, float yaw, float partial) {
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            GlStateManager.pushMatrix(); GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-entity.rotationYaw, 0, 1, 0); GlStateManager.rotate(90 + entity.rotationPitch, 1, 0, 0);
            GlStateManager.scale(-0.4, -0.4, 0.4); GlStateManager.translate(0, -1.501, 0);
            GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240); bindEntityTexture(entity);
            model.render(entity, 0, 0, 7, 0, 0, 0.0625F);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.disableBlend(); GlStateManager.enableCull(); GlStateManager.enableLighting();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA); GlStateManager.popMatrix();
        }
    }

    public static final class Beam extends Render<WorkerBeeBeam> {
        private static final ResourceLocation COLUMN = texture("worker_bee_beam");
        private static final ResourceLocation PLASMA = texture("worker_bee_plasma");
        private static final ResourceLocation GLOW = texture("worker_bee_glow");
        private static final ResourceLocation EXHAUST = texture("worker_bee_exhaust");
        public Beam(RenderManager manager) { super(manager); }
        private static ResourceLocation texture(String name) { return new ResourceLocation(SnowsBosses.MOD_ID, "textures/effects/" + name + ".png"); }
        @Override protected ResourceLocation getEntityTexture(WorkerBeeBeam entity) { return COLUMN; }
        @Override public void doRender(WorkerBeeBeam beam, double x, double y, double z, float yaw, float partial) {
            if (renderOutlines) return;
            boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
            boolean fog = GL11.glIsEnabled(GL11.GL_FOG), texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
            boolean depth = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            int src = GL11.glGetInteger(32969), dst = GL11.glGetInteger(32968);
            int srcAlpha = GL11.glGetInteger(32971), dstAlpha = GL11.glGetInteger(32970);
            float lightX = OpenGlHelper.lastBrightnessX, lightY = OpenGlHelper.lastBrightnessY;
            double t = (beam.world.getTotalWorldTime() + partial) / 20.0;
            double growth = (0.025 + 0.725 * Math.min(1, (beam.age() + partial) / 20.0)) / 0.75;
            double height = Math.max(1, beam.world.getActualHeight() - beam.posY);
            double angle = Math.toRadians(renderManager.playerViewY), rightX = Math.cos(angle), rightZ = Math.sin(angle);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(x, y, z);
                GlStateManager.disableLighting(); GlStateManager.disableCull(); GlStateManager.disableAlpha();
                GlStateManager.disableFog(); GlStateManager.enableTexture2D(); GlStateManager.enableBlend(); GlStateManager.depthMask(false);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                band(height, growth, rightX, rightZ, 0.90, 1, 0.045F, 0.006F, 0.85F, PLASMA, t * 1.8, 0.16, 0, t);
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                band(height, growth, rightX, rightZ, 1.45, 1, 0.09F, 0.008F, 0.22F, COLUMN, t * 0.5, 0.14, 2, t);
                for (int lane = 0; lane < 4; lane++)
                    band(height, growth, rightX, rightZ, 0.67 + lane * 0.055, 1, 0.18F + lane * 0.035F, 0.014F, 0.35F,
                            PLASMA, t * (1.25 + lane * 0.17) + lane * 0.19, 0.17 + lane * 0.025, lane, t);
                band(height, growth, rightX, rightZ, 0.24, 1, 0.95F, 0.78F, 1, COLUMN, t * 2.8, 0.035, 0, t);
                impact(t, growth);
                GlStateManager.scale(growth, growth, growth);
                exhaust(t);
            } finally {
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
                GlStateManager.bindTexture(oldTexture);
                GlStateManager.tryBlendFuncSeparate(src, dst, srcAlpha, dstAlpha);
                GlStateManager.depthMask(depth);
                if (lighting) GlStateManager.enableLighting(); else GlStateManager.disableLighting();
                if (cull) GlStateManager.enableCull(); else GlStateManager.disableCull();
                if (alpha) GlStateManager.enableAlpha(); else GlStateManager.disableAlpha();
                if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
                if (fog) GlStateManager.enableFog(); else GlStateManager.disableFog();
                if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
                GlStateManager.popMatrix();
            }
        }
        private void band(double height, double growth, double rightX, double rightZ, double radius,
                float r, float g, float b, float a, ResourceLocation texture, double scroll, double warp, int lane, double t) {
            bindTexture(texture);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer(); buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            int count = Math.max(1, Math.min(384, (int) Math.ceil(height / 0.65)));
            double rotation = t + lane * 2.1, axisX = Math.cos(rotation), axisZ = Math.sin(rotation);
            for (int i = 0; i < count; i++) {
                double y0 = height * i / count, y1 = height * (i + 1) / count;
                double c0 = growth * warp * Math.sin(y0 * 1.7 + t * 5 + lane * 2.1);
                double c1 = growth * warp * Math.sin(y1 * 1.7 + t * 5 + lane * 2.1);
                double w0 = growth * radius * (1 + 0.18 * Math.sin(y0 * 3.4 + t * 8 + lane));
                double w1 = growth * radius * (1 + 0.18 * Math.sin(y1 * 3.4 + t * 8 + lane));
                double v0 = scroll + y0 * 0.31, v1 = scroll + y1 * 0.31;
                point(buffer, axisX * c0 - rightX * w0, y0, axisZ * c0 - rightZ * w0, 0, v0, r, g, b, a);
                point(buffer, axisX * c0 + rightX * w0, y0, axisZ * c0 + rightZ * w0, 1, v0, r, g, b, a);
                point(buffer, axisX * c1 + rightX * w1, y1, axisZ * c1 + rightZ * w1, 1, v1, r, g, b, a);
                point(buffer, axisX * c1 - rightX * w1, y1, axisZ * c1 - rightZ * w1, 0, v1, r, g, b, a);
            }
            Tessellator.getInstance().draw();
        }
        private void impact(double t, double growth) {
            bindTexture(GLOW);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer(); buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            disc(buffer, 0.025, 3.2 * growth, 1, 0.055F, 0.008F, 0.6F);
            disc(buffer, 0.035, 0.90 * growth, 1, 0.40F, 0.08F, 0.9F);
            disc(buffer, 0.045, 0.40 * growth, 1, 0.98F, 0.88F, 1);
            double yaw = Math.toRadians(renderManager.playerViewY), pitch = Math.toRadians(renderManager.playerViewX);
            double rx = Math.cos(yaw), rz = Math.sin(yaw), ux = -Math.sin(yaw) * Math.sin(pitch);
            double uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
            for (int i = 0; i < 46; i++) {
                double life = (t * 0.9 + i * 0.61803398875) % 1, angle = i * 2.39996322973;
                double radius = growth * (0.2 + life * (2.2 + i % 5 * 0.25));
                double px = Math.cos(angle) * radius, pz = Math.sin(angle) * radius;
                double py = 0.12 + Math.sin(life * Math.PI) * (0.3 + i % 4 * 0.25);
                double size = growth * (0.025 + i % 3 * 0.013);
                float alpha = (float) (Math.sin(life * Math.PI) * 0.9);
                point(buffer, px - (rx + ux) * size, py - uy * size, pz - (rz + uz) * size, 0, 1, 1, 0.22F, 0.015F, alpha);
                point(buffer, px + (rx - ux) * size, py - uy * size, pz + (rz - uz) * size, 1, 1, 1, 0.22F, 0.015F, alpha);
                point(buffer, px + (rx + ux) * size, py + uy * size, pz + (rz + uz) * size, 1, 0, 1, 0.62F, 0.08F, alpha);
                point(buffer, px + (ux - rx) * size, py + uy * size, pz + (uz - rz) * size, 0, 0, 1, 0.62F, 0.08F, alpha);
            }
            Tessellator.getInstance().draw();
            GlStateManager.disableTexture2D(); buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            double wave = t * 0.7 % 1, radius = growth * (0.38 + wave * 1.8), width = growth * 0.04;
            float alpha = (float) ((1 - wave) * 0.38);
            for (int i = 0; i < 96; i++) {
                double a = i * Math.PI * 2 / 96, b = (i + 1) * Math.PI * 2 / 96;
                buffer.pos(Math.cos(a) * (radius - width), 0.05, Math.sin(a) * (radius - width)).color(1, 0.12F, 0.012F, 0).endVertex();
                buffer.pos(Math.cos(a) * (radius + width), 0.05, Math.sin(a) * (radius + width)).color(1, 0.12F, 0.012F, alpha).endVertex();
                buffer.pos(Math.cos(b) * (radius + width), 0.05, Math.sin(b) * (radius + width)).color(1, 0.12F, 0.012F, alpha).endVertex();
                buffer.pos(Math.cos(b) * (radius - width), 0.05, Math.sin(b) * (radius - width)).color(1, 0.12F, 0.012F, 0).endVertex();
            }
            Tessellator.getInstance().draw(); GlStateManager.enableTexture2D();
        }
        private void exhaust(double t){
            bindTexture(EXHAUST);BufferBuilder b=Tessellator.getInstance().getBuffer();
            b.begin(GL11.GL_TRIANGLE_STRIP,DefaultVertexFormats.POSITION_TEX_COLOR);
            for(int layer=0;layer<3;layer++)for(int ring=0;ring<24;ring++){
                double q0=ring/24.0,q1=(ring+1)/24.0;
                jetVertex(b,q0,0,t,layer);
                for(int side=0;side<=128;side++){
                    double angle=side*Math.PI*2/128;
                    jetVertex(b,q0,angle,t,layer);jetVertex(b,q1,angle,t,layer);
                }
                jetVertex(b,q1,Math.PI*2,t,layer);
            }
            Tessellator.getInstance().draw();
            b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            for(int i=0;i<52;i++){
                double life=(t*.8+i*.61803398875)%1,angle=i*2.39996322973+.09*Math.sin(t*2+i);
                double head=.25+4.5*life,tail=Math.max(.12,head-(.55+life*1.25));
                for(int j=0;j<10;j++){
                    double u0=j/10.0,u1=(j+1)/10.0;
                    tongue(b,tail,head,angle,life,u0,-1,i,t);tongue(b,tail,head,angle,life,u0,1,i,t);
                    tongue(b,tail,head,angle,life,u1,1,i,t);tongue(b,tail,head,angle,life,u1,-1,i,t);
                }
            }
            Tessellator.getInstance().draw();
            bindTexture(GLOW);b.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_TEX_COLOR);
            for(int i=0;i<64;i++){
                double angle=i*Math.PI*2/64,size=.045+.035*(.5+.5*Math.sin(t*3+i*2.1));
                float alpha=(float)(.24+.36*(.5+.5*Math.sin(t*2.2+i*2.39996)));
                sprite(b,Math.cos(angle)*5,.035+Math.sin(t*2+i)*.015,Math.sin(angle)*5,size,1,.17F,.015F,alpha);
            }
            for(int i=0;i<38;i++){
                double life=(t*.65+i*.61803398875)%1,angle=i*2.39996322973,r=.6+life*4.3;
                sprite(b,Math.cos(angle)*r,0.1+Math.sin(life*Math.PI)*(.3+i%4*.22),Math.sin(angle)*r,.025+i%3*.012,1,.28F,.015F,(float)(Math.sin(life*Math.PI)*.65));
            }
            Tessellator.getInstance().draw();
        }
        private void jetVertex(BufferBuilder b,double q,double angle,double t,int layer){
            double edge=4.6+.22*Math.sin(angle*5+t*2)+.12*Math.sin(angle*11-t*4);
            double r=.18+q*(edge-.18);
            double curl=Math.max(0,Math.sin(angle*9-t*5+layer*1.7));
            double height=.10+.52*Math.exp(-r*2)+.28*q*q+.64*Math.pow(q,4)*curl*curl;
            height+=(.04+.08*q)*Math.sin(r*7-t*12+angle*3+layer*2)+layer*.08*Math.sin(q*Math.PI);
            float fade=(float)(Math.pow(Math.max(0,1-q),.4)*(.42-layer*.07));
            float green=(float)(.045+.82*Math.pow(1-q,2)),blue=(float)(.005+.35*Math.pow(1-q,4));
            point(b,Math.cos(angle)*r,Math.max(.035,height),Math.sin(angle)*r,angle/Math.PI*1.5+layer*.17,q*2-t*1.3,1,green,blue,fade);
        }
        private void tongue(BufferBuilder b,double tail,double head,double angle,double life,double u,int side,int index,double t){
            double r=tail+(head-tail)*u,phase=Math.sin(u*Math.PI);
            double width=(.06+.16*life)*Math.sqrt(Math.max(0,phase));
            double h=.12+.36*Math.exp(-r*2)+.18*life*life+phase*(.25+.85*life)*(.65+(index%4)*.16);
            double bend=.08*Math.sin(u*7-t*9+index)*phase;
            double px=Math.cos(angle)*r-Math.sin(angle)*(side*width+bend),pz=Math.sin(angle)*r+Math.cos(angle)*(side*width+bend);
            float alpha=(float)(phase*Math.sin(life*Math.PI)*.5);
            point(b,px,h,pz,side<0?0:1,u*.8-t*1.5,1,(float)(.08+.5*(1-life)),.012F,alpha);
        }
        private void sprite(BufferBuilder b,double x,double y,double z,double size,float r,float g,float blue,float alpha){
            double yaw=Math.toRadians(renderManager.playerViewY),pitch=Math.toRadians(renderManager.playerViewX);
            double rx=Math.cos(yaw)*size,rz=Math.sin(yaw)*size,ux=-Math.sin(yaw)*Math.sin(pitch)*size,uy=Math.cos(pitch)*size,uz=Math.cos(yaw)*Math.sin(pitch)*size;
            point(b,x-rx-ux,y-uy,z-rz-uz,0,1,r,g,blue,alpha);point(b,x+rx-ux,y-uy,z+rz-uz,1,1,r,g,blue,alpha);
            point(b,x+rx+ux,y+uy,z+rz+uz,1,0,r,g,blue,alpha);point(b,x-rx+ux,y+uy,z-rz+uz,0,0,r,g,blue,alpha);
        }
        private static void disc(BufferBuilder buffer, double y, double radius, float r, float g, float b, float a) {
            point(buffer, -radius, y, -radius, 0, 0, r, g, b, a); point(buffer, radius, y, -radius, 1, 0, r, g, b, a);
            point(buffer, radius, y, radius, 1, 1, r, g, b, a); point(buffer, -radius, y, radius, 0, 1, r, g, b, a);
        }
        private static void point(BufferBuilder buffer, double x, double y, double z, double u, double v, float r, float g, float b, float a) {
            buffer.pos(x, y, z).tex(u, v).color(r, g, b, a).endVertex();
        }
    }
}
