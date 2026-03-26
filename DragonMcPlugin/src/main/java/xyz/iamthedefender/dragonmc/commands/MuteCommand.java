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

import java.util.Arrays;

public class MuteCommand implements CommandExecutor {
    private final PluginDB db;

    public MuteCommand(PluginDB db) {
        this.db = db;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragon.mute")) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(Messages.MUTE_USAGE));
            return true;
        }
        
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Muted by staff";
        String mutedBy = sender instanceof Player player ? player.getName() : "Console";
        
        try {
            if (db.isMuted(args[0])) {
                sender.sendMessage(ColorUtil.color(Messages.MUTE_ALREADY_MUTED.replace("{player}", args[0])));
                return true;
            }
            
            db.mutePlayer(args[0], reason, mutedBy);
            Player targetPlayer = Bukkit.getPlayerExact(args[0]);
            
            if (targetPlayer != null) {
                targetPlayer.sendMessage(Component.text(ColorUtil.color(Messages.MUTE_SUCCESS_TARGET.replace("{reason}", reason))));
            }
            
            sender.sendMessage(ColorUtil.color(Messages.MUTE_SUCCESS_SENDER.replace("{player}", args[0])));
        } catch (Exception exception) {
            sender.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
        }
        return true;
    }
}
