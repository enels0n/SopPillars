package net.enelson.soppillars.match;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.ArenaState;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-match fake border timings and optional rising lava.
 */
public final class RunningMatchEffects {

    private static boolean visibleBorderUnsupportedLogged;

    private final SopPillarsPlugin plugin;
    private final MatchManager matchManager;
    private final RunningMatch match;

    private boolean fakeBorderConfigured;
    private boolean shrinkApplied;
    private boolean visibleBorderShrinkStarted;
    private double initialDiameter;
    private double targetDiameter;
    private double borderCenterX;
    private double borderCenterZ;
    private int endingStartedAtElapsed = -1;

    private int elapsedSeconds;

    private boolean lavaAllowed;
    private int nextLavaY;
    private boolean lavaFinished;
    private final List<Location> lavaChanges = new ArrayList<Location>();
    private WorldBorder visibleBorder;

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
        double minX = Math.min(area.getMin().getX(), area.getMax().getX());
        double maxX = Math.max(area.getMin().getX(), area.getMax().getX());
        double minZ = Math.min(area.getMin().getZ(), area.getMax().getZ());
        double maxZ = Math.max(area.getMin().getZ(), area.getMax().getZ());
        borderCenterX = (minX + maxX) / 2.0D;
        borderCenterZ = (minZ + maxZ) / 2.0D;
        initialDiameter = computeCoverDiameter(area);
        targetDiameter = clampEndDiameter(arena.getSettings(), initialDiameter);
        fakeBorderConfigured = true;
        lavaAllowed = arena.getSettings().isLavaEnabled();
        nextLavaY = (int) Math.floor(Math.min(area.getMin().getY(), area.getMax().getY()));
        visibleBorder = createVisibleBorderReflective();
        initializeVisibleBorder();
    }

    public void tickSecond() {
        elapsedSeconds++;
        PillarsArena arena = match.getArena();
        ArenaSettings settings = arena.getSettings();
        if (arena.getState() == ArenaState.ENDING && endingStartedAtElapsed < 0) {
            endingStartedAtElapsed = elapsedSeconds;
        }

        if (fakeBorderConfigured && !shrinkApplied) {
            int shrinkAt = settings.getCageSeconds() + settings.getPreBorderDelaySeconds();
            if (elapsedSeconds >= shrinkAt) {
                shrinkApplied = true;
                startVisibleBorderShrink();
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
        int cageSeconds = Math.max(1, settings.getCageSeconds());
        int lootInterval = Math.max(1, settings.getLootIntervalSeconds());
        if (elapsedSeconds <= cageSeconds) {
            return;
        }
        int activeCombatSeconds = elapsedSeconds - cageSeconds;
        if (activeCombatSeconds % lootInterval != 0) {
            return;
        }
        matchManager.grantLootTick(match);
    }

    public int getSecondsUntilNextLoot() {
        ArenaSettings settings = match.getArena().getSettings();
        if (!settings.isLootEnabled()) {
            return 0;
        }
        int cageSeconds = Math.max(1, settings.getCageSeconds());
        int lootInterval = Math.max(1, settings.getLootIntervalSeconds());
        if (elapsedSeconds <= cageSeconds) {
            return (cageSeconds - elapsedSeconds) + lootInterval;
        }
        int activeCombatSeconds = elapsedSeconds - cageSeconds;
        int remainder = activeCombatSeconds % lootInterval;
        return remainder == 0 ? lootInterval : (lootInterval - remainder);
    }

    public int getSecondsUntilEstimatedGameEnd() {
        ArenaSettings settings = match.getArena().getSettings();
        if (match.getArena().getState() == ArenaState.ENDING) {
            if (endingStartedAtElapsed < 0) {
                return Math.max(0, settings.getCelebrationSeconds());
            }
            int remainingCelebration = settings.getCelebrationSeconds() - (elapsedSeconds - endingStartedAtElapsed);
            return Math.max(0, remainingCelebration);
        }
        int planned = Math.max(1, settings.getCageSeconds())
                + Math.max(0, settings.getPreBorderDelaySeconds())
                + Math.max(1, settings.getBorderShrinkSeconds())
                + Math.max(0, settings.getPostShrinkEndDelaySeconds());
        return Math.max(0, planned - elapsedSeconds);
    }

    public void cleanup() {
        clearVisibleBorder();
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

    public boolean isOutsideFakeBorder(Location location) {
        if (!fakeBorderConfigured || location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(match.getArena().getWorldName())) {
            return false;
        }
        double radius = Math.max(0.5D, getCurrentBorderDiameter() / 2.0D);
        double dx = Math.abs(location.getX() - borderCenterX);
        double dz = Math.abs(location.getZ() - borderCenterZ);
        return dx > radius || dz > radius;
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

    private double getCurrentBorderDiameter() {
        if (!fakeBorderConfigured) {
            return 0.0D;
        }
        ArenaSettings settings = match.getArena().getSettings();
        int shrinkAt = settings.getCageSeconds() + settings.getPreBorderDelaySeconds();
        if (elapsedSeconds <= shrinkAt) {
            return initialDiameter;
        }
        int shrinkSeconds = Math.max(1, settings.getBorderShrinkSeconds());
        double progress = (double) (elapsedSeconds - shrinkAt) / (double) shrinkSeconds;
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        return initialDiameter + ((targetDiameter - initialDiameter) * progress);
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

    private WorldBorder createVisibleBorderReflective() {
        try {
            Method bukkitFactory = Bukkit.class.getMethod("createWorldBorder");
            Object created = bukkitFactory.invoke(null);
            return created instanceof WorldBorder ? (WorldBorder) created : null;
        } catch (Exception ignored) {
        }
        try {
            Method serverFactory = Bukkit.getServer().getClass().getMethod("createWorldBorder");
            Object created = serverFactory.invoke(Bukkit.getServer());
            return created instanceof WorldBorder ? (WorldBorder) created : null;
        } catch (Exception ignored) {
        }
        if (!visibleBorderUnsupportedLogged) {
            visibleBorderUnsupportedLogged = true;
            plugin.getLogger().warning("Visible match border is not supported by this runtime API path; using damage-only fake border.");
        }
        return null;
    }

    private void initializeVisibleBorder() {
        if (visibleBorder == null) {
            return;
        }
        visibleBorder.setCenter(borderCenterX, borderCenterZ);
        visibleBorder.setSize(Math.max(1.0D, initialDiameter));
        visibleBorder.setDamageAmount(0.0D);
        visibleBorder.setDamageBuffer(0.0D);
        visibleBorder.setWarningDistance(2);
        visibleBorder.setWarningTime(0);
        applyVisibleBorderToPlayers();
    }

    private void startVisibleBorderShrink() {
        if (visibleBorder == null || visibleBorderShrinkStarted) {
            return;
        }
        visibleBorder.setCenter(borderCenterX, borderCenterZ);
        visibleBorder.setDamageAmount(0.0D);
        visibleBorder.setDamageBuffer(0.0D);
        visibleBorder.setWarningDistance(2);
        visibleBorder.setWarningTime(0);
        visibleBorder.setSize(Math.max(1.0D, targetDiameter), Math.max(1L, (long) match.getArena().getSettings().getBorderShrinkSeconds()));
        visibleBorderShrinkStarted = true;
        applyVisibleBorderToPlayers();
    }

    private void applyVisibleBorderToPlayers() {
        if (visibleBorder == null) {
            return;
        }
        for (java.util.UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            try {
                Method setter = player.getClass().getMethod("setWorldBorder", WorldBorder.class);
                setter.invoke(player, visibleBorder);
            } catch (Exception ignored) {
                if (!visibleBorderUnsupportedLogged) {
                    visibleBorderUnsupportedLogged = true;
                    plugin.getLogger().warning("Visible match border player assignment is not supported by this runtime API path; using damage-only fake border.");
                }
                visibleBorder = null;
                return;
            }
        }
    }

    private void clearVisibleBorder() {
        if (visibleBorder == null) {
            return;
        }
        for (java.util.UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            try {
                Method setter = player.getClass().getMethod("setWorldBorder", WorldBorder.class);
                setter.invoke(player, new Object[] { null });
            } catch (Exception ignored) {
                return;
            }
        }
    }

}
