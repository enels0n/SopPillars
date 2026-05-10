package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.ArenaState;
import net.enelson.soppillars.arena.PillarsArena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class ArenaProtectionListener implements Listener {

    private final SopPillarsPlugin plugin;

    public ArenaProtectionListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!shouldDeny(event.getPlayer(), event.getBlock().getLocation())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!shouldDeny(event.getPlayer(), event.getBlock().getLocation())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "arena-build-denied");
    }

    private boolean shouldDeny(Player player, org.bukkit.Location location) {
        PillarsArena arena = plugin.getArenaManager().findArenaContaining(location);
        if (arena == null) {
            return false;
        }
        if (arena.getState() == ArenaState.RUNNING) {
            return false;
        }
        if (arena.getState() == ArenaState.EDITING) {
            String editing = plugin.getEditorManager().getEditedArena(player.getUniqueId());
            if (arena.getName().equalsIgnoreCase(editing)) {
                return false;
            }
        }
        return true;
    }
}
