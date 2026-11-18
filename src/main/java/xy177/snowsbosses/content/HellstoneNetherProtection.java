package xy177.snowsbosses.content;

import java.lang.reflect.Field;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class HellstoneNetherProtection {
    private static final String PROVOKED = "SnowHellstoneProvokedBy";
    private static final Class<?> PIGLIN = piglinClass();
    private static final Field PIGLIN_TARGET = piglinField("angerTarget");
    private static final Field PIGLIN_TICKS = piglinField("angerTicks");

    private HellstoneNetherProtection() {
    }

    private static Class<?> piglinClass() {
        try {
            return Class.forName("xy177.farmersfuturedelight.common.entity.EntityPiglin", false,
                    HellstoneNetherProtection.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static Field piglinField(String name) {
        if (PIGLIN == null) return null;
        try {
            Field field = PIGLIN.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new IllegalStateException("Cannot access piglin anger state", exception);
        }
    }

    private static boolean covered(Entity entity) {
        return entity instanceof EntityMagmaCube || entity instanceof EntityWitherSkeleton
                || entity instanceof EntityBlaze || entity instanceof EntityGhast
                || entity instanceof EntityPigZombie || PIGLIN != null && PIGLIN.isInstance(entity);
    }

    private static boolean protectedPlayer(EntityLivingBase target) {
        return target instanceof EntityPlayer
                && target.getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem() == HellstoneContent.BOOTS;
    }

    private static boolean provoked(Entity mob, Entity target) {
        NBTTagCompound data = mob.getEntityData();
        return data.hasUniqueId(PROVOKED) && data.getUniqueId(PROVOKED).equals(target.getUniqueID());
    }

    static boolean passiveContact(Entity mob, EntityLivingBase target) {
        return mob instanceof EntityMagmaCube && protectedPlayer(target) && !provoked(mob, target);
    }

    @SubscribeEvent
    public static void attacking(AttackEntityEvent event) {
        if (!event.getEntityPlayer().world.isRemote && covered(event.getTarget())) {
            event.getTarget().getEntityData().setUniqueId(PROVOKED, event.getEntityPlayer().getUniqueID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacked(LivingAttackEvent event) {
        EntityLivingBase victim = event.getEntityLiving();
        Entity attacker = event.getSource().getTrueSource();
        if (!victim.world.isRemote && event.getAmount() > 0 && attacker instanceof EntityPlayer && covered(victim)) {
            victim.getEntityData().setUniqueId(PROVOKED, attacker.getUniqueID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void targeting(LivingSetAttackTargetEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving) || event.getEntityLiving().world.isRemote
                || event.getTarget() == null || !covered(event.getEntityLiving())) return;
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        if (protectedPlayer(event.getTarget()) && !provoked(mob, event.getTarget())) clear(mob);
    }

    private static void clear(EntityLiving mob) {
        mob.setAttackTarget(null);
        mob.setRevengeTarget(null);
        mob.getNavigator().clearPath();
        if (mob instanceof EntityPigZombie) {
            ObfuscationReflectionHelper.setPrivateValue(EntityPigZombie.class, (EntityPigZombie) mob, 0,
                    "angerLevel", "field_70837_d");
            ObfuscationReflectionHelper.setPrivateValue(EntityPigZombie.class, (EntityPigZombie) mob, null,
                    "angerTargetUUID", "field_175459_bn");
        } else if (PIGLIN != null && PIGLIN.isInstance(mob)) {
            try {
                PIGLIN_TARGET.set(mob, null);
                PIGLIN_TICKS.setInt(mob, 0);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot clear piglin anger state", exception);
            }
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntityLiving) || event.getEntityLiving().world.isRemote
                || !covered(event.getEntityLiving())) return;
        EntityLiving mob = (EntityLiving) event.getEntityLiving();
        EntityLivingBase target = mob.getAttackTarget();
        EntityLivingBase revenge = mob.getRevengeTarget();
        if (target != null && protectedPlayer(target) && !provoked(mob, target)
                || revenge != null && protectedPlayer(revenge) && !provoked(mob, revenge)) {
            clear(mob);
            return;
        }
        NBTTagCompound data = mob.getEntityData();
        if (!data.hasUniqueId(PROVOKED)) return;
        UUID player = data.getUniqueId(PROVOKED);
        if (target != null && player.equals(target.getUniqueID()) && target.isEntityAlive()
                || revenge != null && player.equals(revenge.getUniqueID()) && revenge.isEntityAlive()
                || mob instanceof EntityPigZombie && ((EntityPigZombie) mob).isAngry()) return;
        if (PIGLIN != null && PIGLIN.isInstance(mob)) {
            try {
                if (PIGLIN_TICKS.getInt(mob) > 0 && player.equals(PIGLIN_TARGET.get(mob))) return;
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot read piglin anger state", exception);
            }
        }
        data.removeTag(PROVOKED + "Most");
        data.removeTag(PROVOKED + "Least");
    }
}
