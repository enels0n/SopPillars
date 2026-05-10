package net.enelson.soppillars.match;

import net.enelson.soppillars.arena.PillarsArena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WaitingMatch {

    private final PillarsArena arena;
    private final Map<UUID, Integer> teamByPlayer = new LinkedHashMap<UUID, Integer>();
    private int countdownRemaining = -1;

    public WaitingMatch(PillarsArena arena) {
        this.arena = arena;
    }

    public PillarsArena getArena() {
        return arena;
    }

    public boolean contains(UUID playerId) {
        return teamByPlayer.containsKey(playerId);
    }

    public int size() {
        return teamByPlayer.size();
    }

    public boolean isEmpty() {
        return teamByPlayer.isEmpty();
    }

    public int getTeam(UUID playerId) {
        Integer team = teamByPlayer.get(playerId);
        return team == null ? 0 : team.intValue();
    }

    public void addPlayer(UUID playerId, int team) {
        teamByPlayer.put(playerId, Integer.valueOf(team));
    }

    public void removePlayer(UUID playerId) {
        teamByPlayer.remove(playerId);
    }

    public boolean canJoin() {
        return size() < arena.getMaxPlayers();
    }

    public boolean hasFreeSlotInTeam(int teamNumber) {
        return getTeamSize(teamNumber) < arena.getPlayersPerTeam();
    }

    public int getTeamSize(int teamNumber) {
        int count = 0;
        for (Integer team : teamByPlayer.values()) {
            if (team != null && team.intValue() == teamNumber) {
                count++;
            }
        }
        return count;
    }

    public int findBestAutoTeam() {
        int bestTeam = 0;
        int bestSize = Integer.MAX_VALUE;
        for (int team = 1; team <= arena.getTeams(); team++) {
            int size = getTeamSize(team);
            if (size >= arena.getPlayersPerTeam()) {
                continue;
            }
            if (size < bestSize) {
                bestSize = size;
                bestTeam = team;
            }
        }
        return bestTeam;
    }

    public List<UUID> getPlayers() {
        return Collections.unmodifiableList(new ArrayList<UUID>(teamByPlayer.keySet()));
    }

    public List<UUID> getPlayersInTeam(int teamNumber) {
        List<UUID> players = new ArrayList<UUID>();
        for (Map.Entry<UUID, Integer> entry : teamByPlayer.entrySet()) {
            if (entry.getValue() != null && entry.getValue().intValue() == teamNumber) {
                players.add(entry.getKey());
            }
        }
        return players;
    }

    public int getFilledTeamCount() {
        int count = 0;
        for (int team = 1; team <= arena.getTeams(); team++) {
            if (getTeamSize(team) > 0) {
                count++;
            }
        }
        return count;
    }

    public int getCountdownRemaining() {
        return countdownRemaining;
    }

    public void setCountdownRemaining(int countdownRemaining) {
        this.countdownRemaining = countdownRemaining;
    }

    public boolean hasCountdown() {
        return countdownRemaining >= 0;
    }
}
