package xy177.snowsbosses.content;

import java.util.List;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.Mechasent;
import xy177.snowsbosses.entity.MechasentLaser;
import xy177.snowsbosses.entity.MechasentWave;
import xy177.snowsbosses.entity.MechasentWorkerBee;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class MechasentEquipment {
    private static final String DATA = "SnowMechasent";
    private static final String[] BEES = {"Bee", "ExtraBee"};
    private MechasentEquipment() { }

    public static NBTTagCompound state(EntityPlayer player) {
        NBTTagCompound root = player.getEntityData();
        if (!root.hasKey(EntityPlayer.PERSISTED_NBT_TAG,10)) root.setTag(EntityPlayer.PERSISTED_NBT_TAG,new NBTTagCompound());
        root = root.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!root.hasKey(DATA,10)) root.setTag(DATA,new NBTTagCompound());
        return root.getCompoundTag(DATA);
    }

    public static long time(Entity entity) {
        return entity.world.isRemote || entity.getServer() == null ? entity.world.getTotalWorldTime()
                : entity.getServer().getWorld(0).getTotalWorldTime();
    }

    public static boolean fullSet(EntityLivingBase player) {
        return player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem()==MechasentContent.HELMET
                && player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getItem()==MechasentContent.CHESTPLATE
                && player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getItem()==MechasentContent.LEGGINGS
                && player.getItemStackFromSlot(EntityEquipmentSlot.FEET).getItem()==MechasentContent.BOOTS;
    }

    public static boolean allies(EntityPlayer player, Entity entity) {
        if (entity == null) return false;
        if (entity == player || player.isOnSameTeam(entity)) return true;
        if (entity instanceof EntityPlayer) return !player.canAttackPlayer((EntityPlayer) entity);
        if (entity instanceof EntityTameable) {
            EntityLivingBase owner = ((EntityTameable) entity).getOwner();
            return owner == player || owner != null && player.isOnSameTeam(owner);
        }
        if (entity instanceof MechasentWorkerBee) {
            EntityPlayer owner = ((MechasentWorkerBee) entity).playerOwner();
            return owner == player || owner != null && allies(player,owner);
        }
        return false;
    }

    public static boolean hostileTo(EntityPlayer player, EntityLivingBase entity) {
        if (allies(player,entity) || !entity.isEntityAlive() || entity instanceof EntityPlayer
                && (((EntityPlayer) entity).isSpectator() || ((EntityPlayer) entity).capabilities.isCreativeMode)) return false;
        return entity instanceof net.minecraft.entity.monster.IMob || entity instanceof EntityLiving
                && (((EntityLiving) entity).getAttackTarget()==player || entity.getRevengeTarget()==player);
    }

    public static boolean armUsable(EntityPlayer player, ItemStack stack) {
        return stack.getItem()==MechasentContent.ARM && !player.getCooldownTracker().hasCooldown(MechasentContent.ARM)
                && (player.world.isRemote || state(player).getLong("ArmUntil") <= time(player));
    }

    public static int charge(ItemStack stack) { return stack.hasTagCompound() ? Math.max(0,Math.min(5,stack.getTagCompound().getInteger("MechasentCharge"))) : 0; }
    public static void charge(ItemStack stack, int value) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger("MechasentCharge",Math.max(0,Math.min(5,value)));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void critical(CriticalHitEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote || player.getHeldItemMainhand().getItem()!=MechasentContent.ARM
                || event.getResult()==net.minecraftforge.fml.common.eventhandler.Event.Result.DENY
                || !event.isVanillaCritical() && event.getResult()!=net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW) return;
        player.getEntityData().setInteger("MechasentCriticalTarget",event.getTarget().getEntityId());
        player.getEntityData().setLong("MechasentCriticalTime",time(player));
    }

    public static void hit(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
        if (!(attacker instanceof EntityPlayer) || attacker.world.isRemote) return;
        EntityPlayer player = (EntityPlayer) attacker;
        NBTTagCompound data = player.getEntityData();
        boolean critical = data.hasKey("MechasentCriticalTarget") && data.getInteger("MechasentCriticalTarget")==target.getEntityId()
                && data.getLong("MechasentCriticalTime")==time(player);
        data.removeTag("MechasentCriticalTarget");
        if (!critical || !armUsable(player,stack) || allies(player,target)) return;
        double damage = Mechasent.n("attackDamage",16) * Mechasent.n("defense.laserDamageMultiplier",0.5);
        laser(player,target,damage);
        for (EntityLivingBase other : player.world.getEntitiesWithinAABB(EntityLivingBase.class,target.getEntityBoundingBox().grow(3,2,3))) {
            if (other == target || !(other instanceof EntityLiving) || ((EntityLiving) other).getAttackTarget()!=player && other.getRevengeTarget()!=player
                    || Math.hypot(other.posX-target.posX,other.posZ-target.posZ)>3 || allies(player,other)) continue;
            laser(player,other,damage);
        }
        if (charge(stack)>=5) {
            player.world.spawnEntity(new MechasentWave(player.world,player,damage));
            charge(stack,0);
        } else {
            int next=charge(stack)+(MechasentModules.ARM_EXOSKELETON.worn(player) ? 2 : 1);
            charge(stack,next);
            for(int i=5;i<next;i++)laser(player,target,damage);
        }
    }

    private static void laser(EntityPlayer player, EntityLivingBase target, double damage) {
        player.world.spawnEntity(new MechasentLaser(player.world,player.getUniqueID(),false,target.posX,target.posY,target.posZ,damage).forPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent event) {
        EntityPlayer player=event.getEntityPlayer();
        if (player.getHeldItemMainhand().getItem()==MechasentContent.ARM && !armUsable(player,player.getHeldItemMainhand())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void block(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) return;
        EntityPlayer player=(EntityPlayer)event.getEntityLiving();
        ItemStack arm=player.getActiveItemStack();
        DamageSource source=event.getSource();
        if (!armUsable(player,arm) || !player.isActiveItemStackBlocking() || source.isUnblockable()
                || event.getAmount()<=0 || player.isEntityInvulnerable(source)) return;
        Vec3d location=source.getDamageLocation();
        if (location==null && source.getImmediateSource()!=null) location=source.getImmediateSource().getPositionVector();
        if (location==null && !source.isExplosion()) return;
        if (location!=null) {
            Vec3d direction=location.subtract(player.getPositionVector());
            if (new Vec3d(player.getLookVec().x,0,player.getLookVec().z).dotProduct(new Vec3d(direction.x,0,direction.z))<=0) return;
        }
        event.setCanceled(true);
        player.world.setEntityState(player,(byte)29);
        arm.damageItem(Math.max(1,(int)Math.floor(event.getAmount())+1),player);
        if (charge(arm)>=5 && player.isSneaking() && (source.isProjectile() || source.isExplosion())) summon(player,arm);
    }

    public static boolean summon(EntityPlayer player, ItemStack arm) {
        NBTTagCompound data=state(player);
        if (player.world.isRemote || !armUsable(player,arm)) return false;
        for(String key:BEES)if(data.hasUniqueId(key) || data.hasKey("Suspended"+key,10))return false;
        java.util.ArrayList<MechasentWorkerBee> created=new java.util.ArrayList<>();
        int count=MechasentModules.ARM_EXOSKELETON.worn(player)?2:1;
        for(int i=0;i<count;i++){
            MechasentWorkerBee bee=new MechasentWorkerBee(player.world);
            bee.bind(player);
            if(!bee.teleportNearOwner() || !player.world.spawnEntity(bee)){
                for(MechasentWorkerBee existing:created)existing.setDead();
                return false;
            }
            created.add(bee);
        }
        for(int i=0;i<created.size();i++)data.setUniqueId(BEES[i],created.get(i).getUniqueID());
        data.setInteger("BeeDimension",player.dimension);
        data.setLong("ArmUntil",time(player)+2400);
        player.getCooldownTracker().setCooldown(MechasentContent.ARM,2400);
        player.stopActiveHand(); charge(arm,0);
        return true;
    }

    public static boolean bound(EntityPlayer player, MechasentWorkerBee bee) {
        NBTTagCompound data=state(player);
        for(String key:BEES)if(data.hasUniqueId(key) && data.getUniqueId(key).equals(bee.getUniqueID()))return true;
        return false;
    }

    public static void release(EntityPlayer player, UUID bee) {
        NBTTagCompound data=state(player);
        boolean found=bee==null;
        for(String key:BEES){
            if(bee!=null && (!data.hasUniqueId(key) || !data.getUniqueId(key).equals(bee)))continue;
            data.removeTag(key+"Most");data.removeTag(key+"Least");data.removeTag("Suspended"+key);found=true;
        }
        if(!found)return;
        data.removeTag("ArmUntil");
        player.getCooldownTracker().removeCooldown(MechasentContent.ARM);
    }

    public static void clearBee(EntityPlayer player) {
        detachBee(player,false);
        release(player,null);
    }

    private static void detachBee(EntityPlayer player, boolean keep) {
        NBTTagCompound data=state(player);
        if(player.getServer()==null)return;
        WorldServer world=player.getServer().getWorld(data.getInteger("BeeDimension"));
        java.util.ArrayList<Entity> detached=new java.util.ArrayList<>();
        for(String key:BEES){
            if(!data.hasUniqueId(key))continue;
            Entity bee=world==null?null:world.getEntityFromUuid(data.getUniqueId(key));
            data.removeTag(key+"Most");data.removeTag(key+"Least");
            if(bee instanceof MechasentWorkerBee && !bee.isDead){
                if(keep){NBTTagCompound saved=new NBTTagCompound();bee.writeToNBT(saved);data.setTag("Suspended"+key,saved);}
                detached.add(bee);
            }
        }
        for(Entity bee:detached)bee.setDead();
    }

    private static void restoreBee(EntityPlayer player) {
        NBTTagCompound data=state(player);
        if(data.hasKey("ArmUntil") && data.getLong("ArmUntil")<=time(player)){clearBee(player);return;}
        for(String key:BEES){
            String saved="Suspended"+key;
            if(!data.hasKey(saved,10))continue;
            MechasentWorkerBee bee=new MechasentWorkerBee(player.world);
            bee.readFromNBT(data.getCompoundTag(saved));
            bee.setUniqueId(UUID.randomUUID());
            data.setUniqueId(key,bee.getUniqueID());
            data.setInteger("BeeDimension",player.dimension);
            if(bee.teleportNearOwner() && player.world.spawnEntity(bee))data.removeTag(saved);
            else {data.removeTag(key+"Most");data.removeTag(key+"Least");}
        }
        syncCooldown(player);
    }

    private static void syncCooldown(EntityPlayer player) {
        long remaining=state(player).getLong("ArmUntil")-time(player);
        if (remaining>0) player.getCooldownTracker().setCooldown(MechasentContent.ARM,(int)Math.min(Integer.MAX_VALUE,remaining));
        else player.getCooldownTracker().removeCooldown(MechasentContent.ARM);
    }

    public static void playerDied(EntityPlayer player) {
        detachBee(player,false);
        for(String key:BEES)state(player).removeTag("Suspended"+key);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.side!=Side.SERVER || event.phase!=TickEvent.Phase.END || event.player.ticksExisted%20!=0) return;
        EntityPlayer player=event.player;
        if (state(player).getLong("ArmUntil")<=time(player) && state(player).hasKey("ArmUntil")) clearBee(player);
        else for(String key:BEES)if(state(player).hasKey("Suspended"+key,10)){restoreBee(player);break;}
    }

    @SubscribeEvent
    public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        NBTTagCompound old=state(event.getOriginal()).copy();
        state(event.getEntityPlayer()).merge(old);
        if(event.isWasDeath()) playerDied(event.getEntityPlayer());
    }

    @SubscribeEvent
    public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        detachBee(event.player,true);restoreBee(event.player);
    }
    @SubscribeEvent
    public static void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        event.player.getEntityData().removeTag("MechasentMining");detachBee(event.player,true);
    }
    @SubscribeEvent
    public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) { restoreBee(event.player); }
    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) { syncCooldown(event.player); }

    public static void addInformation(ItemStack stack, List<String> lines) {
        for (int i=0;i<2;i++) {
            MechasentModules module=MechasentModules.get(stack,i);
            boolean upgraded=module!=null && module.totem() && MechasentModules.upgraded(stack);
            String color=(module==null ? TextFormatting.GRAY : TextFormatting.GREEN).toString();
            lines.add(color+I18n.translateToLocal("tooltip.snows_bosses.slot"+(i==0 ? "A" : "B"))+"["
                    +I18n.translateToLocal(module==null ? "tooltip.snows_bosses.uninstalled" : module.label(upgraded))+"]");
            lines.add(color+(module==MechasentModules.EARTH_HAMMER
                    ? I18n.translateToLocalFormatted(module.label(false)+".description",SnowsBosses.PROXY.miningKeyTooltip())
                    : I18n.translateToLocal(module==null ? "tooltip.snows_bosses.no_effect" : module.label(upgraded)+".description")));
            if (module==MechasentModules.SAFE_WORK || module==MechasentModules.LEG_EXOSKELETON) {
                lines.add(color+xy177.snowsbosses.SnowsBosses.PROXY.moduleKeyTooltip(module==MechasentModules.SAFE_WORK,stack));
            }
        }
    }
}
