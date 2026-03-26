package xyz.iamthedefender.dragonmc.commands;

import xyz.iamthedefender.dragonmc.util.PluginConfig;
import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.util.Messages;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.ResultSet;

public class DragonLinkCommand implements CommandExecutor {
    private final PluginDB db;
    private final PluginConfig cfg;

    public DragonLinkCommand(PluginDB db, PluginConfig cfg) {
        this.db = db;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.color(Messages.DRAGONLINK_PLAYERS_ONLY));
            return true;
        }
        
        String minecraftUsername = player.getName();
        
        try {
            if (db.isLinked(minecraftUsername)) {
                player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_ALREADY_LINKED));
                return true;
            }
            
            if (args.length < 1) {
                player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_USAGE));
                return true;
            }
            
            String linkCode = args[0].toUpperCase();
            ResultSet resultSet = db.getPendingLinkByCode(linkCode);
            
            if (!resultSet.next()) {
                player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_INVALID_CODE));
                return true;
            }
            
            String pendingMinecraftUsername = resultSet.getString("mc_username"), discordId = resultSet.getString("discord_id");
            long ageMillis = System.currentTimeMillis() - resultSet.getLong("created_at");
            
            if (ageMillis > 10 * 60 * 1000L) {
                player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_CODE_EXPIRED));
                return true;
            }
            
            if (!pendingMinecraftUsername.equalsIgnoreCase(minecraftUsername)) {
                player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_CODE_MISMATCH
                        .replace("{pending}", pendingMinecraftUsername)
                        .replace("{actual}", minecraftUsername)));
                return true;
            }
            
            db.completeLinkFromCode(linkCode, player.getUniqueId().toString());
            player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_SUCCESS));
            player.sendMessage(ColorUtil.color(Messages.DRAGONLINK_IP_CONFIRMATION));
        } catch (Exception exception) {
            player.sendMessage(ColorUtil.color(Messages.ERROR_PREFIX + exception.getMessage()));
            exception.printStackTrace();
        }
        return true;
    }
}
