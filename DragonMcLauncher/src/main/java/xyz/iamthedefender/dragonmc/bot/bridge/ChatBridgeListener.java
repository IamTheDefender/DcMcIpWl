package xyz.iamthedefender.dragonmc.bot.bridge;

import xyz.iamthedefender.dragonmc.Main;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ChatBridgeListener extends ListenerAdapter {

    public static void relayToDiscord(String mcUsername, String message) {
        if (Main.jda == null) return;
        var ch = Main.jda.getTextChannelById(Main.config.getChatChannelId());
        if (ch == null) return;
        ch.sendMessage("**[MC]** " + mcUsername + ": " + message).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(Main.config.getChatChannelId())) return;

        String name = event.getMember() != null
                ? event.getMember().getEffectiveName()
                : event.getAuthor().getName();
        String message = event.getMessage().getContentDisplay();

        if (!message.isBlank()) {
            Main.bridge.broadcastToMc(name, message);
        }
    }
}
