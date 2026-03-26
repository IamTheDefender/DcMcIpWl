package xyz.iamthedefender.dragonmc.listener;

import xyz.iamthedefender.dragonmc.database.PluginDB;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MuteListener implements Listener {
    private final PluginDB db;

    public MuteListener(PluginDB db) {
        this.db = db;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        try {
            if (db.isMuted(event.getPlayer().getName())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("§cYou are muted."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
