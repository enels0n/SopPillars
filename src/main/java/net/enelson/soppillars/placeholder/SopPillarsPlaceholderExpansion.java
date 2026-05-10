package net.enelson.soppillars.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.RunningMatch;
import net.enelson.soppillars.match.WaitingMatch;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class SopPillarsPlaceholderExpansion extends PlaceholderExpansion {

    private final SopPillarsPlugin plugin;

    public SopPillarsPlaceholderExpansion(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "soppillars";
    }

    @Override
    public String getAuthor() {
        return "E_NeLsOn";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return "";
        }
        String key = params.trim().toLowerCase(Locale.ROOT);
        UUID playerId = player.getUniqueId();

        WaitingMatch waiting = plugin.getMatchManager().getWaitingMatch(playerId);
        RunningMatch running = plugin.getMatchManager().getRunningMatch(playerId);

        switch (key) {
            case "in_game":
                return (waiting != null || running != null) ? "yes" : "no";
            case "game_status":
                return resolveGameStatus(player, waiting, running);
            case "arena":
                return plugin.getMatchManager().getTrackedArenaName(playerId);
            case "mode":
                if (waiting != null) {
                    return waiting.getArena().getMode();
                }
                if (running != null) {
                    return running.getArena().getMode();
                }
                return "";
            case "team":
                if (waiting != null) {
                    return String.valueOf(waiting.getTeam(playerId));
                }
                if (running != null) {
                    return String.valueOf(running.getTeam(playerId));
                }
                return "0";
            case "alive":
                return (running != null && running.isAlive(playerId)) ? "yes" : "no";
            case "countdown":
                if (waiting == null) {
                    return "0";
                }
                return String.valueOf(Math.max(0, waiting.getCountdownRemaining()));
            case "alive_players":
                return running == null ? "0" : String.valueOf(running.getAliveCount());
            case "players_total":
                if (waiting != null) {
                    return String.valueOf(waiting.size());
                }
                if (running != null) {
                    return String.valueOf(running.getPlayers().size());
                }
                return "0";
            case "stats_games":
                return String.valueOf(plugin.getStatistics().getInt("games", playerId));
            case "stats_wins":
                return String.valueOf(plugin.getStatistics().getInt("wins", playerId));
            case "stats_kills":
                return String.valueOf(plugin.getStatistics().getInt("kills", playerId));
            case "stats_deaths":
                return String.valueOf(plugin.getStatistics().getInt("deaths", playerId));
            default:
                return null;
        }
    }

    private String resolveGameStatus(Player player, WaitingMatch waiting, RunningMatch running) {
        if (waiting != null) {
            return waiting.hasCountdown() ? "starting" : "waiting";
        }
        if (running == null) {
            return "none";
        }
        if (plugin.getMatchManager().isEndingWinnerProtected(player)) {
            return "winner";
        }
        if (!running.isAlive(player.getUniqueId())) {
            return "spectator";
        }
        return "running";
    }
}
