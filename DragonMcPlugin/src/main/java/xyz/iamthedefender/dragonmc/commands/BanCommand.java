package xyz.iamthedefender.dragonmc.commands;

import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class BanCommand implements CommandExecutor {
    private final PluginDB db;

    public BanCommand(PluginDB db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragon.ban")) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(Messages.BAN_USAGE));
            return true;
        }
        
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Banned by staff";
        String bannedBy = sender instanceof Player player ? player.getName() : "Console";
        
        try {
            if (db.isBanned(args[0])) {
                sender.sendMessage(ColorUtil.color(Messages.BAN_ALREADY_BANNED.replace("{player}", args[0])));
                return true;
            }
            
            Player targetPlayer = Bukkit.getPlayerExact(args[0]);
            db.banPlayer(args[0], targetPlayer != null ? targetPlayer.getUniqueId().toString() : null, reason, bannedBy, "minecraft");
            
            if (targetPlayer != null) {
                targetPlayer.kickPlayer(ColorUtil.color(Messages.BAN_KICK_MESSAGE.replace("{reason}", reason)));
            }
            
            Bukkit.broadcastMessage(ColorUtil.color(Messages.BAN_SUCCESS.replace("{player}", args[0]).replace("{reason}", reason)));
        } catch (Exception exception) {
            sender.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
        }
        return true;
    }
}
