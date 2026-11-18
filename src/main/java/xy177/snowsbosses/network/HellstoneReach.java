package xy177.snowsbosses.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import xy177.snowsbosses.content.HellstoneContent;
import xy177.snowsbosses.content.HellstoneEffects;

public final class HellstoneReach {
    private static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("sb_hellstone_reach");
    private HellstoneReach() { }
    public static void register() { CHANNEL.registerMessage(Handler.class, Message.class, 0, Side.SERVER); }
    public static void attack(Entity target) { CHANNEL.sendToServer(new Message(target.getEntityId())); }
    public static Entity pick(EntityPlayer player) {
        double reach = (player.capabilities.isCreativeMode ? 6 : 3) + HellstoneEffects.n("machete.entityReachBonus", 0.5);
        Vec3d eye = player.getPositionEyes(1), end = eye.add(player.getLookVec().scale(reach));
        RayTraceResult block = player.world.rayTraceBlocks(eye, end, false, false, false);
        double nearest = block == null ? reach : eye.distanceTo(block.hitVec);
        Entity result = null;
        for (Entity target : player.world.getEntitiesWithinAABBExcludingEntity(player, player.getEntityBoundingBox().expand(
                end.x - eye.x, end.y - eye.y, end.z - eye.z).grow(1))) {
            if (!target.canBeCollidedWith() || target instanceof EntityPlayer && ((EntityPlayer) target).isSpectator()
                    || target.getLowestRidingEntity() == player.getLowestRidingEntity()) continue;
            RayTraceResult hit = target.getEntityBoundingBox().grow(target.getCollisionBorderSize()).calculateIntercept(eye, end);
            double distance = target.getEntityBoundingBox().contains(eye) ? 0 : hit == null ? Double.MAX_VALUE : eye.distanceTo(hit.hitVec);
            if (distance < nearest) { nearest = distance; result = target; }
        }
        return result;
    }
    public static final class Message implements IMessage {
        private int id;
        public Message() { }
        Message(int id) { this.id = id; }
        @Override public void fromBytes(ByteBuf buffer) { id = buffer.readInt(); }
        @Override public void toBytes(ByteBuf buffer) { buffer.writeInt(id); }
    }
    public static final class Handler implements IMessageHandler<Message, IMessage> {
        @Override
        public IMessage onMessage(Message message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                if (!player.isEntityAlive() || player.isSpectator() || player.getHeldItemMainhand().getItem() != HellstoneContent.MACHETE) return;
                Entity target = pick(player);
                if (target == null || target.getEntityId() != message.id) return;
                player.attackTargetEntityWithCurrentItem(target); player.swingArm(EnumHand.MAIN_HAND);
            });
            return null;
        }
    }
}
