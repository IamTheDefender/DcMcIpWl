package xyz.iamthedefender.dragonmc.bot.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ServerControlCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("server")) return;
        if (!Helpers.isAdmin(event)) {
            event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
            return;
        }

        if (!event.getChannel().getId().equals(Main.config.getServerControlChannelId())) {
            event.reply(Message.SERVER_WRONG_CHANNEL).setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("action").getAsString().trim().toLowerCase();
        event.deferReply().queue();
        switch (action) {
            case "start" ->
                    Main.bridge.start().thenAccept(r -> event.getHook().sendMessageEmbeds(embed("▶️ Starting", "Start signal sent.", Color.GREEN).build()).queue());
            case "stop" ->
                    Main.bridge.stop().thenAccept(r -> event.getHook().sendMessageEmbeds(embed("⏹️ Stopping", "Stop signal sent.", Color.RED).build()).queue());
            case "restart" ->
                    Main.bridge.restart().thenAccept(r -> event.getHook().sendMessageEmbeds(embed("🔄 Restarting", "Restart signal sent.", Color.YELLOW).build()).queue());
            case "status" -> Main.bridge.status().thenAccept(resp -> {
                boolean up = resp.contains("\"running\":true");
                long uptime = longField(resp, "uptimeSec"), p = longField(resp, "pid");
                event.getHook().sendMessageEmbeds(new EmbedBuilder()
                        .setTitle("Server Status")
                        .setColor(up ? Color.GREEN : Color.RED)
                        .addField("State", up ? "🟢 Online" : "🔴 Offline", true)
                        .addField("PID", p > 0 ? String.valueOf(p) : "--", true)
                        .addField("Uptime", up ? fmt(uptime) : "--", true)
                        .build()).queue();
            });
            default -> event.getHook().sendMessage(Message.SERVER_UNKNOWN_ACTION).queue();
        }
    }

    private static final List<String> ACTIONS = List.of("start", "stop", "restart", "status");

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("server") || !event.getFocusedOption().getName().equals("action")) return;

        String search = event.getFocusedOption().getValue().toLowerCase();
        List<Command.Choice> options = ACTIONS.stream()
                .filter(action -> action.startsWith(search))
                .map(action -> new Command.Choice(action, action))
                .collect(Collectors.toList());

        event.replyChoices(options).queue();
    }

    private EmbedBuilder embed(String t, String d, Color c) {
        return new EmbedBuilder().setTitle(t).setDescription(d).setColor(c);
    }

    private long longField(String json, String key) {
        String s = "\"" + key + "\":";
        int i = json.indexOf(s);
        if (i < 0) return -1;
        i += s.length();
        int e = json.indexOf(',', i);
        if (e < 0) e = json.indexOf('}', i);
        try {
            return Long.parseLong(json.substring(i, e).trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private String fmt(long secs) {
        return String.format("%02d:%02d:%02d", secs / 3600, (secs % 3600) / 60, secs % 60);
    }
}