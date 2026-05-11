package net.enelson.soppillars.edit;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.SerializedLocation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class EditWizardManager {

    private static final int SLOT_SET = 0;
    private static final int SLOT_BACK = 1;
    private static final int SLOT_SKIP = 2;

    private final SopPillarsPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    private final Map<UUID, ItemStack[]> backups = new HashMap<UUID, ItemStack[]>();

    public EditWizardManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isActive(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public void start(Player player, PillarsArena arena) {
        UUID playerId = player.getUniqueId();
        sessions.put(playerId, new Session(arena.getName()));
        installControls(player);
        sendStepHint(player);
        plugin.getMessageService().send(player, "wizard-started", mapOf("arena", arena.getName()));
    }

    public void stop(Player player, boolean restoreControls) {
        UUID playerId = player.getUniqueId();
        sessions.remove(playerId);
        if (!restoreControls) {
            backups.remove(playerId);
            return;
        }
        ItemStack[] backup = backups.remove(playerId);
        if (backup == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        inv.setItem(SLOT_SET, cloneOrNull(backup[0]));
        inv.setItem(SLOT_BACK, cloneOrNull(backup[1]));
        inv.setItem(SLOT_SKIP, cloneOrNull(backup[2]));
        player.updateInventory();
    }

    public void stopAll() {
        sessions.clear();
        backups.clear();
    }

    public void handleSet(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        PillarsArena arena = plugin.getArenaManager().getArena(session.arenaName);
        if (arena == null) {
            stop(player, true);
            return;
        }
        Location current = player.getLocation();
        Step step = session.currentStep();
        switch (step) {
            case GAMEPLAY_POS1:
                plugin.getEditorManager().setGameplayPos1(player.getUniqueId(), current);
                plugin.getMessageService().send(player, "gameplay-pos1-set", mapOf("arena", arena.getName()));
                break;
            case GAMEPLAY_POS2:
                plugin.getEditorManager().setGameplayPos2(player.getUniqueId(), current);
                plugin.getMessageService().send(player, "gameplay-pos2-set", mapOf("arena", arena.getName()));
                trySaveGameplayArea(player, arena);
                break;
            case LOBBY_POS1:
                plugin.getEditorManager().setLobbyPos1(player.getUniqueId(), current);
                plugin.getMessageService().send(player, "lobby-pos1-set", mapOf("arena", arena.getName()));
                break;
            case LOBBY_POS2:
                plugin.getEditorManager().setLobbyPos2(player.getUniqueId(), current);
                plugin.getMessageService().send(player, "lobby-pos2-set", mapOf("arena", arena.getName()));
                trySaveLobbyArea(player, arena);
                break;
            case SET_SPECTATOR:
                arena.setSpectatorSpawn(SerializedLocation.fromLocation(current));
                plugin.getArenaManager().saveArena(arena);
                plugin.getMessageService().send(player, "spectator-spawn-set", mapOf("arena", arena.getName()));
                break;
            case SET_LOBBY_SPAWN:
                arena.setLobbySpawn(SerializedLocation.fromLocation(current));
                plugin.getArenaManager().saveArena(arena);
                plugin.getMessageService().send(player, "lobby-spawn-set", mapOf("arena", arena.getName()));
                break;
            case SET_END_SPAWN:
                arena.setPostGameSpawn(SerializedLocation.fromLocation(current));
                plugin.getArenaManager().saveArena(arena);
                plugin.getMessageService().send(player, "end-spawn-set", mapOf("arena", arena.getName()));
                break;
            case SET_TEAM_SPAWN:
                int team = session.spawnTeam;
                int slot = session.spawnSlot;
                arena.setSpawn(team, slot, SerializedLocation.fromLocation(current));
                plugin.getArenaManager().saveArena(arena);
                plugin.getMessageService().send(player, "spawn-set", mapOf(
                        "arena", arena.getName(),
                        "team", String.valueOf(team),
                        "player", String.valueOf(slot)
                ));
                break;
            default:
                break;
        }
        if (step == Step.SET_TEAM_SPAWN) {
            advanceSpawnSlot(session, arena);
        } else {
            session.stepIndex++;
        }
        if (session.isFinished()) {
            plugin.getMessageService().send(player, "wizard-finished", mapOf("arena", arena.getName()));
            stop(player, true);
            return;
        }
        installControls(player);
        sendStepHint(player);
    }

    public void handleBack(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.currentStep() == Step.GAMEPLAY_POS1 && session.spawnTeam == 1 && session.spawnSlot == 1) {
            plugin.getMessageService().send(player, "wizard-back-unavailable");
            return;
        }
        if (session.currentStep() == Step.SET_TEAM_SPAWN) {
            retreatSpawnSlot(session, plugin.getArenaManager().getArena(session.arenaName));
        } else {
            session.stepIndex = Math.max(0, session.stepIndex - 1);
        }
        installControls(player);
        sendStepHint(player);
    }

    public void handleSkip(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        PillarsArena arena = plugin.getArenaManager().getArena(session.arenaName);
        if (arena == null) {
            stop(player, true);
            return;
        }
        if (!canSkipCurrentStep(session, arena)) {
            plugin.getMessageService().send(player, "wizard-skip-unavailable");
            return;
        }
        if (session.currentStep() == Step.SET_TEAM_SPAWN) {
            advanceSpawnSlot(session, arena);
        } else {
            session.stepIndex++;
        }
        if (session.isFinished()) {
            plugin.getMessageService().send(player, "wizard-finished", mapOf("arena", arena.getName()));
            stop(player, true);
            return;
        }
        installControls(player);
        sendStepHint(player);
    }

    private void installControls(Player player) {
        UUID playerId = player.getUniqueId();
        PlayerInventory inv = player.getInventory();
        if (!backups.containsKey(playerId)) {
            backups.put(playerId, new ItemStack[] {
                    cloneOrNull(inv.getItem(SLOT_SET)),
                    cloneOrNull(inv.getItem(SLOT_BACK)),
                    cloneOrNull(inv.getItem(SLOT_SKIP))
            });
        }
        Session session = sessions.get(playerId);
        Step step = session.currentStep();
        inv.setItem(SLOT_SET, controlItem(Material.LIME_DYE, "&aSet: &f" + step.display(session)));
        inv.setItem(SLOT_BACK, controlItem(Material.ARROW, "&eBack"));
        PillarsArena arena = plugin.getArenaManager().getArena(session.arenaName);
        boolean canSkip = arena != null && canSkipCurrentStep(session, arena);
        String skipLabel = canSkip
                ? (step == Step.SET_END_SPAWN ? "&7Skip (end spawn)" : "&7Skip (already configured)")
                : "&8Skip unavailable";
        inv.setItem(SLOT_SKIP, canSkip ? controlItem(Material.GRAY_DYE, skipLabel) : controlItem(Material.BARRIER, skipLabel));
        player.updateInventory();
    }

    private void sendStepHint(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        PillarsArena arena = plugin.getArenaManager().getArena(session.arenaName);
        String stepLabel = session.currentStep().display(session);
        plugin.getMessageService().send(player, "wizard-step", mapOf("step", stepLabel));
        player.sendActionBar(buildProgressText(session, arena, stepLabel));
    }

    private Component buildProgressText(Session session, PillarsArena arena, String stepLabel) {
        int stepNumber = Math.min(session.stepIndex + 1, Session.ORDER.length);
        int total = Session.ORDER.length;
        if (session.currentStep() != Step.SET_TEAM_SPAWN || arena == null) {
            return Component.text("Step " + stepNumber + "/" + total + ": ", NamedTextColor.GOLD)
                    .append(Component.text(stepLabel, NamedTextColor.YELLOW));
        }
        int perTeam = Math.max(1, arena.getPlayersPerTeam());
        int teams = Math.max(1, arena.getTeams());
        int spawnIndex = Math.max(1, (session.spawnTeam - 1) * perTeam + session.spawnSlot);
        int spawnTotal = teams * perTeam;
        return Component.text("Step " + stepNumber + "/" + total + " ", NamedTextColor.GOLD)
                .append(Component.text("(" + spawnIndex + "/" + spawnTotal + "): ", NamedTextColor.GRAY))
                .append(Component.text(stepLabel, NamedTextColor.YELLOW));
    }

    private void trySaveGameplayArea(Player player, PillarsArena arena) {
        Location first = plugin.getEditorManager().getGameplayPos1(player.getUniqueId());
        Location second = plugin.getEditorManager().getGameplayPos2(player.getUniqueId());
        if (first == null || second == null) {
            return;
        }
        if (!sameWorld(first, second)) {
            plugin.getMessageService().send(player, "gameplay-world-mismatch");
            return;
        }
        arena.setGameplayArea(SerializedCuboid.fromLocations(first, second));
        plugin.getArenaManager().saveArena(arena);
        plugin.getMessageService().send(player, "gameplay-area-saved", mapOf("arena", arena.getName()));
    }

    private void trySaveLobbyArea(Player player, PillarsArena arena) {
        Location first = plugin.getEditorManager().getLobbyPos1(player.getUniqueId());
        Location second = plugin.getEditorManager().getLobbyPos2(player.getUniqueId());
        if (first == null || second == null) {
            return;
        }
        if (!sameWorld(first, second)) {
            plugin.getMessageService().send(player, "lobby-world-mismatch");
            return;
        }
        arena.setLobbyArea(SerializedCuboid.fromLocations(first, second));
        plugin.getArenaManager().saveArena(arena);
        plugin.getMessageService().send(player, "lobby-area-saved", mapOf("arena", arena.getName()));
    }

    private void advanceSpawnSlot(Session session, PillarsArena arena) {
        if (arena == null) {
            session.stepIndex = Integer.MAX_VALUE;
            return;
        }
        session.spawnSlot++;
        if (session.spawnSlot > arena.getPlayersPerTeam()) {
            session.spawnSlot = 1;
            session.spawnTeam++;
        }
        if (session.spawnTeam > arena.getTeams()) {
            session.stepIndex = Integer.MAX_VALUE;
        }
    }

    private void retreatSpawnSlot(Session session, PillarsArena arena) {
        if (arena == null) {
            session.stepIndex = Math.max(0, session.stepIndex - 1);
            return;
        }
        if (session.spawnSlot > 1) {
            session.spawnSlot--;
            return;
        }
        if (session.spawnTeam > 1) {
            session.spawnTeam--;
            session.spawnSlot = arena.getPlayersPerTeam();
            return;
        }
        session.stepIndex = Math.max(0, session.stepIndex - 1);
    }

    private boolean canSkipCurrentStep(Session session, PillarsArena arena) {
        Step step = session.currentStep();
        switch (step) {
            case GAMEPLAY_POS1:
            case GAMEPLAY_POS2:
                return arena.getGameplayArea() != null;
            case LOBBY_POS1:
            case LOBBY_POS2:
                return arena.getLobbyArea() != null;
            case SET_SPECTATOR:
                return arena.getSpectatorSpawn() != null;
            case SET_LOBBY_SPAWN:
                return arena.getLobbySpawn() != null;
            case SET_END_SPAWN:
                return true;
            case SET_TEAM_SPAWN:
                return arena.getSpawn(session.spawnTeam, session.spawnSlot) != null;
            default:
                return false;
        }
    }

    private boolean sameWorld(Location first, Location second) {
        if (first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().getName().equalsIgnoreCase(second.getWorld().getName());
    }

    private ItemStack controlItem(Material type, String displayName) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack cloneOrNull(ItemStack input) {
        return input == null ? null : input.clone();
    }

    private Map<String, String> mapOf(String key, String value) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(key, value);
        return values;
    }

    private Map<String, String> mapOf(String k1, String v1, String k2, String v2, String k3, String v3) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(k1, v1);
        values.put(k2, v2);
        values.put(k3, v3);
        return values;
    }

    private enum Step {
        GAMEPLAY_POS1,
        GAMEPLAY_POS2,
        LOBBY_POS1,
        LOBBY_POS2,
        SET_SPECTATOR,
        SET_LOBBY_SPAWN,
        SET_END_SPAWN,
        SET_TEAM_SPAWN;

        String display(Session session) {
            switch (this) {
                case GAMEPLAY_POS1:
                    return "gameplay pos1";
                case GAMEPLAY_POS2:
                    return "gameplay pos2";
                case LOBBY_POS1:
                    return "lobby pos1";
                case LOBBY_POS2:
                    return "lobby pos2";
                case SET_SPECTATOR:
                    return "spectator spawn";
                case SET_LOBBY_SPAWN:
                    return "lobby spawn";
                case SET_END_SPAWN:
                    return "end spawn (optional)";
                case SET_TEAM_SPAWN:
                    return ("team" + session.spawnTeam + " player" + session.spawnSlot).toLowerCase(Locale.ROOT);
                default:
                    return "";
            }
        }
    }

    static final class Session {
        static final Step[] ORDER = new Step[] {
                Step.GAMEPLAY_POS1,
                Step.GAMEPLAY_POS2,
                Step.LOBBY_POS1,
                Step.LOBBY_POS2,
                Step.SET_SPECTATOR,
                Step.SET_LOBBY_SPAWN,
                Step.SET_END_SPAWN,
                Step.SET_TEAM_SPAWN
        };

        private final String arenaName;
        private int stepIndex = 0;
        private int spawnTeam = 1;
        private int spawnSlot = 1;

        private Session(String arenaName) {
            this.arenaName = arenaName;
        }

        private boolean isFinished() {
            return stepIndex >= ORDER.length;
        }

        private Step currentStep() {
            if (stepIndex >= ORDER.length) {
                return Step.SET_TEAM_SPAWN;
            }
            return ORDER[stepIndex];
        }
    }
}
