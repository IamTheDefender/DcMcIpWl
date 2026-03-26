package xyz.iamthedefender.dragonmc.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import xyz.iamthedefender.dragonmc.DragonMcPlugin;

public class Messages {

    public static final String NO_PERMISSION = getAndSet("no_permission", "&cYou do not have permission to use this command.");
    
    // Ban Command Messages
    public static final String BAN_USAGE = getAndSet("ban_usage", "&cUsage: /dragonban <player> [reason]");
    public static final String BAN_ALREADY_BANNED = getAndSet("ban_already_banned", "&e{player} is already banned.");
    public static final String BAN_SUCCESS = getAndSet("ban_success", "&c{player} was banned. Reason: {reason}");
    public static final String BAN_KICK_MESSAGE = getAndSet("ban_kick_message", "&cBanned: {reason}");
    
    // Kick Command Messages
    public static final String KICK_USAGE = getAndSet("kick_usage", "&cUsage: /dragonkick <player> [reason]");
    public static final String KICK_PLAYER_NOT_FOUND = getAndSet("kick_player_not_found", "&e{player} is not online.");
    public static final String KICK_SUCCESS = getAndSet("kick_success", "&e{player} was kicked. Reason: {reason}");
    public static final String KICK_MESSAGE = getAndSet("kick_message", "&cKicked: {reason}");
    
    // Mute Command Messages
    public static final String MUTE_USAGE = getAndSet("mute_usage", "&cUsage: /dragonmute <player> [reason]");
    public static final String MUTE_ALREADY_MUTED = getAndSet("mute_already_muted", "&e{player} is already muted.");
    public static final String MUTE_SUCCESS_SENDER = getAndSet("mute_success_sender", "&a{player} muted.");
    public static final String MUTE_SUCCESS_TARGET = getAndSet("mute_success_target", "&cYou are muted: {reason}");
    
    // Unban Command Messages
    public static final String UNBAN_USAGE = getAndSet("unban_usage", "&cUsage: /dragonunban <player>");
    public static final String UNBAN_NOT_BANNED = getAndSet("unban_not_banned", "&e{player} is not banned.");
    public static final String UNBAN_SUCCESS = getAndSet("unban_success", "&a{player} unbanned.");
    
    // Unmute Command Messages
    public static final String UNMUTE_USAGE = getAndSet("unmute_usage", "&cUsage: /dragonunmute <player>");
    public static final String UNMUTE_NOT_MUTED = getAndSet("unmute_not_muted", "&e{player} is not muted.");
    public static final String UNMUTE_SUCCESS_SENDER = getAndSet("unmute_success_sender", "&a{player} unmuted.");
    public static final String UNMUTE_SUCCESS_TARGET = getAndSet("unmute_success_target", "&aYou have been unmuted.");
    
    // DragonLink Command Messages
    public static final String DRAGONLINK_PLAYERS_ONLY = getAndSet("dragonlink_players_only", "&cPlayers only.");
    public static final String DRAGONLINK_ALREADY_LINKED = getAndSet("dragonlink_already_linked", "&aYour account is already linked!");
    public static final String DRAGONLINK_USAGE = getAndSet("dragonlink_usage", "&eUsage: &b/dragonlink <code>\n&7Get your code from Discord with /link <username>");
    public static final String DRAGONLINK_INVALID_CODE = getAndSet("dragonlink_invalid_code", "&cInvalid or expired code. Run /link again in Discord.");
    public static final String DRAGONLINK_CODE_EXPIRED = getAndSet("dragonlink_code_expired", "&cCode expired (10 min limit). Generate a new one in Discord.");
    public static final String DRAGONLINK_CODE_MISMATCH = getAndSet("dragonlink_code_mismatch", "&cThis code was generated for &e{pending}&c, not &e{actual}");
    public static final String DRAGONLINK_SUCCESS = getAndSet("dragonlink_success", "&a✔ Account linked successfully!");
    public static final String DRAGONLINK_IP_CONFIRMATION = getAndSet("dragonlink_ip_confirmation", "&7Join again — you'll get an IP confirmation in your Discord.");
    
    // Internal Command Messages
    public static final String INTERNAL_UNKNOWN_SUBCOMMAND = getAndSet("internal_unknown_subcommand", "&cUnknown sub: {subcommand}");
    public static final String INTERNAL_MUTE_TARGET = getAndSet("internal_mute_target", "&cMuted via Discord.");
    public static final String INTERNAL_UNMUTE_TARGET = getAndSet("internal_unmute_target", "&aUnmuted.");
    public static final String INTERNAL_BAN_REASON = getAndSet("internal_ban_reason", "Banned via Discord");
    public static final String INTERNAL_KICK_REASON = getAndSet("internal_kick_reason", "Kicked via Discord");
    
    // Custom Command Messages
    public static final String CUSTOM_COMMAND_ADMIN_ONLY = getAndSet("custom_command_admin_only", "&cThis command is for admins only.");
    public static final String CUSTOM_COMMAND_USAGE = getAndSet("custom_command_usage", "&cUsage: {usage}");
    public static final String CUSTOM_COMMAND_INVALID_VALUE = getAndSet("custom_command_invalid_value", "&cInvalid value for &e{variable}&c ({description}). Expected: &e{type}");
    public static final String CUSTOM_COMMAND_COOLDOWN = getAndSet("custom_command_cooldown", "&cPlease wait &e{time}s &cbefore using this command again.");
    
    // General Messages
    public static final String ERROR_PREFIX = getAndSet("error_prefix", "&cError: ");



    public static @NotNull String getAndSet(String path, String def) {
        YamlConfiguration config = DragonMcPlugin.getMessagesConfig();

        if (config.getString(path) == null) {
            config.set(path, def);
        }

        return config.getString(path);
    }

}
