package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.bot.db.AccountDB;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import xyz.iamthedefender.dragonmc.util.Message;

import java.awt.*;
import java.sql.ResultSet;
import java.util.UUID;

public class LinkCommand extends ListenerAdapter {

    public static void sendIpConfirmPrompt(String discordId, String mcUsername, String ip, String location) {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("\uD83D\uDD10 New Login — IP Confirmation Required")
                .setColor(new Color(0xFF6600))
                .setDescription("<@" + discordId + ">, someone joined as **" + mcUsername + "** from an unrecognised IP.")
                .addField("IP Address", "`" + ip + "`", true)
                .addField("Location", location, true)
                .setFooter("DragonMC — Confirm or reject this IP to continue");

        var row = ActionRow.of(
                Button.success("ipconfirm:" + ip, "✅ That's me — whitelist it"),
                Button.danger("ipdeny:" + ip, "\uD83D\uDEAB Not me — reject")
        );

        String channelId = Main.config.getJoinAlertsChannelId();
        var channel = Main.jda.getTextChannelById(channelId);

        if (channel != null) {
            channel.sendMessageEmbeds(eb.build()).setComponents(row).queue(msg -> {
                try {
                    new AccountDB(Main.database).savePendingIpConfirm(discordId, mcUsername, ip, location, msg.getId());
                } catch (Exception e) {
                    System.err.println("[Dragon] Failed to save IP confirm: " + e.getMessage());
                }
            });
        } else {
            Main.jda.retrieveUserById(discordId).queue(user -> {
                if (user == null) return;
                user.openPrivateChannel().queue(dm ->
                        dm.sendMessageEmbeds(eb.build()).setComponents(row).queue(msg -> {
                            try {
                                new AccountDB(Main.database).savePendingIpConfirm(discordId, mcUsername, ip, location, msg.getId());
                            } catch (Exception e) {
                                System.err.println("[Dragon] DM confirm save failed: " + e.getMessage());
                            }
                        })
                );
            });
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("link")) return;

        String discordId = event.getUser().getId();
        String mcUsername = event.getOption("username").getAsString().trim();

        event.deferReply(true).queue();

        try {
            AccountDB db = new AccountDB(Main.database);

            if (db.isLinked(discordId)) {
                event.getHook().sendMessage(
                        Message.LINK_ALREADY_LINKED.replace("{username}", db.getUsernameForDiscord(discordId))
                ).queue();
                return;
            }

            if (!mcUsername.matches("[a-zA-Z0-9_]{3,16}")) {
                event.getHook().sendMessage(Message.LINK_INVALID_USERNAME.replace("{username}", mcUsername)).queue();
                return;
            }

            if (db.isUsernameTaken(mcUsername)) {
                event.getHook().sendMessage(Message.LINK_USERNAME_TAKEN.replace("{username}", mcUsername)).queue();
                return;
            }

            String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            db.savePendingLink(discordId, mcUsername, code);

            event.getHook().sendMessageEmbeds(new EmbedBuilder()
                    .setTitle("\uD83D\uDD17 Link Your Minecraft Account")
                    .setColor(new Color(0xFF6600))
                    .setDescription(Message.LINK_CODE_SENT.replace("{code}", code))
                    .addField("Username", "`" + mcUsername + "`", true)
                    .addField("Expires", "10 minutes", true)
                    .setFooter("DragonMC — Only you can see this message")
                    .build()
            ).queue();

        } catch (Exception e) {
            event.getHook().sendMessage(Message.ERROR_PREFIX + "Error: " + e.getMessage()).queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (!id.startsWith("ipconfirm:") && !id.startsWith("ipdeny:")) return;

        try {
            AccountDB db = new AccountDB(Main.database);
            ResultSet rs = db.getPendingIpConfirmByMessage(event.getMessageId());

            if (!rs.next()) {
                event.reply(Message.IP_CONFIRM_ALREADY_HANDLED).setEphemeral(true).queue();
                return;
            }

            String discordId = rs.getString("discord_id");
            String mcUsername = rs.getString("mc_username");
            String ip = rs.getString("ip");
            String location = rs.getString("location");

            if (!event.getUser().getId().equals(discordId)) {
                event.reply(Message.IP_CONFIRM_NOT_YOURS).setEphemeral(true).queue();
                return;
            }

            db.removePendingIpConfirm(discordId, ip);

            if (id.startsWith("ipconfirm:")) {
                boolean isAdmin = db.isAdmin(discordId);
                int max = isAdmin ? 3 : 1;
                int current = db.getIpCount(discordId);

                if (current >= max) {
                    event.editMessageEmbeds(new EmbedBuilder()
                            .setTitle("❌ IP Limit Reached")
                            .setColor(Color.RED)
                            .setDescription(Message.IP_LIMIT_REACHED.replace("{max}", String.valueOf(max)))
                            .build()
                    ).setComponents().queue();
                    return;
                }

                db.addWhitelistedIp(discordId, ip);
                Main.bridge.sendCommand("dragonmc ip-allow " + mcUsername + " " + ip);

                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("✅ IP Confirmed")
                        .setColor(Color.GREEN)
                        .setDescription(Message.IP_CONFIRMED.replace("{ip}", ip))
                        .addField("Location", location != null ? location : "Unknown", false)
                        .setFooter("You can now join the server — DragonMC")
                        .build()
                ).setComponents().queue();

            } else {
                event.editMessageEmbeds(new EmbedBuilder()
                        .setTitle("\uD83D\uDEAB IP Rejected")
                        .setColor(Color.DARK_GRAY)
                        .setDescription(Message.IP_REJECTED.replace("{ip}", ip))
                        .setFooter("If this was a mistake, try joining again and confirm when prompted.")
                        .build()
                ).setComponents().queue();
            }

        } catch (Exception e) {
            event.reply(Message.ERROR_PREFIX + "Error: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}
