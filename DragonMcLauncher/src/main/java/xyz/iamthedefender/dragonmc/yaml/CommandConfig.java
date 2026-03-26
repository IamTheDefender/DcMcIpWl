package xyz.iamthedefender.dragonmc.yaml;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandConfig extends YamlConfig {

    public @NotNull String getDefaultCommand(String commandId) {
        return getString("default-commands." + commandId + ".name", commandId);
    }

    public @NotNull String getDefaultCommandDescription(String commandId) {
        return getString("default-commands." + commandId + ".description", "No description provided for " + getDefaultCommand(commandId) + ".");
    }

    public @NotNull List<CustomCommand> getCustomCommands() {
        ConfigurationSection commandsSection = getSection("commands");
        List<CustomCommand> customCommands = new ArrayList<>();

        if (commandsSection == null) {
            System.err.println("No commands section found.");
            return customCommands;
        }

        for (String id : commandsSection.getKeys()) {
            String path = id + ".";
            String command = commandsSection.getString(path + "command");
            long cooldown = commandsSection.getLong(path + "cooldown");
            String cooldownMessage = commandsSection.getString(path + "cooldown_message");
            boolean adminOnly = commandsSection.getBoolean(path + "admin_only");
            List<CustomCommandVariable> variables = new ArrayList<>();

            ConfigurationSection variablesSection = commandsSection.getSection(path + "variables");

            if (variablesSection != null && !variablesSection.getKeys().isEmpty()) {
                for (String variableId : variablesSection.getKeys()) {
                    String variablePath = path + "variables." + variableId + ".";
                    String variableType = commandsSection.getString(variablePath + "type");
                    String variableDescription = commandsSection.getString(variablePath + "description", "No description provided for " + variableId + ".");
                    variables.add(new CustomCommandVariable(variableId, CustomCommandVariable.Type.valueOf(variableType.toUpperCase()), variableDescription));
                }
            }

            String description = commandsSection.getString(path + "description", "None provided.");

            CustomCommand customCommand = new CustomCommand(
                    id, description, command,
                    cooldown, cooldownMessage,
                    variables, adminOnly
            );

            System.out.println("Loaded custom command " + id);
            customCommands.add(customCommand);
        }

        System.out.println("Loaded " + customCommands.size() + " custom commands.");

        return customCommands;
    }


    public record CustomCommand(@NotNull String name, @NotNull String description, @NotNull String command,
                                long cooldown, @Nullable String cooldownMessage,
                                @NotNull List<CustomCommandVariable> variables,
                                boolean adminOnly) {
    }

    public record CustomCommandVariable(@NotNull String id, @NotNull Type type, @NotNull String description) {

        @Getter
        @RequiredArgsConstructor
        public enum Type {
            PLAYER(OptionType.STRING),
            STRING(OptionType.STRING),
            LONG(OptionType.NUMBER),
            INTEGER(OptionType.NUMBER),
            DOUBLE(OptionType.NUMBER),
            BOOLEAN(OptionType.BOOLEAN),
            ENTITY(OptionType.STRING);

            private final OptionType optionType;

        }

    }


}
