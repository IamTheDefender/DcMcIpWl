package xyz.iamthedefender.dragonmc.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class KickCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("dragon.kick")) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(ColorUtil.color(Messages.KICK_USAGE));
            return true;
        }
        
        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "Kicked by staff";
        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        
        if (targetPlayer == null) {
            sender.sendMessage(ColorUtil.color(Messages.KICK_PLAYER_NOT_FOUND.replace("{player}", args[0])));
            return true;
        }
        
        targetPlayer.kick(Component.text(ColorUtil.color(Messages.KICK_MESSAGE.replace("{reason}", reason))));
        Bukkit.broadcast(Component.text(ColorUtil.color(Messages.KICK_SUCCESS.replace("{player}", args[0]).replace("{reason}", reason))));
        return true;
    }
}
