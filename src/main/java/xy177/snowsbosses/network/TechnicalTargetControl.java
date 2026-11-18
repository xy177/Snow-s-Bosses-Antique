package xy177.snowsbosses.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.content.ModItems;
import xy177.snowsbosses.content.TechnicalTargetTool;

public final class TechnicalTargetControl {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("sb_technical_target");

    private TechnicalTargetControl() { }

    public static void register() { CHANNEL.registerMessage(Handler.class, Message.class, 0, Side.SERVER); }

    public static void use(boolean clear, boolean sneaking) { CHANNEL.sendToServer(new Message(clear ? 2 : sneaking ? 1 : 0)); }

    public static final class Message implements IMessage {
        private int action;
        public Message() { }
        Message(int action) { this.action = action; }
        @Override public void fromBytes(ByteBuf buffer) { action = buffer.readUnsignedByte(); }
        @Override public void toBytes(ByteBuf buffer) { buffer.writeByte(action); }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage> {
        @Override
        public IMessage onMessage(Message message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!player.isEntityAlive() || player.isSpectator()
                        || player.getHeldItemMainhand().getItem() != ModItems.TECHNICAL_TARGET_TOOL) return;
                if (message.action == 2) TechnicalTargetTool.clear(player.world);
                else if (message.action == 0 || message.action == 1)
                    ((TechnicalTargetTool) ModItems.TECHNICAL_TARGET_TOOL).summon(player.world, player, message.action == 1);
            });
            return null;
        }
    }
}
