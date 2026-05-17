package net.enelson.soppillars.arena;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.SerializedLocation;
import net.enelson.soppillars.model.VictoryEffectShape;
import net.enelson.soppillars.model.VictoryEffectType;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaManager {

    private final SopPillarsPlugin plugin;
    private final Map<String, PillarsArena> arenas = new LinkedHashMap<String, PillarsArena>();

    public ArenaManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        arenas.clear();
        File[] files = plugin.getPillarsConfig().getArenasFolder().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            PillarsArena arena = loadArena(file);
            if (arena != null) {
                arenas.put(normalize(arena.getName()), arena);
            }
        }
    }

    public PillarsArena createArena(String name, String mode, int teams, int playersPerTeam, String worldName) {
        PillarsArena arena = PillarsArena.createDefault(
                name,
                mode,
                teams,
                playersPerTeam,
                worldName,
                plugin.getPillarsConfig().createDefaultArenaSettings()
        );
        arena.setPostGameSpawn(plugin.getPillarsConfig().getDefaultPostGameSpawn());
        arenas.put(normalize(name), arena);
        saveArena(arena);
        return arena;
    }

    public boolean saveArena(PillarsArena arena) {
        File targetFile = new File(plugin.getPillarsConfig().getArenasFolder(), arena.getName() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("arena.name", arena.getName());
        config.set("arena.mode", arena.getMode());
        config.set("arena.teams", arena.getTeams());
        config.set("arena.players-per-team", arena.getPlayersPerTeam());
        config.set("arena.world", arena.getWorldName());
        config.set("arena.state", arena.getState().name());
        config.set("arena.team-names", new ArrayList<String>(arena.getTeamNames()));

        if (arena.getGameplayArea() != null) {
            arena.getGameplayArea().save(config.createSection("arena.gameplay-area"));
        }
        if (arena.getLobbyArea() != null) {
            arena.getLobbyArea().save(config.createSection("arena.lobby-area"));
        }
        if (arena.getLobbySpawn() != null) {
            arena.getLobbySpawn().save(config.createSection("arena.lobby-spawn"));
        }
        if (arena.getSpectatorSpawn() != null) {
            arena.getSpectatorSpawn().save(config.createSection("arena.spectator-spawn"));
        }
        if (arena.getPostGameSpawn() != null) {
            arena.getPostGameSpawn().save(config.createSection("arena.post-game-spawn"));
        }

        ConfigurationSection spawnsSection = config.createSection("arena.spawns");
        for (int team = 1; team <= arena.getTeams(); team++) {
            ConfigurationSection teamSection = spawnsSection.createSection("team-" + team);
            for (int slot = 1; slot <= arena.getPlayersPerTeam(); slot++) {
                SerializedLocation spawn = arena.getSpawn(team, slot);
                if (spawn != null) {
                    spawn.save(teamSection.createSection("player-" + slot));
                }
            }
        }

        saveSettings(config.createSection("settings"), arena.getSettings());

        try {
            config.save(targetFile);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save arena " + arena.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    public PillarsArena getArena(String name) {
        return arenas.get(normalize(name));
    }

    public Collection<PillarsArena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public List<String> getArenaNames() {
        List<String> names = new ArrayList<String>();
        for (PillarsArena arena : arenas.values()) {
            names.add(arena.getName());
        }
        return names;
    }

    public List<String> getModes() {
        List<String> modes = new ArrayList<String>();
        for (PillarsArena arena : arenas.values()) {
            if (!modes.contains(arena.getMode())) {
                modes.add(arena.getMode());
            }
        }
        return modes;
    }

    public boolean hasArena(String name) {
        return arenas.containsKey(normalize(name));
    }

    /**
     * Copies the arena YAML to the edit-backups folder before {@code /pillars edit} or {@code /pillars create}.
     */
    public boolean snapshotArenaBeforeEdit(String arenaName) {
        PillarsArena arena = getArena(arenaName);
        if (arena == null) {
            return false;
        }
        File src = new File(plugin.getPillarsConfig().getArenasFolder(), arena.getName() + ".yml");
        if (!src.isFile()) {
            return false;
        }
        File dest = editBackupYmlFile(arena.getName());
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not snapshot arena YAML before edit (" + arena.getName() + "): " + exception.getMessage());
            return false;
        }
    }

    /**
     * Restores arena YAML from the edit backup (used when cancelling an edit session).
     */
    public boolean restoreArenaFromEditBackup(String arenaName) {
        PillarsArena arena = getArena(arenaName);
        if (arena == null) {
            return false;
        }
        File backup = editBackupYmlFile(arena.getName());
        if (!backup.isFile()) {
            return false;
        }
        File target = new File(plugin.getPillarsConfig().getArenasFolder(), arena.getName() + ".yml");
        try {
            Files.copy(backup.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not restore arena YAML from edit backup (" + arena.getName() + "): " + exception.getMessage());
            return false;
        }
    }

    public void clearEditBackup(String arenaName) {
        PillarsArena arena = getArena(arenaName);
        if (arena == null) {
            return;
        }
        File backup = editBackupYmlFile(arena.getName());
        if (backup.isFile()) {
            backup.delete();
        }
    }

    /**
     * Removes arena registry entry, YAML, edit backup, and block snapshot file.
     */
    public void deleteArenaData(PillarsArena arena) {
        if (arena == null) {
            return;
        }
        String key = normalize(arena.getName());
        plugin.getMatchManager().unregisterArena(arena);
        File yml = new File(plugin.getPillarsConfig().getArenasFolder(), arena.getName() + ".yml");
        if (yml.isFile()) {
            yml.delete();
        }
        File backup = editBackupYmlFile(arena.getName());
        if (backup.isFile()) {
            backup.delete();
        }
        plugin.getArenaSnapshotManager().deleteSnapshotIfExists(arena.getName());
        arenas.remove(key);
    }

    private File editBackupYmlFile(String arenaDisplayName) {
        String safe = arenaDisplayName == null ? "unknown" : arenaDisplayName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return new File(plugin.getPillarsConfig().getArenaEditBackupsFolder(), safe + ".yml");
    }

    /**
     * Reloads one arena YAML from disk into the live registry (used when cancelling an edit session).
     */
    public boolean reloadArenaFromDisk(String name) {
        PillarsArena current = getArena(name);
        if (current == null) {
            return false;
        }
        File file = new File(plugin.getPillarsConfig().getArenasFolder(), current.getName() + ".yml");
        if (!file.isFile()) {
            return false;
        }
        PillarsArena loaded = loadArena(file);
        if (loaded == null) {
            return false;
        }
        arenas.put(normalize(loaded.getName()), loaded);
        return true;
    }

    /**
     * First arena whose gameplay or lobby cuboid contains {@code location} (same world as arena).
     */
    public PillarsArena findArenaContaining(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String worldName = location.getWorld().getName();
        for (PillarsArena arena : arenas.values()) {
            if (!arena.getWorldName().equalsIgnoreCase(worldName)) {
                continue;
            }
            SerializedCuboid gameplay = arena.getGameplayArea();
            if (gameplay != null && gameplay.contains(location)) {
                return arena;
            }
            SerializedCuboid lobby = arena.getLobbyArea();
            if (lobby != null && lobby.contains(location)) {
                return arena;
            }
        }
        return null;
    }

    private PillarsArena loadArena(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String name = config.getString("arena.name", file.getName().replace(".yml", ""));
        String mode = config.getString("arena.mode", "default");
        int teams = config.getInt("arena.teams", 2);
        int playersPerTeam = config.getInt("arena.players-per-team", 1);
        String world = config.getString("arena.world", "");
        ArenaState state = parseState(config.getString("arena.state", ArenaState.DISABLED.name()));
        List<String> teamNames = config.getStringList("arena.team-names");
        if (teamNames.isEmpty()) {
            for (int index = 1; index <= teams; index++) {
                teamNames.add("team" + index);
            }
        }

        List<SerializedLocation> spawns = new ArrayList<SerializedLocation>();
        ConfigurationSection spawnsSection = config.getConfigurationSection("arena.spawns");
        if (spawnsSection != null) {
            for (int team = 1; team <= teams; team++) {
                ConfigurationSection teamSection = spawnsSection.getConfigurationSection("team-" + team);
                if (teamSection == null) {
                    continue;
                }
                for (int slot = 1; slot <= playersPerTeam; slot++) {
                    while (spawns.size() < ((team - 1) * playersPerTeam) + slot) {
                        spawns.add(null);
                    }
                    spawns.set(((team - 1) * playersPerTeam) + (slot - 1),
                            SerializedLocation.fromSection(teamSection.getConfigurationSection("player-" + slot)));
                }
            }
        }

        PillarsArena arena = new PillarsArena(
                name,
                mode,
                teams,
                playersPerTeam,
                world,
                state,
                teamNames,
                spawns,
                loadSettings(config.getConfigurationSection("settings"))
        );
        arena.setGameplayArea(SerializedCuboid.fromSection(config.getConfigurationSection("arena.gameplay-area")));
        arena.setLobbyArea(SerializedCuboid.fromSection(config.getConfigurationSection("arena.lobby-area")));
        arena.setLobbySpawn(SerializedLocation.fromSection(config.getConfigurationSection("arena.lobby-spawn")));
        arena.setSpectatorSpawn(SerializedLocation.fromSection(config.getConfigurationSection("arena.spectator-spawn")));
        SerializedLocation postGameSpawn = SerializedLocation.fromSection(config.getConfigurationSection("arena.post-game-spawn"));
        if (postGameSpawn == null) {
            postGameSpawn = plugin.getPillarsConfig().getDefaultPostGameSpawn();
        }
        arena.setPostGameSpawn(postGameSpawn);
        return arena;
    }

    private ArenaState parseState(String raw) {
        try {
            return ArenaState.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return ArenaState.DISABLED;
        }
    }

    private void saveSettings(ConfigurationSection section, ArenaSettings settings) {
        section.set("countdown-seconds", settings.getCountdownSeconds());
        section.set("cage-seconds", settings.getCageSeconds());
        section.set("pre-border-delay-seconds", settings.getPreBorderDelaySeconds());
        section.set("border-shrink-seconds", settings.getBorderShrinkSeconds());
        section.set("end-border-diameter", settings.getEndBorderDiameter());
        section.set("lava-enabled", settings.isLavaEnabled());
        section.set("lava-start-delay-seconds", settings.getLavaStartDelaySeconds());
        section.set("lava-rise-interval-seconds", settings.getLavaRiseIntervalSeconds());
        section.set("post-shrink-end-delay-seconds", settings.getPostShrinkEndDelaySeconds());
        section.set("min-players", settings.getMinPlayers());
        section.set("min-filled-teams", settings.getMinFilledTeams());
        section.set("allow-place-blocks", settings.isAllowPlaceBlocks());
        section.set("allow-break-original-blocks", settings.isAllowBreakOriginalBlocks());
        section.set("allow-break-player-blocks", settings.isAllowBreakPlayerBlocks());
        section.set("allow-smooth-fall", settings.isAllowSmoothFall());
        section.set("smooth-fall-seconds", settings.getSmoothFallSeconds());
        section.set("friendly-fire", settings.isFriendlyFire());
        section.set("loot.blacklist-mode", settings.isBlacklistMode());
        section.set("loot.allow-enchanted-books", settings.isAllowEnchantedBooks());
        section.set("loot.allow-potions", settings.isAllowPotions());
        section.set("loot.allow-tipped-arrows", settings.isAllowTippedArrows());
        section.set("loot.allow-spawn-eggs", settings.isAllowSpawnEggs());
        section.set("loot.enabled", settings.isLootEnabled());
        section.set("loot.interval-seconds", settings.getLootIntervalSeconds());
        section.set("loot.whitelist", new ArrayList<String>(settings.getLootWhitelist()));
        section.set("loot.blacklist", new ArrayList<String>(settings.getLootBlacklist()));
        section.set("allowed-commands", new ArrayList<String>(settings.getAllowedCommands()));
        section.set("celebration-seconds", settings.getCelebrationSeconds());
        section.set("victory-effect.type", settings.getVictoryEffectType().name().toLowerCase(Locale.ROOT));
        section.set("victory-effect.shape", settings.getVictoryEffectShape().name().toLowerCase(Locale.ROOT));
        section.set("victory-effect.radius", settings.getVictoryEffectRadius());
        section.set("victory-effect.interval-ticks", settings.getVictoryEffectIntervalTicks());
        section.set("victory-effect.spawn-height", settings.getVictoryEffectSpawnHeight());
        section.set("victory-effect.amount-per-wave", settings.getVictoryEffectAmountPerWave());
        section.set("victory-effect.block-material", settings.getVictoryEffectBlockMaterial().name());
        section.set("victory-commands", new ArrayList<String>(settings.getVictoryCommands()));
    }

    private ArenaSettings loadSettings(ConfigurationSection section) {
        ArenaSettings defaults = plugin.getPillarsConfig().createDefaultArenaSettings();
        if (section == null) {
            return defaults;
        }
        defaults.setCountdownSeconds(section.getInt("countdown-seconds", defaults.getCountdownSeconds()));
        defaults.setCageSeconds(section.getInt("cage-seconds", defaults.getCageSeconds()));
        defaults.setPreBorderDelaySeconds(section.getInt("pre-border-delay-seconds", defaults.getPreBorderDelaySeconds()));
        defaults.setBorderShrinkSeconds(section.getInt("border-shrink-seconds", defaults.getBorderShrinkSeconds()));
        defaults.setEndBorderDiameter(section.getDouble("end-border-diameter", defaults.getEndBorderDiameter()));
        defaults.setLavaEnabled(section.getBoolean("lava-enabled", defaults.isLavaEnabled()));
        defaults.setLavaStartDelaySeconds(section.getInt("lava-start-delay-seconds", defaults.getLavaStartDelaySeconds()));
        defaults.setLavaRiseIntervalSeconds(section.getInt("lava-rise-interval-seconds", defaults.getLavaRiseIntervalSeconds()));
        defaults.setPostShrinkEndDelaySeconds(section.getInt("post-shrink-end-delay-seconds", defaults.getPostShrinkEndDelaySeconds()));
        defaults.setMinPlayers(section.getInt("min-players", defaults.getMinPlayers()));
        defaults.setMinFilledTeams(section.getInt("min-filled-teams", defaults.getMinFilledTeams()));
        defaults.setAllowPlaceBlocks(section.getBoolean("allow-place-blocks", defaults.isAllowPlaceBlocks()));
        defaults.setAllowBreakOriginalBlocks(section.getBoolean("allow-break-original-blocks", defaults.isAllowBreakOriginalBlocks()));
        defaults.setAllowBreakPlayerBlocks(section.getBoolean("allow-break-player-blocks", defaults.isAllowBreakPlayerBlocks()));
        defaults.setAllowSmoothFall(section.getBoolean("allow-smooth-fall", defaults.isAllowSmoothFall()));
        defaults.setSmoothFallSeconds(section.getInt("smooth-fall-seconds", defaults.getSmoothFallSeconds()));
        defaults.setFriendlyFire(section.getBoolean("friendly-fire", defaults.isFriendlyFire()));
        defaults.setBlacklistMode(section.getBoolean("loot.blacklist-mode", defaults.isBlacklistMode()));
        defaults.setAllowEnchantedBooks(section.getBoolean("loot.allow-enchanted-books", defaults.isAllowEnchantedBooks()));
        defaults.setAllowPotions(section.getBoolean("loot.allow-potions", defaults.isAllowPotions()));
        defaults.setAllowTippedArrows(section.getBoolean("loot.allow-tipped-arrows", defaults.isAllowTippedArrows()));
        defaults.setAllowSpawnEggs(section.getBoolean("loot.allow-spawn-eggs", defaults.isAllowSpawnEggs()));
        defaults.setLootEnabled(section.getBoolean("loot.enabled", defaults.isLootEnabled()));
        defaults.setLootIntervalSeconds(section.getInt("loot.interval-seconds", defaults.getLootIntervalSeconds()));
        defaults.setLootWhitelist(section.getStringList("loot.whitelist"));
        defaults.setLootBlacklist(section.getStringList("loot.blacklist"));
        defaults.setAllowedCommands(section.getStringList("allowed-commands"));
        defaults.setCelebrationSeconds(section.getInt("celebration-seconds", defaults.getCelebrationSeconds()));
        defaults.setVictoryEffectType(VictoryEffectType.parse(section.getString("victory-effect.type"), defaults.getVictoryEffectType()));
        defaults.setVictoryEffectShape(VictoryEffectShape.parse(section.getString("victory-effect.shape"), defaults.getVictoryEffectShape()));
        defaults.setVictoryEffectRadius(section.getDouble("victory-effect.radius", defaults.getVictoryEffectRadius()));
        defaults.setVictoryEffectIntervalTicks(section.getInt("victory-effect.interval-ticks", defaults.getVictoryEffectIntervalTicks()));
        defaults.setVictoryEffectSpawnHeight(section.getDouble("victory-effect.spawn-height", defaults.getVictoryEffectSpawnHeight()));
        defaults.setVictoryEffectAmountPerWave(section.getInt("victory-effect.amount-per-wave", defaults.getVictoryEffectAmountPerWave()));
        defaults.setVictoryEffectBlockMaterial(parseMaterial(section.getString("victory-effect.block-material"), defaults.getVictoryEffectBlockMaterial()));
        if (section.contains("victory-commands")) {
            defaults.setVictoryCommands(section.getStringList("victory-commands"));
        }
        return defaults;
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            Material material = Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return material == null ? fallback : material;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
