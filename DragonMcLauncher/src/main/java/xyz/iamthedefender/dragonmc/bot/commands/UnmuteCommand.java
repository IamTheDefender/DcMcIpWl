package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.bot.db.AccountDB;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class UnmuteCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("unmute")) return;
        if (!Helpers.isModOrAdmin(event)) {
            event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
            return;
        }
        String player = event.getOption("player").getAsString();
        event.deferReply().queue();
        try {
            AccountDB db = new AccountDB(Main.database);
            if (!db.isMuted(player)) {
                event.getHook().sendMessage(Message.UNMUTE_NOT_MUTED.replace("{player}", player)).queue();
                return;
            }
            db.unmutePlayer(player);
            Main.bridge.unmutePlayer(player).thenAccept(r -> event.getHook().sendMessage(Message.UNMUTE_SUCCESS.replace("{player}", player)).queue());
        } catch (Exception e) {
            event.getHook().sendMessage(Message.ERROR_PREFIX + e.getMessage()).queue();
        }
    }
}