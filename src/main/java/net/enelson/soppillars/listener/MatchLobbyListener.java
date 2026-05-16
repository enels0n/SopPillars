package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class MatchLobbyListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchLobbyListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (plugin.getMatchManager().isLeaveArenaItem(item)) {
            event.setCancelled(true);
            plugin.getMatchManager().leaveArena(player, false);
            return;
        }
        if (!plugin.getMatchManager().isTeamSelectorItem(item)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMatchManager().openTeamSelector(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getMatchManager().isTeamSelectorInventory(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        plugin.getMatchManager().handleTeamSelectorClick(player, event.getSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getMatchManager().isTeamSelectorInventory(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getMatchManager().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getMatchManager().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getMatchManager().isLobbyProtected(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }
}
