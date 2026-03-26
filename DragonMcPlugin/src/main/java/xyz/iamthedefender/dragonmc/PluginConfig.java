package xyz.iamthedefender.dragonmc;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

public class PluginConfig {
    private final FileConfiguration cfg;

    public PluginConfig(DragonMcPlugin plugin) {
        this.cfg = plugin.getConfig();
    }

    public String getDbPath() {
        return cfg.getString("db_path", "plugins/DragonMC/dragon.db");
    }

    public String getBotUrl() {
        return cfg.getString("bot_url", "http://localhost:9090");
    }

    public boolean isWhitelistEnabled() {
        return cfg.getBoolean("whitelist.enabled", true);
    }

    public boolean allowPremiumBypass() {
        return cfg.getBoolean("whitelist.premium_bypass", true);
    }

    public boolean isChatEnabled() {
        return cfg.getBoolean("chat.enabled", true);
    }

    public Map<String, String> getAliases() {
        Map<String, String> map = new LinkedHashMap<>();
        if (cfg.isConfigurationSection("aliases"))
            for (String k : cfg.getConfigurationSection("aliases").getKeys(false))
                map.put(k.toLowerCase(), cfg.getString("aliases." + k, ""));
        return map;
    }
}
