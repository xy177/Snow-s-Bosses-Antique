package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import xy177.snowsbosses.SnowsBosses;

public final class PortParticle extends Particle {
    private final String kind;

    public PortParticle(World world, String kind, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x, y, z);
        this.kind = kind;
        boolean water = kind.equals("piranha_water");
        particleScale *= water ? 2 : 4;
        int jitter = water ? 12 : 0;
        particleMaxAge = Math.max(1, (water ? 36 : 5)
                + (jitter == 0 ? 0 : rand.nextInt(jitter) - jitter / 2));
        particleGravity = water ? 0.1F : 0;
        canCollide = water;
        motionX = water ? 0 : dx;
        motionY = water ? 0 : dy;
        motionZ = water ? 0 : dz;
        sprite(0);
    }

    public static boolean supports(String name) {
        return name.equals("piranha_water") || name.equals("trident_p") || name.equals("trident_red");
    }

    private void sprite(int age) {
        String name = kind.equals("piranha_water") ? kind : kind + "_" + (Math.min(5, age) + 1);
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                new ResourceLocation(SnowsBosses.MOD_ID, "particle/" + name).toString()));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!isExpired) sprite(particleAge);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        return kind.equals("piranha_water") ? super.getBrightnessForRender(partialTicks) : 15728880;
    }
}
