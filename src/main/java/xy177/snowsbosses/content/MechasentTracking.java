package xy177.snowsbosses.content;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.IThrowableEntity;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.MechasentWorkerBee;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID)
public final class MechasentTracking {
    private static final Map<World,Map<Entity,Shot>> ACTIVE=new IdentityHashMap<>();
    private MechasentTracking(){ }
    private static final class Shot {
        final UUID target;
        int steeredAge=-1;
        Shot(UUID target){this.target=target;}
    }

    public static EntityPlayer owner(Entity entity){
        Entity owner=entity instanceof EntityArrow?((EntityArrow)entity).shootingEntity
                :entity instanceof EntityThrowable?((EntityThrowable)entity).getThrower()
                :entity instanceof EntityFireball?((EntityFireball)entity).shootingEntity
                :entity instanceof IThrowableEntity?((IThrowableEntity)entity).getThrower():null;
        return owner instanceof EntityPlayer?(EntityPlayer)owner:null;
    }

    private static boolean valid(EntityPlayer player,EntityLivingBase target){
        return target!=player && target.isEntityAlive() && !(target instanceof EntityAnimal)
                && !MechasentEquipment.allies(player,target)
                && (!(target instanceof EntityPlayer) || !((EntityPlayer)target).isSpectator() && !((EntityPlayer)target).capabilities.isCreativeMode);
    }

    public static EntityLivingBase select(EntityPlayer player,Vec3d direction){
        Vec3d look=direction.lengthSquared()>1.0E-8?direction.normalize():player.getLookVec();
        Vec3d eye=player.getPositionEyes(1);
        Vec3d horizontal=new Vec3d(look.x,0,look.z);
        if(horizontal.lengthSquared()<1.0E-8){
            double yaw=Math.toRadians(player.rotationYaw);
            horizontal=new Vec3d(-Math.sin(yaw),0,Math.cos(yaw));
        }
        EntityLivingBase selected=null;
        int priority=Integer.MAX_VALUE;
        double alignment=-2,nearest=Double.MAX_VALUE;
        for(EntityLivingBase target:player.world.getEntitiesWithinAABB(EntityLivingBase.class,player.getEntityBoundingBox().grow(30))){
            if(!valid(player,target))continue;
            Vec3d delta=target.getPositionEyes(1).subtract(eye);
            double distance=delta.lengthSquared();
            if(distance>900)continue;
            double dot=distance<1.0E-8?1:look.dotProduct(delta.normalize());
            if(horizontal.x*delta.x+horizontal.z*delta.z<-1.0E-7)continue;
            if(!target.isPotionActive(MechasentStatus.ALERT)){
                if(distance<=100 && dot>=Math.cos(Math.toRadians(10)) && MechasentEquipment.hostileTo(player,target))return null;
                continue;
            }
            int rank=target instanceof MechasentWorkerBee && !((MechasentWorkerBee)target).sleeping()?0:!target.isNonBoss()?1:2;
            if(rank<priority || rank==priority && (dot>alignment+1.0E-7 || Math.abs(dot-alignment)<1.0E-7 && distance<nearest)){
                selected=target;priority=rank;alignment=dot;nearest=distance;
            }
        }
        return selected;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void launch(EntityJoinWorldEvent event){
        if(event.getWorld().isRemote)return;
        Entity projectile=event.getEntity();NBTTagCompound data=projectile.getEntityData();
        boolean restored=data.hasUniqueId("MechasentCoilOwner");
        if(data.getBoolean("MechasentCoilSpent") || restored && !data.hasUniqueId("MechasentCoilTarget"))return;
        EntityPlayer player=restored?event.getWorld().getMinecraftServer().getPlayerList().getPlayerByUUID(data.getUniqueId("MechasentCoilOwner")):owner(projectile);
        if(player==null || !restored && !MechasentModules.TRACKING.worn(player))return;
        if(!restored)data.setUniqueId("MechasentCoilOwner",player.getUniqueID());
        if(restored || new Vec3d(projectile.motionX,projectile.motionY,projectile.motionZ).lengthSquared()<1.0E-8)
            MechasentDeferred.next(()->register(projectile,player,restored));
        else register(projectile,player,false);
    }

    private static void register(Entity projectile,EntityPlayer player,boolean restored){
        NBTTagCompound data=projectile.getEntityData();
        if(projectile.isDead || projectile.world!=player.world || !player.isEntityAlive() || data.getBoolean("MechasentCoilSpent"))return;
        if(projectile instanceof EntityArrow){
            NBTTagCompound saved=new NBTTagCompound();projectile.writeToNBT(saved);
            if(saved.getBoolean("inGround")){stop(projectile);return;}
        }
        Entity target=restored?((WorldServer)projectile.world).getEntityFromUuid(data.getUniqueId("MechasentCoilTarget"))
                :select(player,player.getLookVec());
        if(target instanceof EntityLivingBase && valid(player,(EntityLivingBase)target)){
            Shot shot=new Shot(target.getUniqueID());
            data.setUniqueId("MechasentCoilTarget",shot.target);
            ACTIVE.computeIfAbsent(projectile.world,world->new IdentityHashMap<>()).put(projectile,shot);
            steer(projectile,(EntityLivingBase)target,shot);
        }
    }

    private static void stop(Entity projectile){
        NBTTagCompound data=projectile.getEntityData();
        data.removeTag("MechasentCoilTargetMost");data.removeTag("MechasentCoilTargetLeast");
        data.setBoolean("MechasentCoilSpent",true);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void impact(ProjectileImpactEvent event){
        Entity projectile=event.getEntity();
        if(projectile.world.isRemote)return;
        EntityPlayer player=owner(projectile);
        if(player!=null && event.getRayTraceResult().entityHit instanceof EntityLivingBase)
            mark(player,(EntityLivingBase)event.getRayTraceResult().entityHit);
        if(!projectile.getEntityData().hasUniqueId("MechasentCoilOwner"))return;
        Map<Entity,Shot> shots=ACTIVE.get(projectile.world);
        if(shots!=null)shots.remove(projectile);
        stop(projectile);
    }

    @SubscribeEvent public static void tick(TickEvent.WorldTickEvent event){
        if(event.phase!=TickEvent.Phase.START || event.world.isRemote)return;
        Map<Entity,Shot> shots=ACTIVE.get(event.world);
        if(shots==null)return;
        WorldServer world=(WorldServer)event.world;
        Iterator<Map.Entry<Entity,Shot>> iterator=shots.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Entity,Shot> entry=iterator.next();Entity projectile=entry.getKey();
            if(projectile.isDead || projectile.world!=world || world.getEntityByID(projectile.getEntityId())!=projectile
                    || !world.isBlockLoaded(projectile.getPosition())){iterator.remove();continue;}
            EntityPlayer player=world.getMinecraftServer().getPlayerList().getPlayerByUUID(projectile.getEntityData().getUniqueId("MechasentCoilOwner"));
            Entity target=world.getEntityFromUuid(entry.getValue().target);
            Entity actual=owner(projectile);
            if(player==null || !player.isEntityAlive() || player.world!=world || player.isSpectator()
                    || actual!=null && actual!=player || !(target instanceof EntityLivingBase)
                    || !valid(player,(EntityLivingBase)target) || !((EntityLivingBase)target).isPotionActive(MechasentStatus.ALERT)){
                iterator.remove();stop(projectile);continue;
            }
            steer(projectile,(EntityLivingBase)target,entry.getValue());
        }
        if(shots.isEmpty())ACTIVE.remove(world);
    }

    private static void steer(Entity projectile,EntityLivingBase target,Shot shot){
        if(shot.steeredAge==projectile.ticksExisted)return;
        shot.steeredAge=projectile.ticksExisted;
        Vec3d motion=new Vec3d(projectile.motionX,projectile.motionY,projectile.motionZ);
        double speed=motion.lengthVector();
        Vec3d delta=new Vec3d(target.posX-projectile.posX,target.posY+target.height*0.5-projectile.posY,target.posZ-projectile.posZ);
        if(speed<1.0E-6 || delta.lengthSquared()<1.0E-8)return;
        double lead=Math.min(5,delta.lengthVector()/speed);
        Vec3d aim=delta.addVector(target.motionX*lead,0,target.motionZ*lead);
        Vec3d direction=(aim.lengthSquared()<1.0E-8?delta:aim).normalize();
        projectile.motionX=direction.x*speed;projectile.motionY=direction.y*speed;projectile.motionZ=direction.z*speed;
        if(projectile instanceof EntityFireball){
            EntityFireball fireball=(EntityFireball)projectile;
            double acceleration=Math.sqrt(fireball.accelerationX*fireball.accelerationX+fireball.accelerationY*fireball.accelerationY+fireball.accelerationZ*fireball.accelerationZ);
            fireball.accelerationX=direction.x*acceleration;fireball.accelerationY=direction.y*acceleration;fireball.accelerationZ=direction.z*acceleration;
        }
        projectile.velocityChanged=true;
    }

    @SubscribeEvent public static void unload(WorldEvent.Unload event){if(!event.getWorld().isRemote)ACTIVE.remove(event.getWorld());}
    public static void clear(){ACTIVE.clear();}

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void hit(LivingHurtEvent event){
        if(event.getEntityLiving().world.isRemote || event.getAmount()<=0 || !event.getSource().isProjectile())return;
        Entity source=event.getSource().getTrueSource();
        if(!(source instanceof EntityPlayer))return;
        mark((EntityPlayer)source,event.getEntityLiving());
    }

    private static void mark(EntityPlayer player,EntityLivingBase target){
        if(MechasentModules.TRACKING.worn(player) && valid(player,target) && !target.isPotionActive(MechasentStatus.ALERT))
            target.addPotionEffect(new PotionEffect(MechasentStatus.ALERT,600,0,false,false));
    }
}
