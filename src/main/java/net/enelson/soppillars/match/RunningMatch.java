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
    private List<Material> blacklistLootPool;
    private final List<CapturedBlockState> hiddenLobbyBlocks = new ArrayList<CapturedBlockState>();

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

    private static String blockKey(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
