package net.enelson.soppillars.arena;

import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.SerializedLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PillarsArena {

    private final String name;
    private final String mode;
    private final int teams;
    private final int playersPerTeam;
    private final String worldName;
    private final List<String> teamNames;
    private ArenaState state;
    private SerializedCuboid gameplayArea;
    private SerializedCuboid lobbyArea;
    private SerializedLocation lobbySpawn;
    private final List<SerializedLocation> playerSpawns;
    private SerializedLocation spectatorSpawn;
    private SerializedLocation postGameSpawn;
    private ArenaSettings settings;

    public PillarsArena(String name,
                        String mode,
                        int teams,
                        int playersPerTeam,
                        String worldName,
                        ArenaState state,
                        List<String> teamNames,
                        List<SerializedLocation> playerSpawns,
                        ArenaSettings settings) {
        this.name = name;
        this.mode = mode;
        this.teams = teams;
        this.playersPerTeam = playersPerTeam;
        this.worldName = worldName;
        this.state = state;
        this.teamNames = new ArrayList<String>(teamNames);
        this.playerSpawns = new ArrayList<SerializedLocation>(playerSpawns);
        this.settings = settings;
    }

    public static PillarsArena createDefault(String name,
                                             String mode,
                                             int teams,
                                             int playersPerTeam,
                                             String worldName,
                                             ArenaSettings settings) {
        List<String> generatedTeamNames = new ArrayList<String>();
        for (int teamIndex = 1; teamIndex <= teams; teamIndex++) {
            generatedTeamNames.add("team" + teamIndex);
        }
        return new PillarsArena(
                name,
                mode,
                teams,
                playersPerTeam,
                worldName,
                ArenaState.EDITING,
                generatedTeamNames,
                new ArrayList<SerializedLocation>(),
                settings
        );
    }

    public String getName() {
        return name;
    }

    public String getMode() {
        return mode;
    }

    public int getTeams() {
        return teams;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public String getWorldName() {
        return worldName;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public List<String> getTeamNames() {
        return Collections.unmodifiableList(teamNames);
    }

    public SerializedCuboid getGameplayArea() {
        return gameplayArea;
    }

    public void setGameplayArea(SerializedCuboid gameplayArea) {
        this.gameplayArea = gameplayArea;
    }

    public SerializedCuboid getLobbyArea() {
        return lobbyArea;
    }

    public void setLobbyArea(SerializedCuboid lobbyArea) {
        this.lobbyArea = lobbyArea;
    }

    public SerializedLocation getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(SerializedLocation lobbySpawn) {
        this.lobbySpawn = lobbySpawn;
    }

    public List<SerializedLocation> getPlayerSpawns() {
        return Collections.unmodifiableList(playerSpawns);
    }

    public SerializedLocation getSpawn(int teamNumber, int playerNumber) {
        int index = toSpawnIndex(teamNumber, playerNumber);
        if (index < 0 || index >= playerSpawns.size()) {
            return null;
        }
        return playerSpawns.get(index);
    }

    public void setSpawn(int teamNumber, int playerNumber, SerializedLocation location) {
        int index = toSpawnIndex(teamNumber, playerNumber);
        while (playerSpawns.size() <= index) {
            playerSpawns.add(null);
        }
        playerSpawns.set(index, location);
    }

    public SerializedLocation getSpectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(SerializedLocation spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public SerializedLocation getPostGameSpawn() {
        return postGameSpawn;
    }

    public void setPostGameSpawn(SerializedLocation postGameSpawn) {
        this.postGameSpawn = postGameSpawn;
    }

    public ArenaSettings getSettings() {
        return settings;
    }

    public void setSettings(ArenaSettings settings) {
        this.settings = settings;
    }

    public int getMaxPlayers() {
        return teams * playersPerTeam;
    }

    public boolean isConfigured() {
        return gameplayArea != null && spectatorSpawn != null;
    }

    private int toSpawnIndex(int teamNumber, int playerNumber) {
        return ((teamNumber - 1) * playersPerTeam) + (playerNumber - 1);
    }
}
