package xy177.snowsbosses.network;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.SnowsBosses;

public final class Visuals {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("snows_bosses");

    private Visuals() {
    }

    public static void register() {
        CHANNEL.registerMessage(Handler.class, Message.class, 0, Side.CLIENT);
        CHANNEL.registerMessage(BossBarHandler.class, BossBarMessage.class, 1, Side.CLIENT);
    }

    public static void bossBar(EntityPlayerMP player, UUID id, boolean visible) {
        bossBar(player, id, visible, 0xFF8C00);
    }

    public static void bossBar(EntityPlayerMP player, UUID id, boolean visible, int color) {
        bossBar(player, id, visible, color, -1);
    }

    public static void bossBar(EntityPlayerMP player, UUID id, boolean visible, int color, int musicEntityId) {
        BossBarMessage message = new BossBarMessage();
        message.id = id;
        message.visible = visible;
        message.color = color;
        message.musicEntityId = musicEntityId;
        CHANNEL.sendTo(message, player);
    }

    public static void particles(World world, String kind, double x, double y, double z,
            int count, double spread, double speed) {
        if (world.isRemote) return;
        Message message = new Message();
        message.kind = kind;
        message.x = x;
        message.y = y;
        message.z = z;
        message.count = count;
        message.spread = spread;
        message.speed = speed;
        send(world, message);
    }

    public static void totem(EntityLivingBase entity) {
        Message message = new Message();
        message.kind = "hellstone_totem";
        message.entityId = entity.getEntityId();
        message.x = entity.posX;
        message.y = entity.posY;
        message.z = entity.posZ;
        send(entity.world, message);
    }

    public static void innerVoice(EntityLivingBase entity, int event) {
        if (entity.world.isRemote) return;
        Message message = new Message();
        message.kind = "inner_voice";
        message.entityId = entity.getEntityId();
        message.count = event;
        message.x = entity.posX;
        message.y = entity.posY + entity.height;
        message.z = entity.posZ;
        send(entity.world, message);
    }

    public static void freeze(EntityLivingBase entity, int ticks) {
        Message message = new Message();
        message.kind = "freeze";
        message.entityId = entity.getEntityId();
        message.x = entity.posX;
        message.y = entity.posY;
        message.z = entity.posZ;
        message.count = ticks;
        send(entity.world, message);
    }

    public static void drownerCold(EntityLivingBase entity, int ticks) {
        Message message = new Message();
        message.kind = "drowner_cold";
        message.entityId = entity.getEntityId();
        message.x = entity.posX;
        message.y = entity.posY;
        message.z = entity.posZ;
        message.count = ticks;
        send(entity.world, message);
    }

    public static void drownerFreeze(EntityLivingBase entity, int ticks) {
        Message message = new Message();
        message.kind = "drowner_freeze";
        message.entityId = entity.getEntityId();
        message.x = entity.posX;
        message.y = entity.posY;
        message.z = entity.posZ;
        message.count = ticks;
        send(entity.world, message);
    }

    public static void drownerThaw(EntityLivingBase entity) {
        Message message = new Message();
        message.kind = "drowner_thaw";
        message.entityId = entity.getEntityId();
        message.x = entity.posX;
        message.y = entity.posY;
        message.z = entity.posZ;
        send(entity.world, message);
    }

    public static void shake(World world, double x, double y, double z, int ticks) {
        Message message = new Message();
        message.kind = "altar_shake";
        message.x = x;
        message.y = y;
        message.z = z;
        message.count = ticks;
        send(world, message);
    }

    public static void stopShake(World world, double x, double y, double z) {
        Message message = new Message();
        message.kind = "altar_shake_stop";
        message.x = x;
        message.y = y;
        message.z = z;
        send(world, message);
    }

    private static void send(World world, Message message) {
        CHANNEL.sendToAllAround(message, new NetworkRegistry.TargetPoint(
                world.provider.getDimension(), message.x, message.y, message.z, 64));
    }

    public static final class Message implements IMessage {
        public String kind;
        public double x;
        public double y;
        public double z;
        public double spread;
        public double speed;
        public int count;
        public int entityId;

        @Override
        public void fromBytes(ByteBuf buffer) {
            kind = ByteBufUtils.readUTF8String(buffer);
            x = buffer.readDouble();
            y = buffer.readDouble();
            z = buffer.readDouble();
            spread = buffer.readDouble();
            speed = buffer.readDouble();
            count = buffer.readInt();
            entityId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            ByteBufUtils.writeUTF8String(buffer, kind);
            buffer.writeDouble(x);
            buffer.writeDouble(y);
            buffer.writeDouble(z);
            buffer.writeDouble(spread);
            buffer.writeDouble(speed);
            buffer.writeInt(count);
            buffer.writeInt(entityId);
        }
    }

    public static final class BossBarMessage implements IMessage {
        public UUID id;
        public boolean visible;
        public int color;
        public int musicEntityId = -1;

        @Override
        public void fromBytes(ByteBuf buffer) {
            id = new UUID(buffer.readLong(), buffer.readLong());
            visible = buffer.readBoolean();
            color = buffer.readInt();
            musicEntityId = buffer.readInt();
        }

        @Override
        public void toBytes(ByteBuf buffer) {
            buffer.writeLong(id.getMostSignificantBits());
            buffer.writeLong(id.getLeastSignificantBits());
            buffer.writeBoolean(visible);
            buffer.writeInt(color);
            buffer.writeInt(musicEntityId);
        }
    }

    public static final class BossBarHandler implements IMessageHandler<BossBarMessage, IMessage> {
        @Override
        public IMessage onMessage(BossBarMessage message, MessageContext context) {
            SnowsBosses.PROXY.bossBar(message);
            return null;
        }
    }

    public static final class Handler implements IMessageHandler<Message, IMessage> {
        @Override
        public IMessage onMessage(Message message, MessageContext context) {
            SnowsBosses.PROXY.visual(message);
            return null;
        }
    }
}
