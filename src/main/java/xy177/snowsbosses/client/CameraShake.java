package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

final class CameraShake {
    private static final CameraShake INSTANCE = new CameraShake();
    private static boolean registered;
    private double x;
    private double y;
    private double z;
    private int remaining;
    private int elapsed;

    private CameraShake() {
    }

    static void register() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(INSTANCE);
            registered = true;
        }
    }

    static void start(double x, double y, double z, int ticks) {
        INSTANCE.x = x;
        INSTANCE.y = y;
        INSTANCE.z = z;
        INSTANCE.remaining = Math.max(INSTANCE.remaining, ticks);
        INSTANCE.elapsed = 0;
    }

    static void stop(double x, double y, double z) {
        if (INSTANCE.remaining > 0 && INSTANCE.x == x && INSTANCE.y == y && INSTANCE.z == z) INSTANCE.remaining = 0;
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || remaining <= 0) return;
        remaining--;
        elapsed++;
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event) {
        Minecraft client = Minecraft.getMinecraft();
        if (remaining <= 0 || client.player == null) return;
        double distance = client.player.getDistance(x, y, z);
        float strength = MathHelper.clamp((float) (1.0 - distance / 32.0), 0, 1);
        if (strength <= 0) return;
        float phase = elapsed * 0.85F;
        event.setYaw(event.getYaw() + (MathHelper.sin(phase * 1.7F) * 1.4F
                + MathHelper.cos(phase * 2.3F) * 0.6F) * strength);
        event.setPitch(event.getPitch() + (MathHelper.cos(phase * 1.9F) * 1.0F
                + MathHelper.sin(phase * 2.7F) * 0.4F) * strength);
        event.setRoll(event.getRoll() + MathHelper.sin(phase * 2.1F) * 0.8F * strength);
    }
}
