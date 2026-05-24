package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.RunningMatch;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
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
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
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
            for (Location location : relatedPlacedBlockLocations(event.getBlock())) {
                if (gameplay.contains(location)) {
                    match.markPlayerPlacedBlock(location);
                }
            }
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
        boolean playerPlaced = false;
        for (Location location : relatedPlacedBlockLocations(event.getBlock())) {
            if (match.isPlayerPlacedBlock(location)) {
                playerPlaced = true;
                break;
            }
        }
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
        for (Location location : relatedPlacedBlockLocations(event.getBlock())) {
            if (match.isPlayerPlacedBlock(location)) {
                match.unmarkPlayerPlacedBlock(location);
            }
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
            event.setCancelled(true);
            plugin.getMessageService().send(player, "match-build-place-denied");
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
            match.markTrackedFluidBlock(fluid.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        Block source = event.getBlock();
        if (source == null) {
            return;
        }
        switch (source.getType()) {
            case WATER:
            case LAVA:
                break;
            default:
                return;
        }

        RunningMatch match = plugin.getMatchManager().getRunningMatchAt(source.getLocation());
        if (match == null) {
            return;
        }
        if (!match.isTrackedFluidBlock(source.getLocation())) {
            return;
        }
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        if (gameplay == null) {
            return;
        }

        Block to = event.getToBlock();
        if (to == null || to.getWorld() == null) {
            event.setCancelled(true);
            return;
        }
        if (!to.getWorld().getName().equalsIgnoreCase(gameplay.getMin().getWorld())) {
            event.setCancelled(true);
            return;
        }
        if (to.getY() < gameplay.getMin().getY()) {
            event.setCancelled(true);
            return;
        }
        if (to.getX() < gameplay.getMin().getX() || to.getX() > gameplay.getMax().getX()
                || to.getZ() < gameplay.getMin().getZ() || to.getZ() > gameplay.getMax().getZ()) {
            event.setCancelled(true);
            return;
        }
        match.markTrackedFluidBlock(to.getLocation());
    }

    private static java.util.List<Location> relatedPlacedBlockLocations(Block block) {
        java.util.ArrayList<Location> locations = new java.util.ArrayList<Location>(2);
        if (block == null || block.getWorld() == null) {
            return locations;
        }
        locations.add(block.getLocation());

        BlockData data = block.getBlockData();
        if (data instanceof Bed && data instanceof Directional) {
            Bed bed = (Bed) data;
            BlockFace facing = ((Directional) data).getFacing();
            Block counterpart = bed.getPart() == Bed.Part.HEAD
                    ? block.getRelative(facing.getOppositeFace())
                    : block.getRelative(facing);
            locations.add(counterpart.getLocation());
            return locations;
        }

        if (data instanceof Bisected) {
            Bisected bisected = (Bisected) data;
            Block counterpart = bisected.getHalf() == Bisected.Half.TOP
                    ? block.getRelative(BlockFace.DOWN)
                    : block.getRelative(BlockFace.UP);
            locations.add(counterpart.getLocation());
        }

        return locations;
    }
}
