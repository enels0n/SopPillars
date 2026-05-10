package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class MatchStateListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchStateListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (plugin.getMatchManager().isLobbyProtected(player)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getMatchManager().isEndingWinnerProtected(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        plugin.getMatchManager().enforceArenaBounds(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!plugin.getMatchManager().isManagedChatPlayer(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMatchManager().routeChat(event.getPlayer(), event.getMessage());
    }

    /**
     * Players in a SopPillars queue/match do not receive normal global chat (SPEC isolation).
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChatStripArenaRecipients(AsyncPlayerChatEvent event) {
        if (plugin.getMatchManager().isManagedChatPlayer(event.getPlayer())) {
            return;
        }
        event.getRecipients().removeIf(plugin.getMatchManager()::isManagedChatPlayer);
    }
}
