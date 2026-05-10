package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Blocks ambient mob spawning inside configured arena regions (SPEC: natural spawns off).
 */
public final class MatchArenaEnvironmentListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchArenaEnvironmentListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent event) {
        if (!plugin.getPillarsConfig().isBlockNaturalSpawnsInArenas()) {
            return;
        }
        if (!isAmbientSpawn(event.getSpawnReason())) {
            return;
        }
        Location loc = event.getLocation();
        if (plugin.getArenaManager().findArenaContaining(loc) == null) {
            return;
        }
        event.setCancelled(true);
    }

    private static boolean isAmbientSpawn(CreatureSpawnEvent.SpawnReason reason) {
        switch (reason) {
            case NATURAL:
            case JOCKEY:
            case PATROL:
                return true;
            default:
                return false;
        }
    }
}
