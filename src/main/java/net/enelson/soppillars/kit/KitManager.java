package net.enelson.soppillars.kit;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KitManager {

    private static final String KIT_MENU_TITLE = ChatColor.DARK_AQUA + "Choose Kit";
    private static final String NO_KIT_ID = "__none__";

    private final SopPillarsPlugin plugin;
    private final Map<String, KitDefinition> kits = new LinkedHashMap<String, KitDefinition>();
    private final Map<UUID, String> selectedKitByPlayer = new LinkedHashMap<UUID, String>();

    public KitManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        kits.clear();
        ensureDefaultKit();
        File[] files = plugin.getPillarsConfig().getKitsFolder().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            KitDefinition definition = loadKit(file);
            if (definition != null) {
                kits.put(normalize(definition.getId()), definition);
            }
        }
    }

    public void openKitMenu(Player player) {
        List<KitDefinition> available = getAvailableKits(player);
        int size = Math.max(9, ((available.size()) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, KIT_MENU_TITLE);
        for (int index = 0; index < available.size(); index++) {
            inventory.setItem(index, createKitIcon(player, available.get(index)));
        }
        inventory.setItem(available.size(), createNoKitIcon(player));
        player.openInventory(inventory);
    }

    public void handleKitMenuClick(Player player, int slot) {
        List<KitDefinition> available = getAvailableKits(player);
        if (slot < 0 || slot >= available.size()) {
            if (slot == available.size()) {
                selectedKitByPlayer.put(player.getUniqueId(), NO_KIT_ID);
                player.closeInventory();
                plugin.getMessageService().send(player, "kit-disabled");
            }
            return;
        }
        KitDefinition selected = available.get(slot);
        selectedKitByPlayer.put(player.getUniqueId(), normalize(selected.getId()));
        player.closeInventory();
        plugin.getMessageService().send(player, "kit-selected", Collections.singletonMap("kit", selected.getDisplayName()));
    }

    public boolean isKitInventory(String title) {
        return KIT_MENU_TITLE.equals(title);
    }

    public void giveSelectedKit(Player player) {
        KitDefinition definition = getSelectedKit(player);
        if (definition == null) {
            return;
        }
        for (ItemStack item : definition.getItems()) {
            if (item != null) {
                player.getInventory().addItem(item.clone());
            }
        }
        player.updateInventory();
    }

    public SopPillarsPlugin getPlugin() {
        return plugin;
    }

    public List<String> getKitIds() {
        List<String> ids = new ArrayList<String>();
        for (KitDefinition definition : kits.values()) {
            ids.add(definition.getId());
        }
        return ids;
    }

    public int getKitItemCount(String kitId) {
        KitDefinition kit = kits.get(normalize(kitId));
        if (kit == null) {
            return -1;
        }
        return kit.getItems().size();
    }

    public boolean addItemToKit(String kitId, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        File file = findKitFileById(kitId);
        if (file == null) {
            return false;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = readItems(config);
        items.add(item.clone());
        config.set("items", items);
        if (!saveKitFile(file, config)) {
            return false;
        }
        reload();
        return true;
    }

    public boolean removeItemFromKit(String kitId, int oneBasedIndex) {
        if (oneBasedIndex <= 0) {
            return false;
        }
        File file = findKitFileById(kitId);
        if (file == null) {
            return false;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ItemStack> items = readItems(config);
        int index = oneBasedIndex - 1;
        if (index < 0 || index >= items.size()) {
            return false;
        }
        items.remove(index);
        config.set("items", items);
        if (!saveKitFile(file, config)) {
            return false;
        }
        reload();
        return true;
    }

    public boolean clearKitItems(String kitId) {
        File file = findKitFileById(kitId);
        if (file == null) {
            return false;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("items", new ArrayList<ItemStack>());
        if (!saveKitFile(file, config)) {
            return false;
        }
        reload();
        return true;
    }

    private KitDefinition getSelectedKit(Player player) {
        String selectedId = selectedKitByPlayer.get(player.getUniqueId());
        if (NO_KIT_ID.equals(selectedId)) {
            return null;
        }
        KitDefinition selected = selectedId == null ? null : kits.get(selectedId);
        if (selected != null && hasAccess(player, selected)) {
            return selected;
        }
        List<KitDefinition> available = getAvailableKits(player);
        if (available.isEmpty()) {
            return null;
        }
        KitDefinition fallback = available.get(0);
        selectedKitByPlayer.put(player.getUniqueId(), normalize(fallback.getId()));
        return fallback;
    }

    private List<KitDefinition> getAvailableKits(Player player) {
        List<KitDefinition> available = new ArrayList<KitDefinition>();
        for (KitDefinition definition : kits.values()) {
            if (hasAccess(player, definition)) {
                available.add(definition);
            }
        }
        return available;
    }

    private boolean hasAccess(Player player, KitDefinition definition) {
        String permission = definition.getPermission();
        return permission == null || permission.isEmpty() || player.hasPermission(permission) || player.isOp();
    }

    private ItemStack createKitIcon(Player player, KitDefinition definition) {
        ItemStack item = new ItemStack(definition.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + definition.getDisplayName());
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Kit id: " + definition.getId());
            if (normalize(definition.getId()).equals(selectedKitByPlayer.get(player.getUniqueId()))) {
                lore.add(ChatColor.GREEN + "Currently selected");
            } else {
                lore.add(ChatColor.YELLOW + "Click to select");
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoKitIcon(Player player) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "No kit");
            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Play without starting kit.");
            if (NO_KIT_ID.equals(selectedKitByPlayer.get(player.getUniqueId()))) {
                lore.add(ChatColor.GREEN + "Currently selected");
            } else {
                lore.add(ChatColor.YELLOW + "Click to disable kit");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private KitDefinition loadKit(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String id = config.getString("id", file.getName().replace(".yml", ""));
        String displayName = config.getString("display-name", id);
        Material icon = parseMaterial(config.getString("icon", "CHEST"), Material.CHEST);
        String permission = config.getString("permission", "");
        List<?> rawItems = config.getList("items");
        List<ItemStack> items = new ArrayList<ItemStack>();
        if (rawItems != null) {
            for (Object raw : rawItems) {
                if (raw instanceof ItemStack) {
                    items.add((ItemStack) raw);
                }
            }
        }
        return new KitDefinition(id, displayName, icon, permission, items);
    }

    private void ensureDefaultKit() {
        File file = new File(plugin.getPillarsConfig().getKitsFolder(), "default.yml");
        if (file.exists()) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", "default");
        config.set("display-name", "Default");
        config.set("icon", "STONE_SWORD");
        config.set("permission", "soppillars.kit.default");
        List<ItemStack> items = new ArrayList<ItemStack>();
        items.add(new ItemStack(Material.STONE_SWORD, 1));
        items.add(new ItemStack(Material.COOKED_BEEF, 8));
        config.set("items", items);
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create default kit file: " + exception.getMessage());
        }
    }

    private List<ItemStack> readItems(YamlConfiguration config) {
        List<?> rawItems = config.getList("items");
        List<ItemStack> items = new ArrayList<ItemStack>();
        if (rawItems == null) {
            return items;
        }
        for (Object raw : rawItems) {
            if (raw instanceof ItemStack) {
                items.add(((ItemStack) raw).clone());
            }
        }
        return items;
    }

    private File findKitFileById(String kitId) {
        String normalized = normalize(kitId);
        if (normalized.isEmpty()) {
            return null;
        }
        File[] files = plugin.getPillarsConfig().getKitsFolder().listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (!file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                continue;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = config.getString("id", file.getName().replace(".yml", ""));
            if (normalized.equals(normalize(id))) {
                return file;
            }
        }
        return null;
    }

    private boolean saveKitFile(File file, YamlConfiguration config) {
        try {
            config.save(file);
            return true;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save kit file " + file.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            Material parsed = Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return parsed == null ? fallback : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
