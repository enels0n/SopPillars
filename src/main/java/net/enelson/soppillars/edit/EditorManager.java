package net.enelson.soppillars.edit;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EditorManager {

    private final Map<UUID, String> activeEditors = new HashMap<UUID, String>();
    private final Map<UUID, Location> gameplayPos1 = new HashMap<UUID, Location>();
    private final Map<UUID, Location> gameplayPos2 = new HashMap<UUID, Location>();
    private final Map<UUID, Location> lobbyPos1 = new HashMap<UUID, Location>();
    private final Map<UUID, Location> lobbyPos2 = new HashMap<UUID, Location>();
    private final Set<String> editedArenas = new HashSet<String>();

    public void enterEditor(UUID playerId, String arenaName) {
        leaveEditor(playerId);
        activeEditors.put(playerId, arenaName);
        editedArenas.add(arenaName.toLowerCase());
    }

    public void leaveEditor(UUID playerId) {
        String arena = activeEditors.remove(playerId);
        gameplayPos1.remove(playerId);
        gameplayPos2.remove(playerId);
        lobbyPos1.remove(playerId);
        lobbyPos2.remove(playerId);
        if (arena != null) {
            editedArenas.remove(arena.toLowerCase());
        }
    }

    public String getEditedArena(UUID playerId) {
        return activeEditors.get(playerId);
    }

    public boolean isArenaBeingEdited(String arenaName) {
        return editedArenas.contains(arenaName.toLowerCase());
    }

    public void setGameplayPos1(UUID playerId, Location location) {
        gameplayPos1.put(playerId, location.clone());
    }

    public void setGameplayPos2(UUID playerId, Location location) {
        gameplayPos2.put(playerId, location.clone());
    }

    public Location getGameplayPos1(UUID playerId) {
        return gameplayPos1.get(playerId);
    }

    public Location getGameplayPos2(UUID playerId) {
        return gameplayPos2.get(playerId);
    }

    public void setLobbyPos1(UUID playerId, Location location) {
        lobbyPos1.put(playerId, location.clone());
    }

    public void setLobbyPos2(UUID playerId, Location location) {
        lobbyPos2.put(playerId, location.clone());
    }

    public Location getLobbyPos1(UUID playerId) {
        return lobbyPos1.get(playerId);
    }

    public Location getLobbyPos2(UUID playerId) {
        return lobbyPos2.get(playerId);
    }
}
