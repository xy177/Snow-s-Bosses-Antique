package xy177.snowsbosses.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.content.MechasentModuleEffects;
import xy177.snowsbosses.content.MechasentModules;

public final class MechasentControls {
    private static final SimpleNetworkWrapper CHANNEL=NetworkRegistry.INSTANCE.newSimpleChannel("sb_mechasent_keys");
    private MechasentControls(){ }
    public static void register(){
        CHANNEL.registerMessage(Handler.class,Message.class,0,Side.SERVER);
        CHANNEL.registerMessage(StepHandler.class,StepMessage.class,1,Side.CLIENT);
    }
    public static void syncStep(EntityPlayerMP player,boolean active){CHANNEL.sendTo(new StepMessage(active),player);}
    public static final class StepMessage implements IMessage {
        private boolean active;
        public StepMessage(){ }
        StepMessage(boolean active){this.active=active;}
        @Override public void fromBytes(ByteBuf buffer){active=buffer.readBoolean();}
        @Override public void toBytes(ByteBuf buffer){buffer.writeBoolean(active);}
    }
    public static final class StepHandler implements IMessageHandler<StepMessage,IMessage> {
        @Override public IMessage onMessage(StepMessage message,MessageContext context){
            xy177.snowsbosses.SnowsBosses.PROXY.moduleStep(message.active);
            return null;
        }
    }
    public static void send(int action){CHANNEL.sendToServer(new Message(action));}
    public static final class Message implements IMessage {
        private int action;
        public Message(){ }
        Message(int action){this.action=action;}
        @Override public void fromBytes(ByteBuf buffer){action=buffer.readUnsignedByte();}
        @Override public void toBytes(ByteBuf buffer){buffer.writeByte(action);}
    }
    public static final class Handler implements IMessageHandler<Message,IMessage> {
        @Override public IMessage onMessage(Message message,MessageContext context){
            EntityPlayerMP player=context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(()->{
                if(message.action==3){player.getEntityData().removeTag("MechasentMining");return;}
                if(message.action==5){player.getEntityData().removeTag("MechasentClimbing");return;}
                if(!player.isEntityAlive() || player.isSpectator() || xy177.snowsbosses.entity.DrownerCold.isFrozen(player))return;
                if(message.action==0 || message.action==1)MechasentModuleEffects.toggle(player,message.action==0);
                else if(message.action==2 && MechasentModules.EARTH_HAMMER.worn(player))player.getEntityData().setBoolean("MechasentMining",true);
                else if(message.action==4 && MechasentModuleEffects.enabled(player,false))player.getEntityData().setBoolean("MechasentClimbing",true);
            });
            return null;
        }
    }
}
