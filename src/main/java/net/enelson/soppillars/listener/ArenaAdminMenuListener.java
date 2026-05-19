package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.menu.ArenaAdminMenus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ArenaAdminMenuListener implements Listener {

    private final SopPillarsPlugin plugin;

    public ArenaAdminMenuListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (plugin.getCosmeticManager().isManagedInventory(title)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
                plugin.getCosmeticManager().handleMenuClick((Player) event.getWhoClicked(), title, event.getSlot(), event.getInventory().getSize());
            }
            return;
        }
        if (ArenaAdminMenus.isGlobalSettingsTitle(title)) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null) {
                ArenaAdminMenus.handleGlobalSettingsClick(plugin, (Player) event.getWhoClicked(), event.getSlot());
            }
            return;
        }
        if (!ArenaAdminMenus.isSettingsTitle(title)) {
            return;
        }
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        String editedName = plugin.getEditorManager().getEditedArena(player.getUniqueId());
        PillarsArena arena = editedName == null ? null : plugin.getArenaManager().getArena(editedName);
        if (arena == null) {
            player.closeInventory();
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }
        ArenaAdminMenus.handleSettingsClick(plugin, player, arena, event.getSlot(), event.isRightClick(), event.isShiftClick());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (ArenaAdminMenus.isSettingsTitle(title) || plugin.getCosmeticManager().isManagedInventory(title)
                || ArenaAdminMenus.isGlobalSettingsTitle(title)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        String title = event.getView().getTitle();
        if (!plugin.getLootListEditorManager().isLootListEditorTitle(title)) {
            return;
        }
        plugin.getLootListEditorManager().handleClose((Player) event.getPlayer(), event.getInventory());
    }
}
