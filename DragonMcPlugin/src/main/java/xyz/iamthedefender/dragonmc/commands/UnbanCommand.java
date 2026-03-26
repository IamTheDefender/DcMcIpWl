package xyz.iamthedefender.dragonmc.commands;

import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class UnbanCommand implements CommandExecutor {
    private final PluginDB db;

    public UnbanCommand(PluginDB db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragon.ban")) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(Messages.UNBAN_USAGE));
            return true;
        }
        
        try {
            if (!db.isBanned(args[0])) {
                sender.sendMessage(ColorUtil.color(Messages.UNBAN_NOT_BANNED.replace("{player}", args[0])));
                return true;
            }
            
            db.unbanPlayer(args[0]);
            sender.sendMessage(ColorUtil.color(Messages.UNBAN_SUCCESS.replace("{player}", args[0])));
        } catch (Exception exception) {
            sender.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
        }
        return true;
    }
}
