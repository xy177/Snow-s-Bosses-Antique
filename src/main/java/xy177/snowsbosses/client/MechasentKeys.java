package xy177.snowsbosses.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.input.Keyboard;
import xy177.snowsbosses.SnowsBosses;
import xy177.snowsbosses.content.MechasentContent;
import xy177.snowsbosses.content.MechasentModules;
import xy177.snowsbosses.network.MechasentControls;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID,value=Side.CLIENT)
public final class MechasentKeys {
    public static final KeyBinding SAFE=new KeyBinding("key.snows_bosses.safe_work",Keyboard.KEY_L,"itemGroup.snows_bosses");
    public static final KeyBinding CLIMB=new KeyBinding("key.snows_bosses.climb",Keyboard.KEY_P,"itemGroup.snows_bosses");
    public static final KeyBinding MINE=new KeyBinding("key.snows_bosses.earth_hammer",Keyboard.KEY_GRAVE,"itemGroup.snows_bosses");
    private static boolean mining,climbing;
    private MechasentKeys(){ }
    public static void register(){
        ClientRegistry.registerKeyBinding(SAFE);ClientRegistry.registerKeyBinding(CLIMB);ClientRegistry.registerKeyBinding(MINE);
        MechasentContent.ARM.addPropertyOverride(new ResourceLocation("blocking"),(stack,world,entity)->
                entity!=null && entity.isHandActive() && entity.getActiveItemStack()==stack?1:0);
    }
    public static String tooltip(boolean safe,ItemStack stack){
        boolean off=stack.hasTagCompound() && stack.getTagCompound().getCompoundTag(MechasentModules.SLOTS).getBoolean(safe?"SafeOff":"ClimbOff");
        return I18n.translateToLocalFormatted(safe?"tooltip.snows_bosses.safe_key":"tooltip.snows_bosses.climb_key",
                (safe?SAFE:CLIMB).getDisplayName(),I18n.translateToLocal(off?"options.off":"options.on"));
    }
    @SubscribeEvent public static void key(InputEvent.KeyInputEvent event){
        Minecraft client=Minecraft.getMinecraft();
        if(client.player==null || client.currentScreen!=null || !client.inGameHasFocus){release();return;}
        if(SAFE.isPressed())MechasentControls.send(0);
        if(CLIMB.isPressed())MechasentControls.send(1);
        syncMining();
    }
    @SubscribeEvent public static void mouse(InputEvent.MouseInputEvent event){syncMining();}
    private static void syncMining(){
        Minecraft client=Minecraft.getMinecraft();
        boolean next=client.player!=null && client.currentScreen==null && client.inGameHasFocus
                && MINE.isKeyDown() && MechasentModules.EARTH_HAMMER.worn(client.player);
        if(next!=mining){mining=next;if(client.player!=null)MechasentControls.send(next?2:3);}
    }
    @SubscribeEvent public static void tick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event){
        if(event.phase!=net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END)return;
        syncMining();
        Minecraft client=Minecraft.getMinecraft();
        boolean next=client.player!=null && client.currentScreen==null && client.inGameHasFocus
                && xy177.snowsbosses.content.MechasentModuleEffects.climb(client.player,client.player.moveForward>0);
        if(next!=climbing){climbing=next;if(client.player!=null)MechasentControls.send(next?4:5);}
    }
    public static void focusLost(){release();}
    private static void release(){if(mining && Minecraft.getMinecraft().player!=null)MechasentControls.send(3);mining=false;}
    @SubscribeEvent public static void screen(GuiOpenEvent event){if(event.getGui()!=null)release();}
    @SubscribeEvent public static void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event){mining=false;climbing=false;}
}
