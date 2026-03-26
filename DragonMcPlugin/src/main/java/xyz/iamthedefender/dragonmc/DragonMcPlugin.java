package xyz.iamthedefender.dragonmc;

import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import xyz.iamthedefender.dragonmc.commands.*;
import xyz.iamthedefender.dragonmc.commands.custom.CustomCommand;
import xyz.iamthedefender.dragonmc.database.PluginDB;
import xyz.iamthedefender.dragonmc.listener.ChatListener;
import xyz.iamthedefender.dragonmc.listener.JoinListener;
import xyz.iamthedefender.dragonmc.listener.MuteListener;
import xyz.iamthedefender.dragonmc.util.CustomCommandConfig;
import xyz.iamthedefender.dragonmc.util.PluginConfig;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class DragonMcPlugin extends JavaPlugin {

    @Getter
    private static DragonMcPlugin instance;
    private PluginDB db;
    private PluginConfig cfg;

    @Getter
    private static CustomCommandConfig customCommandConfig;
    @Getter
    private static List<CustomCommandConfig.CustomCommand> customCommands;

    @Getter
    private static YamlConfiguration messagesConfig;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        cfg = new PluginConfig(this);
        loadMessages();

        try {
            db = new PluginDB();
            db.init();
        } catch (Exception e) {
            getLogger().severe("Failed to init DB: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        customCommandConfig = new CustomCommandConfig();
        customCommands = customCommandConfig.loadCustomCommands();
        
        CommandMap commandMap = getServer().getCommandMap();

        Map<String, CommandExecutor> commands = new LinkedHashMap<>();
        commands.put("link", new DragonLinkCommand(db, cfg));
        commands.put("ban", new BanCommand(db));
        commands.put("unban", new UnbanCommand(db));
        commands.put("kick", new KickCommand());
        commands.put("mute", new MuteCommand(db));
        commands.put("unmute", new UnmuteCommand(db));
        commands.put("dragonmc", new InternalCommand(db, cfg));

        commands.forEach((key, executor) -> {
            String name = getCustomCommandConfig().getDefaultCommand(key);
            commandMap.register(getName(), new BukkitCommand(name) {
                @Override
                public boolean execute(@NonNull CommandSender sender, @NonNull String label, String @NonNull []
                        args) {
                    return executor.onCommand(sender, this, label, args);
                }

                @Override
                public @NonNull List<String> tabComplete(@NonNull CommandSender sender, @NonNull String alias, String @NonNull [] args) {
                    if (executor instanceof TabCompleter tc) {
                        List<String> results = tc.onTabComplete(sender, this, alias, args);
                        if (results != null) return results;
                    }
                    return super.tabComplete(sender, alias, args);
                }
            });
        });
        getServer().getPluginManager().registerEvents(new JoinListener(db, cfg), this);
        getServer().getPluginManager().registerEvents(new MuteListener(db), this);
        getServer().getPluginManager().registerEvents(new ChatListener(db, cfg), this);

        // -- custom command logic
        customCommands.forEach(customCommand -> commandMap.register("dragonmc", new CustomCommand(customCommand)));

        getLogger().info("DragonMcPlugin enabled");
    }

    @Override
    public void onDisable() {
        if (db != null) {
            try {
                db.close();
            } catch (Exception ignored) {
            }
        }
        getLogger().info("DragonMcPlugin disabled.");
    }

    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messagesFile.getParentFile().mkdirs();

        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }


}
