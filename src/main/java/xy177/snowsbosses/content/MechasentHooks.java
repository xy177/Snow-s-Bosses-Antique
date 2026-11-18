package xy177.snowsbosses.content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.util.DamageSource;

public final class MechasentHooks {
    private MechasentHooks() { }
    public static boolean nightVision(EntityLivingBase entity, net.minecraft.potion.Potion potion){
        return entity.isPotionActive(potion) || potion==net.minecraft.init.MobEffects.NIGHT_VISION && MechasentModules.NIGHT_VISION.worn(entity);
    }
    public static boolean nightVisionResult(EntityLivingBase entity,net.minecraft.potion.Potion potion,boolean original){
        return original || potion==net.minecraft.init.MobEffects.NIGHT_VISION && MechasentModules.NIGHT_VISION.worn(entity);
    }
    public static boolean illuminated(EntityLivingBase entity){return MechasentModules.NIGHT_VISION.worn(entity);}
    public static boolean gazeShield(EntityPlayer player){return MechasentModules.GAZE_SHIELD.worn(player);}
    public static boolean safeWalk(boolean original,Entity entity){
        return original || entity instanceof EntityPlayer && !((EntityPlayer)entity).capabilities.isFlying
                && !entity.isRiding() && MechasentModuleEffects.enabled((EntityPlayer)entity,true);
    }
    public static void worldWalk(EntityLivingBase entity,net.minecraft.util.math.BlockPos pos){
        if(entity instanceof EntityPlayer && !entity.world.isRemote && entity.onGround && MechasentModules.WORLD_WALKER.worn(entity))
            MechasentModuleEffects.walk((EntityPlayer)entity,pos);
    }
    public static boolean glowing(boolean original,Entity entity){return original || entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MechasentStatus.ALERT);}
    public static boolean glowingPotion(EntityLivingBase entity,net.minecraft.potion.Potion potion){
        return entity.isPotionActive(potion) || potion==net.minecraft.init.MobEffects.GLOWING && entity.isPotionActive(MechasentStatus.ALERT);
    }
    public static int outlineColor(int original,Entity entity){return entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isPotionActive(MechasentStatus.ALERT)?0xFF2020:original;}
    public static boolean anvilAllowed(boolean original,ContainerRepair container){return original || MechasentAnvil.free(container);}
    public static boolean afterDeathEvent(boolean canceled,EntityLivingBase entity,DamageSource source){
        if(canceled)return true;
        if(entity instanceof EntityPlayer && !entity.world.isRemote){
            EntityPlayer player=(EntityPlayer)entity;
            if(MechasentSurvival.rescue(player,source))return true;
            MechasentEquipment.playerDied(player);
        }
        return false;
    }
}
