package xy177.snowsbosses.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xy177.snowsbosses.content.HellstoneContent;
import xy177.snowsbosses.content.MechasentRaids;
import xy177.snowsbosses.content.ModSounds;
import xy177.snowsbosses.entity.Drowner;
import xy177.snowsbosses.entity.HellstoneGolem;
import xy177.snowsbosses.entity.Mechasent;

final class MechasentMusic {
    private static final Set<Integer> BOSSES = new LinkedHashSet<>();
    private static final List<Track> TRACKS = new ArrayList<>();
    private static boolean playing;

    static void register() {
        MinecraftForge.EVENT_BUS.register(new MechasentMusic());
    }

    static void track(int entityId, boolean visible) {
        if (entityId < 0) return;
        if (visible) BOSSES.add(entityId);
        else BOSSES.remove(entityId);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getMinecraft();
        SoundEvent music = selectMusic(client);
        playing = music != null;
        TRACKS.removeIf(track -> !client.getSoundHandler().isSoundPlaying(track));
        for (Track track : TRACKS) track.setActive(track.music == music);
        if (!playing || client.isGamePaused() || client.gameSettings.getSoundLevel(SoundCategory.MASTER) <= 0
                || client.gameSettings.getSoundLevel(SoundCategory.MUSIC) <= 0
                || TRACKS.stream().anyMatch(track -> track.music == music && !track.isDonePlaying())) return;
        if (TRACKS.isEmpty()) client.getSoundHandler().stop("", SoundCategory.MUSIC);
        Track track = new Track(music);
        TRACKS.add(track);
        client.getSoundHandler().playSound(track);
    }

    private static SoundEvent selectMusic(Minecraft client) {
        if (client.world == null || client.player == null) return null;
        SoundEvent music = null;
        for (int id : BOSSES) {
            Entity entity = client.world.getEntityByID(id);
            if (entity == null || !entity.isEntityAlive()) continue;
            if (entity instanceof Mechasent) music = MechasentRaids.MUSIC;
            else if (entity instanceof HellstoneGolem) music = HellstoneContent.MUSIC;
            else if (entity instanceof Drowner) music = entity.isRiding() ? ModSounds.DROWNER_MOUNTED_MUSIC : ModSounds.DROWNER_MUSIC;
        }
        return music;
    }

    @SubscribeEvent
    public void sound(PlaySoundEvent event) {
        ISound sound = event.getSound();
        if (sound != null && !TRACKS.contains(sound) && sound.getCategory() == SoundCategory.MUSIC
                && (playing || !TRACKS.isEmpty())) event.setResultSound(null);
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.getWorld().isRemote) return;
        BOSSES.clear();
        playing = false;
        for (Track track : TRACKS) Minecraft.getMinecraft().getSoundHandler().stopSound(track);
        TRACKS.clear();
    }

    private static final class Track extends MovingSound {
        private final SoundEvent music;
        private boolean active = true;
        private float fadeStart = 1.0F;
        private int fadeTicks = 60;

        private Track(SoundEvent music) {
            super(music, SoundCategory.MUSIC);
            this.music = music;
            if (music == ModSounds.DROWNER_MOUNTED_MUSIC || music == ModSounds.DROWNER_MUSIC) {
                fadeStart = 0.0F;
                fadeTicks = 0;
                volume = 1.0F / 60.0F;
            }
            repeat = true;
            attenuationType = ISound.AttenuationType.NONE;
        }

        private void setActive(boolean active) {
            if (this.active == active || donePlaying) return;
            this.active = active;
            fadeStart = volume;
            fadeTicks = 0;
        }

        @Override
        public void update() {
            if (Minecraft.getMinecraft().isGamePaused()) return;
            fadeTicks = Math.min(60, fadeTicks + 1);
            volume = fadeStart + ((active ? 1.0F : 0.0F) - fadeStart) * fadeTicks / 60.0F;
            if (!active && fadeTicks == 60) {
                donePlaying = true;
                repeat = false;
            }
        }
    }
}
