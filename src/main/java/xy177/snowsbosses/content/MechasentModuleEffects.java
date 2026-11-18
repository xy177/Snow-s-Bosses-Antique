package xy177.snowsbosses.content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.AbstractIllager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import xy177.farmersfuturedelight.common.raid.FFDRaidManager;
import xy177.snowsbosses.Balance;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.entity.DrownerCold;
import xy177.snowsbosses.entity.MechasentWorkerBee;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID)
public final class MechasentModuleEffects {
    private static final UUID SPEED=UUID.fromString("f07a6356-1094-4dbc-bfe7-922ae27207df");
    private static final UUID ARM_DAMAGE=UUID.fromString("51ff491f-0c8d-400c-93ef-94a279717b75");
    private static final UUID ARM_SPEED=UUID.fromString("12ed9f56-c345-410b-8525-e5d59f776c1e");
    private static final UUID RESISTANCE=UUID.fromString("c0c58530-2280-4b6e-a76a-2f0d286b8d2c");
    private static final ThreadLocal<Boolean> MINING=ThreadLocal.withInitial(()->false);
    private MechasentModuleEffects() { }

    public static boolean enabled(EntityLivingBase player,boolean safe) {
        MechasentModules module=safe?MechasentModules.SAFE_WORK:MechasentModules.LEG_EXOSKELETON;
        ItemStack armor=player.getItemStackFromSlot(module.slot);
        return MechasentModules.has(armor,module) && (!armor.hasTagCompound() || !armor.getTagCompound().getCompoundTag(MechasentModules.SLOTS).getBoolean(safe?"SafeOff":"ClimbOff"));
    }

    public static void toggle(EntityPlayer player,boolean safe) {
        MechasentModules module=safe?MechasentModules.SAFE_WORK:MechasentModules.LEG_EXOSKELETON;
        ItemStack armor=player.getItemStackFromSlot(module.slot);
        if(!MechasentModules.has(armor,module)) return;
        NBTTagCompound data=MechasentModules.slots(armor);String key=safe?"SafeOff":"ClimbOff";
        data.setBoolean(key,!data.getBoolean(key));refreshStep(player);player.inventory.markDirty();player.inventoryContainer.detectAndSendChanges();
    }

    private static void attribute(IAttributeInstance instance,UUID id,String name,double value,int operation) {
        AttributeModifier current=instance.getModifier(id);
        if(current!=null && current.getAmount()==value && current.getOperation()==operation) return;
        if(current!=null)instance.removeModifier(id);
        if(value!=0)instance.applyModifier(new AttributeModifier(id,name,value,operation).setSaved(false));
    }

    public static void attributes(EntityPlayer player) {
        attribute(player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED),SPEED,"Leg exoskeleton",MechasentModules.LEG_EXOSKELETON.worn(player)?0.5:0,2);
        boolean arm=MechasentModules.ARM_EXOSKELETON.worn(player)&&player.getHeldItemMainhand().getItem()==MechasentContent.ARM;
        attribute(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE),ARM_DAMAGE,"Arm exoskeleton",arm?Balance.number("mechasent.arm.attackDamage",11)*0.3:0,0);
        attribute(player.getEntityAttribute(SharedMonsterAttributes.ATTACK_SPEED),ARM_SPEED,"Arm exoskeleton",arm?Balance.number("mechasent.arm.attackSpeed",1.6)*0.3:0,0);
        IAttributeInstance resistance=player.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
        AttributeModifier current=resistance.getModifier(RESISTANCE);
        double base=resistance.getAttributeValue()-(current==null?0:current.getAmount());
        attribute(resistance,RESISTANCE,"Safe construction",MechasentModules.SAFE_WORK.worn(player)?Math.max(0,0.9-base):0,0);
    }

    @SubscribeEvent
    public static void equipment(LivingEquipmentChangeEvent event) {
        if(event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player=(EntityPlayer)event.getEntityLiving();
            if(event.getSlot()==EntityEquipmentSlot.FEET)refreshStep(player);
            if(!player.world.isRemote)MechasentDeferred.next(()->{if(!player.isDead){attributes(player);refreshModules(player);}});
        }
    }

    private static void refreshStep(EntityPlayer player){
        boolean active=enabled(player,false);
        applyStep(player,active);
        if(player instanceof EntityPlayerMP)xy177.snowsbosses.network.MechasentControls.syncStep((EntityPlayerMP)player,active);
    }

    public static void applyStep(EntityPlayer player,boolean active){
        NBTTagCompound data=player.getEntityData();
        if(active){
            if(!data.hasKey("MechasentStep"))data.setFloat("MechasentStep",player.stepHeight);
            player.stepHeight=Math.max(player.stepHeight,2);
        }else if(data.hasKey("MechasentStep")){
            data.removeTag("MechasentClimbing");
            if(player.stepHeight==2)player.stepHeight=data.getFloat("MechasentStep");
            data.removeTag("MechasentStep");
        }
    }

    public static boolean climb(EntityPlayer player,boolean advancing){
        if(!advancing || !player.getEntityData().hasKey("MechasentStep") || !enabled(player,false)
                || !player.isEntityAlive() || player.isSpectator() || player.capabilities.isFlying || player.isElytraFlying()
                || player.isRiding() || player.isPlayerSleeping() || player.isInWater() || player.isInLava() || DrownerCold.isFrozen(player))return false;
        double yaw=Math.toRadians(player.rotationYaw);
        if(!player.world.collidesWithAnyBlock(player.getEntityBoundingBox().offset(-Math.sin(yaw)*0.12,0,Math.cos(yaw)*0.12)))return false;
        if(player.world.isRemote)player.motionY=Math.max(player.motionY,0.2);
        player.fallDistance=0;
        return true;
    }

    private static void refreshModules(EntityPlayer player){
        if(MechasentModules.HERO_FOOTSTEPS.worn(player)){
            Potion omen=Potion.REGISTRY.getObject(new ResourceLocation("farmers_future_delight:raid_omen"));
            if(omen!=null && player.isPotionActive(omen))player.removePotionEffect(omen);
        }
        if(MechasentModules.METABOLISM.worn(player)){
            for(PotionEffect effect:new ArrayList<>(player.getActivePotionEffects()))capPotion(player,effect.getPotion());
            if(!blacklisted("snows_bosses:malevolent_burn") && player.getEntityData().getInteger(HellstoneEffects.BURN)>Balance.integer("mechasent.modules.debuffDuration",60))
                player.getEntityData().setInteger(HellstoneEffects.BURN,Balance.integer("mechasent.modules.debuffDuration",60));
        }
    }

    private static void capPotion(EntityLivingBase entity,Potion potion){
        PotionEffect current=entity.getActivePotionEffect(potion);
        int cap=Balance.integer("mechasent.modules.debuffDuration",60);
        if(current==null || !potion.isBadEffect() || current.getDuration()<=cap || blacklisted(String.valueOf(potion.getRegistryName()))
                || !MechasentModules.METABOLISM.worn(entity))return;
        NBTTagCompound saved=current.writeCustomPotionEffectToNBT(new NBTTagCompound());saved.setInteger("Duration",cap);
        entity.removePotionEffect(potion);
        if(!entity.isPotionActive(potion))entity.addPotionEffect(PotionEffect.readCustomPotionEffectFromNBT(saved));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END) return;
        EntityPlayer player=event.player;
        NBTTagCompound data=player.getEntityData();
        if(player.world.isRemote)return;
        if(data.getBoolean("MechasentClimbing"))climb(player,true);
        if(player.ticksExisted%20!=0)return;
        attributes(player);
        if(MechasentModules.RADAR.worn(player))for(EntityLivingBase target:player.world.getEntitiesWithinAABB(EntityLivingBase.class,player.getEntityBoundingBox().grow(30))){
            if(player.getDistanceSq(target)<=900 && MechasentEquipment.hostileTo(player,target))target.addPotionEffect(new PotionEffect(MechasentStatus.ALERT,100,0,false,false));
        }
        if(MechasentModules.HERO_FOOTSTEPS.worn(player)){
            Potion omen=Potion.REGISTRY.getObject(new ResourceLocation("farmers_future_delight:raid_omen"));
            if(omen!=null && player.isPotionActive(omen))player.removePotionEffect(omen);
            data.removeTag("FFDRaidOmenPosition");data.removeTag("FFDRaidOmenExpired");
        }
    }

    public static void walk(EntityPlayer player,BlockPos center){
        WorldServer world=(WorldServer)player.world;
        for(BlockPos pos:BlockPos.getAllInBox(center.add(-5,-1,-5),center.add(5,-1,5))){
            if(pos.distanceSqToCenter(player.posX,player.posY,player.posZ)>25 || !world.isBlockLoaded(pos) || !world.isAirBlock(pos.up()))continue;
            IBlockState state=world.getBlockState(pos),replacement=null;
            if(state.getMaterial()==Material.WATER && state.getBlock() instanceof net.minecraft.block.BlockLiquid
                    && state.getValue(net.minecraft.block.BlockLiquid.LEVEL)==0)replacement=Blocks.FROSTED_ICE.getDefaultState();
            else if(state.getMaterial()==Material.LAVA && state.getBlock() instanceof net.minecraft.block.BlockLiquid
                    && state.getValue(net.minecraft.block.BlockLiquid.LEVEL)==0){
                Block basalt=Block.REGISTRY.getObject(new ResourceLocation("farmers_future_delight:basalt"));
                replacement=(basalt!=null && basalt!=Blocks.AIR?basalt:Blocks.NETHERRACK).getDefaultState();
            }
            if(replacement==null || !world.mayPlace(replacement.getBlock(),pos,false,EnumFacing.DOWN,player)
                    || !world.getWorldBorder().contains(pos))continue;
            net.minecraftforge.common.util.BlockSnapshot snapshot=net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(world,pos);
            if(!world.setBlockState(pos,replacement,2))continue;
            if(net.minecraftforge.event.ForgeEventFactory.onPlayerBlockPlace(player,snapshot,EnumFacing.UP,net.minecraft.util.EnumHand.MAIN_HAND).isCanceled()){
                snapshot.restore(true,false);
                continue;
            }
            world.notifyNeighborsOfStateChange(pos,state.getBlock(),false);
            if(replacement.getBlock()==Blocks.FROSTED_ICE)world.scheduleUpdate(pos,Blocks.FROSTED_ICE,60+world.rand.nextInt(61));
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void fall(LivingFallEvent event){if(MechasentModules.LUCKY_HORSESHOE.worn(event.getEntityLiving()))event.setCanceled(true);}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void dodge(LivingAttackEvent event){
        if(!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote
                || !MechasentModules.HERO_FOOTSTEPS.worn(event.getEntityLiving()) || event.getSource().canHarmInCreative())return;
        if(event.getEntityLiving().getRNG().nextDouble()<(event.getSource().isProjectile()?0.1:0.2))event.setCanceled(true);
    }

    @SubscribeEvent
    public static void hurt(LivingHurtEvent event){
        Entity attacker=event.getSource().getTrueSource();
        if(attacker instanceof EntityLivingBase && ((EntityLivingBase)attacker).isPotionActive(MechasentStatus.STIMULANT)){
            int level=((EntityLivingBase)attacker).getActivePotionEffect(MechasentStatus.STIMULANT).getAmplifier();
            event.setAmount(event.getAmount()*(1.2F+0.1F*level));
        }
        if(event.getEntityLiving() instanceof EntityPlayer){
            EntityPlayer player=(EntityPlayer)event.getEntityLiving();
            MechasentWorkerBee bee=bee(player);
            if(bee!=null && !bee.sleeping())event.setAmount(event.getAmount()*0.9F*(bee.action()==4?0.5F:1));
        }
    }

    public static MechasentWorkerBee bee(EntityPlayer player){
        if(!(player.world instanceof WorldServer))return null;
        NBTTagCompound state=MechasentEquipment.state(player);
        Entity bee=state.hasUniqueId("Bee")?((WorldServer)player.world).getEntityFromUuid(state.getUniqueId("Bee")):null;
        return bee instanceof MechasentWorkerBee && bee.isEntityAlive()?(MechasentWorkerBee)bee:null;
    }

    @SubscribeEvent
    public static void target(LivingSetAttackTargetEvent event){
        if(event.getEntityLiving().world.isRemote || !(event.getTarget() instanceof EntityPlayer)
                || !(event.getEntityLiving() instanceof AbstractIllager))return;
        EntityPlayer player=(EntityPlayer)event.getTarget();EntityLivingBase mob=event.getEntityLiving();
        if(MechasentModules.GAZE_SHIELD.worn(player) && mob.getRevengeTarget()!=player
                && !FFDRaidManager.hasActiveRaid((WorldServer)player.world,player.getPosition()))((EntityLiving)mob).setAttackTarget(null);
    }

    @SubscribeEvent
    public static void food(LivingEntityUseItemEvent.Start event){
        if(MechasentModules.FEEDING.worn(event.getEntityLiving()) && event.getItem().getItemUseAction()==EnumAction.EAT && event.getDuration()>20)event.setDuration(20);
    }

    @SubscribeEvent
    public static void drink(LivingEntityUseItemEvent.Finish event){
        EntityLivingBase entity=event.getEntityLiving();
        if(entity.world.isRemote || !MechasentModules.METABOLISM.worn(entity))return;
        ResourceLocation id=event.getItem().getItem().getRegistryName();
        if(id!=null && id.getResourcePath().equals("ominous_bottle")){
            Potion hero=Potion.REGISTRY.getObject(new ResourceLocation("farmers_future_delight:hero_of_the_village"));
            if(hero!=null)entity.addPotionEffect(new PotionEffect(hero,1000,0,false,false));
        }
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void applicable(PotionEvent.PotionApplicableEvent event){
        ResourceLocation id=event.getPotionEffect().getPotion().getRegistryName();
        if(id!=null && id.getResourcePath().equals("raid_omen") && MechasentModules.HERO_FOOTSTEPS.worn(event.getEntityLiving()))event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void potion(PotionEvent.PotionAddedEvent event){
        EntityLivingBase entity=event.getEntityLiving();PotionEffect effect=event.getPotionEffect();
        if(entity.world.isRemote)return;

        if(!MechasentModules.METABOLISM.worn(entity) || !effect.getPotion().isBadEffect())return;
        int cap=Balance.integer("mechasent.modules.debuffDuration",60);
        String id=String.valueOf(effect.getPotion().getRegistryName());
        if(effect.getDuration()<=cap || blacklisted(id))return;
        MechasentDeferred.next(()->{
            if(!entity.isDead)capPotion(entity,effect.getPotion());
        });
    }

    public static boolean blacklisted(String id){
        for(String entry:Balance.text("mechasent.modules.debuffBlacklist").split(","))if(entry.trim().equals(id))return true;
        return false;
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void death(LivingDeathEvent event){
        if(!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote || event.getSource().canHarmInCreative())return;
        EntityPlayer player=(EntityPlayer)event.getEntityLiving();
        if(player.getHeldItemMainhand().getItem()==HellstoneContent.TOTEM || player.getHeldItemOffhand().getItem()==HellstoneContent.TOTEM)return;
        ItemStack chest=player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
        long now=MechasentEquipment.time(player);
        for(int i=0;i<2;i++){
            MechasentModules module=MechasentModules.get(chest,i);
            if(module==null || !module.totem() || MechasentModules.slots(chest).getLong("Cooldown"+i)>now)continue;
            boolean upgraded=MechasentModules.upgraded(chest);
            if(upgraded)MechasentModules.slots(chest).setLong("Cooldown"+i,now+Balance.integer("mechasent.modules.totemCooldown",3600));
            else MechasentModules.slots(chest).removeTag(i==0?"A":"B");
            event.setCanceled(true);player.setHealth(1);player.clearActivePotions();
            if(module==MechasentModules.PACEMAKER){
                player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION,900,1));
                player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION,100,1));
                player.addPotionEffect(new PotionEffect(MobEffects.FIRE_RESISTANCE,800,0));
                player.addPotionEffect(new PotionEffect(MechasentStatus.STIMULANT,3000,0,false,false));
                player.world.setEntityState(player,(byte)35);
            }else HellstoneEffects.activateTotem(player);
            player.inventory.markDirty();return;
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void mine(BlockEvent.BreakEvent event){
        if(MINING.get() || !(event.getPlayer() instanceof EntityPlayerMP))return;
        EntityPlayerMP player=(EntityPlayerMP)event.getPlayer();
        if(!player.getEntityData().getBoolean("MechasentMining") || !MechasentModules.EARTH_HAMMER.worn(player))return;
        BlockPos origin=event.getPos().toImmutable();IBlockState source=event.getState();
        ItemStack originalTool=player.getHeldItemMainhand();boolean emptyHand=originalTool.isEmpty();
        MechasentDeferred.next(()->{
            if(player.world!=event.getWorld() || !player.isEntityAlive() || event.isCanceled()
                    || event.getWorld().getBlockState(origin).getBlock()==source.getBlock())return;
            Set<BlockPos> visited=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
            queue.add(origin);visited.add(origin);MINING.set(true);
            try{
                while(!queue.isEmpty()){
                    BlockPos current=queue.removeFirst();
                    for(EnumFacing face:EnumFacing.values()){
                        ItemStack held=player.getHeldItemMainhand();
                        if(!player.isEntityAlive() || player.world!=event.getWorld()
                                || !player.getEntityData().getBoolean("MechasentMining") || !MechasentModules.EARTH_HAMMER.worn(player)
                                || (emptyHand?!held.isEmpty():originalTool.isEmpty() || held!=originalTool))return;
                        BlockPos pos=current.offset(face);
                        if(pos.distanceSq(origin)>25 || !visited.add(pos) || !player.world.isBlockLoaded(pos))continue;
                        IBlockState state=player.world.getBlockState(pos);
                        if(state.getBlock()!=source.getBlock() || state.getBlock().damageDropped(state)!=source.getBlock().damageDropped(source))continue;
                        if(player.interactionManager.tryHarvestBlock(pos))queue.addLast(pos);
                    }
                }
            }finally{MINING.set(false);}
        });
    }
}
