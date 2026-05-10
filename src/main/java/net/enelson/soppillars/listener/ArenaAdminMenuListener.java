package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.menu.ArenaAdminMenus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
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
        if (ArenaAdminMenus.isCosmeticsTitle(title)) {
            event.setCancelled(true);
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
        if (ArenaAdminMenus.isSettingsTitle(title) || ArenaAdminMenus.isCosmeticsTitle(title)) {
            event.setCancelled(true);
        }
    }
}
