package net.enelson.soppillars.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.match.RunningMatch;
import net.enelson.soppillars.match.WaitingMatch;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
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
        if (params == null) {
            return "";
        }
        String key = params.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) {
            return "";
        }

        if (key.startsWith("stats_")) {
            if (player == null) {
                return "";
            }
            return resolveStatsPlaceholder(player, key);
        }

        ArenaLookup namedArenaLookup = resolveNamedArenaLookup(key);
        if (namedArenaLookup != null) {
            return resolveArenaProperty(namedArenaLookup.arena, namedArenaLookup.propertyKey);
        }

        if (player == null) {
            return "";
        }

        UUID playerId = player.getUniqueId();

        WaitingMatch waiting = plugin.getMatchManager().getWaitingMatch(playerId);
        RunningMatch running = plugin.getMatchManager().getRunningMatch(playerId);
        PillarsArena arena = resolveArena(waiting, running);

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
            case "max_players":
                return asWholeString(resolveMaxPlayers(waiting, running));
            case "min_filled_teams":
                return asWholeString(resolveMinFilledTeams(waiting, running));
            default:
                if (key.startsWith("arena_") || key.startsWith("gameplay_")) {
                    return resolveArenaProperty(arena, key);
                }
                return null;
        }
    }

    private String resolveStatsPlaceholder(Player player, String key) {
        UUID playerId = player.getUniqueId();
        switch (key) {
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

    private ArenaLookup resolveNamedArenaLookup(String key) {
        ArenaLookup best = null;
        for (String arenaName : plugin.getArenaManager().getArenaNames()) {
            String normalizedName = arenaName == null ? "" : arenaName.trim().toLowerCase(Locale.ROOT);
            if (normalizedName.isEmpty()) {
                continue;
            }
            String prefix = normalizedName + "_";
            if (!key.startsWith(prefix) || key.length() <= prefix.length()) {
                continue;
            }
            PillarsArena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                continue;
            }
            String propertyKey = key.substring(prefix.length());
            if (best == null || normalizedName.length() > best.arenaNameLength) {
                best = new ArenaLookup(arena, propertyKey, normalizedName.length());
            }
        }
        return best;
    }

    private String resolveArenaProperty(PillarsArena arena, String propertyKey) {
        ArenaSettings settings = arena == null ? null : arena.getSettings();
        SerializedCuboid gameplayArea = arena == null ? null : arena.getGameplayArea();

        switch (propertyKey) {
            case "world":
            case "arena_world":
                return arena == null ? "" : arena.getWorldName();
            case "teams":
            case "arena_teams":
                return asWholeString(arena == null ? 0 : arena.getTeams());
            case "players_per_team":
            case "arena_players_per_team":
                return asWholeString(arena == null ? 0 : arena.getPlayersPerTeam());
            case "max_players":
            case "arena_max_players":
                return asWholeString(arena == null ? 0 : arena.getMaxPlayers());
            case "min_players":
            case "arena_min_players":
                return asWholeString(settings == null ? 0 : settings.getMinPlayers());
            case "min_filled_teams":
            case "arena_min_filled_teams":
                return asWholeString(settings == null ? 0 : settings.getMinFilledTeams());
            case "countdown_seconds":
            case "arena_countdown_seconds":
                return asWholeString(settings == null ? 0 : settings.getCountdownSeconds());
            case "cage_seconds":
            case "arena_cage_seconds":
                return asWholeString(settings == null ? 0 : settings.getCageSeconds());
            case "pre_border_delay_seconds":
            case "arena_pre_border_delay_seconds":
                return asWholeString(settings == null ? 0 : settings.getPreBorderDelaySeconds());
            case "border_shrink_seconds":
            case "arena_border_shrink_seconds":
                return asWholeString(settings == null ? 0 : settings.getBorderShrinkSeconds());
            case "end_border_diameter":
            case "arena_end_border_diameter":
                return asDecimalString(settings == null ? 0.0D : settings.getEndBorderDiameter());
            case "lava_enabled":
            case "arena_lava_enabled":
                return yesNo(settings != null && settings.isLavaEnabled());
            case "lava_start_delay_seconds":
            case "arena_lava_start_delay_seconds":
                return asWholeString(settings == null ? 0 : settings.getLavaStartDelaySeconds());
            case "lava_rise_interval_seconds":
            case "arena_lava_rise_interval_seconds":
                return asWholeString(settings == null ? 0 : settings.getLavaRiseIntervalSeconds());
            case "post_shrink_end_delay_seconds":
            case "arena_post_shrink_end_delay_seconds":
                return asWholeString(settings == null ? 0 : settings.getPostShrinkEndDelaySeconds());
            case "friendly_fire":
            case "arena_friendly_fire":
                return yesNo(settings != null && settings.isFriendlyFire());
            case "allow_place_blocks":
            case "arena_allow_place_blocks":
                return yesNo(settings != null && settings.isAllowPlaceBlocks());
            case "allow_break_original_blocks":
            case "arena_allow_break_original_blocks":
                return yesNo(settings != null && settings.isAllowBreakOriginalBlocks());
            case "allow_break_player_blocks":
            case "arena_allow_break_player_blocks":
                return yesNo(settings != null && settings.isAllowBreakPlayerBlocks());
            case "allow_smooth_fall":
            case "arena_allow_smooth_fall":
                return yesNo(settings != null && settings.isAllowSmoothFall());
            case "smooth_fall_seconds":
            case "arena_smooth_fall_seconds":
                return asWholeString(settings == null ? 0 : settings.getSmoothFallSeconds());
            case "loot_enabled":
            case "arena_loot_enabled":
                return yesNo(settings != null && settings.isLootEnabled());
            case "loot_interval_seconds":
            case "arena_loot_interval_seconds":
                return asWholeString(settings == null ? 0 : settings.getLootIntervalSeconds());
            case "celebration_seconds":
            case "arena_celebration_seconds":
                return asWholeString(settings == null ? 0 : settings.getCelebrationSeconds());
            case "victory_effect_shape":
            case "arena_victory_effect_shape":
                return settings == null || settings.getVictoryEffectShape() == null
                        ? ""
                        : settings.getVictoryEffectShape().name().toLowerCase(Locale.ROOT);
            case "victory_effect_radius":
            case "arena_victory_effect_radius":
                return asDecimalString(settings == null ? 0.0D : settings.getVictoryEffectRadius());
            case "victory_effect_interval_ticks":
            case "arena_victory_effect_interval_ticks":
                return asWholeString(settings == null ? 0 : settings.getVictoryEffectIntervalTicks());
            case "victory_effect_spawn_height":
            case "arena_victory_effect_spawn_height":
                return asDecimalString(settings == null ? 0.0D : settings.getVictoryEffectSpawnHeight());
            case "victory_effect_amount_per_wave":
            case "arena_victory_effect_amount_per_wave":
                return asWholeString(settings == null ? 0 : settings.getVictoryEffectAmountPerWave());
            case "gameplay_min_x":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMin().getX());
            case "gameplay_min_y":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMin().getY());
            case "gameplay_min_z":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMin().getZ());
            case "gameplay_max_x":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getX());
            case "gameplay_max_y":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getY());
            case "gameplay_max_z":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getZ());
            case "gameplay_size_x":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getX() - gameplayArea.getMin().getX());
            case "gameplay_size_y":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getY() - gameplayArea.getMin().getY());
            case "gameplay_size_z":
                return gameplayArea == null ? "0" : asDecimalString(gameplayArea.getMax().getZ() - gameplayArea.getMin().getZ());
            default:
                return null;
        }
    }

    private PillarsArena resolveArena(WaitingMatch waiting, RunningMatch running) {
        if (waiting != null) {
            return waiting.getArena();
        }
        if (running != null) {
            return running.getArena();
        }
        return null;
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

    private int resolveMaxPlayers(WaitingMatch waiting, RunningMatch running) {
        if (waiting != null) {
            return waiting.getArena().getMaxPlayers();
        }
        if (running != null) {
            return running.getArena().getMaxPlayers();
        }
        return 0;
    }

    private String asWholeString(int value) {
        return Integer.toString(value);
    }

    private String asDecimalString(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001D) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static final class ArenaLookup {
        private final PillarsArena arena;
        private final String propertyKey;
        private final int arenaNameLength;

        private ArenaLookup(PillarsArena arena, String propertyKey, int arenaNameLength) {
            this.arena = arena;
            this.propertyKey = propertyKey;
            this.arenaNameLength = arenaNameLength;
        }
    }
}
