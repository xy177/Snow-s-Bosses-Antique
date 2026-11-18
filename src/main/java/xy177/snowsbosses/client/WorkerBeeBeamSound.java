package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.util.SoundCategory;
import xy177.snowsbosses.content.MechasentRaids;
import xy177.snowsbosses.entity.WorkerBeeBeam;

final class WorkerBeeBeamSound extends MovingSound {
    private final WorkerBeeBeam beam;

    WorkerBeeBeamSound(WorkerBeeBeam beam) {
        super(MechasentRaids.BEAM_LOOP, SoundCategory.HOSTILE);
        this.beam = beam;
        repeat = true;
        position();
    }

    private void position() {
        xPosF = (float) beam.posX;
        yPosF = (float) beam.posY;
        zPosF = (float) beam.posZ;
    }

    @Override
    public void update() {
        position();
        if (beam.isDead || beam.isSilent() || beam.age() > beam.duration() || Minecraft.getMinecraft().world != beam.world) {
            donePlaying = true;
            repeat = false;
        }
    }
}
