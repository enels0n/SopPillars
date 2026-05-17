package net.enelson.soppillars.cosmetic;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.model.VictoryEffectType;
import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.item.ItemUtils;
import net.enelson.sopli.lib.text.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CosmeticManager {

    private static final String ROOT_TITLE = ChatColor.DARK_PURPLE + "SopPillars cosmetics";
    private static final String CAGES_TITLE = ChatColor.DARK_PURPLE + "Cages";
    private static final String VICTORY_TITLE = ChatColor.DARK_PURPLE + "Victory Effects";
    private static final String KILL_TITLE = ChatColor.DARK_PURPLE + "Kill Effects";
    private static final String DEATH_TITLE = ChatColor.DARK_PURPLE + "Death Effects";

    private final SopPillarsPlugin plugin;
    private final Map<UUID, String> selectedCageByPlayer = new LinkedHashMap<UUID, String>();
    private final Map<UUID, String> selectedVictoryEffectByPlayer = new LinkedHashMap<UUID, String>();
    private final Map<UUID, String> selectedKillEffectByPlayer = new LinkedHashMap<UUID, String>();
    private final Map<UUID, String> selectedDeathEffectByPlayer = new LinkedHashMap<UUID, String>();
    private final Map<String, CageDefinition> cagesById = new LinkedHashMap<String, CageDefinition>();
    private final Map<String, VictoryEffectDefinition> victoryEffectsById = new LinkedHashMap<String, VictoryEffectDefinition>();
    private final Map<String, BurstEffectDefinition> killEffectsById = new LinkedHashMap<String, BurstEffectDefinition>();
    private final Map<String, BurstEffectDefinition> deathEffectsById = new LinkedHashMap<String, BurstEffectDefinition>();
    private File selectionFile;
    private File cagesFile;
    private File victoryEffectsFile;
    private File killEffectsFile;
    private File deathEffectsFile;
    private YamlConfiguration selectionStorage;

    public CosmeticManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        selectionFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "selections.yml");
        cagesFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "cages.yml");
        victoryEffectsFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "victory-effects.yml");
        killEffectsFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "kill-effects.yml");
        deathEffectsFile = new File(plugin.getPillarsConfig().getCosmeticsFolder(), "death-effects.yml");
        ensureFile(selectionFile);
        ensureDefaultCagesFile();
        ensureDefaultVictoryEffectsFile();
        ensureDefaultKillEffectsFile();
        ensureDefaultDeathEffectsFile();
        selectionStorage = YamlConfiguration.loadConfiguration(selectionFile);
        selectedCageByPlayer.clear();
        selectedVictoryEffectByPlayer.clear();
        selectedKillEffectByPlayer.clear();
        selectedDeathEffectByPlayer.clear();
        cagesById.clear();
        victoryEffectsById.clear();
        killEffectsById.clear();
        deathEffectsById.clear();
        loadSelections();
        loadCages();
        loadVictoryEffects();
        loadKillEffects();
        loadDeathEffects();
    }

    public boolean isManagedInventory(String title) {
        return ROOT_TITLE.equals(title)
                || CAGES_TITLE.equals(title)
                || VICTORY_TITLE.equals(title)
                || KILL_TITLE.equals(title)
                || DEATH_TITLE.equals(title);
    }

    public void openMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 18, ROOT_TITLE);
        inventory.setItem(1, buildMenuItem(player, Material.BLAZE_POWDER, "&cKill Effects",
                Arrays.asList(
                        "&7Choose your elimination burst.",
                        "&8Selected: &f" + stripColor(selectedBurstName(resolveSelectedKillEffect(player), "Default Kill Burst"))
                )));
        inventory.setItem(3, buildMenuItem(player, Material.GLASS, "&bCages",
                Arrays.asList(
                        "&7Choose your cage schematic.",
                        "&8Selected: &f" + stripColor(selectedCageName(player))
                )));
        inventory.setItem(5, buildMenuItem(player, Material.FIREWORK_ROCKET, "&6Victory Effects",
                Arrays.asList(
                        "&7Choose your win celebration.",
                        "&8Selected: &f" + stripColor(selectedVictoryName(player))
                )));
        inventory.setItem(7, buildMenuItem(player, Material.WITHER_ROSE, "&8Death Effects",
                Arrays.asList(
                        "&7Choose your death burst.",
                        "&8Selected: &f" + stripColor(selectedBurstName(resolveSelectedDeathEffect(player), "Default Death Burst"))
                )));
        player.openInventory(inventory);
    }

    public void handleMenuClick(Player player, String title, int slot) {
        if (ROOT_TITLE.equals(title)) {
            if (slot == 1) {
                openKillEffectsMenu(player);
            } else if (slot == 3) {
                openCagesMenu(player);
            } else if (slot == 5) {
                openVictoryEffectsMenu(player);
            } else if (slot == 7) {
                openDeathEffectsMenu(player);
            }
            return;
        }
        if (CAGES_TITLE.equals(title)) {
            handleCageMenuClick(player, slot);
            return;
        }
        if (VICTORY_TITLE.equals(title)) {
            handleVictoryMenuClick(player, slot);
            return;
        }
        if (KILL_TITLE.equals(title)) {
            handleKillMenuClick(player, slot);
            return;
        }
        if (DEATH_TITLE.equals(title)) {
            handleDeathMenuClick(player, slot);
        }
    }

    public String resolveSelectedCageId(Player player) {
        String selected = selectedCageByPlayer.get(player.getUniqueId());
        List<CageDefinition> allowed = getSelectableCages(player);
        for (CageDefinition definition : allowed) {
            if (definition.getId().equals(selected)) {
                return definition.getId();
            }
        }
        for (CageDefinition definition : allowed) {
            if ("default".equals(definition.getId())) {
                return "default";
            }
        }
        return allowed.isEmpty() ? "default" : allowed.get(0).getId();
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

    public VictoryEffectDefinition resolveSelectedVictoryEffect(Player player) {
        String selected = selectedVictoryEffectByPlayer.get(player.getUniqueId());
        List<VictoryEffectDefinition> allowed = getSelectableVictoryEffects(player);
        for (VictoryEffectDefinition effect : allowed) {
            if (effect.getId().equals(selected)) {
                return effect;
            }
        }
        for (VictoryEffectDefinition effect : allowed) {
            if ("default_fireworks".equals(effect.getId())) {
                return effect;
            }
        }
        return allowed.isEmpty() ? null : allowed.get(0);
    }

    public BurstEffectDefinition resolveSelectedKillEffect(Player player) {
        return resolveSelectedBurstEffect(player, selectedKillEffectByPlayer, getSelectableKillEffects(player), "default_kill");
    }

    public BurstEffectDefinition resolveSelectedDeathEffect(Player player) {
        return resolveSelectedBurstEffect(player, selectedDeathEffectByPlayer, getSelectableDeathEffects(player), "default_death");
    }

    public void playBurstEffect(Location location, BurstEffectDefinition definition) {
        if (location == null || definition == null || location.getWorld() == null) {
            return;
        }
        switch (definition.getType()) {
            case TOTEM:
                location.getWorld().spawnParticle(Particle.TOTEM, location, Math.max(1, definition.getParticleCount()),
                        definition.getOffsetX(), definition.getOffsetY(), definition.getOffsetZ(), definition.getExtra());
                break;
            case LIGHTNING_FAKE:
                location.getWorld().strikeLightningEffect(location);
                if (definition.getParticle() != null && definition.getParticleCount() > 0) {
                    location.getWorld().spawnParticle(definition.getParticle(), location, definition.getParticleCount(),
                            definition.getOffsetX(), definition.getOffsetY(), definition.getOffsetZ(), definition.getExtra());
                }
                break;
            case PARTICLE_BURST:
            default:
                if (definition.getParticle() != null && definition.getParticleCount() > 0) {
                    location.getWorld().spawnParticle(definition.getParticle(), location, definition.getParticleCount(),
                            definition.getOffsetX(), definition.getOffsetY(), definition.getOffsetZ(), definition.getExtra());
                }
                break;
        }
        if (definition.getSound() != null) {
            location.getWorld().playSound(location, definition.getSound(), definition.getSoundVolume(), definition.getSoundPitch());
        }
    }

    private void openCagesMenu(Player player) {
        List<CageDefinition> cages = getSelectableCages(player);
        if (cages.isEmpty()) {
            Inventory inventory = Bukkit.createInventory(null, 9, CAGES_TITLE);
            inventory.setItem(4, buildMenuItem(player, Material.BARRIER, ChatColor.RED + "No cages available", Collections.<String>emptyList()));
            player.openInventory(inventory);
            return;
        }
        int size = Math.max(9, ((cages.size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, CAGES_TITLE);
        String selected = resolveSelectedCageId(player);
        for (int i = 0; i < cages.size(); i++) {
            CageDefinition definition = cages.get(i);
            inventory.setItem(i, cageIcon(player, definition, definition.getId().equals(selected)));
        }
        player.openInventory(inventory);
    }

    private void openVictoryEffectsMenu(Player player) {
        List<VictoryEffectDefinition> effects = getSelectableVictoryEffects(player);
        if (effects.isEmpty()) {
            Inventory inventory = Bukkit.createInventory(null, 9, VICTORY_TITLE);
            inventory.setItem(4, buildMenuItem(player, Material.BARRIER, ChatColor.RED + "No victory effects available", Collections.<String>emptyList()));
            player.openInventory(inventory);
            return;
        }
        int size = Math.max(9, ((effects.size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, VICTORY_TITLE);
        VictoryEffectDefinition selected = resolveSelectedVictoryEffect(player);
        String selectedId = selected == null ? "" : selected.getId();
        for (int i = 0; i < effects.size(); i++) {
            VictoryEffectDefinition effect = effects.get(i);
            inventory.setItem(i, victoryIcon(player, effect, effect.getId().equals(selectedId)));
        }
        player.openInventory(inventory);
    }

    private void openKillEffectsMenu(Player player) {
        openBurstEffectsMenu(player, KILL_TITLE, getSelectableKillEffects(player), resolveSelectedKillEffect(player));
    }

    private void openDeathEffectsMenu(Player player) {
        openBurstEffectsMenu(player, DEATH_TITLE, getSelectableDeathEffects(player), resolveSelectedDeathEffect(player));
    }

    private void openBurstEffectsMenu(Player player, String title, List<BurstEffectDefinition> effects, BurstEffectDefinition selected) {
        if (effects.isEmpty()) {
            Inventory inventory = Bukkit.createInventory(null, 9, title);
            inventory.setItem(4, buildMenuItem(player, Material.BARRIER, "&cNo effects available", Collections.<String>emptyList()));
            player.openInventory(inventory);
            return;
        }
        int size = Math.max(9, ((effects.size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, title);
        String selectedId = selected == null ? "" : selected.getId();
        for (int i = 0; i < effects.size(); i++) {
            BurstEffectDefinition effect = effects.get(i);
            inventory.setItem(i, burstIcon(player, effect, effect.getId().equals(selectedId)));
        }
        player.openInventory(inventory);
    }

    private void handleCageMenuClick(Player player, int slot) {
        List<CageDefinition> cages = getSelectableCages(player);
        if (slot < 0 || slot >= cages.size()) {
            return;
        }
        CageDefinition definition = cages.get(slot);
        setSelectedCageId(player.getUniqueId(), definition.getId());
        plugin.getMessageService().send(player, "cage-selected", Collections.singletonMap("cage", definition.getDisplayName()));
        openCagesMenu(player);
    }

    private void handleVictoryMenuClick(Player player, int slot) {
        List<VictoryEffectDefinition> effects = getSelectableVictoryEffects(player);
        if (slot < 0 || slot >= effects.size()) {
            return;
        }
        VictoryEffectDefinition definition = effects.get(slot);
        setSelectedVictoryEffectId(player.getUniqueId(), definition.getId());
        plugin.getMessageService().send(player, "victory-effect-selected", Collections.singletonMap("effect", stripColor(definition.getDisplayName())));
        openVictoryEffectsMenu(player);
    }

    private void handleKillMenuClick(Player player, int slot) {
        List<BurstEffectDefinition> effects = getSelectableKillEffects(player);
        if (slot < 0 || slot >= effects.size()) {
            return;
        }
        BurstEffectDefinition definition = effects.get(slot);
        setSelectedKillEffectId(player.getUniqueId(), definition.getId());
        plugin.getMessageService().send(player, "kill-effect-selected", Collections.singletonMap("effect", stripColor(definition.getDisplayName())));
        openKillEffectsMenu(player);
    }

    private void handleDeathMenuClick(Player player, int slot) {
        List<BurstEffectDefinition> effects = getSelectableDeathEffects(player);
        if (slot < 0 || slot >= effects.size()) {
            return;
        }
        BurstEffectDefinition definition = effects.get(slot);
        setSelectedDeathEffectId(player.getUniqueId(), definition.getId());
        plugin.getMessageService().send(player, "death-effect-selected", Collections.singletonMap("effect", stripColor(definition.getDisplayName())));
        openDeathEffectsMenu(player);
    }

    private void setSelectedCageId(UUID playerId, String cageId) {
        String normalized = normalize(cageId);
        selectedCageByPlayer.put(playerId, normalized);
        selectionStorage.set(playerId.toString() + ".cage", normalized);
        saveSelections();
    }

    private void setSelectedVictoryEffectId(UUID playerId, String effectId) {
        String normalized = normalize(effectId);
        selectedVictoryEffectByPlayer.put(playerId, normalized);
        selectionStorage.set(playerId.toString() + ".victory-effect", normalized);
        saveSelections();
    }

    private void setSelectedKillEffectId(UUID playerId, String effectId) {
        String normalized = normalize(effectId);
        selectedKillEffectByPlayer.put(playerId, normalized);
        selectionStorage.set(playerId.toString() + ".kill-effect", normalized);
        saveSelections();
    }

    private void setSelectedDeathEffectId(UUID playerId, String effectId) {
        String normalized = normalize(effectId);
        selectedDeathEffectByPlayer.put(playerId, normalized);
        selectionStorage.set(playerId.toString() + ".death-effect", normalized);
        saveSelections();
    }

    private void loadSelections() {
        for (String key : selectionStorage.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                selectedCageByPlayer.put(uuid, normalize(selectionStorage.getString(key + ".cage", "default")));
                selectedVictoryEffectByPlayer.put(uuid, normalize(selectionStorage.getString(key + ".victory-effect", "default_fireworks")));
                selectedKillEffectByPlayer.put(uuid, normalize(selectionStorage.getString(key + ".kill-effect", "default_kill")));
                selectedDeathEffectByPlayer.put(uuid, normalize(selectionStorage.getString(key + ".death-effect", "default_death")));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid keys
            }
        }
    }

    private void loadCages() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(cagesFile);
        ConfigurationSection section = config.getConfigurationSection("cages");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection cageSection = section.getConfigurationSection(id);
            if (cageSection == null) {
                continue;
            }
            String displayName = colorize(cageSection.getString("display-name", id));
            String permission = cageSection.getString("permission", "soppillars.cage." + id);
            Material icon = parseMaterial(cageSection.getString("icon"), Material.GRAY_STAINED_GLASS);
            List<String> lore = cageSection.getStringList("lore");
            cagesById.put(normalize(id), new CageDefinition(normalize(id), displayName, permission, icon, lore));
        }
    }

    private void loadVictoryEffects() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(victoryEffectsFile);
        ConfigurationSection section = config.getConfigurationSection("effects");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(id);
            if (effectSection == null) {
                continue;
            }
            String displayName = colorize(effectSection.getString("display-name", id));
            String permission = effectSection.getString("permission", "soppillars.victory-effect." + id);
            Material icon = parseMaterial(effectSection.getString("icon"), Material.FIREWORK_ROCKET);
            List<String> lore = effectSection.getStringList("lore");
            VictoryEffectType type = VictoryEffectType.parse(effectSection.getString("type", "fireworks"), VictoryEffectType.FIREWORKS);
            EntityType entityType = parseEntityType(effectSection.getString("entity"));
            Material blockMaterial = parseMaterial(effectSection.getString("block"), Material.DIAMOND_BLOCK);
            Sound sound = parseSound(effectSection.getString("sound"));
            int soundIntervalTicks = Math.max(0, effectSection.getInt("sound-interval-ticks", 0));
            float soundVolume = (float) effectSection.getDouble("sound-volume", 1.0D);
            float soundPitch = (float) effectSection.getDouble("sound-pitch", 1.0D);
            VictoryEffectDefinition definition = new VictoryEffectDefinition(normalize(id), displayName, permission, icon, lore, type, entityType, blockMaterial, sound, soundIntervalTicks, soundVolume, soundPitch);
            victoryEffectsById.put(definition.getId(), definition);
        }
    }

    private void loadKillEffects() {
        loadBurstEffects(killEffectsFile, "effects", killEffectsById);
    }

    private void loadDeathEffects() {
        loadBurstEffects(deathEffectsFile, "effects", deathEffectsById);
    }

    private void loadBurstEffects(File file, String rootKey, Map<String, BurstEffectDefinition> output) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection(rootKey);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection effectSection = section.getConfigurationSection(id);
            if (effectSection == null) {
                continue;
            }
            String displayName = colorize(effectSection.getString("display-name", id));
            String permission = effectSection.getString("permission", "soppillars.effect." + id);
            Material icon = parseMaterial(effectSection.getString("icon"), Material.NETHER_STAR);
            List<String> lore = effectSection.getStringList("lore");
            BurstEffectType type = BurstEffectType.parse(effectSection.getString("type", "particle_burst"), BurstEffectType.PARTICLE_BURST);
            Particle particle = parseParticle(effectSection.getString("particle"));
            int particleCount = Math.max(0, effectSection.getInt("particle-count", 20));
            double offsetX = effectSection.getDouble("offset-x", 0.35D);
            double offsetY = effectSection.getDouble("offset-y", 0.45D);
            double offsetZ = effectSection.getDouble("offset-z", 0.35D);
            double extra = effectSection.getDouble("extra", 0.02D);
            Sound sound = parseSound(effectSection.getString("sound"));
            float soundVolume = (float) effectSection.getDouble("sound-volume", 1.0D);
            float soundPitch = (float) effectSection.getDouble("sound-pitch", 1.0D);
            BurstEffectDefinition definition = new BurstEffectDefinition(
                    normalize(id),
                    displayName,
                    permission,
                    icon,
                    lore,
                    type,
                    particle,
                    particleCount,
                    offsetX,
                    offsetY,
                    offsetZ,
                    extra,
                    sound,
                    soundVolume,
                    soundPitch
            );
            output.put(definition.getId(), definition);
        }
    }

    private List<CageDefinition> getSelectableCages(Player player) {
        List<CageDefinition> allowed = new ArrayList<CageDefinition>();
        for (String cageId : getAvailableCageIds()) {
            CageDefinition definition = cagesById.get(cageId);
            if (definition == null) {
                definition = new CageDefinition(cageId, colorize("&b" + cageId), "soppillars.cage." + cageId, Material.GRAY_STAINED_GLASS,
                        Collections.singletonList("&7Select this cage."));
            }
            if (definition.getPermission().isEmpty() || player.isOp() || player.hasPermission(definition.getPermission()) || "default".equals(definition.getId())) {
                allowed.add(definition);
            }
        }
        return allowed;
    }

    private List<VictoryEffectDefinition> getSelectableVictoryEffects(Player player) {
        List<VictoryEffectDefinition> effects = new ArrayList<VictoryEffectDefinition>();
        for (VictoryEffectDefinition definition : victoryEffectsById.values()) {
            if (definition.getPermission().isEmpty() || player.isOp() || player.hasPermission(definition.getPermission())) {
                effects.add(definition);
            }
        }
        return effects;
    }

    private List<BurstEffectDefinition> getSelectableKillEffects(Player player) {
        return getSelectableBurstEffects(player, killEffectsById);
    }

    private List<BurstEffectDefinition> getSelectableDeathEffects(Player player) {
        return getSelectableBurstEffects(player, deathEffectsById);
    }

    private List<BurstEffectDefinition> getSelectableBurstEffects(Player player, Map<String, BurstEffectDefinition> source) {
        List<BurstEffectDefinition> effects = new ArrayList<BurstEffectDefinition>();
        for (BurstEffectDefinition definition : source.values()) {
            if (definition.getPermission().isEmpty() || player.isOp() || player.hasPermission(definition.getPermission())) {
                effects.add(definition);
            }
        }
        return effects;
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
            ids.add(normalize(file.getName().substring(0, file.getName().length() - 6)));
        }
        Collections.sort(ids);
        return ids;
    }

    private void ensureFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            file.createNewFile();
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create cosmetics storage: " + exception.getMessage());
        }
    }

    private void ensureDefaultCagesFile() {
        if (cagesFile.exists()) {
            return;
        }
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("cages.default.display-name", "&bDefault Cage");
        defaults.set("cages.default.permission", "");
        defaults.set("cages.default.icon", "LIGHT_BLUE_STAINED_GLASS");
        defaults.set("cages.default.lore", Collections.singletonList("&7Classic glass cage."));
        try {
            defaults.save(cagesFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create cages.yml: " + exception.getMessage());
        }
    }

    private void ensureDefaultVictoryEffectsFile() {
        if (victoryEffectsFile.exists()) {
            return;
        }
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("effects.default_fireworks.display-name", "&6Default Fireworks");
        defaults.set("effects.default_fireworks.permission", "");
        defaults.set("effects.default_fireworks.icon", "FIREWORK_ROCKET");
        defaults.set("effects.default_fireworks.lore", Collections.singletonList("&7Classic winner fireworks."));
        defaults.set("effects.default_fireworks.type", "fireworks");
        defaults.set("effects.default_fireworks.sound", "ENTITY_FIREWORK_ROCKET_LAUNCH");
        defaults.set("effects.default_fireworks.sound-interval-ticks", 20);
        defaults.set("effects.default_fireworks.sound-volume", 1.0D);
        defaults.set("effects.default_fireworks.sound-pitch", 1.0D);
        defaults.set("effects.pig_rain.display-name", "&dPig Rain");
        defaults.set("effects.pig_rain.permission", "soppillars.victory-effect.pig_rain");
        defaults.set("effects.pig_rain.icon", "PORKCHOP");
        defaults.set("effects.pig_rain.lore", Collections.singletonList("&7Celebration with falling pigs."));
        defaults.set("effects.pig_rain.type", "entity_rain");
        defaults.set("effects.pig_rain.entity", "PIG");
        defaults.set("effects.pig_rain.sound", "ENTITY_PIG_AMBIENT");
        defaults.set("effects.pig_rain.sound-interval-ticks", 20);
        defaults.set("effects.pig_rain.sound-volume", 1.0D);
        defaults.set("effects.pig_rain.sound-pitch", 1.0D);
        defaults.set("effects.cow_rain.display-name", "&6Cow Rain");
        defaults.set("effects.cow_rain.permission", "soppillars.victory-effect.cow_rain");
        defaults.set("effects.cow_rain.icon", "BEEF");
        defaults.set("effects.cow_rain.lore", Collections.singletonList("&7Celebration with falling cows."));
        defaults.set("effects.cow_rain.type", "entity_rain");
        defaults.set("effects.cow_rain.entity", "COW");
        defaults.set("effects.cow_rain.sound", "ENTITY_COW_AMBIENT");
        defaults.set("effects.cow_rain.sound-interval-ticks", 20);
        defaults.set("effects.cow_rain.sound-volume", 1.0D);
        defaults.set("effects.cow_rain.sound-pitch", 1.0D);
        defaults.set("effects.chicken_rain.display-name", "&eChicken Rain");
        defaults.set("effects.chicken_rain.permission", "soppillars.victory-effect.chicken_rain");
        defaults.set("effects.chicken_rain.icon", "FEATHER");
        defaults.set("effects.chicken_rain.lore", Collections.singletonList("&7Celebration with falling chickens."));
        defaults.set("effects.chicken_rain.type", "entity_rain");
        defaults.set("effects.chicken_rain.entity", "CHICKEN");
        defaults.set("effects.chicken_rain.sound", "ENTITY_CHICKEN_AMBIENT");
        defaults.set("effects.chicken_rain.sound-interval-ticks", 20);
        defaults.set("effects.chicken_rain.sound-volume", 1.0D);
        defaults.set("effects.chicken_rain.sound-pitch", 1.0D);
        defaults.set("effects.anvil_rain.display-name", "&8Anvil Rain");
        defaults.set("effects.anvil_rain.permission", "soppillars.victory-effect.anvil_rain");
        defaults.set("effects.anvil_rain.icon", "ANVIL");
        defaults.set("effects.anvil_rain.lore", Collections.singletonList("&7Heavy metal celebration."));
        defaults.set("effects.anvil_rain.type", "block_rain");
        defaults.set("effects.anvil_rain.block", "ANVIL");
        defaults.set("effects.anvil_rain.sound", "BLOCK_ANVIL_FALL");
        defaults.set("effects.anvil_rain.sound-interval-ticks", 10);
        defaults.set("effects.anvil_rain.sound-volume", 1.0D);
        defaults.set("effects.anvil_rain.sound-pitch", 1.0D);
        defaults.set("effects.diamond_rain.display-name", "&bDiamond Rain");
        defaults.set("effects.diamond_rain.permission", "soppillars.victory-effect.diamond_rain");
        defaults.set("effects.diamond_rain.icon", "DIAMOND_BLOCK");
        defaults.set("effects.diamond_rain.lore", Collections.singletonList("&7Shower the map with diamonds."));
        defaults.set("effects.diamond_rain.type", "block_rain");
        defaults.set("effects.diamond_rain.block", "DIAMOND_BLOCK");
        defaults.set("effects.diamond_rain.sound", "BLOCK_AMETHYST_BLOCK_CHIME");
        defaults.set("effects.diamond_rain.sound-interval-ticks", 20);
        defaults.set("effects.diamond_rain.sound-volume", 1.0D);
        defaults.set("effects.diamond_rain.sound-pitch", 1.0D);
        try {
            defaults.save(victoryEffectsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create victory-effects.yml: " + exception.getMessage());
        }
    }

    private void ensureDefaultKillEffectsFile() {
        if (killEffectsFile.exists()) {
            return;
        }
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("effects.default_kill.display-name", "&cDefault Kill Burst");
        defaults.set("effects.default_kill.permission", "");
        defaults.set("effects.default_kill.icon", "BLAZE_POWDER");
        defaults.set("effects.default_kill.lore", Collections.singletonList("&7Simple flame burst on kill."));
        defaults.set("effects.default_kill.type", "particle_burst");
        defaults.set("effects.default_kill.particle", "FLAME");
        defaults.set("effects.default_kill.particle-count", 18);
        defaults.set("effects.default_kill.sound", "ENTITY_BLAZE_SHOOT");
        defaults.set("effects.default_kill.sound-volume", 0.9D);
        defaults.set("effects.default_kill.sound-pitch", 1.2D);
        defaults.set("effects.totem_flash.display-name", "&6Totem Flash");
        defaults.set("effects.totem_flash.permission", "soppillars.kill-effect.totem_flash");
        defaults.set("effects.totem_flash.icon", "TOTEM_OF_UNDYING");
        defaults.set("effects.totem_flash.lore", Collections.singletonList("&7A bright totem-like burst."));
        defaults.set("effects.totem_flash.type", "totem");
        defaults.set("effects.totem_flash.sound", "ITEM_TOTEM_USE");
        try {
            defaults.save(killEffectsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create kill-effects.yml: " + exception.getMessage());
        }
    }

    private void ensureDefaultDeathEffectsFile() {
        if (deathEffectsFile.exists()) {
            return;
        }
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("effects.default_death.display-name", "&8Default Death Burst");
        defaults.set("effects.default_death.permission", "");
        defaults.set("effects.default_death.icon", "WITHER_ROSE");
        defaults.set("effects.default_death.lore", Collections.singletonList("&7Smoke burst on death."));
        defaults.set("effects.default_death.type", "particle_burst");
        defaults.set("effects.default_death.particle", "SMOKE_LARGE");
        defaults.set("effects.default_death.particle-count", 20);
        defaults.set("effects.default_death.sound", "ENTITY_ZOMBIE_ATTACK_IRON_DOOR");
        defaults.set("effects.default_death.sound-volume", 0.8D);
        defaults.set("effects.default_death.sound-pitch", 0.8D);
        defaults.set("effects.fake_lightning.display-name", "&bFake Lightning");
        defaults.set("effects.fake_lightning.permission", "soppillars.death-effect.fake_lightning");
        defaults.set("effects.fake_lightning.icon", "LIGHTNING_ROD");
        defaults.set("effects.fake_lightning.lore", Collections.singletonList("&7Strike the death location with fake lightning."));
        defaults.set("effects.fake_lightning.type", "lightning_fake");
        defaults.set("effects.fake_lightning.sound", "ENTITY_LIGHTNING_BOLT_IMPACT");
        try {
            defaults.save(deathEffectsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to create death-effects.yml: " + exception.getMessage());
        }
    }

    private void saveSelections() {
        try {
            selectionStorage.save(selectionFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save cosmetics storage: " + exception.getMessage());
        }
    }

    private ItemStack cageIcon(Player player, CageDefinition definition, boolean selected) {
        List<String> lore = new ArrayList<String>();
        lore.add(selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select");
        lore.addAll(definition.getLore());
        if (!definition.getPermission().isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "Permission: " + definition.getPermission());
        }
        ItemStack item = buildMenuItem(player, definition.getIcon(), definition.getDisplayName(), lore);
        applyItemFlags(item);
        return item;
    }

    private ItemStack victoryIcon(Player player, VictoryEffectDefinition definition, boolean selected) {
        List<String> lore = new ArrayList<String>();
        lore.add(selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select");
        lore.addAll(definition.getLore());
        lore.add(ChatColor.GRAY + "Type: " + definition.getType().name().toLowerCase(Locale.ROOT));
        if (definition.getEntityType() != null) {
            lore.add(ChatColor.GRAY + "Entity: " + definition.getEntityType().name());
        }
        if (definition.getBlockMaterial() != null) {
            lore.add(ChatColor.GRAY + "Block: " + definition.getBlockMaterial().name());
        }
        if (definition.getSound() != null) {
            lore.add(ChatColor.GRAY + "Sound: " + definition.getSound().name());
            lore.add(ChatColor.GRAY + "Sound interval: " + definition.getSoundIntervalTicks());
            lore.add(ChatColor.GRAY + "Volume: " + definition.getSoundVolume());
            lore.add(ChatColor.GRAY + "Pitch: " + definition.getSoundPitch());
        }
        if (!definition.getPermission().isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "Permission: " + definition.getPermission());
        }
        ItemStack item = buildMenuItem(player, definition.getIcon(), definition.getDisplayName(), lore);
        applyItemFlags(item);
        return item;
    }

    private ItemStack burstIcon(Player player, BurstEffectDefinition definition, boolean selected) {
        List<String> lore = new ArrayList<String>();
        lore.add(selected ? ChatColor.GREEN + "Selected" : ChatColor.YELLOW + "Click to select");
        lore.addAll(definition.getLore());
        lore.add(ChatColor.GRAY + "Type: " + definition.getType().name().toLowerCase(Locale.ROOT));
        if (definition.getParticle() != null) {
            lore.add(ChatColor.GRAY + "Particle: " + definition.getParticle().name());
        }
        if (definition.getSound() != null) {
            lore.add(ChatColor.GRAY + "Sound: " + definition.getSound().name());
        }
        if (!definition.getPermission().isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "Permission: " + definition.getPermission());
        }
        ItemStack item = buildMenuItem(player, definition.getIcon(), definition.getDisplayName(), lore);
        applyItemFlags(item);
        return item;
    }

    private ItemStack buildMenuItem(Player player, Material material, String displayName, List<String> lore) {
        String parsedName = decorateText(player, displayName);
        List<String> parsedLore = new ArrayList<String>();
        for (String line : lore) {
            parsedLore.add(decorateText(player, line));
        }
        ItemUtils itemUtils = SopLib.getInstance() == null ? null : SopLib.getInstance().getItemUtils();
        if (itemUtils != null) {
            ItemStack item = itemUtils.createItem(material.name(), 1, null, parsedName, Collections.<String>emptyList(), parsedLore, Collections.<String>emptyList());
            applyItemFlags(item);
            return item;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(parsedName);
            meta.setLore(parsedLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyItemFlags(ItemStack item) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
    }

    private String applyPlaceholders(Player player, String input) {
        if (input == null) {
            return "";
        }
        if (player == null || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return input;
        }
        return PlaceholderAPI.setPlaceholders(player, input);
    }

    private String decorateText(Player player, String input) {
        String withPlaceholders = applyPlaceholders(player, input == null ? "" : input);
        TextUtils textUtils = SopLib.getInstance() == null ? null : SopLib.getInstance().getTextUtils();
        if (textUtils != null) {
            return textUtils.color(withPlaceholders);
        }
        return colorize(withPlaceholders);
    }

    private Material parseMaterial(String raw, Material fallback) {
        if (raw == null || raw.trim().isEmpty()) {
            return fallback;
        }
        try {
            Material material = Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            return material == null ? fallback : material;
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private Particle parseParticle(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Particle.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private Sound parseSound(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            return Sound.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private String stripColor(String text) {
        return ChatColor.stripColor(text == null ? "" : text);
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private BurstEffectDefinition resolveSelectedBurstEffect(Player player,
                                                             Map<UUID, String> selectedMap,
                                                             List<BurstEffectDefinition> allowed,
                                                             String preferredFallbackId) {
        String selected = selectedMap.get(player.getUniqueId());
        for (BurstEffectDefinition definition : allowed) {
            if (definition.getId().equals(selected)) {
                return definition;
            }
        }
        for (BurstEffectDefinition definition : allowed) {
            if (preferredFallbackId.equals(definition.getId())) {
                return definition;
            }
        }
        return allowed.isEmpty() ? null : allowed.get(0);
    }

    private String selectedCageName(Player player) {
        String selectedId = resolveSelectedCageId(player);
        CageDefinition definition = cagesById.get(selectedId);
        return definition == null ? selectedId : definition.getDisplayName();
    }

    private String selectedVictoryName(Player player) {
        VictoryEffectDefinition definition = resolveSelectedVictoryEffect(player);
        return definition == null ? "None" : definition.getDisplayName();
    }

    private String selectedBurstName(BurstEffectDefinition definition, String fallback) {
        return definition == null ? fallback : definition.getDisplayName();
    }
}
