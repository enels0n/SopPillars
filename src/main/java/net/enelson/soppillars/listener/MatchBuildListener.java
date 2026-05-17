package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.RunningMatch;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

/**
 * Enforces {@link ArenaSettings} place/break rules during {@link net.enelson.soppillars.arena.ArenaState#RUNNING}.
 * Tracks player-placed blocks inside the gameplay cuboid to distinguish from map blocks.
 */
public final class MatchBuildListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchBuildListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        RunningMatch match = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (match == null) {
            return;
        }
        if (plugin.getCageManager().isActiveCageBlock(event.getBlock().getLocation())
                || plugin.getCageManager().isActiveCageBlock(event.getBlockAgainst().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (!match.isAlive(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getMatchManager().isOutsideFakeBorder(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
            return;
        }
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay == null || !gameplay.contains(event.getBlock().getLocation())) {
            return;
        }
        if (!match.getArena().getSettings().isAllowPlaceBlocks()) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceMonitor(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        RunningMatch match = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (match == null || !match.isAlive(player.getUniqueId())) {
            return;
        }
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay != null && gameplay.contains(event.getBlock().getLocation())) {
            match.markPlayerPlacedBlock(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        RunningMatch match = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (match == null) {
            return;
        }
        if (plugin.getCageManager().isActiveCageBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (!match.isAlive(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "spectator-no-build");
            return;
        }
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay == null || !gameplay.contains(event.getBlock().getLocation())) {
            return;
        }
        ArenaSettings settings = match.getArena().getSettings();
        boolean playerPlaced = match.isPlayerPlacedBlock(event.getBlock().getLocation());
        if (playerPlaced) {
            if (!settings.isAllowBreakPlayerBlocks()) {
                event.setCancelled(true);
            }
        } else {
            if (!settings.isAllowBreakOriginalBlocks()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreakMonitor(BlockBreakEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        RunningMatch match = plugin.getMatchManager().getRunningMatch(event.getPlayer().getUniqueId());
        if (match == null) {
            return;
        }
        if (match.isPlayerPlacedBlock(event.getBlock().getLocation())) {
            match.unmarkPlayerPlacedBlock(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        RunningMatch match = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (match == null) {
            return;
        }
        if (!match.isAlive(player.getUniqueId())) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "spectator-no-build");
            return;
        }
        Block fluid = event.getBlockClicked().getRelative(event.getBlockFace());
        if (plugin.getCageManager().isActiveCageBlock(fluid.getLocation())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getMatchManager().isOutsideFakeBorder(player, fluid.getLocation())) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
            return;
        }
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay == null || !gameplay.contains(fluid.getLocation())) {
            return;
        }
        if (!match.getArena().getSettings().isAllowPlaceBlocks()) {
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmptyMonitor(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        RunningMatch match = plugin.getMatchManager().getRunningMatch(player.getUniqueId());
        if (match == null || !match.isAlive(player.getUniqueId())) {
            return;
        }
        Block fluid = event.getBlockClicked().getRelative(event.getBlockFace());
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay != null && gameplay.contains(fluid.getLocation())) {
            match.markPlayerPlacedBlock(fluid.getLocation());
        }
    }
}
