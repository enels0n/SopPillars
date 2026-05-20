package net.enelson.soppillars.match;

import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.cage.CapturedBlockState;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class RunningMatch {

    private final PillarsArena arena;
    private final Map<UUID, Integer> teamByPlayer = new LinkedHashMap<UUID, Integer>();
    private final Map<UUID, Boolean> aliveByPlayer = new LinkedHashMap<UUID, Boolean>();
    private int lastWinningTeam;
    private final Set<String> playerPlacedBlockKeys = new HashSet<String>();
    private final Set<TrackedBlock> trackedFluidBlocks = new HashSet<TrackedBlock>();
    private final Set<UUID> celebrationEntityIds = new HashSet<UUID>();
    private List<Material> blacklistLootPool;
    private final List<CapturedBlockState> hiddenLobbyBlocks = new ArrayList<CapturedBlockState>();
    private int victoryEffectTaskId = -1;

    public RunningMatch(PillarsArena arena) {
        this.arena = arena;
        this.lastWinningTeam = -1;
    }

    public void setLastWinningTeam(int team) {
        this.lastWinningTeam = team;
    }

    public int getLastWinningTeam() {
        return lastWinningTeam;
    }

    public PillarsArena getArena() {
        return arena;
    }

    public void addPlayer(UUID playerId, int team) {
        teamByPlayer.put(playerId, Integer.valueOf(team));
        aliveByPlayer.put(playerId, Boolean.TRUE);
    }

    public boolean contains(UUID playerId) {
        return teamByPlayer.containsKey(playerId);
    }

    public int getTeam(UUID playerId) {
        Integer team = teamByPlayer.get(playerId);
        return team == null ? 0 : team.intValue();
    }

    public boolean isAlive(UUID playerId) {
        Boolean alive = aliveByPlayer.get(playerId);
        return alive != null && alive.booleanValue();
    }

    public void setAlive(UUID playerId, boolean alive) {
        if (aliveByPlayer.containsKey(playerId)) {
            aliveByPlayer.put(playerId, Boolean.valueOf(alive));
        }
    }

    public List<UUID> getPlayers() {
        return Collections.unmodifiableList(new ArrayList<UUID>(teamByPlayer.keySet()));
    }

    public int getAliveCount() {
        int count = 0;
        for (Boolean alive : aliveByPlayer.values()) {
            if (alive != null && alive.booleanValue()) {
                count++;
            }
        }
        return count;
    }

    public int getAliveTeamCount() {
        int count = 0;
        for (int team = 1; team <= arena.getTeams(); team++) {
            if (getAlivePlayersInTeam(team) > 0) {
                count++;
            }
        }
        return count;
    }

    public int getAlivePlayersInTeam(int team) {
        int count = 0;
        for (Map.Entry<UUID, Integer> entry : teamByPlayer.entrySet()) {
            if (entry.getValue() != null && entry.getValue().intValue() == team && isAlive(entry.getKey())) {
                count++;
            }
        }
        return count;
    }

    public int findWinningTeam() {
        for (int team = 1; team <= arena.getTeams(); team++) {
            if (getAlivePlayersInTeam(team) > 0) {
                return team;
            }
        }
        return 0;
    }

    public List<Material> getBlacklistLootPool() {
        return blacklistLootPool;
    }

    public void setBlacklistLootPool(List<Material> blacklistLootPool) {
        this.blacklistLootPool = blacklistLootPool;
    }

    public void addHiddenLobbyBlock(CapturedBlockState state) {
        if (state != null) {
            hiddenLobbyBlocks.add(state);
        }
    }

    public void restoreHiddenLobbyBlocks() {
        for (CapturedBlockState state : hiddenLobbyBlocks) {
            state.restore();
        }
        hiddenLobbyBlocks.clear();
    }

    public void markPlayerPlacedBlock(Location location) {
        playerPlacedBlockKeys.add(blockKey(location));
    }

    public void unmarkPlayerPlacedBlock(Location location) {
        playerPlacedBlockKeys.remove(blockKey(location));
    }

    public boolean isPlayerPlacedBlock(Location location) {
        return playerPlacedBlockKeys.contains(blockKey(location));
    }

    public void markTrackedFluidBlock(Location location) {
        TrackedBlock tracked = trackedBlock(location);
        if (tracked != null) {
            trackedFluidBlocks.add(tracked);
        }
    }

    public boolean isTrackedFluidBlock(Location location) {
        TrackedBlock tracked = trackedBlock(location);
        return tracked != null && trackedFluidBlocks.contains(tracked);
    }

    public Set<TrackedBlock> getTrackedFluidBlocks() {
        return new HashSet<TrackedBlock>(trackedFluidBlocks);
    }

    public void trackCelebrationEntity(UUID entityId) {
        if (entityId != null) {
            celebrationEntityIds.add(entityId);
        }
    }

    public void untrackCelebrationEntity(UUID entityId) {
        celebrationEntityIds.remove(entityId);
    }

    public boolean isTrackedCelebrationEntity(UUID entityId) {
        return celebrationEntityIds.contains(entityId);
    }

    public Set<UUID> getCelebrationEntityIds() {
        return new HashSet<UUID>(celebrationEntityIds);
    }

    public void clearCelebrationEntities() {
        celebrationEntityIds.clear();
    }

    public int getVictoryEffectTaskId() {
        return victoryEffectTaskId;
    }

    public void setVictoryEffectTaskId(int victoryEffectTaskId) {
        this.victoryEffectTaskId = victoryEffectTaskId;
    }

    private static String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static TrackedBlock trackedBlock(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        return new TrackedBlock(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
    }

    public static final class TrackedBlock {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;

        public TrackedBlock(String worldName, int x, int y, int z) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public String getWorldName() {
            return worldName;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof TrackedBlock)) {
                return false;
            }
            TrackedBlock that = (TrackedBlock) other;
            return x == that.x
                    && y == that.y
                    && z == that.z
                    && worldName.equals(that.worldName);
        }

        @Override
        public int hashCode() {
            int result = worldName.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
