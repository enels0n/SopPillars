package net.enelson.soppillars.command;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.ArenaState;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.menu.ArenaAdminMenus;
import net.enelson.soppillars.model.SerializedCuboid;
import net.enelson.soppillars.model.SerializedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PillarsCommand implements TabExecutor {

    private final SopPillarsPlugin plugin;

    public PillarsCommand(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if ("reload".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            if (plugin.reloadPlugin()) {
                plugin.getMessageService().send(sender, "reload-success");
            } else {
                plugin.getMessageService().send(sender, "reload-failed");
            }
            return true;
        }

        if ("setglobalspawn".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            Location location = player.getLocation();
            if (location.getWorld() == null) {
                return true;
            }
            plugin.getConfig().set("settings.global-spawn.world", location.getWorld().getName());
            plugin.getConfig().set("settings.global-spawn.x", location.getX());
            plugin.getConfig().set("settings.global-spawn.y", location.getY());
            plugin.getConfig().set("settings.global-spawn.z", location.getZ());
            plugin.getConfig().set("settings.global-spawn.yaw", location.getYaw());
            plugin.getConfig().set("settings.global-spawn.pitch", location.getPitch());
            plugin.saveConfig();
            plugin.getPillarsConfig().reload();
            plugin.getMessageService().send(player, "global-spawn-set");
            return true;
        }

        if ("list".equals(subcommand)) {
            List<String> arenaNames = plugin.getArenaManager().getArenaNames();
            if (arenaNames.isEmpty()) {
                plugin.getMessageService().send(sender, "arena-list-empty");
                return true;
            }
            plugin.getMessageService().send(sender, "arena-list", singletonReplacement("arenas", String.join(", ", arenaNames)));
            return true;
        }

        if ("leave".equals(subcommand)) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            plugin.getMatchManager().leaveArena(player, false);
            return true;
        }

        if ("kits".equals(subcommand)) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!hasPlay(player)) {
                return true;
            }
            plugin.getKitManager().openKitMenu(player);
            return true;
        }

        if ("kitadd".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageService().send(player, "usage-kitadd");
                return true;
            }
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held == null || held.getType() == Material.AIR) {
                plugin.getMessageService().send(player, "kit-hand-empty");
                return true;
            }
            String kitId = args[1];
            if (plugin.getKitManager().getKitItemCount(kitId) < 0) {
                plugin.getMessageService().send(player, "kit-missing", singletonReplacement("kit", kitId));
                return true;
            }
            if (!plugin.getKitManager().addItemToKit(kitId, held)) {
                plugin.getMessageService().send(player, "reload-failed");
                return true;
            }
            plugin.getMessageService().send(player, "kit-item-added", singletonReplacement("kit", kitId));
            return true;
        }

        if ("kitremove".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length < 3) {
                plugin.getMessageService().send(player, "usage-kitremove");
                return true;
            }
            String kitId = args[1];
            if (plugin.getKitManager().getKitItemCount(kitId) < 0) {
                plugin.getMessageService().send(player, "kit-missing", singletonReplacement("kit", kitId));
                return true;
            }
            if ("all".equalsIgnoreCase(args[2])) {
                if (!plugin.getKitManager().clearKitItems(kitId)) {
                    plugin.getMessageService().send(player, "reload-failed");
                    return true;
                }
                plugin.getMessageService().send(player, "kit-items-cleared", singletonReplacement("kit", kitId));
                return true;
            }
            int index = parseInt(args[2], 0);
            if (index <= 0) {
                plugin.getMessageService().send(player, "kit-remove-index-invalid");
                return true;
            }
            if (!plugin.getKitManager().removeItemFromKit(kitId, index)) {
                plugin.getMessageService().send(player, "kit-remove-index-invalid");
                return true;
            }
            plugin.getMessageService().send(player, "kit-item-removed", replacements(
                    "kit", kitId,
                    "slot", String.valueOf(index)
            ));
            return true;
        }

        if ("create".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length < 5) {
                plugin.getMessageService().send(sender, "usage-create");
                return true;
            }
            String name = args[1];
            String mode = args[2];
            int teams = parseInt(args[3], 0);
            int playersPerTeam = parseInt(args[4], 0);
            if (teams < 2 || playersPerTeam < 1) {
                plugin.getMessageService().send(sender, "create-invalid-teams");
                return true;
            }
            if (plugin.getArenaManager().hasArena(name)) {
                plugin.getMessageService().send(sender, "create-arena-exists", singletonReplacement("arena", name));
                return true;
            }
            PillarsArena arena = plugin.getArenaManager().createArena(name, mode, teams, playersPerTeam, player.getWorld().getName());
            plugin.getArenaManager().snapshotArenaBeforeEdit(arena.getName());
            plugin.getEditorManager().enterEditor(player.getUniqueId(), arena.getName());
            plugin.getMessageService().send(sender, "arena-created", singletonReplacement("arena", arena.getName()));
            plugin.getMessageService().send(sender, "editor-enter", singletonReplacement("arena", arena.getName()));
            return true;
        }

        if ("edit".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageService().send(sender, "usage-edit");
                return true;
            }
            PillarsArena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
                plugin.getMessageService().send(sender, "arena-missing", singletonReplacement("arena", args[1]));
                return true;
            }
            plugin.getArenaManager().snapshotArenaBeforeEdit(arena.getName());
            arena.setState(ArenaState.EDITING);
            plugin.getEditorManager().enterEditor(player.getUniqueId(), arena.getName());
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(sender, "editor-enter", singletonReplacement("arena", arena.getName()));
            return true;
        }

        if ("save".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            PillarsArena arena = requireEditedArena(player);
            if (arena == null) {
                return true;
            }
            arena.setState(ArenaState.WAITING);
            plugin.getArenaManager().saveArena(arena);
            plugin.getEditorManager().leaveEditor(player.getUniqueId());
            plugin.getMessageService().send(sender, "editor-save", singletonReplacement("arena", arena.getName()));
            if (plugin.getArenaSnapshotManager().captureSnapshot(arena)) {
                plugin.getMessageService().send(sender, "snapshot-saved", singletonReplacement("arena", arena.getName()));
            } else {
                plugin.getMessageService().send(sender, "snapshot-failed", singletonReplacement("arena", arena.getName()));
            }
            plugin.getArenaManager().clearEditBackup(arena.getName());
            return true;
        }

        if ("cancel".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageService().send(sender, "usage-cancel");
                return true;
            }
            PillarsArena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
                plugin.getMessageService().send(sender, "arena-missing", singletonReplacement("arena", args[1]));
                return true;
            }
            String editing = plugin.getEditorManager().getEditedArena(player.getUniqueId());
            if (editing == null || !editing.equalsIgnoreCase(arena.getName())) {
                plugin.getMessageService().send(sender, "not-editing");
                return true;
            }
            plugin.getEditorManager().leaveEditor(player.getUniqueId());
            if (!plugin.getArenaManager().restoreArenaFromEditBackup(arena.getName())) {
                plugin.getLogger().info("No edit YAML backup for " + arena.getName() + "; reloading arena file as-is.");
            }
            plugin.getArenaManager().reloadArenaFromDisk(arena.getName());
            PillarsArena updated = plugin.getArenaManager().getArena(arena.getName());
            if (updated != null) {
                updated.setState(ArenaState.WAITING);
                plugin.getArenaManager().saveArena(updated);
            }
            plugin.getArenaManager().clearEditBackup(arena.getName());
            plugin.getMessageService().send(sender, "editor-cancel", singletonReplacement("arena", arena.getName()));
            return true;
        }

        if ("delete".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageService().send(sender, "usage-delete");
                return true;
            }
            PillarsArena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
                plugin.getMessageService().send(sender, "arena-missing", singletonReplacement("arena", args[1]));
                return true;
            }
            if (!plugin.getMatchManager().canDeleteArena(arena)) {
                plugin.getMessageService().send(sender, "arena-delete-busy", singletonReplacement("arena", arena.getName()));
                return true;
            }
            plugin.getArenaManager().deleteArenaData(arena);
            plugin.getMessageService().send(sender, "arena-deleted", singletonReplacement("arena", arena.getName()));
            return true;
        }

        if ("pos1".equals(subcommand) || "pos2".equals(subcommand)
                || "lobbypos1".equals(subcommand) || "lobbypos2".equals(subcommand)
                || "setspawn".equals(subcommand) || "setspectator".equals(subcommand)
                || "setendspawn".equals(subcommand) || "setlobbyspawn".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            PillarsArena arena = requireEditedArena(player);
            if (arena == null) {
                return true;
            }
            return handleEditorSubcommand(player, arena, subcommand, args);
        }

        if ("join".equals(subcommand)) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!hasPlay(player)) {
                return true;
            }
            if (args.length < 2) {
                plugin.getMessageService().send(player, "usage-join");
                return true;
            }
            PillarsArena arena = plugin.getArenaManager().getArena(args[1]);
            if (arena == null) {
                plugin.getMessageService().send(player, "arena-missing", singletonReplacement("arena", args[1]));
                return true;
            }
            plugin.getMatchManager().joinArena(player, arena);
            return true;
        }

        if ("random".equals(subcommand)) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!hasPlay(player)) {
                return true;
            }
            List<String> modes = Collections.emptyList();
            if (args.length >= 2) {
                modes = parseModes(args[1]);
            }
            plugin.getMatchManager().joinRandom(player, modes);
            return true;
        }

        if ("settings".equals(subcommand)) {
            if (!hasAdmin(sender)) {
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            PillarsArena arena = requireEditedArena(player);
            if (arena == null) {
                return true;
            }
            ArenaAdminMenus.openSettings(plugin, player, arena);
            return true;
        }

        if ("cosmetics".equals(subcommand)) {
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!hasPlay(player)) {
                return true;
            }
            ArenaAdminMenus.openCosmetics(plugin, player);
            return true;
        }

        if ("stats".equals(subcommand)) {
            if (args.length >= 2) {
                if (!hasAdmin(sender)) {
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.getMessageService().send(sender, "stats-player-online");
                    return true;
                }
                sendStats(sender, target);
                return true;
            }
            Player player = requirePlayer(sender);
            if (player == null) {
                return true;
            }
            if (!hasStats(sender)) {
                return true;
            }
            sendStats(sender, player);
            return true;
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> root = new ArrayList<String>();
            root.add("list");
            if (canPlaySilently(sender)) {
                root.add("join");
                root.add("random");
                root.add("kits");
                root.add("leave");
                root.add("cosmetics");
            }
            if (canStatsSilently(sender)) {
                root.add("stats");
            }
            if (canAdminSilently(sender)) {
                root.add("create");
                root.add("edit");
                root.add("save");
                root.add("cancel");
                root.add("delete");
                root.add("reload");
                root.add("settings");
                root.add("kitadd");
                root.add("kitremove");
                root.add("pos1");
                root.add("pos2");
                root.add("lobbypos1");
                root.add("lobbypos2");
                root.add("setspawn");
                root.add("setspectator");
                root.add("setendspawn");
                root.add("setlobbyspawn");
                root.add("setglobalspawn");
            }
            return filterByPrefix(root, args[0]);
        }
        if (args.length == 2) {
            if ("edit".equalsIgnoreCase(args[0]) || "join".equalsIgnoreCase(args[0]) || "cancel".equalsIgnoreCase(args[0])
                    || "delete".equalsIgnoreCase(args[0])) {
                if (("edit".equalsIgnoreCase(args[0]) || "cancel".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0]))
                        && !canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                if ("join".equalsIgnoreCase(args[0]) && !canPlaySilently(sender)) {
                    return Collections.emptyList();
                }
                return filterByPrefix(plugin.getArenaManager().getArenaNames(), args[1]);
            }
            if ("random".equalsIgnoreCase(args[0])) {
                if (!canPlaySilently(sender)) {
                    return Collections.emptyList();
                }
                return filterByPrefix(plugin.getArenaManager().getModes(), args[1]);
            }
            if ("stats".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                return filterByPrefix(onlinePlayerNames(), args[1]);
            }
            if ("setspawn".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                Player player = sender instanceof Player ? (Player) sender : null;
                PillarsArena arena = player == null ? null : requireEditedArenaSilently(player);
                if (arena == null) {
                    return Collections.emptyList();
                }
                return integerRange(arena.getTeams(), args[1]);
            }
            if ("kitadd".equalsIgnoreCase(args[0]) || "kitremove".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                return filterByPrefix(plugin.getKitManager().getKitIds(), args[1]);
            }
        }
        if (args.length == 3) {
            if ("create".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                return filterByPrefix(plugin.getArenaManager().getModes(), args[2]);
            }
            if ("setspawn".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                Player player = sender instanceof Player ? (Player) sender : null;
                PillarsArena arena = player == null ? null : requireEditedArenaSilently(player);
                if (arena == null) {
                    return Collections.emptyList();
                }
                return integerRange(arena.getPlayersPerTeam(), args[2]);
            }
            if ("kitremove".equalsIgnoreCase(args[0])) {
                if (!canAdminSilently(sender)) {
                    return Collections.emptyList();
                }
                List<String> values = new ArrayList<String>();
                values.add("all");
                int count = plugin.getKitManager().getKitItemCount(args[1]);
                if (count > 0) {
                    values.addAll(integerRange(count, ""));
                }
                return filterByPrefix(values, args[2]);
            }
        }
        return Collections.emptyList();
    }

    private boolean handleEditorSubcommand(Player player, PillarsArena arena, String subcommand, String[] args) {
        if ("pos1".equals(subcommand)) {
            plugin.getEditorManager().setGameplayPos1(player.getUniqueId(), player.getLocation());
            plugin.getMessageService().send(player, "gameplay-pos1-set", singletonReplacement("arena", arena.getName()));
            trySaveGameplayArea(player, arena);
            return true;
        }
        if ("pos2".equals(subcommand)) {
            plugin.getEditorManager().setGameplayPos2(player.getUniqueId(), player.getLocation());
            plugin.getMessageService().send(player, "gameplay-pos2-set", singletonReplacement("arena", arena.getName()));
            trySaveGameplayArea(player, arena);
            return true;
        }
        if ("lobbypos1".equals(subcommand)) {
            plugin.getEditorManager().setLobbyPos1(player.getUniqueId(), player.getLocation());
            plugin.getMessageService().send(player, "lobby-pos1-set", singletonReplacement("arena", arena.getName()));
            trySaveLobbyArea(player, arena);
            return true;
        }
        if ("lobbypos2".equals(subcommand)) {
            plugin.getEditorManager().setLobbyPos2(player.getUniqueId(), player.getLocation());
            plugin.getMessageService().send(player, "lobby-pos2-set", singletonReplacement("arena", arena.getName()));
            trySaveLobbyArea(player, arena);
            return true;
        }
        if ("setspectator".equals(subcommand)) {
            arena.setSpectatorSpawn(SerializedLocation.fromLocation(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "spectator-spawn-set", singletonReplacement("arena", arena.getName()));
            return true;
        }
        if ("setendspawn".equals(subcommand)) {
            arena.setPostGameSpawn(SerializedLocation.fromLocation(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "end-spawn-set", singletonReplacement("arena", arena.getName()));
            return true;
        }
        if ("setlobbyspawn".equals(subcommand)) {
            arena.setLobbySpawn(SerializedLocation.fromLocation(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "lobby-spawn-set", singletonReplacement("arena", arena.getName()));
            return true;
        }
        if ("setspawn".equals(subcommand)) {
            if (args.length < 3) {
                plugin.getMessageService().send(player, "usage-setspawn");
                return true;
            }
            int team = parseInt(args[1], 0);
            int slot = parseInt(args[2], 0);
            if (team < 1 || team > arena.getTeams() || slot < 1 || slot > arena.getPlayersPerTeam()) {
                plugin.getMessageService().send(player, "invalid-team-slot");
                return true;
            }
            arena.setSpawn(team, slot, SerializedLocation.fromLocation(player.getLocation()));
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "spawn-set", replacements(
                    "arena", arena.getName(),
                    "team", String.valueOf(team),
                    "player", String.valueOf(slot)
            ));
            return true;
        }
        return false;
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
        plugin.getMessageService().send(player, "gameplay-area-saved", singletonReplacement("arena", arena.getName()));
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
        plugin.getMessageService().send(player, "lobby-area-saved", singletonReplacement("arena", arena.getName()));
    }

    private void sendHelp(CommandSender sender) {
        plugin.getMessageService().send(sender, "help-1");
        plugin.getMessageService().send(sender, "help-2");
        plugin.getMessageService().send(sender, "help-3");
        plugin.getMessageService().send(sender, "help-4");
        plugin.getMessageService().send(sender, "help-5");
        plugin.getMessageService().send(sender, "help-6");
        plugin.getMessageService().send(sender, "help-7");
        plugin.getMessageService().send(sender, "help-8");
        plugin.getMessageService().send(sender, "help-9");
        plugin.getMessageService().send(sender, "help-10");
        plugin.getMessageService().send(sender, "help-11");
        plugin.getMessageService().send(sender, "help-12");
        plugin.getMessageService().send(sender, "help-13");
        plugin.getMessageService().send(sender, "help-14");
        plugin.getMessageService().send(sender, "help-15");
        plugin.getMessageService().send(sender, "help-16");
        plugin.getMessageService().send(sender, "help-17");
        plugin.getMessageService().send(sender, "help-18");
        plugin.getMessageService().send(sender, "help-19");
        plugin.getMessageService().send(sender, "help-20");
        plugin.getMessageService().send(sender, "help-21");
        plugin.getMessageService().send(sender, "help-22");
        plugin.getMessageService().send(sender, "help-23");
        plugin.getMessageService().send(sender, "help-24");
    }

    private boolean hasAdmin(CommandSender sender) {
        if (sender.isOp() || sender.hasPermission("soppillars.admin")) {
            return true;
        }
        plugin.getMessageService().send(sender, "no-permission");
        return false;
    }

    private boolean hasPlay(CommandSender sender) {
        if (sender.hasPermission("soppillars.play") || sender.isOp()) {
            return true;
        }
        plugin.getMessageService().send(sender, "no-permission");
        return false;
    }

    private boolean hasStats(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;
        if (player.hasPermission("soppillars.stats") || player.isOp()) {
            return true;
        }
        plugin.getMessageService().send(sender, "no-permission");
        return false;
    }

    private boolean canAdminSilently(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("soppillars.admin");
    }

    private boolean canPlaySilently(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("soppillars.play");
    }

    private boolean canStatsSilently(CommandSender sender) {
        return sender instanceof Player && (((Player) sender).isOp() || sender.hasPermission("soppillars.stats"));
    }

    private void sendStats(CommandSender viewer, Player target) {
        UUID id = target.getUniqueId();
        Map<String, String> values = new HashMap<String, String>();
        values.put("player", target.getName());
        values.put("games", String.valueOf(plugin.getStatistics().getInt("games", id)));
        values.put("wins", String.valueOf(plugin.getStatistics().getInt("wins", id)));
        values.put("kills", String.valueOf(plugin.getStatistics().getInt("kills", id)));
        values.put("deaths", String.valueOf(plugin.getStatistics().getInt("deaths", id)));
        String key = viewer instanceof Player && ((Player) viewer).getUniqueId().equals(id) ? "stats-self" : "stats-other";
        plugin.getMessageService().send(viewer, key, values);
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return (Player) sender;
        }
        plugin.getMessageService().send(sender, "not-player");
        return null;
    }

    private PillarsArena requireEditedArena(Player player) {
        PillarsArena arena = requireEditedArenaSilently(player);
        if (arena == null) {
            plugin.getMessageService().send(player, "not-editing");
        }
        return arena;
    }

    private PillarsArena requireEditedArenaSilently(Player player) {
        String editedArenaName = plugin.getEditorManager().getEditedArena(player.getUniqueId());
        if (editedArenaName == null) {
            return null;
        }
        return plugin.getArenaManager().getArena(editedArenaName);
    }

    private int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean sameWorld(Location first, Location second) {
        if (first.getWorld() == null || second.getWorld() == null) {
            return false;
        }
        return first.getWorld().getName().equalsIgnoreCase(second.getWorld().getName());
    }

    private List<String> filterByPrefix(List<String> source, String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<String>();
        for (String entry : source) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private List<String> integerRange(int max, String rawPrefix) {
        List<String> values = new ArrayList<String>();
        for (int index = 1; index <= max; index++) {
            values.add(String.valueOf(index));
        }
        return filterByPrefix(values, rawPrefix);
    }

    private List<String> parseModes(String input) {
        List<String> modes = new ArrayList<String>();
        for (String part : input.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                modes.add(trimmed);
            }
        }
        return modes;
    }

    private Map<String, String> singletonReplacement(String key, String value) {
        return Collections.singletonMap(key, value);
    }

    private Map<String, String> replacements(String key1, String value1, String key2, String value2) {
        Map<String, String> values = new java.util.HashMap<String, String>();
        values.put(key1, value1);
        values.put(key2, value2);
        return values;
    }

    private Map<String, String> replacements(String key1, String value1, String key2, String value2, String key3, String value3) {
        Map<String, String> values = new java.util.HashMap<String, String>();
        values.put(key1, value1);
        values.put(key2, value2);
        values.put(key3, value3);
        return values;
    }
}
