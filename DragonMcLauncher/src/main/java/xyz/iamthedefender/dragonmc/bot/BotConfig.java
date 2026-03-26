package xyz.iamthedefender.dragonmc.bot;

import xyz.iamthedefender.dragonmc.util.FileUtil;

import java.util.Properties;

public class BotConfig {

    private final Properties p;

    public BotConfig(Properties p) {
        this.p = p;
    }

    public String getToken() {
        return p.getProperty("token");
    }

    public String getGuildId() {
        return p.getProperty("guild_id");
    }

    public String getChatChannelId() {
        return p.getProperty("chat_channel_id");
    }

    public String getServerControlChannelId() {
        return p.getProperty("server_control_channel_id");
    }

    public String getJoinAlertsChannelId() {
        return p.getProperty("join_alerts_channel_id");
    }

    public String getAdminRoleId() {
        return p.getProperty("admin_role_id");
    }

    public String getModRoleId() {
        return p.getProperty("mod_role_id");
    }

    public String getMcJarPath() {
        return p.getProperty("mc_jar_path", "server.jar");
    }

    public int getMcRamMb() {
        return Integer.parseInt(p.getProperty("mc_ram_mb", "2048"));
    }

    public int getApiPort() {
        return Integer.parseInt(p.getProperty("api_port", "8080"));
    }

    public int getBotHttpPort() {
        return Integer.parseInt(p.getProperty("bot_http_port", "9090"));
    }

    public String getDbPath() {
        return FileUtil.getLauncherDir().getAbsolutePath() + "/dragon.db";
    }

    public String getIpApiUrl() {
        return p.getProperty("ip_api_url", "http://ip-api.com/json/");
    }
}
