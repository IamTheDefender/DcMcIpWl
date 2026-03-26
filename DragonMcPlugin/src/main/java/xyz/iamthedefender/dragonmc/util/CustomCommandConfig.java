package xyz.iamthedefender.dragonmc.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.iamthedefender.dragonmc.DragonMcPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomCommandConfig extends YamlConfiguration {

    private final DragonMcPlugin plugin = DragonMcPlugin.getInstance();

    public CustomCommandConfig() {
        try {
            load(
                    new File(plugin.getDataFolder().getParentFile().getParentFile(), "launcher/custom_commands.yml")
            );
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public @NotNull String getDefaultCommand(String commandId) {
        return getString("default-commands." + commandId + ".name", commandId);
    }

    public @NotNull String getDefaultCommandDescription(String commandId) {
        return getString("default-commands." + commandId + ".description", "No description provided for " + getDefaultCommand(commandId) + ".");
    }

    public List<CustomCommand> loadCustomCommands() {
        ConfigurationSection commandsSection = getConfigurationSection("commands");
        List<CustomCommand> customCommands = new ArrayList<>();

        if (commandsSection == null) {
            plugin.getLogger().warning("No commands section found.");
            return customCommands;
        }

        for (String id : commandsSection.getKeys(false)) {
            String path = id + ".";
            String command = commandsSection.getString(path + "command");

            if (command == null) {
                plugin.getLogger().warning("Command " + id + " has no command defined.");
                continue;
            }

            long cooldown = commandsSection.getLong(path + "cooldown");
            String cooldownMessage = commandsSection.getString(path + "cooldown_message");
            boolean adminOnly = commandsSection.getBoolean(path + "admin_only");
            List<CustomCommandVariable> variables = new ArrayList<>();
            String permission = commandsSection.getString(path + "permission");

            ConfigurationSection variablesSection = commandsSection.getConfigurationSection(path + "variables");

            if (variablesSection != null && !variablesSection.getKeys(false).isEmpty()) {
                for (String variableId : variablesSection.getKeys(false)) {
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
                    variables, adminOnly,
                    permission
            );

            plugin.getLogger().info("Loaded custom command " + id);
            customCommands.add(customCommand);
        }

        plugin.getLogger().info("Loaded " + customCommands.size() + " custom commands.");

        return customCommands;
    }


    public record CustomCommand(@NotNull String name, @NotNull String description, @NotNull String command,
                                long cooldown, @Nullable String cooldownMessage,
                                @NotNull List<CustomCommandVariable> variables,
                                boolean adminOnly, String permission) {
    }

    public record CustomCommandVariable(@NotNull String id, @NotNull Type type, @NotNull String description) {

        @Getter
        @RequiredArgsConstructor
        public enum Type {
            PLAYER,
            STRING,
            LONG,
            INTEGER,
            DOUBLE,
            BOOLEAN,
            ENTITY;

        }

    }

}
