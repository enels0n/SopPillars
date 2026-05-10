package net.enelson.soppillars.stats;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.RunningMatch;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persistent player counters (games played, wins, kills, deaths).
 */
public final class PlayerStatisticsManager {

    private final SopPillarsPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PlayerStatisticsManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not create stats.yml: " + exception.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save stats.yml: " + exception.getMessage());
        }
    }

    public void recordMatchFinished(RunningMatch match, int winningTeam) {
        boolean changed = false;
        for (UUID playerId : match.getPlayers()) {
            String key = playerId.toString();
            addInt("games", key, 1);
            changed = true;
            if (winningTeam > 0 && match.getTeam(playerId) == winningTeam) {
                addInt("wins", key, 1);
            }
        }
        if (changed) {
            save();
        }
    }

    public void recordKill(UUID killer) {
        if (killer == null) {
            return;
        }
        addInt("kills", killer.toString(), 1);
        save();
    }

    public void recordDeath(UUID victim) {
        if (victim == null) {
            return;
        }
        addInt("deaths", victim.toString(), 1);
        save();
    }

    public int getInt(String category, UUID playerId) {
        return config.getInt(category + "." + playerId.toString(), 0);
    }

    private void addInt(String category, String uuidKey, int delta) {
        String path = category + "." + uuidKey;
        config.set(path, config.getInt(path, 0) + delta);
    }
}
