package xy177.snowsbosses.client;

import net.minecraft.util.math.MathHelper;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import xy177.snowsbosses.entity.Drowner;

final class DrownerSwimming {
    private static final float PI = (float) Math.PI;

    private DrownerSwimming() {
    }

    static void apply(AnimatedGeoModel<?> model, Drowner entity, float partialTicks) {
        float amount = entity.getSwimAmount(partialTicks);
        if (amount <= 0) return;
        float phase = entity.getSwimCycle(partialTicks);
        float cycle = phase % 26;
        float x;
        float leftZ;
        float rightZ;
        if (cycle < 14) {
            float spread = 1.8707964F * (-65 * cycle + cycle * cycle) / (-65 * 14 + 14 * 14);
            x = 0;
            leftZ = PI + spread;
            rightZ = PI - spread;
        } else if (cycle < 22) {
            float progress = (cycle - 14) / 8;
            x = PI / 2 * progress;
            leftZ = 5.012389F - 1.8707964F * progress;
            rightZ = 1.2707963F + 1.8707964F * progress;
        } else {
            x = PI / 2 * (1 - (cycle - 22) / 4);
            leftZ = rightZ = PI;
        }
        rotate(model, "arm_L", amount, -x, PI, leftZ);
        rotate(model, "arm_R", amount, -x, PI, rightZ);
        float kick = 0.3F;
        rotate(model, "leg_L", amount, -kick * MathHelper.cos(phase / 3 + PI), 0, 0);
        rotate(model, "leg_R", amount, -kick * MathHelper.cos(phase / 3), 0, 0);
        rotate(model, "body2", amount, 0, 0, 0);
        rotate(model, "root", amount, -PI / 2, 0, 0);
        IBone head = model.getAnimationProcessor().getBone("head");
        if (head != null) head.setRotationX(angle(head.getRotationX(), PI / 4, amount));
        for (String name : new String[]{"root", "body2", "leg_L", "leg_R"}) {
            IBone bone = model.getAnimationProcessor().getBone(name);
            if (bone != null) {
                bone.setPositionX(bone.getPositionX() * (1 - amount));
                bone.setPositionY(bone.getPositionY() * (1 - amount));
                bone.setPositionZ(bone.getPositionZ() * (1 - amount));
            }
        }
    }

    private static void rotate(AnimatedGeoModel<?> model, String name, float amount, float x, float y, float z) {
        IBone bone = model.getAnimationProcessor().getBone(name);
        if (bone == null) return;
        bone.setRotationX(angle(bone.getRotationX(), x, amount));
        bone.setRotationY(angle(bone.getRotationY(), y, amount));
        bone.setRotationZ(angle(bone.getRotationZ(), z, amount));
    }

    private static float angle(float from, float to, float amount) {
        return from + MathHelper.wrapDegrees((to - from) * 180 / PI) * PI / 180 * amount;
    }
}
