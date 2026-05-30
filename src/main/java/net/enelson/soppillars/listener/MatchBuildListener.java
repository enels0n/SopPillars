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
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

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

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        filterExplosionBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        filterExplosionBlocks(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (shouldCancelPistonMovement(event.getBlock().getLocation(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtendMonitor(BlockPistonExtendEvent event) {
        applyPistonTracking(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (shouldCancelPistonMovement(event.getBlock().getLocation(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetractMonitor(BlockPistonRetractEvent event) {
        applyPistonTracking(event.getBlocks(), event.getDirection());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        RunningMatch match = plugin.getMatchManager().getRunningMatchAt(block.getLocation());
        if (match == null) {
            return;
        }
        if (isOutsideGameplay(match, block.getLocation())) {
            return;
        }
        if (!match.getArena().getSettings().isAllowFireBlockBurn()) {
            event.setCancelled(true);
        }
    }

    private void filterExplosionBlocks(java.util.List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        java.util.Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            RunningMatch match = plugin.getMatchManager().getRunningMatchAt(block.getLocation());
            if (match == null || isOutsideGameplay(match, block.getLocation())) {
                continue;
            }
            ArenaSettings settings = match.getArena().getSettings();
            if (!settings.isAllowExplosionBlockDamage()) {
                iterator.remove();
                continue;
            }
            if (isProtectedByBreakRules(match, block)) {
                iterator.remove();
                continue;
            }
            for (Location location : relatedPlacedBlockLocations(block)) {
                if (match.isPlayerPlacedBlock(location)) {
                    match.unmarkPlayerPlacedBlock(location);
                }
            }
        }
    }

    private boolean shouldCancelPistonMovement(Location pistonLocation, java.util.List<Block> movedBlocks, BlockFace direction) {
        RunningMatch pistonMatch = plugin.getMatchManager().getRunningMatchAt(pistonLocation);
        if (pistonMatch != null && !pistonMatch.getArena().getSettings().isAllowPistonBlockMovement()) {
            return true;
        }
        if (movedBlocks == null || movedBlocks.isEmpty()) {
            return false;
        }
        for (Block moved : movedBlocks) {
            RunningMatch match = plugin.getMatchManager().getRunningMatchAt(moved.getLocation());
            if (match == null) {
                match = plugin.getMatchManager().getRunningMatchAt(moved.getRelative(direction).getLocation());
            }
            if (match != null && !match.getArena().getSettings().isAllowPistonBlockMovement()) {
                return true;
            }
        }
        return false;
    }

    private void applyPistonTracking(java.util.List<Block> movedBlocks, BlockFace direction) {
        if (movedBlocks == null || movedBlocks.isEmpty()) {
            return;
        }
        java.util.List<Location> toMark = new java.util.ArrayList<Location>();
        for (Block moved : movedBlocks) {
            RunningMatch match = plugin.getMatchManager().getRunningMatchAt(moved.getLocation());
            if (match == null) {
                continue;
            }
            for (Location location : relatedPlacedBlockLocations(moved)) {
                if (match.isPlayerPlacedBlock(location)) {
                    match.unmarkPlayerPlacedBlock(location);
                    toMark.add(location.clone().add(direction.getModX(), direction.getModY(), direction.getModZ()));
                }
            }
            for (Location destination : toMark) {
                if (!isOutsideGameplay(match, destination)) {
                    match.markPlayerPlacedBlock(destination);
                }
            }
            toMark.clear();
        }
    }

    private boolean isProtectedByBreakRules(RunningMatch match, Block block) {
        ArenaSettings settings = match.getArena().getSettings();
        boolean playerPlaced = false;
        for (Location location : relatedPlacedBlockLocations(block)) {
            if (match.isPlayerPlacedBlock(location)) {
                playerPlaced = true;
                break;
            }
        }
        if (playerPlaced) {
            return !settings.isAllowBreakPlayerBlocks();
        }
        return !settings.isAllowBreakOriginalBlocks();
    }

    private boolean isOutsideGameplay(RunningMatch match, Location location) {
        SerializedCuboid gameplay = match.getArena().getGameplayArea();
        return gameplay == null || !gameplay.contains(location);
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
