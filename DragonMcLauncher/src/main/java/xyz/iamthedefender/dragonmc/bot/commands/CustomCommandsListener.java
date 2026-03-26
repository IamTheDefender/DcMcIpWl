package xyz.iamthedefender.dragonmc.bot.commands;

import xyz.iamthedefender.dragonmc.Main;
import xyz.iamthedefender.dragonmc.util.SpamUtil;
import xyz.iamthedefender.dragonmc.yaml.CommandConfig;
import xyz.iamthedefender.dragonmc.util.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class CustomCommandsListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        for (CommandConfig.CustomCommand customCommand : Main.customCommands) {
            if (!event.getName().equals(customCommand.name())) continue;

            if (!Helpers.isModOrAdmin(event) && customCommand.adminOnly()) {
                event.reply(Message.NO_PERMISSION).setEphemeral(true).queue();
                return;
            }

            if (customCommand.cooldown() > 0 && SpamUtil.getAndSet(event.getUser().getId() + "_" + customCommand.name(), customCommand.cooldown())) {
                event.reply(Optional.ofNullable(customCommand.cooldownMessage()).orElse("Please wait before executing the command again."))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            String command = customCommand.command();

            for (CommandConfig.CustomCommandVariable variable : customCommand.variables()) {
                OptionMapping optionMapping = event.getOption(variable.id());
                Objects.requireNonNull(optionMapping);

                Object value = switch (variable.type()) {
                    case LONG -> optionMapping.getAsLong();
                    case STRING, PLAYER, ENTITY -> optionMapping.getAsString();
                    case DOUBLE ->  optionMapping.getAsDouble();
                    case  BOOLEAN -> optionMapping.getAsBoolean();
                    case INTEGER ->  optionMapping.getAsInt();
                };

                command = command.replace("<" + variable.id() + ">", String.valueOf(value));
            }

            event.deferReply().queue();
            String finalCommand = command;
            Main.bridge.sendCommand(command).thenAccept(r -> event.getHook().sendMessage("**Sent command:** `" + finalCommand + "`").queue());
        }

    }

}
