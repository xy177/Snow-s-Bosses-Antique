package xy177.snowsbosses.content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.api.raid.RaidEventCallbacks;
import xy177.farmersfuturedelight.api.raid.RaidEventContext;
import xy177.farmersfuturedelight.api.raid.RaidEventDefinition;
import xy177.farmersfuturedelight.api.raid.RaidEventRegistrationEvent;
import xy177.farmersfuturedelight.api.raid.RaidEventRegistry;
import xy177.farmersfuturedelight.common.raid.FFDRaidManager;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Mechasent;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class MechasentRaids {
    private static final ResourceLocation RAID = new ResourceLocation("farmers_future_delight", "raid");
    public static final SoundEvent ROAR = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "mechasent_roar"))
            .setRegistryName(SnowsBosses.MOD_ID, "mechasent_roar");
    public static final SoundEvent[] BROADCASTS = broadcasts();
    public static final SoundEvent MUSIC = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "mechasent_music"))
            .setRegistryName(SnowsBosses.MOD_ID, "mechasent_music");
    public static final SoundEvent BEAM_LOOP = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "worker_bee_beam_loop"))
            .setRegistryName(SnowsBosses.MOD_ID, "worker_bee_beam_loop");
    public static final SoundEvent BEAM_END = new SoundEvent(new ResourceLocation(SnowsBosses.MOD_ID, "worker_bee_beam_end"))
            .setRegistryName(SnowsBosses.MOD_ID, "worker_bee_beam_end");
    private MechasentRaids() { }

    private static SoundEvent[] broadcasts() {
        SoundEvent[] sounds = new SoundEvent[17];
        for (int i = 0; i < sounds.length; i++) {
            String name = "mechasent_broadcast_" + (i < 9 ? "0" : "") + (i + 1);
            ResourceLocation id = new ResourceLocation(SnowsBosses.MOD_ID, name);
            sounds[i] = new SoundEvent(id).setRegistryName(id);
        }
        return sounds;
    }

    @SubscribeEvent public static void sounds(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().register(ROAR);
        event.getRegistry().register(MUSIC);
        event.getRegistry().registerAll(BEAM_LOOP, BEAM_END);
        event.getRegistry().registerAll(BROADCASTS);
    }

    @SubscribeEvent
    public static void register(RaidEventRegistrationEvent event) {
        RaidEventDefinition definition = RaidEventRegistry.get(RAID);
        if (definition == null) return;
        RaidEventCallbacks original = definition.getCallbacks();
        RaidEventRegistry.register(definition.toBuilder().callbacks(new RaidEventCallbacks() {
            @Override public boolean canTrigger(RaidEventContext context) { return original.canTrigger(context); }
            @Override public Outcome getOutcome(RaidEventContext context) { return original.getOutcome(context); }
            @Override public void onReward(RaidEventContext context, Outcome outcome) { original.onReward(context, outcome); }
            @Override public void onLifecycle(RaidEventContext context, Lifecycle phase) {
                original.onLifecycle(context, phase);
                if (phase != Lifecycle.WAVE_START) return;
                RaidEventDefinition current = RaidEventRegistry.get(context.getEventId());
                if (current == null) return;
                int waves = current.getWaves(context.getDifficulty()).size();
                if (current.getBonusOmenLevel() > 0 && context.getOmenLevel() >= current.getBonusOmenLevel())
                    waves += current.getBonusWaves(context.getDifficulty()).size();
                if (current.getScriptRules() != null)
                    waves = current.getScriptRules().numbers(current, context.getDifficulty(), context.getOmenLevel()).size();
                if (context.getWave() != waves) return;
                spawn(context);
            }
        }).build());
    }

    private static void spawn(RaidEventContext context) {
        WorldServer world = context.getWorld();
        BlockPos anchor = null;
        for (Entity entity : world.loadedEntityList) {
            if (!(entity instanceof EntityLivingBase)) continue;
            RaidEventContext owner = FFDRaidManager.getContext((EntityLivingBase) entity);
            if (owner != null && owner.getInstanceId().equals(context.getInstanceId())) { anchor = entity.getPosition(); break; }
        }
        if (anchor == null) return;
        Mechasent entity = new Mechasent(world);
        boolean found = false;
        for (int radius = 2; radius <= 16 && !found; radius += 2) {
            for (int direction = 0; direction < 8 && !found; direction++) {
                double angle = (direction + world.rand.nextDouble()) * Math.PI / 4;
                double x = anchor.getX() + 0.5 + Math.cos(angle) * radius;
                double z = anchor.getZ() + 0.5 + Math.sin(angle) * radius;
                for (int dy : new int[]{0,1,-1,2,-2,3,-3,4,-4}) {
                    double y = anchor.getY() + dy;
                    entity.setPosition(x, y, z);
                    if (y < 1 || y + entity.height >= world.getActualHeight()
                            || !world.isAreaLoaded(new BlockPos(x - 1, y - 1, z - 1), new BlockPos(x + 1, y + entity.height, z + 1))
                            || !world.getWorldBorder().contains(entity.getEntityBoundingBox())
                            || !world.getCollisionBoxes(entity, entity.getEntityBoundingBox()).isEmpty()
                            || !world.checkNoEntityCollision(entity.getEntityBoundingBox(), entity)
                            || world.containsAnyLiquid(entity.getEntityBoundingBox())
                            || world.getCollisionBoxes(entity, entity.getEntityBoundingBox().offset(0, -0.08, 0)).isEmpty()) continue;
                    found = true; break;
                }
            }
        }
        if (!found) return;
        entity.markRaidSpawned();
        entity.onInitialSpawn(world.getDifficultyForLocation(entity.getPosition()), null);
        if (world.spawnEntity(entity) && !FFDRaidManager.addParticipant(context.getInstanceId(), entity, false)) entity.setDead();
    }
}
