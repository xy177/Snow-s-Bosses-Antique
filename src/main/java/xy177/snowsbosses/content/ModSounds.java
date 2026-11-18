package xy177.snowsbosses.content;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class ModSounds {
    public static final SoundEvent DROWNER_MOUNTED_MUSIC = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "drowner_mounted_music"))
            .setRegistryName(SnowsBosses.MOD_ID, "drowner_mounted_music");
    public static final SoundEvent DROWNER_MUSIC = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "drowner_music"))
            .setRegistryName(SnowsBosses.MOD_ID, "drowner_music");
    private ModSounds() {
    }

    @SubscribeEvent
    public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(DROWNER_MOUNTED_MUSIC, DROWNER_MUSIC);
    }

    public static SoundEvent get(String name) {
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", name));
        if (sound == null) sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("farmers_future_delight", name));
        if (sound == null) {
            String legacy = name.replace("entity.evoker_fangs.", "entity.evocation_fangs.")
                    .replace("entity.evoker.", "entity.evocation_illager.")
                    .replace("entity.iron_golem.", "entity.irongolem.");
            sound = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft", legacy));
        }
        if (sound == null) throw new IllegalArgumentException("Unavailable sound: " + name);
        return sound;
    }
}
