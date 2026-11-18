package xy177.snowsbosses.entity;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class TechnicalTargetAi {
    private TechnicalTargetAi() {
    }

    @SubscribeEvent
    public static void joined(EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote || !(event.getEntity() instanceof EntityCreature)) return;
        EntityCreature creature = (EntityCreature) event.getEntity();
        if (!(creature instanceof Drowner || creature instanceof DeepwaterPiranha
                || creature instanceof TideRaiser || creature instanceof CopperGolem
                || creature instanceof HellstoneGolem || creature instanceof HellstoneImp
                || creature instanceof Mechasent || creature instanceof MechasentWorkerBee)) return;
        for (EntityAITasks.EntityAITaskEntry entry : creature.targetTasks.taskEntries) {
            if (entry.action instanceof TargetTask) return;
        }
        creature.targetTasks.addTask(0, new TargetTask(creature));
    }

    private static final class TargetTask extends EntityAINearestAttackableTarget<TechnicalTarget> {
        TargetTask(EntityCreature creature) {
            super(creature, TechnicalTarget.class, 10, true, false, null);
        }
    }
}