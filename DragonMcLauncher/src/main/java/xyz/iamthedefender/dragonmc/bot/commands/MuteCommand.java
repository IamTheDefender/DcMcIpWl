package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.bot.db.AccountDB;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MuteCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mute")) return;
        if (!Helpers.isModOrAdmin(event)) {
            event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
            return;
        }
        String player = event.getOption("player").getAsString();
        String reason = Helpers.opt(event, "reason", "Muted by staff");
        event.deferReply().queue();
        try {
            AccountDB db = new AccountDB(Main.database);
            if (db.isMuted(player)) {
                event.getHook().sendMessage(Message.MUTE_ALREADY_MUTED.replace("{player}", player)).queue();
                return;
            }
            db.mutePlayer(player, reason, event.getUser().getAsTag());
            Main.bridge.mutePlayer(player).thenAccept(r -> event.getHook().sendMessage(Message.MUTE_SUCCESS.replace("{player}", player).replace("{reason}", reason)).queue());
        } catch (Exception e) {
            event.getHook().sendMessage(Message.ERROR_PREFIX + e.getMessage()).queue();
        }
    }
}