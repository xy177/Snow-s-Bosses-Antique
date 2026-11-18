package xy177.snowsbosses.world;

import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public final class LocateCommand extends CommandBase {
    private static final String[] CUSTOM = {"the_ocean_shrine", "tide_raiser_tower", "hellstone_ruin"};
    private static final String[] VANILLA = {"Stronghold", "Monument", "Village", "Mansion", "EndCity", "Fortress", "Temple", "Mineshaft"};

    @Override
    public String getName() {
        return "locate";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "commands.locate.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 1) throw new WrongUsageException(getUsage(sender));
        if (StructureLocator.normalize(args[0]) != null) {
            if (StructureLocator.request(sender, args[0], false, position -> {
                if (position == null) sender.sendMessage(new TextComponentTranslation("commands.locate.failure", args[0]));
                else sender.sendMessage(new TextComponentTranslation("commands.locate.success", args[0], position.getX(), position.getZ()));
            }));
            return;
        }
        BlockPos position = sender.getEntityWorld().findNearestStructure(args[0], sender.getPosition(), false);
        if (position == null) throw new CommandException("commands.locate.failure", args[0]);
        sender.sendMessage(new TextComponentTranslation("commands.locate.success", args[0], position.getX(), position.getZ()));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
        if (args.length != 1) return Collections.emptyList();
        String[] names = new String[VANILLA.length + CUSTOM.length];
        System.arraycopy(VANILLA, 0, names, 0, VANILLA.length);
        System.arraycopy(CUSTOM, 0, names, VANILLA.length, CUSTOM.length);
        return getListOfStringsMatchingLastWord(args, names);
    }
}
