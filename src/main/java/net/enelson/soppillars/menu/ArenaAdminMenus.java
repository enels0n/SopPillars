package net.enelson.soppillars.menu;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.model.ArenaSettings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class ArenaAdminMenus {

    public static final String SETTINGS_TITLE = ChatColor.DARK_BLUE + "SopPillars settings";
    public static final String COSMETICS_TITLE = ChatColor.DARK_PURPLE + "SopPillars cosmetics";

    private ArenaAdminMenus() {
    }

    public static void openSettings(SopPillarsPlugin plugin, Player player, PillarsArena arena) {
        Inventory inventory = Bukkit.createInventory(null, 27, SETTINGS_TITLE);
        fillSettings(plugin, inventory, arena);
        player.openInventory(inventory);
    }

    public static void openCosmetics(SopPillarsPlugin plugin, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 9, COSMETICS_TITLE);
        ItemStack coming = new ItemStack(Material.BARRIER);
        ItemMeta meta = coming.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Coming soon");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Cosmetic cages and effects will be added later.",
                    ChatColor.DARK_GRAY + "Track SPEC.md / MVP.md for scope."
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            coming.setItemMeta(meta);
        }
        inventory.setItem(4, coming);
        player.openInventory(inventory);
    }

    public static void handleSettingsClick(SopPillarsPlugin plugin, Player player, PillarsArena arena, int slot, boolean rightClick, boolean shiftClick) {
        ArenaSettings settings = arena.getSettings();
        int delta = shiftClick ? 5 : 1;
        switch (slot) {
            case 0:
                settings.setAllowPlaceBlocks(!settings.isAllowPlaceBlocks());
                break;
            case 1:
                settings.setAllowBreakOriginalBlocks(!settings.isAllowBreakOriginalBlocks());
                break;
            case 2:
                settings.setAllowBreakPlayerBlocks(!settings.isAllowBreakPlayerBlocks());
                break;
            case 3:
                settings.setFriendlyFire(!settings.isFriendlyFire());
                break;
            case 4:
                settings.setLavaEnabled(!settings.isLavaEnabled());
                break;
            case 5:
                settings.setAllowSmoothFall(!settings.isAllowSmoothFall());
                break;
            case 6:
                settings.setBlacklistMode(!settings.isBlacklistMode());
                break;
            case 7:
                settings.setCountdownSeconds(adjustInt(settings.getCountdownSeconds(), rightClick, delta, 1));
                break;
            case 8:
                settings.setCageSeconds(adjustInt(settings.getCageSeconds(), rightClick, delta, 1));
                break;
            case 9:
                settings.setPreBorderDelaySeconds(adjustInt(settings.getPreBorderDelaySeconds(), rightClick, delta, 0));
                break;
            case 10:
                settings.setBorderShrinkSeconds(adjustInt(settings.getBorderShrinkSeconds(), rightClick, delta, 1));
                break;
            case 11:
                settings.setEndBorderDiameter(adjustDouble(settings.getEndBorderDiameter(), rightClick, shiftClick ? 5.0D : 1.0D, 1.0D));
                break;
            case 12:
                settings.setLavaStartDelaySeconds(adjustInt(settings.getLavaStartDelaySeconds(), rightClick, delta, 0));
                break;
            case 13:
                settings.setLavaRiseIntervalSeconds(adjustInt(settings.getLavaRiseIntervalSeconds(), rightClick, delta, 1));
                break;
            case 14:
                settings.setPostShrinkEndDelaySeconds(adjustInt(settings.getPostShrinkEndDelaySeconds(), rightClick, delta, 0));
                break;
            case 15:
                settings.setLootIntervalSeconds(adjustInt(settings.getLootIntervalSeconds(), rightClick, delta, 1));
                break;
            case 16:
                settings.setCelebrationSeconds(adjustInt(settings.getCelebrationSeconds(), rightClick, delta, 1));
                break;
            case 17:
                settings.setSmoothFallSeconds(adjustInt(settings.getSmoothFallSeconds(), rightClick, delta, 1));
                break;
            default:
                return;
        }
        plugin.getArenaManager().saveArena(arena);
        if (SETTINGS_TITLE.equals(player.getOpenInventory().getTitle())) {
            fillSettings(plugin, player.getOpenInventory().getTopInventory(), arena);
        }
    }

    private static void fillSettings(SopPillarsPlugin plugin, Inventory inventory, PillarsArena arena) {
        ArenaSettings s = arena.getSettings();
        inventory.setItem(0, toggleRow(Material.GRASS_BLOCK, "Place blocks", s.isAllowPlaceBlocks()));
        inventory.setItem(1, toggleRow(Material.STONE, "Break map / original blocks", s.isAllowBreakOriginalBlocks()));
        inventory.setItem(2, toggleRow(Material.COBBLESTONE, "Break player-placed blocks", s.isAllowBreakPlayerBlocks()));
        inventory.setItem(3, toggleRow(Material.DIAMOND_SWORD, "Friendly fire", s.isFriendlyFire()));
        inventory.setItem(4, toggleRow(Material.LAVA_BUCKET, "Lava rises during match", s.isLavaEnabled()));
        inventory.setItem(5, toggleRow(Material.FEATHER, "Slow fall after cages open", s.isAllowSmoothFall()));
        inventory.setItem(6, toggleRow(Material.BOOK, "Loot blacklist mode (vs whitelist)", s.isBlacklistMode()));
        inventory.setItem(7, timingRow(Material.CLOCK, "Countdown (sec)", s.getCountdownSeconds()));
        inventory.setItem(8, timingRow(Material.GLASS, "Cage duration (sec)", s.getCageSeconds()));
        inventory.setItem(9, timingRow(Material.IRON_BARS, "Pre-border delay (sec)", s.getPreBorderDelaySeconds()));
        inventory.setItem(10, timingRow(Material.BARRIER, "Border shrink time (sec)", s.getBorderShrinkSeconds()));
        inventory.setItem(11, timingRow(Material.MAP, "End border diameter", s.getEndBorderDiameter()));
        inventory.setItem(12, timingRow(Material.LAVA_BUCKET, "Lava start delay (sec)", s.getLavaStartDelaySeconds()));
        inventory.setItem(13, timingRow(Material.MAGMA_BLOCK, "Lava rise interval (sec)", s.getLavaRiseIntervalSeconds()));
        inventory.setItem(14, timingRow(Material.REPEATER, "Post-shrink delay (sec)", s.getPostShrinkEndDelaySeconds()));
        inventory.setItem(15, timingRow(Material.CHEST, "Loot interval (sec)", s.getLootIntervalSeconds()));
        inventory.setItem(16, timingRow(Material.TOTEM_OF_UNDYING, "Celebration time (sec)", s.getCelebrationSeconds()));
        inventory.setItem(17, timingRow(Material.FEATHER, "Slow fall duration (sec)", s.getSmoothFallSeconds()));
        for (int slot = 18; slot <= 25; slot++) {
            inventory.setItem(slot, filler());
        }
        inventory.setItem(26, infoBook(plugin, arena));
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack infoBook(SopPillarsPlugin plugin, PillarsArena arena) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + arena.getName());
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Mode: " + arena.getMode());
            lore.add(ChatColor.GRAY + "Teams: " + arena.getTeams() + " x " + arena.getPlayersPerTeam());
            lore.add(ChatColor.DARK_GRAY + "Left click: +1  Right click: -1");
            lore.add(ChatColor.DARK_GRAY + "Shift-click: +/-5");
            meta.setLore(lore);
            book.setItemMeta(meta);
        }
        return book;
    }

    private static ItemStack timingRow(Material icon, String label, int value) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + label);
            meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Value: " + ChatColor.GREEN + value,
                    ChatColor.GRAY + "Left: +1, Right: -1",
                    ChatColor.DARK_GRAY + "Shift-click: +/-5"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack timingRow(Material icon, String label, double value) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + label);
            meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "Value: " + ChatColor.GREEN + String.format(Locale.US, "%.1f", value),
                    ChatColor.GRAY + "Left: +1, Right: -1",
                    ChatColor.DARK_GRAY + "Shift-click: +/-5"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static int adjustInt(int current, boolean rightClick, int delta, int min) {
        int next = rightClick ? current - delta : current + delta;
        return Math.max(min, next);
    }

    private static double adjustDouble(double current, boolean rightClick, double delta, double min) {
        double next = rightClick ? current - delta : current + delta;
        return Math.max(min, next);
    }

    private static ItemStack toggleRow(Material icon, String label, boolean enabled) {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + label);
            meta.setLore(Arrays.asList(
                    ChatColor.YELLOW + "State: " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                    ChatColor.GRAY + "Click to toggle"
            ));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isSettingsTitle(String title) {
        return SETTINGS_TITLE.equals(title);
    }

    public static boolean isCosmeticsTitle(String title) {
        return COSMETICS_TITLE.equals(title);
    }
}
