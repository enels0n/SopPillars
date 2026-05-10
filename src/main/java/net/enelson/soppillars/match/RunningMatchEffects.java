package net.enelson.soppillars.match;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-match world border animation and optional rising lava. Vanilla world border is per-world;
 * only one SopPillars match should run per world when these effects are enabled.
 */
public final class RunningMatchEffects {

    private final SopPillarsPlugin plugin;
    private final MatchManager matchManager;
    private final RunningMatch match;

    private WorldBorderBackup borderBackup;
    private boolean borderConfigured;
    private boolean shrinkApplied;
    private double initialDiameter;

    private int elapsedSeconds;

    private boolean lavaAllowed;
    private int nextLavaY;
    private boolean lavaFinished;
    private final List<Location> lavaChanges = new ArrayList<Location>();

    public RunningMatchEffects(SopPillarsPlugin plugin, MatchManager matchManager, RunningMatch match) {
        this.plugin = plugin;
        this.matchManager = matchManager;
        this.match = match;
        this.initialDiameter = -1.0D;
        this.nextLavaY = Integer.MIN_VALUE;
    }

    public void start() {
        PillarsArena arena = match.getArena();
        SerializedCuboid area = arena.getGameplayArea();
        if (area == null) {
            return;
        }
        World world = Bukkit.getWorld(area.getMin().getWorld());
        if (world == null) {
            return;
        }

        if (matchManager.hasOtherRunningMatchInSameWorld(arena.getWorldName(), arena.getName())) {
            plugin.getLogger().warning("[SopPillars] Skipping world border and lava for arena "
                    + arena.getName() + " because another match is already active in world " + arena.getWorldName() + ".");
            return;
        }

        WorldBorder border = world.getWorldBorder();
        borderBackup = WorldBorderBackup.capture(border);

        Location center = area.getCenter(world);
        border.setCenter(center.getX(), center.getZ());

        initialDiameter = computeCoverDiameter(area);
        border.setSize(initialDiameter);

        borderConfigured = true;
        lavaAllowed = arena.getSettings().isLavaEnabled();
        nextLavaY = (int) Math.floor(Math.min(area.getMin().getY(), area.getMax().getY()));
    }

    public void tickSecond() {
        elapsedSeconds++;
        PillarsArena arena = match.getArena();
        ArenaSettings settings = arena.getSettings();

        if (borderConfigured && !shrinkApplied && borderBackup != null) {
            int shrinkAt = settings.getCageSeconds() + settings.getPreBorderDelaySeconds();
            if (elapsedSeconds >= shrinkAt) {
                SerializedCuboid area = arena.getGameplayArea();
                if (area != null) {
                    World world = Bukkit.getWorld(area.getMin().getWorld());
                    if (world != null && initialDiameter > 0.0D) {
                        WorldBorder border = world.getWorldBorder();
                        double target = clampEndDiameter(settings, initialDiameter);
                        if (target < initialDiameter - 0.5D) {
                            long shrinkDuration = Math.max(1L, (long) settings.getBorderShrinkSeconds());
                            border.setSize(target, shrinkDuration);
                        }
                    }
                }
                shrinkApplied = true;
            }
        }

        if (lavaAllowed && !lavaFinished) {
            SerializedCuboid area = arena.getGameplayArea();
            if (area != null) {
                int maxY = (int) Math.floor(Math.max(area.getMin().getY(), area.getMax().getY()));
                if (nextLavaY != Integer.MIN_VALUE && nextLavaY <= maxY) {
                    int lavaDelay = settings.getLavaStartDelaySeconds();
                    if (elapsedSeconds >= lavaDelay) {
                        int sinceStart = elapsedSeconds - lavaDelay;
                        int interval = Math.max(1, settings.getLavaRiseIntervalSeconds());
                        if (sinceStart % interval == 0) {
                            World world = Bukkit.getWorld(area.getMin().getWorld());
                            if (world != null) {
                                riseLavaLayer(world, area, nextLavaY);
                                nextLavaY++;
                                if (nextLavaY > maxY) {
                                    lavaFinished = true;
                                }
                            }
                        }
                    }
                } else {
                    lavaFinished = true;
                }
            }
        }

        tickLoot(settings);
    }

    private void tickLoot(ArenaSettings settings) {
        if (!settings.isLootEnabled()) {
            return;
        }
        if (elapsedSeconds < Math.max(1, settings.getCageSeconds())) {
            return;
        }
        int interval = Math.max(1, settings.getLootIntervalSeconds());
        if (elapsedSeconds % interval != 0) {
            return;
        }
        matchManager.grantLootTick(match);
    }

    public void cleanup() {
        if (borderConfigured && borderBackup != null) {
            PillarsArena arena = match.getArena();
            SerializedCuboid area = arena.getGameplayArea();
            if (area != null) {
                World world = Bukkit.getWorld(area.getMin().getWorld());
                if (world != null) {
                    borderBackup.restore(world.getWorldBorder());
                }
            }
            borderBackup = null;
        }

        for (Location loc : lavaChanges) {
            World world = loc.getWorld();
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(loc);
            if (block.getType() == Material.LAVA) {
                block.setType(Material.AIR, false);
            }
        }
        lavaChanges.clear();
    }

    private void riseLavaLayer(World world, SerializedCuboid area, int y) {
        int minX = (int) Math.floor(Math.min(area.getMin().getX(), area.getMax().getX()));
        int maxX = (int) Math.floor(Math.max(area.getMin().getX(), area.getMax().getX()));
        int minZ = (int) Math.floor(Math.min(area.getMin().getZ(), area.getMax().getZ()));
        int maxZ = (int) Math.floor(Math.max(area.getMin().getZ(), area.getMax().getZ()));

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                Block block = world.getBlockAt(x, y, z);
                lavaChanges.add(block.getLocation().clone());
                block.setType(Material.LAVA, false);
            }
        }
    }

    private static double computeCoverDiameter(SerializedCuboid area) {
        double dx = area.getMax().getX() - area.getMin().getX();
        double dz = area.getMax().getZ() - area.getMin().getZ();
        double diagonal = Math.sqrt(dx * dx + dz * dz);
        return Math.max(16.0D, diagonal + 4.0D);
    }

    private static double clampEndDiameter(ArenaSettings settings, double initialDiameter) {
        double requested = settings.getEndBorderDiameter();
        if (requested < 1.0D) {
            requested = 1.0D;
        }
        if (requested >= initialDiameter) {
            return initialDiameter;
        }
        return requested;
    }
}
