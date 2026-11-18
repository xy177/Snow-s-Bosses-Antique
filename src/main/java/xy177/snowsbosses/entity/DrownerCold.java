package xy177.snowsbosses.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import xy177.farmersfuturedelight.common.FFDPowderSnowEvents;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.network.Visuals;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class DrownerCold {
    public static final String COLD_TAG = FFDPowderSnowEvents.FROZEN_TICKS_TAG;
    public static final String FREEZE_TICKS_TAG = "snows_bosses.drownerFreezeTicks";
    private static final String FREEZE_UNTIL = "snows_bosses.drownerFreezeUntil";
    private static final String FREEZE_X = "snows_bosses.drownerFreezeX";
    private static final String FREEZE_Y = "snows_bosses.drownerFreezeY";
    private static final String FREEZE_Z = "snows_bosses.drownerFreezeZ";
    private static final int MAX_COLD = 140;
    private static final int FREEZE_DURATION = 70;

    private DrownerCold() {
    }

    public static void apply(EntityPlayer player) {
        if (player.world.isRemote) return;
        NBTTagCompound data = player.getEntityData();
        long now = player.world.getTotalWorldTime();
        if (data.getLong(FREEZE_UNTIL) > now) return;
        int cold = Math.min(MAX_COLD, data.getInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG) + MAX_COLD / 2);
        data.setInteger(COLD_TAG, cold);
        Visuals.drownerCold(player, cold);
        if (cold < MAX_COLD) return;
        data.setLong(FREEZE_UNTIL, now + FREEZE_DURATION);
        data.setInteger(FREEZE_TICKS_TAG, FREEZE_DURATION);
        data.setDouble(FREEZE_X, player.posX);
        data.setDouble(FREEZE_Y, player.posY);
        data.setDouble(FREEZE_Z, player.posZ);
        if (player.isRiding()) player.dismountRidingEntity();
        player.motionX = player.motionY = player.motionZ = 0;
        Visuals.drownerFreeze(player, FREEZE_DURATION);
    }

    public static boolean isFrozen(EntityPlayer player) {
        if (player == null) return false;
        return player.world.isRemote ? player.getEntityData().getInteger(FREEZE_TICKS_TAG) > 0
                : player.getEntityData().getLong(FREEZE_UNTIL) > player.world.getTotalWorldTime();
    }

    @SubscribeEvent
    public static void attacked(LivingDamageEvent event) {
        if (event.getEntityLiving().world.isRemote || !(event.getEntityLiving() instanceof Drowner)
                || event.getAmount() <= 0) return;
        Drowner drowner = (Drowner) event.getEntityLiving();
        if (drowner.isRiding() || !drowner.isHalfHealthCombat()
                && drowner.getHealth() - event.getAmount() > drowner.getMaxHealth() * 0.5F) return;
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)
                || event.getSource().getImmediateSource() != event.getSource().getTrueSource()) return;
        apply((EntityPlayer) event.getSource().getTrueSource());
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.side != Side.SERVER || event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        NBTTagCompound data = player.getEntityData();
        long until = data.getLong(FREEZE_UNTIL);
        long now = player.world.getTotalWorldTime();
        if (until > 0 && until <= now) {
            data.removeTag(FREEZE_UNTIL);
            data.removeTag(FREEZE_X);
            data.removeTag(FREEZE_Y);
            data.removeTag(FREEZE_Z);
            data.setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, 0);
            data.setInteger(FREEZE_TICKS_TAG, 0);
            Visuals.drownerCold(player, 0);
            Visuals.drownerThaw(player);
            return;
        }
        if (until > now) {
            data.setInteger(FREEZE_TICKS_TAG, (int) Math.min(Integer.MAX_VALUE, until - now));
            data.setInteger(FFDPowderSnowEvents.FROZEN_TICKS_TAG, MAX_COLD);
        }
        if (until <= now) return;
        if (player.openContainer != player.inventoryContainer) player.closeScreen();
        player.motionX = player.motionY = player.motionZ = 0;
        player.setPosition(data.getDouble(FREEZE_X), data.getDouble(FREEZE_Y), data.getDouble(FREEZE_Z));
        player.velocityChanged = true;
        if (player.isHandActive()) player.stopActiveHand();
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent event) {
        if (isFrozen(event.getEntityPlayer())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void interact(PlayerInteractEvent event) {
        if (isFrozen(event.getEntityPlayer()) && event.isCancelable()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void useItem(LivingEntityUseItemEvent.Start event) {
        if (event.getEntityLiving() instanceof EntityPlayer && isFrozen((EntityPlayer) event.getEntityLiving())) {
            event.setCanceled(true);
        }
    }
}
