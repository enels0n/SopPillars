package net.enelson.soppillars.cosmetic;

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

public final class CosmeticManager {

    private static final String MENU_TITLE = ChatColor.DARK_PURPLE + "SopPillars cosmetics";

    private final SopPillarsPlugin plugin;
    private final Map<UUID, String> selectedCageByPlayer = new LinkedHashMap<UUID, String>();
    private File storageFile;
    private YamlConfiguration storage;

    public CosmeticManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        storageFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "cages.yml");
        if (!storageFile.exists()) {
            try {
                storageFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().warning("Failed to create cosmetics storage: " + exception.getMessage());
            }
        }
        storage = YamlConfiguration.loadConfiguration(storageFile);
        selectedCageByPlayer.clear();
        for (String key : storage.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                selectedCageByPlayer.put(uuid, normalize(storage.getString(key, "default")));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid keys
            }
        }
    }

    public boolean isCosmeticsInventory(String title) {
        return MENU_TITLE.equals(title);
    }

    public void openMenu(Player player) {
        List<String> cages = getSelectableCages(player);
        if (cages.isEmpty()) {
            Inventory inventory = Bukkit.createInventory(null, 9, MENU_TITLE);
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.RED + "No cages available");
                barrier.setItemMeta(meta);
            }
            inventory.setItem(4, barrier);
            player.openInventory(inventory);
            return;
        }
        int size = Math.max(9, ((cages.size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, MENU_TITLE);
        String selected = resolveSelectedCageId(player);
        for (int i = 0; i < cages.size(); i++) {
            String cageId = cages.get(i);
            inventory.setItem(i, cageIcon(cageId, cageId.equals(selected)));
        }
        player.openInventory(inventory);
    }

    public void handleMenuClick(Player player, int slot) {
        List<String> cages = getSelectableCages(player);
        if (slot < 0 || slot >= cages.size()) {
            return;
        }
        String id = cages.get(slot);
        setSelectedCageId(player.getUniqueId(), id);
        plugin.getMessageService().send(player, "cage-selected", Collections.singletonMap("cage", id));
        openMenu(player);
    }

    public String resolveSelectedCageId(Player player) {
        String selected = selectedCageByPlayer.get(player.getUniqueId());
        List<String> allowed = getSelectableCages(player);
        if (selected != null && allowed.contains(selected)) {
            return selected;
        }
        if (allowed.contains("default")) {
            return "default";
        }
        return allowed.isEmpty() ? "default" : allowed.get(0);
    }

    public File resolveSelectedCageFile(Player player) {
        String selectedId = resolveSelectedCageId(player);
        File selected = new File(plugin.getPillarsConfig().getCagesFolder(), selectedId + ".schem");
        if (selected.isFile()) {
            return selected;
        }
        File fallback = new File(plugin.getPillarsConfig().getCagesFolder(), "default.schem");
        return fallback.isFile() ? fallback : selected;
    }

    private void setSelectedCageId(UUID playerId, String cageId) {
        String normalized = normalize(cageId);
        selectedCageByPlayer.put(playerId, normalized);
        storage.set(playerId.toString(), normalized);
        saveStorage();
    }

    private void saveStorage() {
        try {
            storage.save(storageFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save cosmetics storage: " + exception.getMessage());
        }
    }

    private List<String> getSelectableCages(Player player) {
        List<String> available = getAvailableCageIds();
        List<String> allowed = new ArrayList<String>();
        for (String id : available) {
            if ("default".equals(id) || player.isOp() || player.hasPermission("soppillars.cage." + id)) {
                allowed.add(id);
            }
        }
        return allowed;
    }

    private List<String> getAvailableCageIds() {
        File[] files = plugin.getPillarsConfig().getCagesFolder().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> ids = new ArrayList<String>();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (!name.endsWith(".schem")) {
                continue;
            }
            ids.add(file.getName().substring(0, file.getName().length() - 6));
        }
        Collections.sort(ids);
        return ids;
    }

    private ItemStack cageIcon(String id, boolean selected) {
        ItemStack item = new ItemStack(selected ? Material.LIME_STAINED_GLASS : Material.GRAY_STAINED_GLASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + id);
            List<String> lore = new ArrayList<String>();
            lore.add(selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select");
            lore.add(ChatColor.DARK_GRAY + "Permission: soppillars.cage." + id);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
