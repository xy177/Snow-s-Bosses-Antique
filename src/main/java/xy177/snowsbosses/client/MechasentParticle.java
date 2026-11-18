package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import xy177.snowsbosses.network.Visuals;

public final class MechasentParticle extends Particle {
    private final boolean ring;
    private final float initialSize;

    public MechasentParticle(World world, String kind, double x, double y, double z, double dx, double dy, double dz) {
        super(world, x, y, z);
        ring = kind.equals("mechasent_particle_2");
        initialSize = particleScale;
        particleMaxAge = ring ? 30 : 20;
        canCollide = false;
        motionX = dx; motionY = dy; motionZ = dz;
        setParticleTexture(Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("snows_bosses:particle/" + kind));
    }
    @Override public void onUpdate() {
        super.onUpdate();
        float progress = Math.min(1, (float) particleAge / particleMaxAge);
        if (ring) {
            float eased = progress * progress * (3 - 2 * progress);
            particleScale = initialSize * 8 * eased;
            particleAlpha = 1 - progress * eased;
        } else {
            particleScale = 2 * (1 - progress);
            particleRed = particleGreen = particleBlue = 1 - progress;
        }
    }
    @Override public int getFXLayer() { return 1; }
    @Override public int getBrightnessForRender(float partial) { return 15728880; }

    public static void stone(World world, Visuals.Message message) {
        for (int ring = 0; ring < 3; ring++) {
            double radius = message.spread * (1 - ring * 0.3);
            for (int index = 0; index < message.count; index++) {
                double angle = Math.toRadians(360.0 * (index + 1) / message.count + (ring == 1 ? message.count / 2.0 : 0));
                double x = message.x + radius * Math.sin(angle), z = message.z + radius * Math.cos(angle);
                IBlockState state = world.getBlockState(new BlockPos(x, message.y - 0.1, z));
                if (state.getMaterial().isReplaceable()) state = world.getBlockState(new BlockPos(message.x, message.y - 0.1, message.z));
                world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, x, message.y + 0.1, z,
                        message.speed * (0.2 + world.rand.nextDouble() * 0.95) * Math.sin(angle),
                        0.1 * (0.85 + world.rand.nextDouble() * 0.3),
                        message.speed * (0.2 + world.rand.nextDouble() * 0.95) * Math.cos(angle), Block.getStateId(state));
            }
        }
    }
}
