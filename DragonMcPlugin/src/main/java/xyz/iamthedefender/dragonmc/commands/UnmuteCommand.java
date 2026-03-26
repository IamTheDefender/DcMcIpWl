package xyz.iamthedefender.dragonmc.commands;

import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnmuteCommand implements CommandExecutor {
    private final PluginDB db;

    public UnmuteCommand(PluginDB db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragon.mute")) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(Messages.UNMUTE_USAGE));
            return true;
        }
        
        try {
            if (!db.isMuted(args[0])) {
                sender.sendMessage(ColorUtil.color(Messages.UNMUTE_NOT_MUTED.replace("{player}", args[0])));
                return true;
            }
            
            db.unmutePlayer(args[0]);
            Player targetPlayer = Bukkit.getPlayerExact(args[0]);
            
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Component.text(ColorUtil.color(Messages.UNMUTE_SUCCESS_TARGET)));
            }
            
            sender.sendMessage(ColorUtil.color(Messages.UNMUTE_SUCCESS_SENDER.replace("{player}", args[0])));
        } catch (Exception exception) {
            sender.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
        }
        return true;
    }
}
