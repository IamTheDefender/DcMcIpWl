package xyz.iamthedefender.dragonmc.util;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.yaml.YamlConfig;

public class Message {

    private static final YamlConfig messageConf = Main.messageConfig;

    public static final String DISCORD_STATUS = getAndSave(
            "discord_status",
            "Minecraft"
    );

    public static final String NO_PERMISSION = getAndSave(
            "no_permission",
            "You don't have permission to do that."
    );

    public static final String BAN_ALREADY_BANNED = getAndSave(
            "ban_already_banned",
            "{player} is already banned."
    );

    public static final String BAN_SUCCESS = getAndSave(
            "ban_success",
            "Banned {player}. Reason: {reason}"
    );

    public static final String KICK_SUCCESS = getAndSave(
            "kick_success",
            "Kicked {player}. Reason: {reason}"
    );

    public static final String MUTE_ALREADY_MUTED = getAndSave(
            "mute_already_muted",
            "{player} is already muted."
    );

    public static final String MUTE_SUCCESS = getAndSave(
            "mute_success",
            "Muted {player}. Reason: {reason}"
    );

    public static final String UNBAN_NOT_BANNED = getAndSave(
            "unban_not_banned",
            "{player} isn't banned."
    );

    public static final String UNBAN_SUCCESS = getAndSave(
            "unban_success",
            "{player} has been unbanned."
    );

    public static final String UNMUTE_NOT_MUTED = getAndSave(
            "unmute_not_muted",
            "{player} isn't muted."
    );

    public static final String UNMUTE_SUCCESS = getAndSave(
            "unmute_success",
            "{player} has been unmuted."
    );

    public static final String LINK_ALREADY_LINKED = getAndSave(
            "link_already_linked",
            "You're already linked to **{username}**. Contact an admin to change it."
    );

    public static final String LINK_INVALID_USERNAME = getAndSave(
            "link_invalid_username",
            "`{username}` doesn't look like a valid Minecraft username."
    );

    public static final String LINK_USERNAME_TAKEN = getAndSave(
            "link_username_taken",
            "**{username}** is already linked to another account."
    );

    public static final String LINK_CODE_SENT = getAndSave(
            "link_code_sent",
            "Join the Minecraft server and run this to finish linking: ```/link {code}```"
    );

    public static final String IP_CONFIRM_ALREADY_HANDLED = getAndSave(
            "ip_confirm_already_handled",
            "This confirmation was already handled."
    );

    public static final String IP_CONFIRM_NOT_YOURS = getAndSave(
            "ip_confirm_not_yours",
            "This confirmation isn't yours."
    );

    public static final String IP_LIMIT_REACHED = getAndSave(
            "ip_limit_reached",
            "You've reached your IP limit ({max}). Contact an admin."
    );

    public static final String IP_CONFIRMED = getAndSave(
            "ip_confirmed",
            "**{ip}** has been whitelisted for your account."
    );

    public static final String IP_REJECTED = getAndSave(
            "ip_rejected",
            "**{ip}** wasn't whitelisted. You won't be able to join from this IP."
    );

    public static final String SERVER_WRONG_CHANNEL = getAndSave(
            "server_wrong_channel",
            "Please use the server control channel for this."
    );

    public static final String SERVER_UNKNOWN_ACTION = getAndSave(
            "server_unknown_action",
            "Unknown action. Use: start, stop, restart, status"
    );

    public static final String ERROR_PREFIX = getAndSave(
            "error_prefix",
            "❌ "
    );


    public static String getAndSave(String path, String toSave) {
        if (messageConf.getString(path) == null) {
            messageConf.set(path, toSave);
        }

        return messageConf.getString(path);
    }

}