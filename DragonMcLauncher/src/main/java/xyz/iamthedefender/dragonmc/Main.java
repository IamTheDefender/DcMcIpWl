package xyz.iamthedefender.dragonmc;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import xyz.iamthedefender.dragonmc.bot.BotConfig;
import xyz.iamthedefender.dragonmc.bot.bridge.BotHttpServer;
import xyz.iamthedefender.dragonmc.bot.bridge.ChatBridgeListener;
import xyz.iamthedefender.dragonmc.bot.bridge.MinecraftBridge;
import xyz.iamthedefender.dragonmc.bot.commands.*;
import xyz.iamthedefender.dragonmc.bot.db.Database;
import xyz.iamthedefender.dragonmc.launcher.ServerLauncher;
import xyz.iamthedefender.dragonmc.setup.SetupWizard;
import xyz.iamthedefender.dragonmc.util.FileUtil;
import xyz.iamthedefender.dragonmc.util.Message;
import xyz.iamthedefender.dragonmc.yaml.CommandConfig;
import xyz.iamthedefender.dragonmc.yaml.YamlConfig;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    public static JDA jda;
    public static Database database;
    public static MinecraftBridge bridge;
    public static BotConfig config;
    public static ServerLauncher launcher;
    public static CommandConfig commandsConfig;
    public static List<CommandConfig.CustomCommand> customCommands;
    public static YamlConfig messageConfig;

    public static void main(String[] args) throws Exception {
        if (System.console() == null && (args.length == 0 || !args[0].equals("--relaunched"))) {
            relaunchInTerminal();
            return;
        }

        Properties props = SetupWizard.runIfNeeded();
        config = new BotConfig(props);
        database = new Database(config.getDbPath());
        database.init();

        launcher = new ServerLauncher(config);
        bridge = new MinecraftBridge(config.getApiPort());
        commandsConfig = new CommandConfig();
        messageConfig = new YamlConfig();

        try {
            commandsConfig.loadFrom(FileUtil.setupYamlFile("custom_commands", "commands.yml"));
            messageConfig.loadFrom(FileUtil.setupYamlFile("messages", null));
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        String discordStatus = Message.DISCORD_STATUS;

        messageConfig.save(FileUtil.setupYamlFile("messages", null));

        customCommands = commandsConfig.getCustomCommands();

        jda = JDABuilder.createDefault(config.getToken())
                .enableIntents(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS
                )
                .setActivity(Activity.watching(discordStatus))
                .addEventListeners(
                        new MuteCommand(),
                        new UnmuteCommand(),
                        new BanCommand(),
                        new UnbanCommand(),
                        new KickCommand(),
                        new LinkCommand(),
                        new ServerControlCommand(),
                        new ChatBridgeListener(),
                        new CustomCommandsListener()
                )
                .build()
                .awaitReady();


        // default commands
        List<CommandData> commandData = new ArrayList<>();

        commandData.addAll(
                List.of(
                        Commands.slash(commandsConfig.getDefaultCommand("mute"), commandsConfig.getDefaultCommandDescription("mute"))
                                .addOption(OptionType.STRING, "player", "Player name", true)
                                .addOption(OptionType.STRING, "reason", "Reason", false),

                        Commands.slash(commandsConfig.getDefaultCommand("unmute"), commandsConfig.getDefaultCommandDescription("unmute"))
                                .addOption(OptionType.STRING, "player", "Player name", true),

                        Commands.slash(commandsConfig.getDefaultCommand("ban"), commandsConfig.getDefaultCommandDescription("ban"))
                                .addOption(OptionType.STRING, "player", "Player name", true)
                                .addOption(OptionType.STRING, "reason", "Reason", false),

                        Commands.slash(commandsConfig.getDefaultCommand("unban"), commandsConfig.getDefaultCommandDescription("unban"))
                                .addOption(OptionType.STRING, "player", "Player name", true),

                        Commands.slash(commandsConfig.getDefaultCommand("kick"), commandsConfig.getDefaultCommandDescription("kick"))
                                .addOption(OptionType.STRING, "player", "Player name", true)
                                .addOption(OptionType.STRING, "reason", "Reason", false),

                        Commands.slash(commandsConfig.getDefaultCommand("link"), commandsConfig.getDefaultCommandDescription("link"))
                                .addOption(OptionType.STRING, "username", "Your exact Minecraft username", true),

                        Commands.slash(commandsConfig.getDefaultCommand("server"), commandsConfig.getDefaultCommandDescription("server"))
                                .addOption(OptionType.STRING, "action", "start / stop / restart / status", true, true)
                )
        );


        // custom commands
        if (!customCommands.isEmpty()) {
            for (CommandConfig.CustomCommand customCommand : customCommands) {
                SlashCommandData command = Commands.slash(customCommand.name(), customCommand.description());

                for (CommandConfig.CustomCommandVariable variable : customCommand.variables()) {
                    command.addOption(variable.type().getOptionType(), variable.id(), variable.description(), true);
                }

                commandData.add(command);
            }
        }

        jda.updateCommands().addCommands(commandData.toArray(new CommandData[0])).queue();

        BotHttpServer.start(config.getBotHttpPort());

        System.out.println("Bot online as " + jda.getSelfUser().getAsTag());
    }

    private static void relaunchInTerminal() throws Exception {
        String jarPath = Paths.get(
                Main.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).toString();

        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb;

        if (os.contains("win")) {
            pb = new ProcessBuilder(
                    "cmd.exe", "/c", "start", "cmd.exe", "/k",
                    "java -jar \"" + jarPath + "\" --relaunched"
            );
        } else if (os.contains("mac")) {
            pb = new ProcessBuilder(
                    "osascript", "-e",
                    "tell app \"Terminal\" to do script \"java -jar '" + jarPath + "' --relaunched\""
            );
        } else {
            String cmd = "java -jar \"" + jarPath + "\" --relaunched";
            pb = findLinuxTerminal(cmd);
        }

        pb.start();
    }

    private static ProcessBuilder findLinuxTerminal(String cmd) {
        String[][] terminals = {
                {"x-terminal-emulator", "-e", cmd},
                {"gnome-terminal", "--", "bash", "-c", cmd + "; exec bash"},
                {"konsole", "-e", cmd},
                {"xterm", "-e", cmd}
        };

        for (String[] terminal : terminals) {
            try {
                new ProcessBuilder("which", terminal[0]).start().waitFor();
                return new ProcessBuilder(terminal);
            } catch (Exception ignored) {}
        }

        return new ProcessBuilder("xterm", "-e", cmd);
    }

}
