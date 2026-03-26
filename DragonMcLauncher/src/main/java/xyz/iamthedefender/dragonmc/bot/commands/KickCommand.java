package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class KickCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("kick")) return;
        if (!Helpers.isModOrAdmin(event)) {
            event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
            return;
        }
        String player = event.getOption("player").getAsString();
        String reason = Helpers.opt(event, "reason", "Kicked by staff");
        event.deferReply().queue();
        Main.bridge.kickPlayer(player, reason).thenAccept(r ->
                event.getHook().sendMessage(Message.KICK_SUCCESS.replace("{player}", player).replace("{reason}", reason)).queue());
    }
}