package net.enelson.soppillars.match;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.ArenaState;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.VictoryEffectShape;
import net.enelson.soppillars.model.VictoryEffectType;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

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
    private boolean victoryEffectStarted;

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
        stopVictoryEffectTask();
        removeCelebrationEntities();
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

    public void beginVictoryEffectIfNeeded() {
        if (victoryEffectStarted || match.getLastWinningTeam() <= 0) {
            return;
        }
        VictoryEffectType type = match.getArena().getSettings().getVictoryEffectType();
        if (type == null || type == VictoryEffectType.NONE) {
            return;
        }
        int intervalTicks = Math.max(2, match.getArena().getSettings().getVictoryEffectIntervalTicks());
        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (match.getArena().getState() != ArenaState.ENDING) {
                    stopVictoryEffectTask();
                    removeCelebrationEntities();
                    return;
                }
                cleanupCelebrationEntities();
                spawnVictoryEffectWave();
            }
        }, 0L, intervalTicks);
        match.setVictoryEffectTaskId(taskId);
        victoryEffectStarted = true;
    }

    public boolean isTrackedCelebrationEntity(Entity entity) {
        return entity != null && match.isTrackedCelebrationEntity(entity.getUniqueId());
    }

    public void handleCelebrationEntityLanded(Entity entity) {
        if (!isTrackedCelebrationEntity(entity)) {
            return;
        }
        match.untrackCelebrationEntity(entity.getUniqueId());
        entity.remove();
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

    private void spawnVictoryEffectWave() {
        World world = Bukkit.getWorld(match.getArena().getWorldName());
        SerializedCuboid area = match.getArena().getGameplayArea();
        if (world == null || area == null) {
            return;
        }
        ArenaSettings settings = match.getArena().getSettings();
        int amount = Math.max(1, settings.getVictoryEffectAmountPerWave());
        VictoryEffectType type = settings.getVictoryEffectType();
        switch (type) {
            case FIREWORKS:
                for (int i = 0; i < amount; i++) {
                    spawnFirework(world, randomEffectLocation(world, area, settings, 0.0D));
                }
                break;
            case PIG_RAIN:
                spawnEntityRain(world, area, settings, EntityType.PIG, amount);
                break;
            case COW_RAIN:
                spawnEntityRain(world, area, settings, EntityType.COW, amount);
                break;
            case CHICKEN_RAIN:
                spawnEntityRain(world, area, settings, EntityType.CHICKEN, amount);
                break;
            case ANVIL_RAIN:
                spawnBlockRain(world, area, settings, Material.ANVIL, amount);
                break;
            case BLOCK_RAIN:
                spawnBlockRain(world, area, settings, settings.getVictoryEffectBlockMaterial(), amount);
                break;
            case NONE:
            default:
                break;
        }
    }

    private void spawnFirework(World world, Location location) {
        if (location == null) {
            return;
        }
        Firework firework = world.spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.ORANGE, Color.YELLOW, Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private void spawnEntityRain(World world, SerializedCuboid area, ArenaSettings settings, EntityType entityType, int amount) {
        for (int i = 0; i < amount; i++) {
            Location spawn = randomEffectLocation(world, area, settings, settings.getVictoryEffectSpawnHeight());
            Entity entity = world.spawnEntity(spawn, entityType);
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                living.setAI(false);
                living.setInvulnerable(true);
                living.setSilent(true);
                living.setRemoveWhenFarAway(true);
            }
            entity.setFallDistance(0.0F);
            match.trackCelebrationEntity(entity.getUniqueId());
        }
    }

    private void spawnBlockRain(World world, SerializedCuboid area, ArenaSettings settings, Material material, int amount) {
        Material safeMaterial = material == null || !material.isBlock() ? Material.DIAMOND_BLOCK : material;
        for (int i = 0; i < amount; i++) {
            Location spawn = randomEffectLocation(world, area, settings, settings.getVictoryEffectSpawnHeight());
            FallingBlock block = world.spawnFallingBlock(spawn, safeMaterial.createBlockData());
            block.setDropItem(false);
            match.trackCelebrationEntity(block.getUniqueId());
        }
    }

    private Location randomEffectLocation(World world, SerializedCuboid area, ArenaSettings settings, double extraY) {
        double centerX = (area.getMin().getX() + area.getMax().getX()) / 2.0D;
        double centerZ = (area.getMin().getZ() + area.getMax().getZ()) / 2.0D;
        double radius = Math.max(0.5D, settings.getVictoryEffectRadius());
        double x;
        double z;
        if (settings.getVictoryEffectShape() == VictoryEffectShape.CIRCLE) {
            double angle = Math.random() * Math.PI * 2.0D;
            double distance = Math.sqrt(Math.random()) * radius;
            x = centerX + Math.cos(angle) * distance;
            z = centerZ + Math.sin(angle) * distance;
        } else {
            x = centerX + (((Math.random() * 2.0D) - 1.0D) * radius);
            z = centerZ + (((Math.random() * 2.0D) - 1.0D) * radius);
        }
        double y = Math.max(area.getMin().getY(), area.getMax().getY()) + extraY;
        return new Location(world, x, y, z);
    }

    private void cleanupCelebrationEntities() {
        SerializedCuboid area = match.getArena().getGameplayArea();
        if (area == null) {
            removeCelebrationEntities();
            return;
        }
        double minY = Math.min(area.getMin().getY(), area.getMax().getY()) - 6.0D;
        for (java.util.UUID entityId : new ArrayList<java.util.UUID>(match.getCelebrationEntityIds())) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity == null || !entity.isValid() || entity.isDead()) {
                match.untrackCelebrationEntity(entityId);
                continue;
            }
            Location location = entity.getLocation();
            if (location.getY() <= minY || entity.isOnGround()) {
                match.untrackCelebrationEntity(entityId);
                entity.remove();
            }
        }
    }

    private void removeCelebrationEntities() {
        for (java.util.UUID entityId : new ArrayList<java.util.UUID>(match.getCelebrationEntityIds())) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
            match.untrackCelebrationEntity(entityId);
        }
        match.clearCelebrationEntities();
    }

    private void stopVictoryEffectTask() {
        int taskId = match.getVictoryEffectTaskId();
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            match.setVictoryEffectTaskId(-1);
        }
    }

}
