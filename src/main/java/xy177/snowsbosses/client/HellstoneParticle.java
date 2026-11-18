package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public final class HellstoneParticle extends Particle {
    private final String kind;
    public HellstoneParticle(World world, String kind, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x, y, z); this.kind = kind;
        motionX = dx; motionY = dy; motionZ = dz; canCollide = false;
        particleMaxAge = kind.equals("hellstone_explode") ? 8 : kind.equals("hellstone_angry") ? 12 : 12 + rand.nextInt(8);
        particleScale = kind.equals("hellstone_explode") ? 6 : kind.equals("hellstone_angry") ? 1.5F : 1.5F;
        sprite();
    }
    private void sprite() {
        String path = kind.equals("hellstone_explode") ? "hellstone_explode_" + Math.min(7, particleAge)
                : kind.equals("hellstone_angry") ? "angry" : "hellstone_fire_" + Math.min(2, particleAge * 3 / particleMaxAge);
        if (path.endsWith("_0")) path = path.substring(0, path.length() - 2);
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(new ResourceLocation("snows_bosses", "particle/" + path).toString()));
    }
    @Override public void onUpdate() {
        super.onUpdate();
        if (!isExpired) sprite();
    }
    @Override public int getFXLayer() { return 1; }
    @Override public int getBrightnessForRender(float partialTick) { return 15728880; }
}
