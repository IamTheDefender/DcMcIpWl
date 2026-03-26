package xyz.iamthedefender.dragonmc.util;

import xyz.iamthedefender.dragonmc.DragonMcPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {
    private final FileConfiguration cfg;

    public PluginConfig(DragonMcPlugin plugin) {
        this.cfg = plugin.getConfig();
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
}
