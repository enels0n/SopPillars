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
                    return asWholeString(waiting.getTeam(playerId));
                }
                if (running != null) {
                    return asWholeString(running.getTeam(playerId));
                }
                return "0";
            case "alive":
                return (running != null && running.isAlive(playerId)) ? "yes" : "no";
            case "countdown":
                if (waiting == null) {
                    return "0";
                }
                return asWholeString(Math.max(0, waiting.getCountdownRemaining()));
            case "alive_players":
                return running == null ? "0" : asWholeString(running.getAliveCount());
            case "players_total":
                if (waiting != null) {
                    return asWholeString(waiting.size());
                }
                if (running != null) {
                    return asWholeString(running.getPlayers().size());
                }
                return "0";
            case "loot_in":
            case "next_loot_in":
                return asWholeString(plugin.getMatchManager().getSecondsUntilNextLoot(playerId));
            case "time_to_end":
            case "game_end_in":
                return asWholeString(plugin.getMatchManager().getSecondsUntilGameEnd(playerId));
            case "min_players":
                return asWholeString(resolveMinPlayers(waiting, running));
            case "min_filled_teams":
                return asWholeString(resolveMinFilledTeams(waiting, running));
            case "stats_games":
                return asWholeString(plugin.getStatistics().getInt("games", playerId));
            case "stats_wins":
                return asWholeString(plugin.getStatistics().getInt("wins", playerId));
            case "stats_kills":
                return asWholeString(plugin.getStatistics().getInt("kills", playerId));
            case "stats_deaths":
                return asWholeString(plugin.getStatistics().getInt("deaths", playerId));
            case "stats_winstreak":
                return asWholeString(plugin.getStatistics().getInt("winstreak", playerId));
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

    private int resolveMinPlayers(WaitingMatch waiting, RunningMatch running) {
        if (waiting != null) {
            return waiting.getArena().getSettings().getMinPlayers();
        }
        if (running != null) {
            return running.getArena().getSettings().getMinPlayers();
        }
        return plugin.getConfig().getInt("settings.default-min-players", 2);
    }

    private int resolveMinFilledTeams(WaitingMatch waiting, RunningMatch running) {
        if (waiting != null) {
            return waiting.getArena().getSettings().getMinFilledTeams();
        }
        if (running != null) {
            return running.getArena().getSettings().getMinFilledTeams();
        }
        return plugin.getConfig().getInt("settings.default-min-filled-teams", 2);
    }

    private String asWholeString(int value) {
        return Integer.toString(value);
    }
}
