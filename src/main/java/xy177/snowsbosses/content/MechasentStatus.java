package xy177.snowsbosses.content;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketRemoveEntityEffect;
import net.minecraft.world.WorldServer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid = SnowsBosses.MOD_ID)
public final class MechasentStatus {
    public static final Potion ALERT = new Status("alert",true,0xFF2020);
    public static final Potion STIMULANT = new Status("stimulant",false,0xFFBB28);
    private MechasentStatus() { }
    @SubscribeEvent public static void register(RegistryEvent.Register<Potion> event) { event.getRegistry().registerAll(ALERT,STIMULANT); }

    private static final class Status extends Potion {
        Status(String id, boolean bad, int color) {
            super(bad,color);setRegistryName(SnowsBosses.MOD_ID,id);setPotionName("effect.snows_bosses."+id);
        }
        @Override public void applyAttributesModifiersToEntity(EntityLivingBase entity,AbstractAttributeMap attributes,int amplifier){
            super.applyAttributesModifiersToEntity(entity,attributes,amplifier);
            if(this==ALERT && entity.world instanceof WorldServer && entity.isPotionActive(this))
                ((WorldServer)entity.world).getEntityTracker().sendToTracking(entity,new SPacketEntityEffect(entity.getEntityId(),entity.getActivePotionEffect(this)));
        }
        @Override public void removeAttributesModifiersFromEntity(EntityLivingBase entity,AbstractAttributeMap attributes,int amplifier){
            super.removeAttributesModifiersFromEntity(entity,attributes,amplifier);
            if(this==ALERT && entity.world instanceof WorldServer && !entity.isPotionActive(this))
                ((WorldServer)entity.world).getEntityTracker().sendToTracking(entity,new SPacketRemoveEntityEffect(entity.getEntityId(),this));
        }
        @SideOnly(Side.CLIENT) private void icon(Minecraft client,int x,int y,float alpha){
            GlStateManager.color(1,1,1,alpha);
            if(this==ALERT){
                client.getTextureManager().bindTexture(new ResourceLocation("minecraft:textures/items/barrier.png"));
                Gui.drawModalRectWithCustomSizedTexture(x,y,0,0,16,16,16,16);
            }else client.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(Items.TOTEM_OF_UNDYING),x,y);
            GlStateManager.color(1,1,1,1);
        }
        @SideOnly(Side.CLIENT) @Override public boolean hasStatusIcon() { return true; }
        @SideOnly(Side.CLIENT) @Override public void renderInventoryEffect(int x,int y,net.minecraft.potion.PotionEffect effect,Minecraft client) {
            icon(client,x+6,y+7,1);
        }
        @SideOnly(Side.CLIENT) @Override public void renderHUDEffect(int x,int y,net.minecraft.potion.PotionEffect effect,Minecraft client,float alpha) {
            icon(client,x+4,y+4,alpha);
        }
    }
}
