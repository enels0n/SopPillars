package net.enelson.soppillars.match;

import net.enelson.sopparty.api.SopPartyApi;
import net.enelson.sopparty.api.SopPartyServices;
import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.ArenaState;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.cage.CapturedBlockState;
import net.enelson.soppillars.loot.LootGenerator;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.SerializedLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class MatchManager {

    private static final String SOPPILLARS_RESERVATION_PREFIX = "soppillars:";
    private static final String RANDOM_RESERVATION_BLOCKED = "__reservation_blocked__";
    private static final long LOBBY_BOUNDS_TELEPORT_COOLDOWN_MS = 750L;
    private static final int TEAM_SELECTOR_SLOT_DEFAULT = 4;
    private static final int LEAVE_ARENA_SLOT_DEFAULT = 8;
    private static final String TEAM_SELECTOR_NAME_DEFAULT = ChatColor.GREEN + "Team Selector";
    private static final String LEAVE_ARENA_NAME_DEFAULT = ChatColor.RED + "Leave Arena";
    private static final List<String> TEAM_SELECTOR_LORE_DEFAULT = Arrays.asList(
            ChatColor.GRAY + "Right click to choose your team.",
            ChatColor.DARK_GRAY + "Waiting lobby only"
    );
    private static final List<String> LEAVE_ARENA_LORE_DEFAULT = Arrays.asList(
            ChatColor.GRAY + "Right click to leave waiting queue.",
            ChatColor.DARK_GRAY + "Waiting lobby only"
    );
    private static final String TEAM_SELECTOR_TITLE = ChatColor.DARK_GREEN + "Choose Team";
    private static final String PDC_LOBBY_ITEM_KEY = "lobby_item";
    private static final String TEAM_SELECTOR_MARKER = "team_selector";
    private static final String LEAVE_ARENA_MARKER = "leave_arena";

    private final SopPillarsPlugin plugin;
    private final NamespacedKey lobbyItemKey;
    private final Map<String, WaitingMatch> waitingMatches = new LinkedHashMap<String, WaitingMatch>();
    private final Map<String, RunningMatch> runningMatches = new LinkedHashMap<String, RunningMatch>();
    private final Map<String, RunningMatchEffects> runningEffects = new LinkedHashMap<String, RunningMatchEffects>();
    private final Map<UUID, String> arenaByPlayer = new HashMap<UUID, String>();
    private final Map<UUID, String> respawnArenaByPlayer = new HashMap<UUID, String>();
    private final Map<UUID, SavedPlayerState> savedStateByPlayer = new HashMap<UUID, SavedPlayerState>();
    private final Map<UUID, Long> lastLobbyBoundsTeleportAt = new HashMap<UUID, Long>();
    private BukkitTask tickerTask;

    public MatchManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
        this.lobbyItemKey = new NamespacedKey(plugin, PDC_LOBBY_ITEM_KEY);
    }

    public void reset() {
        if (tickerTask != null) {
            tickerTask.cancel();
            tickerTask = null;
        }
        clearSopPartyReservationsForTrackedPlayers();
        for (RunningMatchEffects effects : new ArrayList<RunningMatchEffects>(runningEffects.values())) {
            effects.cleanup();
        }
        runningEffects.clear();
        for (RunningMatch match : new ArrayList<RunningMatch>(runningMatches.values())) {
            match.restoreHiddenLobbyBlocks();
        }
        waitingMatches.clear();
        runningMatches.clear();
        arenaByPlayer.clear();
        respawnArenaByPlayer.clear();
        lastLobbyBoundsTeleportAt.clear();
        restoreAllOnlineSavedStates();
        savedStateByPlayer.clear();
    }

    public void startTicker() {
        if (tickerTask != null) {
            tickerTask.cancel();
        }
        tickerTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                tickWaitingMatches();
                tickRunningEffects();
            }
        }, 20L, 20L);
    }

    public boolean joinArena(Player player, PillarsArena arena) {
        List<UUID> party = resolvePartyJoinGroup(player);
        if (party.isEmpty()) {
            return false;
        }
        for (UUID memberId : party) {
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                plugin.getMessageService().send(player, "party-member-offline");
                return false;
            }
            if (!member.isOp() && !member.hasPermission("soppillars.play")) {
                plugin.getMessageService().send(player, "party-member-no-permission", replacement("player", member.getName()));
                return false;
            }
        }

        if (!canActAsPartyJoinLeader(player)) {
            return false;
        }

        String normalizedArenaName = normalize(arena.getName());
        if (hasConflictingSopPartyReservation(player, normalizedArenaName)) {
            return false;
        }

        if (arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) {
            plugin.getMessageService().send(player, "arena-not-joinable", replacement("arena", arena.getName()));
            return false;
        }
        if (arena.getLobbyArea() == null) {
            plugin.getMessageService().send(player, "arena-no-lobby", replacement("arena", arena.getName()));
            return false;
        }

        for (UUID memberId : party) {
            if (arenaByPlayer.containsKey(memberId)) {
                Player member = Bukkit.getPlayer(memberId);
                if (member != null) {
                    if (normalizedArenaName.equals(arenaByPlayer.get(memberId))) {
                        plugin.getMessageService().send(member, "already-in-arena", replacement("arena", arena.getName()));
                    } else {
                        plugin.getMessageService().send(member, "already-in-other-arena");
                    }
                }
                return false;
            }
        }

        WaitingMatch match = getOrCreateWaitingMatch(arena);
        if (match.size() + party.size() > arena.getMaxPlayers()) {
            plugin.getMessageService().send(player, "arena-full", replacement("arena", arena.getName()));
            return false;
        }

        Map<UUID, Integer> assignment = assignPartyToTeams(arena, match, party);
        if (assignment == null) {
            plugin.getMessageService().send(player, "party-does-not-fit");
            return false;
        }

        for (Map.Entry<UUID, Integer> entry : assignment.entrySet()) {
            UUID memberId = entry.getKey();
            int team = entry.getValue().intValue();
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) {
                continue;
            }
            snapshotPlayerState(member);
            match.addPlayer(memberId, team);
            arenaByPlayer.put(memberId, normalizedArenaName);
            teleportToLobby(member, arena);
            clearPlayerForWaitingLobby(member);
            giveTeamSelector(member);
            member.setGameMode(GameMode.SURVIVAL);
            plugin.getMessageService().send(member, "joined-arena", replacements(
                    "arena", arena.getName(),
                    "team", String.valueOf(team)
            ));
        }
        setSopPartyReservation(player, normalizedArenaName);
        return true;
    }

    public boolean joinRandom(Player player, List<String> requestedModes) {
        if (!canActAsPartyJoinLeader(player)) {
            return false;
        }
        List<UUID> partyGroup = resolvePartyJoinGroup(player);
        int partySize = partyGroup.size();
        String reservationArenaLock = resolveRandomReservationArenaLock(player);
        if (RANDOM_RESERVATION_BLOCKED.equals(reservationArenaLock)) {
            return false;
        }

        List<PillarsArena> preferredWaiting = new ArrayList<PillarsArena>();
        List<PillarsArena> preferredEmpty = new ArrayList<PillarsArena>();

        for (PillarsArena arena : plugin.getArenaManager().getArenas()) {
            if ((arena.getState() != ArenaState.WAITING && arena.getState() != ArenaState.STARTING) || arena.getLobbyArea() == null) {
                continue;
            }
            if (!requestedModes.isEmpty() && !containsMode(requestedModes, arena.getMode())) {
                continue;
            }
            if (reservationArenaLock != null && !reservationArenaLock.equals(normalize(arena.getName()))) {
                continue;
            }
            if (!canFitPartyGroup(arena, partySize)) {
                continue;
            }
            if (!canAssignPartyGroup(arena, partyGroup)) {
                continue;
            }
            WaitingMatch match = waitingMatches.get(normalize(arena.getName()));
            if (match != null && !match.isEmpty() && match.canJoin()) {
                preferredWaiting.add(arena);
            } else if (match == null || match.canJoin()) {
                preferredEmpty.add(arena);
            }
        }

        PillarsArena selected = pickRandom(preferredWaiting);
        if (selected == null) {
            selected = pickRandom(preferredEmpty);
        }
        if (selected == null) {
            plugin.getMessageService().send(player, "no-random-arena");
            return false;
        }
        return joinArena(player, selected);
    }

    /**
     * For /pillars random: if reservation points to another game, block immediately; if reservation
     * points to SopPillars, lock random selection to that specific arena key.
     */
    private String resolveRandomReservationArenaLock(Player player) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return null;
        }
        Optional<String> reservation = api.getPartyReservationGameKey(player);
        if (!reservation.isPresent()) {
            return null;
        }
        String key = reservation.get().trim();
        if (key.isEmpty()) {
            return null;
        }
        if (!key.startsWith(SOPPILLARS_RESERVATION_PREFIX)) {
            plugin.getMessageService().send(player, "party-reserved-elsewhere", replacement("reservation", key));
            return RANDOM_RESERVATION_BLOCKED;
        }
        String rawArena = key.substring(SOPPILLARS_RESERVATION_PREFIX.length()).trim();
        if (rawArena.isEmpty()) {
            return null;
        }
        return normalize(rawArena);
    }

    public void leaveArena(Player player, boolean dueToQuit) {
        String arenaName = arenaByPlayer.remove(player.getUniqueId());
        if (arenaName == null) {
            if (!dueToQuit) {
                plugin.getMessageService().send(player, "not-in-arena");
            }
            return;
        }

        WaitingMatch waitingMatch = waitingMatches.get(arenaName);
        if (waitingMatch != null) {
            waitingMatch.removePlayer(player.getUniqueId());
            if (waitingMatch.isEmpty()) {
                waitingMatches.remove(arenaName);
            }
            cleanupLobbyState(player);
            restorePlayerState(player);
            teleportToGlobalSpawn(player);
            if (!dueToQuit) {
                plugin.getMessageService().send(player, "left-arena");
            } else {
                plugin.getMessageService().send(player, "left-waiting-disconnect");
            }
            clearSopPartyReservationIfAbandoned(player, arenaName);
            return;
        }

        RunningMatch runningMatch = runningMatches.get(arenaName);
        if (runningMatch != null) {
            boolean wasAlive = runningMatch.isAlive(player.getUniqueId());
            runningMatch.setAlive(player.getUniqueId(), false);
            respawnArenaByPlayer.remove(player.getUniqueId());
            cleanupRunningState(player);
            if (!dueToQuit) {
                restorePlayerState(player);
                teleportToGlobalSpawn(player);
                plugin.getMessageService().send(player, "match-forfeit", replacement("arena", runningMatch.getArena().getName()));
            } else if (wasAlive) {
                plugin.getMessageService().send(player, "quit-match-alive-disconnect", replacement("arena", runningMatch.getArena().getName()));
            } else {
                plugin.getMessageService().send(player, "quit-match-spectator-disconnect", replacement("arena", runningMatch.getArena().getName()));
            }
            checkForWinner(runningMatch);
            clearSopPartyReservationIfAbandoned(player, arenaName);
        }
    }

    public void handleQuit(Player player) {
        if (arenaByPlayer.containsKey(player.getUniqueId())) {
            leaveArena(player, true);
        }
    }

    public void handleJoin(Player player) {
        if (player == null) {
            return;
        }
        if (arenaByPlayer.containsKey(player.getUniqueId())) {
            return;
        }
        restorePlayerState(player);
    }

    /**
     * {@code true} if the player is still an alive participant in a running match (elimination applies).
     */
    public boolean isArenaEliminationCandidate(Player victim) {
        RunningMatch match = getRunningMatch(victim.getUniqueId());
        return match != null && match.isAlive(victim.getUniqueId());
    }

    /**
     * Elimination without vanilla death: lethal {@link EntityDamageEvent} is cancelled before this call; loot is dropped manually.
     */
    public void eliminateFromLethalDamage(Player victim, EntityDamageEvent lethalEvent) {
        RunningMatch match = getRunningMatch(victim.getUniqueId());
        if (match == null || !match.isAlive(victim.getUniqueId())) {
            return;
        }
        Player killerHint = null;
        if (lethalEvent instanceof EntityDamageByEntityEvent) {
            killerHint = DamageResolver.resolvePlayerAttacker(((EntityDamageByEntityEvent) lethalEvent).getDamager());
        }
        DeathBroadcastResolver.DeathContext ctx = DeathBroadcastResolver.resolve(victim, killerHint, lethalEvent);
        finishEliminationCore(victim, match, ctx);
        MatchEliminationDrops.dropInventoryAndExperience(victim);
        victim.setFireTicks(0);
        victim.setFallDistance(0.0F);
        transitionEliminatedToSpectatorImmediate(victim, match);
    }

    /**
     * Fallback when the player still dies (e.g. {@code /kill}): vanilla already dropped loot — only stats and spectator flow.
     */
    public void eliminateAfterVanillaDeath(Player victim) {
        RunningMatch match = getRunningMatch(victim.getUniqueId());
        if (match == null || !match.isAlive(victim.getUniqueId())) {
            return;
        }
        EntityDamageEvent last = victim.getLastDamageCause();
        DeathBroadcastResolver.DeathContext ctx = DeathBroadcastResolver.resolve(victim, victim.getKiller(), last);
        finishEliminationCore(victim, match, ctx);
        respawnArenaByPlayer.put(victim.getUniqueId(), normalize(match.getArena().getName()));
    }

    private void finishEliminationCore(Player victim, RunningMatch match, DeathBroadcastResolver.DeathContext ctx) {
        plugin.getStatistics().recordDeath(victim.getUniqueId());
        if (ctx.getAttackingPlayer() != null) {
            plugin.getStatistics().recordKill(ctx.getAttackingPlayer().getUniqueId());
        }
        match.setAlive(victim.getUniqueId(), false);
        broadcast(match, ctx.getMessageKey(), deathReplacements(victim, match, ctx));
        if (match.getAlivePlayersInTeam(match.getTeam(victim.getUniqueId())) == 0) {
            broadcast(match, "team-eliminated", replacements(
                    "team", String.valueOf(match.getTeam(victim.getUniqueId())),
                    "arena", match.getArena().getName()
            ));
        }
        checkForWinner(match);
    }

    private void transitionEliminatedToSpectatorImmediate(Player victim, RunningMatch match) {
        final SerializedLocation spectatorSpawn = match.getArena().getSpectatorSpawn();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (spectatorSpawn != null) {
                    World world = Bukkit.getWorld(spectatorSpawn.getWorld());
                    if (world != null && victim.isOnline()) {
                        victim.teleport(spectatorSpawn.toLocation(world));
                    }
                }
                if (victim.isOnline()) {
                    cleanupRunningState(victim);
                    plugin.getMessageService().send(victim, "became-spectator");
                }
            }
        });
    }

    private Map<String, String> deathReplacements(Player victim, RunningMatch match, DeathBroadcastResolver.DeathContext ctx) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("player", victim.getName());
        values.put("arena", match.getArena().getName());
        values.put("team", String.valueOf(match.getTeam(victim.getUniqueId())));
        Player killer = ctx.getAttackingPlayer();
        values.put("killer", killer != null ? killer.getName() : "");
        String mob = ctx.getMobName();
        values.put("mob", mob != null ? mob : "");
        return values;
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        String arenaName = respawnArenaByPlayer.remove(event.getPlayer().getUniqueId());
        if (arenaName == null) {
            return;
        }
        RunningMatch match = runningMatches.get(arenaName);
        if (match == null) {
            return;
        }
        SerializedLocation spectatorSpawn = match.getArena().getSpectatorSpawn();
        if (spectatorSpawn != null) {
            World world = Bukkit.getWorld(spectatorSpawn.getWorld());
            if (world != null) {
                event.setRespawnLocation(spectatorSpawn.toLocation(world));
            }
        }
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                cleanupRunningState(player);
                player.setGameMode(GameMode.SPECTATOR);
                plugin.getMessageService().send(player, "became-spectator");
            }
        });
    }

    public boolean isCommandAllowed(Player player, String message) {
        RunningMatch match = getRunningMatch(player.getUniqueId());
        if (match == null || player.hasPermission("soppillars.admin") || player.isOp()) {
            return true;
        }
        String commandName = extractCommandName(message);
        for (String allowed : match.getArena().getSettings().getAllowedCommands()) {
            if (allowed.equalsIgnoreCase(commandName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFriendlyFireBlocked(Player victim, Player attacker) {
        RunningMatch match = getRunningMatch(victim.getUniqueId());
        if (match == null || match != getRunningMatch(attacker.getUniqueId())) {
            return false;
        }
        return match.getTeam(victim.getUniqueId()) == match.getTeam(attacker.getUniqueId());
    }

    public boolean isManagedChatPlayer(Player player) {
        return getPlayerWaitingMatch(player.getUniqueId()) != null || getRunningMatch(player.getUniqueId()) != null;
    }

    public void routeChat(Player sender, String message) {
        WaitingMatch waitingMatch = getPlayerWaitingMatch(sender.getUniqueId());
        if (waitingMatch != null) {
            String formatted = formatChat(
                    plugin.getMessageService().get("chat.waiting-format"),
                    sender,
                    message,
                    "WAIT"
            );
            for (UUID playerId : waitingMatch.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(formatted);
                }
            }
            return;
        }

        RunningMatch runningMatch = getRunningMatch(sender.getUniqueId());
        if (runningMatch == null) {
            return;
        }

        boolean senderAlive = runningMatch.isAlive(sender.getUniqueId());
        String formatted;
        if (runningMatch.getArena().getState() == ArenaState.ENDING) {
            formatted = formatChat(
                    plugin.getMessageService().get("chat.celebration-format"),
                    sender,
                    message,
                    "WIN"
            );
        } else {
            String formatKey = senderAlive ? "chat.alive-format" : "chat.spectator-format";
            formatted = formatChat(
                    plugin.getMessageService().get(formatKey),
                    sender,
                    message,
                    senderAlive ? "ALIVE" : "SPEC"
            );
        }

        for (UUID playerId : runningMatch.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            boolean receiverAlive = runningMatch.isAlive(playerId);
            if (runningMatch.getArena().getState() == ArenaState.ENDING) {
                player.sendMessage(formatted);
                continue;
            }
            if (senderAlive) {
                if (receiverAlive) {
                    player.sendMessage(formatted);
                }
            } else {
                player.sendMessage(formatted);
            }
        }
    }

    public boolean isLobbyProtected(Player player) {
        WaitingMatch waitingMatch = getPlayerWaitingMatch(player.getUniqueId());
        return waitingMatch != null;
    }

    public void enforceArenaBounds(Player player) {
        WaitingMatch waitingMatch = getPlayerWaitingMatch(player.getUniqueId());
        if (waitingMatch != null) {
            SerializedCuboid lobbyArea = waitingMatch.getArena().getLobbyArea();
            if (lobbyArea != null && !lobbyArea.contains(player.getLocation())) {
                if (canTeleportBackToLobbyNow(player.getUniqueId())) {
                    teleportToLobby(player, waitingMatch.getArena());
                }
                plugin.getMessageService().send(player, "returned-to-lobby");
            }
            return;
        }

        RunningMatch runningMatch = getRunningMatch(player.getUniqueId());
        if (runningMatch == null) {
            return;
        }
        SerializedCuboid gameplayArea = runningMatch.getArena().getGameplayArea();
        SerializedCuboid lobbyArea = runningMatch.getArena().getLobbyArea();
        Location location = player.getLocation();
        if (runningMatch.isAlive(player.getUniqueId())) {
            if (gameplayArea == null) {
                return;
            }
            double minY = gameplayArea.getMin().getY();
            if (location.getY() < minY) {
                if (isEndingWinnerProtected(player)) {
                    teleportToSerializedLocation(player, runningMatch.getArena().getSpectatorSpawn());
                    player.setFallDistance(0.0F);
                    player.setFireTicks(0);
                } else {
                    player.damage(1000.0D);
                }
            }
            return;
        }
        boolean inGameplay = gameplayArea != null && gameplayArea.contains(location);
        boolean inLobby = lobbyArea != null && lobbyArea.contains(location);
        if (inGameplay || inLobby) {
            return;
        }
        SerializedLocation spectatorSpawn = runningMatch.getArena().getSpectatorSpawn();
        if (spectatorSpawn != null) {
            teleportToSerializedLocation(player, spectatorSpawn);
            plugin.getMessageService().send(player, "spectator-bounds");
        }
    }

    public void openTeamSelector(Player player) {
        WaitingMatch match = getPlayerWaitingMatch(player.getUniqueId());
        if (match == null) {
            return;
        }
        Inventory inventory = Bukkit.createInventory(null, 9, TEAM_SELECTOR_TITLE);
        for (int team = 1; team <= match.getArena().getTeams(); team++) {
            inventory.setItem(team - 1, createTeamItem(match, team));
        }
        player.openInventory(inventory);
    }

    public void handleTeamSelectorClick(Player player, int slot) {
        WaitingMatch match = getPlayerWaitingMatch(player.getUniqueId());
        if (match == null) {
            player.closeInventory();
            return;
        }
        int team = slot + 1;
        if (team < 1 || team > match.getArena().getTeams()) {
            return;
        }
        if (!match.hasFreeSlotInTeam(team) && match.getTeam(player.getUniqueId()) != team) {
            plugin.getMessageService().send(player, "team-full");
            return;
        }
        match.addPlayer(player.getUniqueId(), team);
        plugin.getMessageService().send(player, "team-selected", replacements(
                "arena", match.getArena().getName(),
                "team", String.valueOf(team)
        ));
        openTeamSelector(player);
    }

    public boolean isTeamSelectorItem(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String marker = meta.getPersistentDataContainer().get(lobbyItemKey, PersistentDataType.STRING);
        if (TEAM_SELECTOR_MARKER.equals(marker)) {
            return true;
        }
        // Backward compatibility with old items issued before PDC marker rollout.
        return TEAM_SELECTOR_NAME_DEFAULT.equals(meta.getDisplayName());
    }

    public boolean isLeaveArenaItem(ItemStack item) {
        if (item == null || item.getType() != Material.BARRIER || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String marker = meta.getPersistentDataContainer().get(lobbyItemKey, PersistentDataType.STRING);
        if (LEAVE_ARENA_MARKER.equals(marker)) {
            return true;
        }
        // Backward compatibility with old items issued before PDC marker rollout.
        return LEAVE_ARENA_NAME_DEFAULT.equals(meta.getDisplayName());
    }

    public boolean isTeamSelectorInventory(String title) {
        return TEAM_SELECTOR_TITLE.equals(title);
    }

    public SopPillarsPlugin getPlugin() {
        return plugin;
    }

    /**
     * Returns true if another active running match already uses this world (excluding {@code exceptArenaName}).
     */
    public boolean hasOtherRunningMatchInSameWorld(String worldName, String exceptArenaName) {
        String except = normalize(exceptArenaName);
        for (RunningMatch other : runningMatches.values()) {
            if (normalize(other.getArena().getName()).equals(except)) {
                continue;
            }
            if (other.getArena().getWorldName().equalsIgnoreCase(worldName)) {
                return true;
            }
        }
        return false;
    }

    public void grantLootTick(RunningMatch match) {
        PillarsArena arena = match.getArena();
        if (arena.getState() != ArenaState.RUNNING) {
            return;
        }
        ArenaSettings settings = arena.getSettings();
        Random random = LootGenerator.threadLocalRandom();
        List<Material> pool = null;
        if (settings.isBlacklistMode()) {
            pool = match.getBlacklistLootPool();
            if (pool == null || pool.isEmpty()) {
                pool = LootGenerator.buildBlacklistPool(settings);
            }
        }
        for (UUID playerId : match.getPlayers()) {
            if (!match.isAlive(playerId)) {
                continue;
            }
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                continue;
            }
            ItemStack item = LootGenerator.roll(settings, pool, random);
            if (item != null && item.getType() != Material.AIR) {
                p.getInventory().addItem(item);
            }
        }
    }

    private boolean canFitPartyGroup(PillarsArena arena, int partySize) {
        WaitingMatch wm = waitingMatches.get(normalize(arena.getName()));
        int cur = wm == null ? 0 : wm.size();
        return cur + partySize <= arena.getMaxPlayers();
    }

    private boolean canAssignPartyGroup(PillarsArena arena, List<UUID> partyGroup) {
        WaitingMatch wm = waitingMatches.get(normalize(arena.getName()));
        WaitingMatch basis = wm != null ? wm : new WaitingMatch(arena);
        return assignPartyToTeams(arena, basis, partyGroup) != null;
    }

    /**
     * Defensive cleanup for admin reload/disable paths where waiting/running matches are dropped in-memory.
     */
    private void clearSopPartyReservationsForTrackedPlayers() {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return;
        }
        Set<UUID> leadersCleared = new HashSet<UUID>();
        for (UUID playerId : arenaByPlayer.keySet()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                continue;
            }
            if (!api.isInParty(p)) {
                continue;
            }
            UUID leaderId = api.getLeaderUuid(p).orElse(playerId);
            if (!leadersCleared.add(leaderId)) {
                continue;
            }
            Player leader = Bukkit.getPlayer(leaderId);
            if (leader != null) {
                api.sendReservationRequest(leader, "");
            }
        }
    }

    private boolean canActAsPartyJoinLeader(Player player) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return true;
        }
        List<UUID> party = dedupeParty(plugin.getPartyBridge().getMemberUuids(player));
        if (party.size() <= 1) {
            return true;
        }
        if (api.isInParty(player) && !api.isLeader(player)) {
            plugin.getMessageService().send(player, "party-join-not-leader");
            return false;
        }
        return true;
    }

    private boolean hasConflictingSopPartyReservation(Player player, String normalizedArenaName) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return false;
        }
        Optional<String> reservation = api.getPartyReservationGameKey(player);
        if (!reservation.isPresent()) {
            return false;
        }
        String key = reservation.get().trim();
        if (key.isEmpty()) {
            return false;
        }
        String expected = SOPPILLARS_RESERVATION_PREFIX + normalizedArenaName;
        if (key.equals(expected)) {
            return false;
        }
        plugin.getMessageService().send(player, "party-reserved-elsewhere", replacement("reservation", key));
        return true;
    }

    private void setSopPartyReservation(Player actor, String normalizedArenaName) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return;
        }
        if (!api.isInParty(actor)) {
            return;
        }
        UUID leaderId = api.getLeaderUuid(actor).orElse(actor.getUniqueId());
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader != null) {
            api.sendReservationRequest(leader, SOPPILLARS_RESERVATION_PREFIX + normalizedArenaName);
        }
    }

    /**
     * Clears proxy reservation when no member of {@code leaver}'s party remains in arena {@code normalizedArenaName}.
     */
    private void clearSopPartyReservationIfAbandoned(Player leaver, String normalizedArenaName) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return;
        }
        if (!api.isInParty(leaver)) {
            return;
        }
        List<UUID> party = dedupeParty(plugin.getPartyBridge().getMemberUuids(leaver));
        for (UUID memberId : party) {
            if (memberId.equals(leaver.getUniqueId())) {
                continue;
            }
            if (normalizedArenaName.equals(arenaByPlayer.get(memberId))) {
                return;
            }
        }
        UUID leaderId = api.getLeaderUuid(leaver).orElse(leaver.getUniqueId());
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader != null) {
            api.sendReservationRequest(leader, "");
        }
    }

    private void clearSopPartyReservationsForMatch(RunningMatch match) {
        SopPartyApi api = SopPartyServices.get();
        if (api == null) {
            return;
        }
        Set<UUID> leadersCleared = new HashSet<UUID>();
        for (UUID playerId : match.getPlayers()) {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) {
                continue;
            }
            if (!api.isInParty(p)) {
                continue;
            }
            UUID leaderId = api.getLeaderUuid(p).orElse(playerId);
            if (!leadersCleared.add(leaderId)) {
                continue;
            }
            Player leader = Bukkit.getPlayer(leaderId);
            if (leader != null) {
                api.sendReservationRequest(leader, "");
            }
        }
    }

    private List<UUID> dedupeParty(List<UUID> raw) {
        return new ArrayList<UUID>(new LinkedHashSet<UUID>(raw));
    }

    private List<UUID> resolvePartyJoinGroup(Player player) {
        List<UUID> party = dedupeParty(plugin.getPartyBridge().getMemberUuids(player));
        if (party.isEmpty()) {
            return Collections.singletonList(player.getUniqueId());
        }
        if (!party.contains(player.getUniqueId())) {
            party.add(player.getUniqueId());
        }
        SopPartyApi api = SopPartyServices.get();
        if (api != null && !api.isInParty(player)) {
            return Collections.singletonList(player.getUniqueId());
        }
        return party;
    }

    private Map<UUID, Integer> assignPartyToTeams(PillarsArena arena, WaitingMatch match, List<UUID> party) {
        int teams = arena.getTeams();
        int ppt = arena.getPlayersPerTeam();
        int[] used = new int[teams + 1];
        for (int t = 1; t <= teams; t++) {
            used[t] = match.getTeamSize(t);
        }

        for (int t = 1; t <= teams; t++) {
            if (ppt - used[t] >= party.size()) {
                Map<UUID, Integer> single = new LinkedHashMap<UUID, Integer>();
                for (UUID id : party) {
                    single.put(id, Integer.valueOf(t));
                }
                return single;
            }
        }

        List<UUID> remaining = new ArrayList<UUID>(party);
        Map<UUID, Integer> assign = new LinkedHashMap<UUID, Integer>();
        while (!remaining.isEmpty()) {
            boolean progressed = false;
            for (int t = 1; t <= teams && !remaining.isEmpty(); t++) {
                if (used[t] >= ppt) {
                    continue;
                }
                UUID id = remaining.remove(0);
                assign.put(id, Integer.valueOf(t));
                used[t]++;
                progressed = true;
            }
            if (!progressed) {
                return null;
            }
        }
        return assign;
    }

    private void tickRunningEffects() {
        for (RunningMatchEffects effects : runningEffects.values()) {
            effects.tickSecond();
        }
    }

    private void tickWaitingMatches() {
        List<String> emptyMatches = new ArrayList<String>();
        for (Map.Entry<String, WaitingMatch> entry : waitingMatches.entrySet()) {
            WaitingMatch match = entry.getValue();
            PillarsArena arena = match.getArena();

            if (match.isEmpty()) {
                arena.setState(ArenaState.WAITING);
                emptyMatches.add(entry.getKey());
                continue;
            }

            boolean readyToStart = isReadyToStart(match);
            if (!readyToStart) {
                if (match.hasCountdown()) {
                    match.setCountdownRemaining(-1);
                    arena.setState(ArenaState.WAITING);
                    broadcast(match, "countdown-cancelled", replacement("arena", arena.getName()));
                }
                continue;
            }

            if (!match.hasCountdown()) {
                match.setCountdownRemaining(Math.max(1, arena.getSettings().getCountdownSeconds()));
                arena.setState(ArenaState.STARTING);
                broadcast(match, "countdown-started", replacements(
                        "arena", arena.getName(),
                        "seconds", String.valueOf(match.getCountdownRemaining())
                ));
                continue;
            }

            int remaining = match.getCountdownRemaining();
            if (remaining <= 0) {
                startMatch(match);
                continue;
            }

            if (remaining <= 5 || remaining % 10 == 0) {
                broadcast(match, "countdown-tick", replacements(
                        "arena", arena.getName(),
                        "seconds", String.valueOf(remaining)
                ));
            }
            match.setCountdownRemaining(remaining - 1);
        }

        for (String key : emptyMatches) {
            waitingMatches.remove(key);
        }
    }

    private boolean isReadyToStart(WaitingMatch match) {
        return match.size() >= match.getArena().getSettings().getMinPlayers()
                && match.getFilledTeamCount() >= match.getArena().getSettings().getMinFilledTeams();
    }

    private void startMatch(WaitingMatch match) {
        PillarsArena arena = match.getArena();
        if (!hasRequiredSpawns(match)) {
            arena.setState(ArenaState.WAITING);
            match.setCountdownRemaining(-1);
            broadcast(match, "match-start-failed-missing-spawns", replacement("arena", arena.getName()));
            return;
        }

        RunningMatch runningMatch = new RunningMatch(arena);
        for (UUID playerId : match.getPlayers()) {
            runningMatch.addPlayer(playerId, match.getTeam(playerId));
        }
        if (arena.getSettings().isBlacklistMode()) {
            runningMatch.setBlacklistLootPool(LootGenerator.buildBlacklistPool(arena.getSettings()));
        }
        runningMatches.put(normalize(arena.getName()), runningMatch);
        waitingMatches.remove(normalize(arena.getName()));

        RunningMatchEffects effects = new RunningMatchEffects(plugin, this, runningMatch);
        effects.start();
        runningEffects.put(normalize(arena.getName()), effects);

        for (int team = 1; team <= arena.getTeams(); team++) {
            List<UUID> teamPlayers = playersInTeam(match, team);
            for (int index = 0; index < teamPlayers.size(); index++) {
                Player player = Bukkit.getPlayer(teamPlayers.get(index));
                if (player == null) {
                    continue;
                }
                removeTeamSelector(player);
                player.closeInventory();
                clearPlayerForMatch(player);
                plugin.getKitManager().giveSelectedKit(player);
                SerializedLocation spawn = arena.getSpawn(team, index + 1);
                teleportToSerializedLocation(player, spawn);
                player.setGameMode(GameMode.SURVIVAL);
                player.setNoDamageTicks(Math.max(60, arena.getSettings().getCageSeconds() * 20));
                player.setFallDistance(0.0F);
            }
        }

        hideLobbyAreaForMatch(runningMatch);
        arena.setState(ArenaState.RUNNING);
        match.setCountdownRemaining(-1);
        broadcast(runningMatch, "match-started", replacement("arena", arena.getName()));
        plugin.getCageManager().spawnCages(match);
    }

    private boolean hasRequiredSpawns(WaitingMatch match) {
        PillarsArena arena = match.getArena();
        for (int team = 1; team <= arena.getTeams(); team++) {
            List<UUID> teamPlayers = playersInTeam(match, team);
            for (int index = 0; index < teamPlayers.size(); index++) {
                if (arena.getSpawn(team, index + 1) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private void checkForWinner(RunningMatch match) {
        if (match.getArena().getState() != ArenaState.RUNNING) {
            return;
        }
        if (match.getAliveTeamCount() > 1) {
            return;
        }
        int winningTeam = match.findWinningTeam();
        endMatch(match, winningTeam);
    }

    private void endMatch(final RunningMatch match, int winningTeam) {
        final PillarsArena arena = match.getArena();
        match.setLastWinningTeam(winningTeam);
        arena.setState(ArenaState.ENDING);
        if (winningTeam > 0) {
            for (UUID playerId : match.getPlayers()) {
                if (match.getTeam(playerId) != winningTeam) {
                    continue;
                }
                Player winner = Bukkit.getPlayer(playerId);
                if (winner == null) {
                    continue;
                }
                prepareWinnerForCelebration(winner);
            }
        }
        if (winningTeam > 0) {
            broadcast(match, "match-won", replacements(
                    "arena", arena.getName(),
                    "team", String.valueOf(winningTeam)
            ));
        } else {
            broadcast(match, "match-ended-no-winner", replacement("arena", arena.getName()));
        }

        final int winningTeamFinal = winningTeam;
        if (winningTeamFinal > 0) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    runVictoryCommands(match, winningTeamFinal);
                }
            });
        }

        long celebrationTicks = Math.max(1L, (long) arena.getSettings().getCelebrationSeconds() * 20L);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                finishMatch(match);
            }
        }, celebrationTicks);
    }

    private void runVictoryCommands(RunningMatch match, int winningTeam) {
        List<String> templates = match.getArena().getSettings().getVictoryCommands();
        if (templates == null || templates.isEmpty()) {
            return;
        }
        PillarsArena arena = match.getArena();
        for (UUID playerId : match.getPlayers()) {
            if (match.getTeam(playerId) != winningTeam) {
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                continue;
            }
            for (String template : templates) {
                String cmd = substituteVictoryPlaceholders(template, player, arena, winningTeam);
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }
    }

    private static String substituteVictoryPlaceholders(String template, Player player, PillarsArena arena, int winningTeam) {
        String result = template == null ? "" : template;
        result = result.replace("%player%", player.getName());
        result = result.replace("{player}", player.getName());
        result = result.replace("%uuid%", player.getUniqueId().toString());
        result = result.replace("%arena%", arena.getName());
        result = result.replace("%team%", String.valueOf(winningTeam));
        return result;
    }

    private void finishMatch(RunningMatch match) {
        PillarsArena arena = match.getArena();
        clearSopPartyReservationsForMatch(match);
        RunningMatchEffects finishedEffects = runningEffects.remove(normalize(arena.getName()));
        if (finishedEffects != null) {
            finishedEffects.cleanup();
        }
        for (UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            arenaByPlayer.remove(playerId);
            respawnArenaByPlayer.remove(playerId);
            if (player == null) {
                continue;
            }
            player.setInvulnerable(false);
            player.setCanPickupItems(true);
            cleanupRunningState(player);
            restorePlayerState(player);
            teleportToGlobalSpawn(player);
        }
        match.restoreHiddenLobbyBlocks();
        runningMatches.remove(normalize(arena.getName()));

        arena.setState(ArenaState.RESTORING);
        plugin.getArenaSnapshotManager().restoreArenaBaseline(arena);
        plugin.getArenaSnapshotManager().clearForeignEntities(arena);
        plugin.getStatistics().recordMatchFinished(match, match.getLastWinningTeam());

        arena.setState(ArenaState.WAITING);
        plugin.getArenaManager().saveArena(arena);
    }

    private void broadcast(WaitingMatch match, String messageKey, Map<String, String> replacements) {
        for (UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getMessageService().send(player, messageKey, replacements);
            }
        }
    }

    private void broadcast(RunningMatch match, String messageKey, Map<String, String> replacements) {
        for (UUID playerId : match.getPlayers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getMessageService().send(player, messageKey, replacements);
            }
        }
    }

    private WaitingMatch getOrCreateWaitingMatch(PillarsArena arena) {
        String arenaName = normalize(arena.getName());
        WaitingMatch match = waitingMatches.get(arenaName);
        if (match == null) {
            match = new WaitingMatch(arena);
            waitingMatches.put(arenaName, match);
        }
        return match;
    }

    private WaitingMatch getPlayerWaitingMatch(UUID playerId) {
        String arenaName = arenaByPlayer.get(playerId);
        if (arenaName == null) {
            return null;
        }
        return waitingMatches.get(arenaName);
    }

    public boolean canDeleteArena(PillarsArena arena) {
        if (arena == null) {
            return false;
        }
        String n = normalize(arena.getName());
        if (plugin.getEditorManager().isArenaBeingEdited(arena.getName())) {
            return false;
        }
        if (runningMatches.containsKey(n)) {
            return false;
        }
        ArenaState state = arena.getState();
        if (state == ArenaState.RUNNING || state == ArenaState.STARTING || state == ArenaState.ENDING || state == ArenaState.RESTORING) {
            return false;
        }
        WaitingMatch waiting = waitingMatches.get(n);
        if (waiting != null && !waiting.isEmpty()) {
            return false;
        }
        return true;
    }

    public void unregisterArena(PillarsArena arena) {
        if (arena == null) {
            return;
        }
        String n = normalize(arena.getName());
        waitingMatches.remove(n);
        RunningMatch running = runningMatches.remove(n);
        if (running != null) {
            running.restoreHiddenLobbyBlocks();
        }
        RunningMatchEffects effects = runningEffects.remove(n);
        if (effects != null) {
            effects.cleanup();
        }
        List<UUID> clearIds = new ArrayList<UUID>();
        for (Map.Entry<UUID, String> entry : arenaByPlayer.entrySet()) {
            if (n.equals(entry.getValue())) {
                clearIds.add(entry.getKey());
            }
        }
        for (UUID playerId : clearIds) {
            arenaByPlayer.remove(playerId);
            respawnArenaByPlayer.remove(playerId);
        }
    }

    public RunningMatch getRunningMatch(UUID playerId) {
        String arenaName = arenaByPlayer.get(playerId);
        if (arenaName == null) {
            return null;
        }
        return runningMatches.get(arenaName);
    }

    public WaitingMatch getWaitingMatch(UUID playerId) {
        return getPlayerWaitingMatch(playerId);
    }

    public String getTrackedArenaName(UUID playerId) {
        String arenaName = arenaByPlayer.get(playerId);
        if (arenaName == null) {
            return "";
        }
        WaitingMatch waiting = waitingMatches.get(arenaName);
        if (waiting != null) {
            return waiting.getArena().getName();
        }
        RunningMatch running = runningMatches.get(arenaName);
        if (running != null) {
            return running.getArena().getName();
        }
        return "";
    }

    public int getSecondsUntilNextLoot(UUID playerId) {
        RunningMatch running = getRunningMatch(playerId);
        if (running == null) {
            return 0;
        }
        RunningMatchEffects effects = runningEffects.get(normalize(running.getArena().getName()));
        if (effects == null) {
            return 0;
        }
        return effects.getSecondsUntilNextLoot();
    }

    public int getSecondsUntilGameEnd(UUID playerId) {
        WaitingMatch waiting = getWaitingMatch(playerId);
        if (waiting != null) {
            return Math.max(0, waiting.getCountdownRemaining());
        }
        RunningMatch running = getRunningMatch(playerId);
        if (running == null) {
            return 0;
        }
        RunningMatchEffects effects = runningEffects.get(normalize(running.getArena().getName()));
        if (effects == null) {
            return 0;
        }
        return effects.getSecondsUntilEstimatedGameEnd();
    }

    public boolean isRunningSpectator(Player player) {
        RunningMatch match = getRunningMatch(player.getUniqueId());
        return match != null && !match.isAlive(player.getUniqueId());
    }

    public boolean isEndingWinnerProtected(Player player) {
        RunningMatch match = getRunningMatch(player.getUniqueId());
        if (match == null || match.getArena().getState() != ArenaState.ENDING) {
            return false;
        }
        if (!match.isAlive(player.getUniqueId())) {
            return false;
        }
        int winningTeam = match.getLastWinningTeam();
        return winningTeam > 0 && match.getTeam(player.getUniqueId()) == winningTeam;
    }

    private void teleportToLobby(Player player, PillarsArena arena) {
        SerializedLocation explicitLobbySpawn = arena.getLobbySpawn();
        if (explicitLobbySpawn != null) {
            World lobbyWorld = Bukkit.getWorld(explicitLobbySpawn.getWorld());
            if (lobbyWorld != null) {
                player.teleport(explicitLobbySpawn.toLocation(lobbyWorld));
                return;
            }
        }
        SerializedCuboid cuboid = arena.getLobbyArea();
        if (cuboid == null) {
            return;
        }
        World world = Bukkit.getWorld(cuboid.getMin().getWorld());
        if (world != null) {
            player.teleport(cuboid.getCenter(world));
        }
    }

    private void teleportToSerializedLocation(Player player, SerializedLocation location) {
        if (location == null) {
            return;
        }
        World world = Bukkit.getWorld(location.getWorld());
        if (world == null) {
            return;
        }
        player.teleport(location.toLocation(world));
    }

    private void teleportToGlobalSpawn(Player player) {
        SerializedLocation global = plugin.getPillarsConfig().getDefaultPostGameSpawn();
        if (global != null) {
            World globalWorld = Bukkit.getWorld(global.getWorld());
            if (globalWorld != null) {
                player.teleport(global.toLocation(globalWorld));
                return;
            }
        }
        plugin.getLogger().warning("Global spawn is not configured or world is unavailable; teleport skipped for " + player.getName() + ".");
    }

    private void giveTeamSelector(Player player) {
        PlayerInventory inventory = player.getInventory();
        int teamSlot = getTeamSelectorSlot();
        int leaveSlot = getLeaveArenaSlot(teamSlot);
        inventory.setItem(teamSlot, createTeamSelectorItem());
        inventory.setItem(leaveSlot, createLeaveArenaItem());
        player.updateInventory();
    }

    private void removeTeamSelector(Player player) {
        PlayerInventory inventory = player.getInventory();
        int teamSlot = getTeamSelectorSlot();
        int leaveSlot = getLeaveArenaSlot(teamSlot);
        ItemStack current = inventory.getItem(teamSlot);
        if (isTeamSelectorItem(current)) {
            inventory.setItem(teamSlot, null);
        } else {
            removeMatchingItems(inventory);
        }
        ItemStack leaveCurrent = inventory.getItem(leaveSlot);
        if (isLeaveArenaItem(leaveCurrent)) {
            inventory.setItem(leaveSlot, null);
        } else {
            removeMatchingLeaveItems(inventory);
        }
        player.updateInventory();
    }

    private void cleanupLobbyState(Player player) {
        removeTeamSelector(player);
        player.closeInventory();
    }

    private void cleanupRunningState(Player player) {
        removeTeamSelector(player);
        player.closeInventory();
        player.setGameMode(GameMode.SPECTATOR);
        player.setFallDistance(0.0F);
    }

    private void clearPlayerForMatch(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setFireTicks(0);
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);
    }

    private void clearPlayerForWaitingLobby(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.updateInventory();
    }

    private void prepareWinnerForCelebration(Player player) {
        clearPlayerForWaitingLobby(player);
        player.setInvulnerable(true);
        player.setCanPickupItems(false);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);
        player.setGameMode(GameMode.SURVIVAL);
        player.updateInventory();
    }

    private void snapshotPlayerState(Player player) {
        savedStateByPlayer.putIfAbsent(player.getUniqueId(), SavedPlayerState.capture(player));
    }

    private void restorePlayerState(Player player) {
        SavedPlayerState state = savedStateByPlayer.remove(player.getUniqueId());
        if (state != null) {
            state.restore(player);
        }
    }

    private boolean canTeleportBackToLobbyNow(UUID playerId) {
        long now = System.currentTimeMillis();
        Long prev = lastLobbyBoundsTeleportAt.get(playerId);
        if (prev != null && now - prev.longValue() < LOBBY_BOUNDS_TELEPORT_COOLDOWN_MS) {
            return false;
        }
        lastLobbyBoundsTeleportAt.put(playerId, Long.valueOf(now));
        return true;
    }

    private void restoreAllOnlineSavedStates() {
        for (UUID playerId : new ArrayList<UUID>(savedStateByPlayer.keySet())) {
            Player online = Bukkit.getPlayer(playerId);
            if (online != null && online.isOnline()) {
                restorePlayerState(online);
            }
        }
    }

    private void hideLobbyAreaForMatch(RunningMatch match) {
        SerializedCuboid lobby = match.getArena().getLobbyArea();
        if (lobby == null) {
            return;
        }
        World world = Bukkit.getWorld(lobby.getMin().getWorld());
        if (world == null) {
            return;
        }
        int[] b = lobby.getInclusiveBlockBounds();
        for (int x = b[0]; x <= b[3]; x++) {
            for (int y = b[1]; y <= b[4]; y++) {
                for (int z = b[2]; z <= b[5]; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.AIR) {
                        continue;
                    }
                    match.addHiddenLobbyBlock(new CapturedBlockState(block));
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private void removeMatchingItems(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int index = 0; index < contents.length; index++) {
            if (isTeamSelectorItem(contents[index])) {
                inventory.setItem(index, null);
            }
        }
    }

    private void removeMatchingLeaveItems(PlayerInventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int index = 0; index < contents.length; index++) {
            if (isLeaveArenaItem(contents[index])) {
                inventory.setItem(index, null);
            }
        }
    }

    private ItemStack createTeamSelectorItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getTeamSelectorName());
            meta.setLore(getTeamSelectorLore());
            meta.getPersistentDataContainer().set(lobbyItemKey, PersistentDataType.STRING, TEAM_SELECTOR_MARKER);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLeaveArenaItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(getLeaveArenaName());
            meta.setLore(getLeaveArenaLore());
            meta.getPersistentDataContainer().set(lobbyItemKey, PersistentDataType.STRING, LEAVE_ARENA_MARKER);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTeamItem(WaitingMatch match, int team) {
        Material material = pickTeamMaterial(team);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Team " + team);
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Players: " + match.getTeamSize(team) + "/" + match.getArena().getPlayersPerTeam());
            lore.add(ChatColor.GRAY + "Name: " + match.getArena().getTeamNames().get(team - 1));
            lore.add(ChatColor.YELLOW + "Click to join this team.");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material pickTeamMaterial(int team) {
        Material[] materials = new Material[] {
                Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL,
                Material.ORANGE_WOOL, Material.PURPLE_WOOL, Material.BLACK_WOOL, Material.WHITE_WOOL
        };
        return materials[(team - 1) % materials.length];
    }

    private boolean containsMode(Collection<String> requestedModes, String arenaMode) {
        for (String requestedMode : requestedModes) {
            if (requestedMode.equalsIgnoreCase(arenaMode)) {
                return true;
            }
        }
        return false;
    }

    private PillarsArena pickRandom(List<PillarsArena> arenas) {
        if (arenas.isEmpty()) {
            return null;
        }
        return arenas.get((int) (Math.random() * arenas.size()));
    }

    private List<UUID> playersInTeam(WaitingMatch match, int team) {
        List<UUID> players = new ArrayList<UUID>();
        for (UUID playerId : match.getPlayers()) {
            if (match.getTeam(playerId) == team) {
                players.add(playerId);
            }
        }
        return players;
    }

    private String extractCommandName(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        String raw = message.startsWith("/") ? message.substring(1) : message;
        int spaceIndex = raw.indexOf(' ');
        if (spaceIndex >= 0) {
            raw = raw.substring(0, spaceIndex);
        }
        return raw.toLowerCase(Locale.ROOT);
    }

    private Map<String, String> replacement(String key, String value) {
        return Collections.singletonMap(key, value);
    }

    private Map<String, String> replacements(String key1, String value1, String key2, String value2) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(key1, value1);
        values.put(key2, value2);
        return values;
    }

    private int getTeamSelectorSlot() {
        return clampHotbarSlot(plugin.getConfig().getInt("settings.waiting-items.team-selector.slot", TEAM_SELECTOR_SLOT_DEFAULT));
    }

    private int getLeaveArenaSlot(int teamSelectorSlot) {
        int configured = clampHotbarSlot(plugin.getConfig().getInt("settings.waiting-items.leave-arena.slot", LEAVE_ARENA_SLOT_DEFAULT));
        if (configured != teamSelectorSlot) {
            return configured;
        }
        // Avoid overlap if admin accidentally configured same slot for both.
        return configured == 8 ? 7 : 8;
    }

    private String getTeamSelectorName() {
        String raw = plugin.getConfig().getString("settings.waiting-items.team-selector.name");
        if (raw == null || raw.trim().isEmpty()) {
            return TEAM_SELECTOR_NAME_DEFAULT;
        }
        return colorize(raw);
    }

    private List<String> getTeamSelectorLore() {
        List<String> configured = plugin.getConfig().getStringList("settings.waiting-items.team-selector.lore");
        if (configured == null || configured.isEmpty()) {
            return TEAM_SELECTOR_LORE_DEFAULT;
        }
        return colorizeAll(configured);
    }

    private String getLeaveArenaName() {
        String raw = plugin.getConfig().getString("settings.waiting-items.leave-arena.name");
        if (raw == null || raw.trim().isEmpty()) {
            return LEAVE_ARENA_NAME_DEFAULT;
        }
        return colorize(raw);
    }

    private List<String> getLeaveArenaLore() {
        List<String> configured = plugin.getConfig().getStringList("settings.waiting-items.leave-arena.lore");
        if (configured == null || configured.isEmpty()) {
            return LEAVE_ARENA_LORE_DEFAULT;
        }
        return colorizeAll(configured);
    }

    private int clampHotbarSlot(int slot) {
        return Math.max(0, Math.min(8, slot));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private List<String> colorizeAll(List<String> lines) {
        List<String> out = new ArrayList<String>(lines.size());
        for (String line : lines) {
            out.add(colorize(line));
        }
        return out;
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private String formatChat(String template, Player sender, String message, String state) {
        return plugin.getMessageService().resolve(template, chatReplacements(sender, message, state));
    }

    private Map<String, String> chatReplacements(Player sender, String message, String state) {
        Map<String, String> values = new HashMap<String, String>();
        values.put("player", sender.getName());
        values.put("message", message);
        values.put("state", state);
        return values;
    }
}
