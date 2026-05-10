package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class KitMenuListener implements Listener {

    private final SopPillarsPlugin plugin;

    public KitMenuListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!plugin.getKitManager().isKitInventory(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null) {
            return;
        }
        plugin.getKitManager().handleKitMenuClick((Player) event.getWhoClicked(), event.getSlot());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (plugin.getKitManager().isKitInventory(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
