package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import xy177.snowsbosses.SnowsBosses;

public final class OceanParticle extends Particle {
    private final boolean spark;
    private final double friction;

    public OceanParticle(World world, String kind, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x, y, z);
        spark = kind.equals("electric_spark");
        friction = 0.96;
        particleScale *= spark ? 0.75 : 1.5;
        if (spark) {
            int jitter = 2;
            particleMaxAge = 2 + (rand.nextInt(jitter));
            double speed = 0.25;
            motionX = dx * speed;
            motionY = dy * speed;
            motionZ = dz * speed;
            setRBGColorF(1, 0.9F, 1);
            canCollide = false;
        } else {
            particleMaxAge = (int) (8 / (rand.nextDouble() * 0.8 + 0.2)) + 4;
            double drift = 0.01;
            motionX = motionX * drift + dx;
            motionY = motionY * drift + dy;
            motionZ = motionZ * drift + dz;
            double spread = 0.05;
            setPosition(x + (rand.nextFloat() - rand.nextFloat()) * spread,
                    y + (rand.nextFloat() - rand.nextFloat()) * spread,
                    z + (rand.nextFloat() - rand.nextFloat()) * spread);
        }
        sprite();
    }

    private void sprite() {
        int frame = Math.min(spark ? 7 : 10, particleAge * (spark ? 7 : 10) / Math.max(1, particleMaxAge));
        String name = spark ? "spark_" : "soul_";
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(
                new ResourceLocation(SnowsBosses.MOD_ID, "particle/" + name + frame).toString()));
    }

    @Override
    public void onUpdate() {
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setExpired();
            return;
        }
        double previousY = posY;
        move(motionX, motionY, motionZ);
        if (spark && posY == previousY) {
            motionX *= 1.1;
            motionZ *= 1.1;
        }
        motionX *= friction;
        motionY *= friction;
        motionZ *= friction;
        if (onGround) {
            motionX *= 0.7;
            motionZ *= 0.7;
        }
        sprite();
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public int getBrightnessForRender(float partialTicks) {
        int brightness = super.getBrightnessForRender(partialTicks);
        if (!spark) return brightness;
        int light = Math.min(240, (brightness & 255) + (int) (Math.min(1,
                (particleAge + partialTicks) / particleMaxAge) * 240));
        return light | brightness & 0xFF0000;
    }
}
