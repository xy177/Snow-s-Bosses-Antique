package xy177.snowsbosses.content;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraft.util.ResourceLocation;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.CopperGolem;
import xy177.snowsbosses.entity.DeepwaterPiranha;
import xy177.snowsbosses.entity.Drowner;
import xy177.snowsbosses.entity.PhantomPillar;
import xy177.snowsbosses.entity.PhantomShot;
import xy177.snowsbosses.entity.TideRaiser;
import xy177.snowsbosses.entity.StructureEyeEntity;
import xy177.snowsbosses.entity.TechnicalPiranha;

public final class ModEntities {
    private ModEntities() {
    }

    public static void register(Object mod) {
        register(mod, Drowner.class, "drowner", 0, 3);
        register(mod, PhantomShot.class, "phantom_trident", 1, 1);
        register(mod, PhantomPillar.class, "phantom_trident_entity", 2, 3);
        register(mod, PhantomShot.Red.class, "phantom_trident_2", 3, 1);
        register(mod, DeepwaterPiranha.class, "deepwater_piranha", 4, 3);
        register(mod, TideRaiser.class, "tide_raiser", 5, 3);
        register(mod, CopperGolem.class, "copper_golem", 6, 3);
        register(mod, PhantomPillar.Enchanted.class, "enchant_phantom_trident_entity", 7, 3);
        register(mod, CoralTools.FireproofItem.class, "fireproof_item", 8, 20);
        register(mod, xy177.snowsbosses.entity.HellstoneGolem.class, "hellstone_golem", 9, 3);
        register(mod, xy177.snowsbosses.entity.HellstoneImp.class, "hellstone_imp", 10, 3);
        register(mod, xy177.snowsbosses.entity.HellstoneBomb.class, "hellstone_bomb", 11, 1);
        register(mod, xy177.snowsbosses.entity.HellstoneWave.class, "shock_wave", 12, 1);
        register(mod, StructureEyeEntity.class, "structure_eye", 13, 4);
        register(mod, xy177.snowsbosses.entity.Mechasent.class, "mechasent", 14, 3);
        register(mod, xy177.snowsbosses.entity.MechasentWave.class, "mechasent_wave", 15, 1);
        register(mod, xy177.snowsbosses.entity.MechasentLaser.class, "mechasent_laser", 16, 1);
        register(mod, xy177.snowsbosses.entity.MechasentWorkerBee.class, "mechasent_worker_bee", 17, 2);
        register(mod, xy177.snowsbosses.entity.WorkerBeeLaser.class, "worker_bee_laser", 18, 1);
        register(mod, xy177.snowsbosses.entity.WorkerBeeBeam.class, "worker_bee_beam", 19, 3);
        register(mod, xy177.snowsbosses.entity.TechnicalTarget.class, "technical_target", 20, 3);
        register(mod, TechnicalPiranha.class, "technical_piranha", 21, 1);
        register(mod, xy177.snowsbosses.entity.HellstonePillar.class, "hellstone_pillar", 22, 1);
        register(mod, xy177.snowsbosses.entity.HellstonePillarShot.class, "hellstone_pillar_shot", 23, 1);
        register(mod, xy177.snowsbosses.entity.CoralSpearShot.class, "coral_spear", 24, 1);
        SpawnEgg.register("mechasent_worker_bee", 0x666666, 0xCC2525);
        SpawnEgg.register("mechasent", -6710887, -50630);
        SpawnEgg.register("hellstone_golem", -1, -1);
        SpawnEgg.register("hellstone_imp", -1, -1);
        SpawnEgg.register("drowner", -11506070, -4795585);
        SpawnEgg.register("deepwater_piranha", -14072252, -12819102);
        SpawnEgg.register("tide_raiser", -11360634, -6440642);
        SpawnEgg.register("copper_golem", -2720933, -4035764);
    }

    private static void register(Object mod, Class<? extends Entity> entity, String name, int id, int interval) {
        EntityRegistry.registerModEntity(new ResourceLocation(SnowsBosses.MOD_ID, name), entity,
                SnowsBosses.MOD_ID + "." + name, id, mod, 64, interval, true);
    }

}
