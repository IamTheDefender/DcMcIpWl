package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.bot.db.AccountDB;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class BanCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("ban")) return;
        if (!Helpers.isAdmin(event)) {
            event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
            return;
        }

        String player = event.getOption("player").getAsString();
        String reason = Helpers.opt(event, "reason", "Banned by staff");
        event.deferReply().queue();
        try {
            AccountDB db = new AccountDB(Main.database);
            if (db.isBanned(player)) {
                event.getHook().sendMessage(Message.BAN_ALREADY_BANNED.replace("{player}", player)).queue();
                return;
            }
            db.banPlayer(player, null, reason, event.getUser().getAsTag(), "discord");
            Main.bridge.banPlayer(player, reason).thenAccept(r -> event.getHook().sendMessage(Message.BAN_SUCCESS.replace("{player}", player).replace("{reason}", reason)).queue());
        } catch (Exception e) {
            event.getHook().sendMessage(Message.ERROR_PREFIX + e.getMessage()).queue();
        }
    }
}