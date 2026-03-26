package xyz.iamthedefender.dragonmc.setup;

import xyz.iamthedefender.dragonmc.util.FileUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

public class SetupWizard {

    private static final String CONFIG_FILE = "configuration.properties";

    private static final String[] REQUIRED_KEYS = {
            "token", "guild_id",
            "chat_channel_id", "server_control_channel_id", "join_alerts_channel_id",
            "admin_role_id", "mod_role_id",
            "mc_jar_path"
    };

    public static Properties runIfNeeded() throws IOException {
        File configFile = new File(FileUtil.getLauncherDir(), CONFIG_FILE);
        Properties props = new Properties();

        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
            }
            if (isComplete(props)) {
                System.out.println("Loaded config from " + CONFIG_FILE);
                return props;
            }
            System.out.println("Config file exists but is missing some values, running setup...");
        } else {
            System.out.println("No config found, starting setup wizard.\n");
        }

        return runWizard(props, configFile);
    }

    private static boolean isComplete(Properties p) {
        for (String key : REQUIRED_KEYS) {
            String v = p.getProperty(key, "").trim();
            if (v.isEmpty() || v.startsWith("YOUR_") || v.startsWith("CHANNEL_") || v.startsWith("ROLE_")) return false;
        }
        return true;
    }

    private static Properties runWizard(Properties existing, File configFile) throws IOException {
        Scanner sc = new Scanner(System.in);
        Properties p = new Properties();

        p.putAll(existing);

        section("Step 1 - Discord Bot Token");
        hint("Go to https://discord.com/developers/applications and create a bot.");
        hint("Under the Bot tab, copy the token. Make sure MESSAGE CONTENT INTENT and SERVER MEMBERS INTENT are enabled.");
        p.setProperty("token", prompt(sc, "Bot token", p.getProperty("token", ""), false));

        System.out.println();
        section("Step 2 - Guild (Server) ID");
        hint("Turn on Developer Mode in Discord: Settings > App Settings > Advanced.");
        hint("Then right-click your server name and hit Copy Server ID.");
        String guildId = promptAndValidateGuild(sc, p.getProperty("guild_id", ""), p.getProperty("token", ""));
        p.setProperty("guild_id", guildId);

        System.out.println();
        section("Step 3 - Channel IDs");
        hint("Right-click any channel and Copy Channel ID. Developer Mode needs to be on.");
        p.setProperty("chat_channel_id", promptChannel(sc, "Chat bridge channel (MC <-> Discord)", p.getProperty("chat_channel_id", "")));
        p.setProperty("server_control_channel_id", promptChannel(sc, "Server control channel (start/stop)", p.getProperty("server_control_channel_id", "")));
        p.setProperty("join_alerts_channel_id", promptChannel(sc, "Join alerts channel", p.getProperty("join_alerts_channel_id", "")));

        System.out.println();
        section("Step 4 - Role IDs");
        hint("Right-click a role under Server Settings > Roles to copy its ID.");
        p.setProperty("admin_role_id", promptId(sc, "Admin role ID", p.getProperty("admin_role_id", "")));
        p.setProperty("mod_role_id", promptId(sc, "Mod role ID", p.getProperty("mod_role_id", "")));

        System.out.println();
        section("Step 5 - Minecraft Server");
        p.setProperty("mc_jar_path", prompt(sc, "Path to server.jar", p.getProperty("mc_jar_path", "server.jar"), true));
        p.setProperty("mc_ram_mb", promptInt(sc, "How much RAM to give the server (MB)", p.getProperty("mc_ram_mb", "2048")));

        System.out.println();
        section("Step 6 - Ports (just hit Enter to use defaults)");
        p.setProperty("api_port", promptInt(sc, "Launcher API port (default 8080)", p.getProperty("api_port", "8080")));
        p.setProperty("bot_http_port", promptInt(sc, "Bot HTTP port (default 9090)", p.getProperty("bot_http_port", "9090")));


        p.setProperty("ip_api_url", "http://ip-api.com/json/");

        save(p, configFile);

        System.out.println();
        System.out.println("Config saved to " + CONFIG_FILE + ". Starting the launcher now...\n");

        return p;
    }

    private static void section(String title) {
        System.out.println("--- " + title + " ---");
    }

    private static void hint(String text) {
        System.out.println("  " + text);
    }

    private static String prompt(Scanner sc, String label, String current, boolean allowEmpty) {
        while (true) {
            String display = (current != null && !current.isBlank()) ? " [" + current + "]" : "";
            System.out.print("  " + label + display + ": ");
            String input = sc.nextLine().trim();
            if (input.isEmpty() && current != null && !current.isBlank()) return current;
            if (!input.isEmpty()) return input;
            if (allowEmpty) return "";
            System.out.println("  This field can't be empty.");
        }
    }

    private static String promptChannel(Scanner sc, String label, String current) {
        while (true) {
            String val = prompt(sc, label, current, false);
            if (val.matches("\\d{17,20}")) return val;
            System.out.println("  Channel IDs should be 17-20 digits.");
        }
    }

    private static String promptId(Scanner sc, String label, String current) {
        while (true) {
            String val = prompt(sc, label, current, false);
            if (val.matches("\\d{17,20}")) return val;
            System.out.println("  Discord IDs should be 17-20 digits.");
        }
    }

    private static String promptInt(Scanner sc, String label, String current) {
        while (true) {
            String val = prompt(sc, label, current, false);
            try {
                Integer.parseInt(val);
                return val;
            } catch (NumberFormatException e) {
                System.out.println("  That doesn't look like a number.");
            }
        }
    }

    private static String promptAndValidateGuild(Scanner sc, String current, String token) {
        while (true) {
            String val = promptId(sc, "Guild (server) ID", current);
            System.out.print("  Checking token against Discord API...");
            try {
                HttpURLConnection conn = (HttpURLConnection)
                        new URL("https://discord.com/api/v10/users/@me").openConnection();
                conn.setRequestProperty("Authorization", "Bot " + token);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int code = conn.getResponseCode();
                if (code == 200) {
                    System.out.println(" ok");
                    return val;
                } else {
                    System.out.println(" got HTTP " + code + ", token might be wrong. You can fix it later in the config.");
                    return val;
                }
            } catch (Exception e) {
                System.out.println(" couldn't reach Discord (" + e.getMessage() + "), skipping check.");
                return val;
            }
        }
    }

    private static void save(Properties p, File file) throws IOException {
        file.getParentFile().mkdirs();

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("# DragonMcLauncher config");
            pw.println();
            pw.println("# Discord");
            pw.println("token=" + p.getProperty("token", ""));
            pw.println("guild_id=" + p.getProperty("guild_id", ""));
            pw.println();
            pw.println("# Channel IDs");
            pw.println("chat_channel_id=" + p.getProperty("chat_channel_id", ""));
            pw.println("server_control_channel_id=" + p.getProperty("server_control_channel_id", ""));
            pw.println("join_alerts_channel_id=" + p.getProperty("join_alerts_channel_id", ""));
            pw.println();
            pw.println("# Role IDs");
            pw.println("admin_role_id=" + p.getProperty("admin_role_id", ""));
            pw.println("mod_role_id=" + p.getProperty("mod_role_id", ""));
            pw.println();
            pw.println("# Minecraft server");
            pw.println("mc_jar_path=" + p.getProperty("mc_jar_path", "server.jar"));
            pw.println("mc_ram_mb=" + p.getProperty("mc_ram_mb", "2048"));
            pw.println();
            pw.println("# Ports");
            pw.println("api_port=" + p.getProperty("api_port", "8080"));
            pw.println("bot_http_port=" + p.getProperty("bot_http_port", "9090"));
            pw.println();
            pw.println("# IP geolocation");
            pw.println("ip_api_url=" + p.getProperty("ip_api_url", "http://ip-api.com/json/"));
        }
    }
}