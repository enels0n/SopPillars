package net.enelson.soppillars.loot;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public final class LootListEditorManager {

    private static final String TITLE_PREFIX = ChatColor.DARK_GREEN + "Loot List: ";

    private final SopPillarsPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();

    public LootListEditorManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void openArenaWhitelist(Player player, PillarsArena arena) {
        open(player, new Session(Kind.WHITELIST, Scope.ARENA, arena.getName()), arena.getSettings().getLootWhitelist());
    }

    public void openArenaBlacklist(Player player, PillarsArena arena) {
        open(player, new Session(Kind.BLACKLIST, Scope.ARENA, arena.getName()), arena.getSettings().getLootBlacklist());
    }

    public void openGlobalWhitelist(Player player) {
        open(player, new Session(Kind.WHITELIST, Scope.GLOBAL, ""),
                plugin.getConfig().getStringList("settings.default-loot-whitelist"));
    }

    public void openGlobalBlacklist(Player player) {
        open(player, new Session(Kind.BLACKLIST, Scope.GLOBAL, ""),
                plugin.getConfig().getStringList("settings.default-loot-blacklist"));
    }

    public boolean isLootListEditorTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public void handleClose(Player player, Inventory inventory) {
        Session session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        List<String> materials = collectMaterialNames(inventory);
        if (session.scope == Scope.ARENA) {
            PillarsArena arena = plugin.getArenaManager().getArena(session.arenaName);
            if (arena == null) {
                return;
            }
            if (session.kind == Kind.WHITELIST) {
                arena.getSettings().setLootWhitelist(materials);
            } else {
                arena.getSettings().setLootBlacklist(materials);
            }
            plugin.getArenaManager().saveArena(arena);
            plugin.getMessageService().send(player, "loot-list-saved", mapOf(
                    "scope", "arena",
                    "mode", session.kind == Kind.WHITELIST ? "whitelist" : "blacklist"
            ));
            return;
        }
        if (session.kind == Kind.WHITELIST) {
            plugin.getConfig().set("settings.default-loot-whitelist", materials);
        } else {
            plugin.getConfig().set("settings.default-loot-blacklist", materials);
        }
        plugin.saveConfig();
        plugin.getPillarsConfig().reload();
        plugin.getMessageService().send(player, "loot-list-saved", mapOf(
                "scope", "global",
                "mode", session.kind == Kind.WHITELIST ? "whitelist" : "blacklist"
        ));
    }

    private void open(Player player, Session session, List<String> materialNames) {
        sessions.put(player.getUniqueId(), session);
        Inventory inventory = plugin.getServer().createInventory(null, 54, buildTitle(session));
        int slot = 0;
        for (String name : materialNames) {
            if (slot >= inventory.getSize()) {
                break;
            }
            Material material = parseMaterial(name);
            if (material == null || !material.isItem()) {
                continue;
            }
            inventory.setItem(slot++, new ItemStack(material, 1));
        }
        player.openInventory(inventory);
    }

    private List<String> collectMaterialNames(Inventory inventory) {
        Set<String> unique = new LinkedHashSet<String>();
        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            if (!item.getType().isItem()) {
                continue;
            }
            unique.add(item.getType().name());
        }
        return new ArrayList<String>(unique);
    }

    private String buildTitle(Session session) {
        String scope = session.scope == Scope.GLOBAL ? "Global" : ("Arena " + session.arenaName);
        String mode = session.kind == Kind.WHITELIST ? "Whitelist" : "Blacklist";
        return TITLE_PREFIX + scope + " " + mode;
    }

    private Material parseMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> mapOf(String k1, String v1, String k2, String v2) {
        Map<String, String> values = new HashMap<String, String>();
        values.put(k1, v1);
        values.put(k2, v2);
        return values;
    }

    private enum Kind {
        WHITELIST,
        BLACKLIST
    }

    private enum Scope {
        ARENA,
        GLOBAL
    }

    private static final class Session {
        private final Kind kind;
        private final Scope scope;
        private final String arenaName;

        private Session(Kind kind, Scope scope, String arenaName) {
            this.kind = kind;
            this.scope = scope;
            this.arenaName = arenaName;
        }
    }
}
