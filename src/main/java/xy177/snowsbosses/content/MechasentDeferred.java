package xy177.snowsbosses.content;

import java.util.ArrayDeque;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import xy177.snowsbosses.SnowsBosses;

@Mod.EventBusSubscriber(modid=SnowsBosses.MOD_ID)
public final class MechasentDeferred {
    private static final ArrayDeque<Runnable> QUEUE=new ArrayDeque<>();
    private MechasentDeferred(){ }
    public static void next(Runnable task){ QUEUE.addLast(task); }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.START || QUEUE.isEmpty())return;
        int count=QUEUE.size();
        for(int i=0;i<count;i++)QUEUE.removeFirst().run();
    }
    public static void clear(){QUEUE.clear();}
}
