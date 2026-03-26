package xyz.iamthedefender.dragonmc.commands.custom;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.iamthedefender.dragonmc.util.ColorUtil;
import xyz.iamthedefender.dragonmc.util.CustomCommandConfig;
import xyz.iamthedefender.dragonmc.util.CustomCommandConfig.CustomCommandVariable.Type;
import xyz.iamthedefender.dragonmc.util.Messages;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class CustomCommand extends Command {

    private final CustomCommandConfig.CustomCommand customCommand;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CustomCommand(CustomCommandConfig.CustomCommand customCommand) {
        super(
                customCommand.name(),
                customCommand.description(),
                buildUsage(customCommand),
                new ArrayList<>()
        );
        this.customCommand = customCommand;
    }

    @Override
    public boolean execute(
            @NotNull CommandSender sender,
            @NotNull String commandLabel,
            @NotNull String @NotNull [] args) {

        if (customCommand.permission() != null && !sender.hasPermission(customCommand.permission())) {
            sender.sendMessage(ColorUtil.color(Messages.NO_PERMISSION));
            return true;
        }

        if (customCommand.adminOnly() && !sender.isOp()) {
            sender.sendMessage(ColorUtil.color(Messages.CUSTOM_COMMAND_ADMIN_ONLY));
            return true;
        }

        List<CustomCommandConfig.CustomCommandVariable> variables = customCommand.variables();
        if (args.length < variables.size()) {
            sender.sendMessage(ColorUtil.color(Messages.CUSTOM_COMMAND_USAGE.replace("{usage}", getUsage())));
            return true;
        }

        for (int i = 0; i < variables.size(); i++) {
            CustomCommandConfig.CustomCommandVariable variable = variables.get(i);
            if (!isValidArg(args[i], variable.type())) {
                sender.sendMessage(ColorUtil.color(
                        Messages.CUSTOM_COMMAND_INVALID_VALUE
                                .replace("{variable}", variable.id())
                                .replace("{description}", variable.description().endsWith(".") ? variable.description().substring(0, variable.description().length() - 1) : variable.description())
                                .replace("{type}", variable.type().name())
                ));
                return true;
            }
        }

        if (sender instanceof Player player && customCommand.cooldown() > 0) {
            long now = System.currentTimeMillis();
            long elapsed = now - cooldowns.getOrDefault(player.getUniqueId(), 0L);
            long cooldownMs = customCommand.cooldown();

            if (elapsed < cooldownMs) {
                long remaining = (cooldownMs - elapsed) / 1000L;
                String msg = customCommand.cooldownMessage() != null
                        ? customCommand.cooldownMessage().replace("{time}", String.valueOf(remaining))
                        : Messages.CUSTOM_COMMAND_COOLDOWN.replace("{time}", String.valueOf(remaining));
                sender.sendMessage(ColorUtil.color(msg));
                return true;
            }

            cooldowns.put(player.getUniqueId(), now);
        }

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), buildCommand(args));
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(
            @NotNull CommandSender sender,
            @NotNull String alias,
            @NotNull String @NotNull [] args) {

        List<CustomCommandConfig.CustomCommandVariable> variables = customCommand.variables();
        int index = args.length - 1;

        if (index < 0 || index >= variables.size()) {
            return Collections.emptyList();
        }

        CustomCommandConfig.CustomCommandVariable variable = variables.get(index);
        String partial = args[index].toLowerCase();

        return switch (variable.type()) {
            case PLAYER -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());

            case BOOLEAN -> Stream.of("true", "false")
                    .filter(s -> s.startsWith(partial))
                    .collect(Collectors.toList());

            case ENTITY -> Arrays.stream(org.bukkit.entity.EntityType.values())
                    .map(e -> e.name().toLowerCase())
                    .filter(e -> e.startsWith(partial))
                    .collect(Collectors.toList());

            case STRING, LONG, INTEGER, DOUBLE -> List.of("<" + variable.id() + ">");
        };
    }

    private String buildCommand(String[] args) {
        String cmd = customCommand.command();
        List<CustomCommandConfig.CustomCommandVariable> variables = customCommand.variables();

        for (int i = 0; i < variables.size(); i++) {
            String value = i < args.length ? args[i] : "";
            cmd = cmd.replace("<" + i + ">", value);
            cmd = cmd.replace("<" + variables.get(i).id() + ">", value);
        }
        return cmd;
    }

    private boolean isValidArg(String raw, Type type) {
        return switch (type) {
            case PLAYER  -> Bukkit.getPlayerExact(raw) != null;
            case STRING  -> true;
            case LONG    -> parseSafely(raw, Long::parseLong);
            case INTEGER -> parseSafely(raw, Integer::parseInt);
            case DOUBLE  -> parseSafely(raw, Double::parseDouble);
            case BOOLEAN -> raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false");
            case ENTITY  -> {
                try {
                    org.bukkit.entity.EntityType.valueOf(raw.toUpperCase());
                    yield true;
                } catch (IllegalArgumentException e) {
                    yield false;
                }
            }
        };
    }

    private <T> boolean parseSafely(String raw, ThrowingParser<T> parser) {
        try { parser.parse(raw); return true; }
        catch (NumberFormatException e) { return false; }
    }

    @FunctionalInterface
    private interface ThrowingParser<T> {
        T parse(String s) throws NumberFormatException;
    }

    private static String buildUsage(CustomCommandConfig.CustomCommand cmd) {
        if (cmd.variables().isEmpty()) return "/" + cmd.name();
        String vars = cmd.variables().stream()
                .map(v -> "<" + v.id() + ">")
                .collect(Collectors.joining(" "));
        return "/" + cmd.name() + " " + vars;
    }
}