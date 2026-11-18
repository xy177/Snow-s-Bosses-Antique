package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.MechasentContent;
import xy177.snowsbosses.content.MechasentEquipment;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID,value=Side.CLIENT)
public final class MechasentArmRenderer {
    private MechasentArmRenderer(){ }
    @SubscribeEvent public static void hand(RenderSpecificHandEvent event){
        Minecraft client=Minecraft.getMinecraft();
        ItemStack stack=event.getItemStack();
        if(client.player==null || stack.getItem()!=MechasentContent.ARM || !client.player.isHandActive()
                || client.player.getActiveHand()!=event.getHand())return;
        boolean right=(event.getHand()==EnumHand.MAIN_HAND?client.player.getPrimaryHand():client.player.getPrimaryHand().opposite())==EnumHandSide.RIGHT;
        int side=right?1:-1;
        event.setCanceled(true);
        GlStateManager.pushMatrix();
        try{
            GlStateManager.translate(side*0.56F,-0.52F-event.getEquipProgress()*0.6F,-0.72F);
            GlStateManager.rotate(side*45F,0,1,0);
            GlStateManager.scale(0.4F,0.4F,0.4F);
            GlStateManager.translate(side*-0.5F,0.2F,0);
            GlStateManager.rotate(side*30F,0,1,0);
            GlStateManager.rotate(-80F,1,0,0);
            GlStateManager.rotate(side*60F,0,1,0);
            GlStateManager.translate(0,-0.3F,0);
            GlStateManager.scale(1.5F,1.5F,1.5F);
            GlStateManager.rotate(side*50F,0,1,0);
            GlStateManager.rotate(side*-25F,0,0,1);
            GlStateManager.translate(side*-0.4375F,0.4375F,-0.03125F);
            if(right)GlStateManager.rotate(180F,0,1,0);
            client.getItemRenderer().renderItemSide(client.player,stack,ItemCameraTransforms.TransformType.NONE,false);
        }finally{GlStateManager.popMatrix();}
    }
    public static void overlay(ItemStack stack,int x,int y){
        if(stack.isEmpty() || stack.getItem()!=MechasentContent.ARM)return;
        boolean lighting=GL11.glIsEnabled(GL11.GL_LIGHTING),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean texture=GL11.glIsEnabled(GL11.GL_TEXTURE_2D),alpha=GL11.glIsEnabled(GL11.GL_ALPHA_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND);
        GlStateManager.disableLighting();GlStateManager.disableDepth();GlStateManager.disableTexture2D();
        GlStateManager.disableAlpha();GlStateManager.disableBlend();
        try{
            BufferBuilder buffer=Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS,DefaultVertexFormats.POSITION_COLOR);
            rectangle(buffer,x+2,y+11,13,2,0);
            rectangle(buffer,x+2,y+11,Math.round(13F*MechasentEquipment.charge(stack)/5F),1,0xFF2020);
            Tessellator.getInstance().draw();
        }finally{
            if(blend)GlStateManager.enableBlend();if(alpha)GlStateManager.enableAlpha();if(texture)GlStateManager.enableTexture2D();
            if(lighting)GlStateManager.enableLighting();if(depth)GlStateManager.enableDepth();
        }
    }
    private static void rectangle(BufferBuilder buffer,int x,int y,int width,int height,int color){
        int r=color>>16&255,g=color>>8&255,b=color&255;
        buffer.pos(x,y,0).color(r,g,b,255).endVertex();buffer.pos(x,y+height,0).color(r,g,b,255).endVertex();
        buffer.pos(x+width,y+height,0).color(r,g,b,255).endVertex();buffer.pos(x+width,y,0).color(r,g,b,255).endVertex();
    }
}
