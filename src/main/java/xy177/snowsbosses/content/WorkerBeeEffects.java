package xy177.snowsbosses.content;

import java.util.HashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Mechasent;
import xy177.snowsbosses.entity.MechasentWorkerBee;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class WorkerBeeEffects {
    private static final String LOCKS = "SnowWorkerLocks";
    private WorkerBeeEffects() { }

    public static void lock(EntityLivingBase target, MechasentWorkerBee bee) {
        NBTTagCompound locks = target.getEntityData().getCompoundTag(LOCKS);
        NBTTagCompound entry = new NBTTagCompound();
        entry.setLong("Until", target.world.getTotalWorldTime() + 2);
        entry.setBoolean("Raid", bee.raidAlly());
        if(bee.playerOwner()!=null)entry.setUniqueId("Player",bee.playerOwner().getUniqueID());
        locks.setTag(bee.getUniqueID().toString(), entry);
        target.getEntityData().setTag(LOCKS, locks);
    }

    public static void unlock(EntityLivingBase target, MechasentWorkerBee bee) {
        if (target != null) target.getEntityData().getCompoundTag(LOCKS).removeTag(bee.getUniqueID().toString());
    }

    private static NBTTagCompound locks(Entity entity) {
        if (entity == null) return new NBTTagCompound();
        NBTTagCompound locks = entity.getEntityData().getCompoundTag(LOCKS);
        for (String key : new HashSet<>(locks.getKeySet())) {
            if (locks.getCompoundTag(key).getLong("Until") < entity.world.getTotalWorldTime()) locks.removeTag(key);
        }
        if (locks.hasNoTags()) entity.getEntityData().removeTag(LOCKS);
        return locks;
    }

    public static boolean marked(Entity entity) { return !locks(entity).hasNoTags(); }

    private static boolean reducedAgainst(Entity attacker, Entity victim) {
        NBTTagCompound locks = locks(attacker);
        for (String key : locks.getKeySet()) {
            NBTTagCompound entry=locks.getCompoundTag(key);
            if(entry.hasUniqueId("Player")){
                net.minecraft.entity.player.EntityPlayer player=attacker.world.getPlayerEntityByUUID(entry.getUniqueId("Player"));
                if(player!=null && MechasentEquipment.allies(player,victim))return true;
            }else if(Mechasent.protectedTarget(entry.getBoolean("Raid"),victim))return true;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        Entity victim = event.getEntityLiving(), attacker = event.getSource().getTrueSource();
        if (victim.world.isRemote) return;
        if (victim instanceof MechasentWorkerBee && ((MechasentWorkerBee) victim).friendly(attacker)
                || attacker instanceof MechasentWorkerBee && ((MechasentWorkerBee) attacker).friendly(victim)) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        if (victim.world.isRemote) return;
        float factor = marked(victim) ? 1.5F : 1;
        if (reducedAgainst(event.getSource().getTrueSource(), victim)) factor *= 0.7F;
        if (victim instanceof Mechasent && ((Mechasent) victim).workers().supported()
                || victim instanceof MechasentWorkerBee && ((MechasentWorkerBee) victim).action() == 4
                && !((MechasentWorkerBee) victim).sleeping()) factor *= 0.5F;
        event.setAmount(event.getAmount() * factor);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.world.isRemote && entity.ticksExisted % 20 == 0 && entity.getEntityData().hasKey(LOCKS)) locks(entity);
    }
}
