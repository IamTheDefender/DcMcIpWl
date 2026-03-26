package xyz.iamthedefender.dragonmc.listener;

import xyz.iamthedefender.dragonmc.util.PluginConfig;
import xyz.iamthedefender.dragonmc.database.PluginDB;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class JoinListener implements Listener {
    private final PluginDB db;
    private final PluginConfig cfg;

    public JoinListener(PluginDB db, PluginConfig cfg) {
        this.db = db;
        this.cfg = cfg;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName(), ip = event.getAddress().getHostAddress(), uuid = event.getUniqueId().toString();
        try {
            if (db.isBanned(name)) {
                deny(event, "§cYou are banned: " + db.getBanReason(name));
                return;
            }
            if (db.isIpBanned(ip)) {
                deny(event, "§cYour IP is banned.");
                return;
            }
            if (!cfg.isWhitelistEnabled()) return;

            if (!db.isLinked(name) && !db.hasPendingLink(name)) {
                deny(event, "§eThis server requires account linking.\n§7Run §b/link <username>§7 in the Discord server, then\n§7join Minecraft and run §b/dragonlink <code>§7 to complete the link.");
                return;
            }
            String discordId = db.getDiscordIdForMc(name);
            if (discordId == null) return;
            if (!db.isIpWhitelisted(discordId, ip)) {
                if (cfg.allowPremiumBypass()) {
                    try {
                        URL mojang = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
                        HttpURLConnection mojangConn = (HttpURLConnection) mojang.openConnection();
                        mojangConn.setRequestMethod("GET");
                        mojangConn.setConnectTimeout(3000);
                        mojangConn.setReadTimeout(3000);

                        if (mojangConn.getResponseCode() == 200) {
                            String response = new String(mojangConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                            String mojangUuid = response.replaceAll(".*\"id\":\"([a-f0-9]{32})\".*", "$1");
                            String normalizedUuid = uuid.replace("-", "");

                            if (mojangUuid.equalsIgnoreCase(normalizedUuid)) {
                                db.addWhitelistedIp(discordId, ip);
                                return;
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("[DragonMC] Premium check failed: " + ex.getMessage());
                    }
                }


                triggerConfirm(discordId, name, ip);
                deny(event, "§eNew IP detected!\n§7A confirmation has been sent to your Discord.\n§aConfirm it, then rejoin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            deny(event, "§cServer error. Try again.");
        }
    }

    private void deny(AsyncPlayerPreLoginEvent e, String msg) {
        e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(msg));
    }

    private void triggerConfirm(String discordId, String mc, String ip) {
        String url = cfg.getBotUrl();
        if (url == null || url.isBlank()) return;
        try {
            String body = "{\"discordId\":\"" + discordId + "\",\"mcUsername\":\"" + mc + "\",\"ip\":\"" + ip + "\"}";
            HttpURLConnection conn = (HttpURLConnection) new URL(url + "/ip-confirm").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            conn.getResponseCode();
        } catch (Exception e) {
            System.err.println("[DragonMC] IP confirm failed: " + e.getMessage());
        }
    }
}
