package xyz.iamthedefender.dragonmc.listener;

import xyz.iamthedefender.dragonmc.util.PluginConfig;
import xyz.iamthedefender.dragonmc.database.PluginDB;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChatListener implements Listener {
    private final PluginDB db;
    private final PluginConfig cfg;

    public ChatListener(PluginDB db, PluginConfig cfg) {
        this.db = db;
        this.cfg = cfg;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!cfg.isChatEnabled()) return;
        String player = event.getPlayer().getName();
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
        new Thread(() -> {
            try {
                String botUrl = cfg.getBotUrl();
                if (botUrl == null || botUrl.isBlank()) return;
                String body = "{\"player\":\"" + esc(player) + "\",\"message\":\"" + esc(msg) + "\"}";
                HttpURLConnection conn = (HttpURLConnection) new URL(botUrl + "/mc-chat").openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
                conn.getResponseCode();
            } catch (Exception e) {
                System.err.println("[DragonMC] Chat relay error: " + e.getMessage());
            }
        }, "dragon-chat-relay").start();
    }

    private String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
