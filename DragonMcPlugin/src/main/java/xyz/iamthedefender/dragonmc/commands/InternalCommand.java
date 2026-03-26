package xyz.iamthedefender.dragonmc.commands;

import xyz.iamthedefender.dragonmc.util.PluginConfig;
import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class InternalCommand implements CommandExecutor {
    private final PluginDB db;
    private final PluginConfig cfg;

    public InternalCommand(PluginDB db, PluginConfig cfg) {
        this.db = db;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) return false;
        try {
            switch (args[0].toLowerCase()) {
                case "mute" -> {
                    if (args.length < 2) return false;
                    
                    db.mutePlayer(args[1], "Muted via Discord", "Discord");
                    Player targetPlayer = Bukkit.getPlayerExact(args[1]);
                    
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(Component.text(ColorUtil.color(Messages.INTERNAL_MUTE_TARGET)));
                    }
                }
                case "unmute" -> {
                    if (args.length < 2) return false;
                    
                    db.unmutePlayer(args[1]);
                    Player targetPlayer = Bukkit.getPlayerExact(args[1]);
                    
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage(Component.text(ColorUtil.color(Messages.INTERNAL_UNMUTE_TARGET)));
                    }
                }
                case "ban" -> {
                    if (args.length < 2) return false;
                    
                    String banReason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : Messages.INTERNAL_BAN_REASON;
                    db.banPlayer(args[1], null, banReason, "Discord", "discord");
                    Player targetPlayer = Bukkit.getPlayerExact(args[1]);
                    
                    if (targetPlayer != null) {
                        targetPlayer.kick(Component.text(ColorUtil.color("§c" + banReason)));
                    }
                }
                case "unban" -> {
                    if (args.length < 2) return false;
                    
                    db.unbanPlayer(args[1]);
                }
                case "kick" -> {
                    if (args.length < 2) return false;
                    
                    String kickReason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : Messages.INTERNAL_KICK_REASON;
                    Player targetPlayer = Bukkit.getPlayerExact(args[1]);
                    
                    if (targetPlayer != null) {
                        targetPlayer.kick(Component.text(ColorUtil.color("§c" + kickReason)));
                    }
                }
                case "chat" -> {
                    if (args.length < 2) return false;
                    
                    Bukkit.broadcast(Component.text(String.join(" ", Arrays.copyOfRange(args, 1, args.length)), NamedTextColor.AQUA));
                }
                case "ip-allow" -> {
                    if (args.length < 3) return false;
                    
                    String discordId = db.getDiscordIdForMc(args[1]);
                    if (discordId != null) {
                        db.addWhitelistedIp(discordId, args[2]);
                    }
                }
                default -> sender.sendMessage(ColorUtil.color(Messages.INTERNAL_UNKNOWN_SUBCOMMAND.replace("{subcommand}", args[0])));
            }
        } catch (Exception exception) {
            sender.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
            exception.printStackTrace();
        }
        return true;
    }
}
