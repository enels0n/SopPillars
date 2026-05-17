package net.enelson.soppillars.config;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.model.ArenaSettings;
import net.enelson.soppillars.model.SerializedLocation;
import net.enelson.soppillars.model.VictoryEffectShape;
import net.enelson.soppillars.model.VictoryEffectType;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public final class PillarsConfig {

    private final SopPillarsPlugin plugin;
    private File arenasFolder;
    private File cagesFolder;
    private File cosmeticsFolder;
    private File kitsFolder;
    private File snapshotsFolder;
    private File arenaEditBackupsFolder;
    private ArenaSettings defaultArenaSettings;
    private SerializedLocation defaultPostGameSpawn;
    private boolean blockNaturalSpawnsInArenas;

    public PillarsConfig(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.arenasFolder = ensureFolder(plugin.getConfig().getString("paths.arenas-folder", "arenas"));
        this.cagesFolder = ensureFolder(plugin.getConfig().getString("paths.cages-folder", "cages"));
        ensureDefaultCageSchematic();
        this.cosmeticsFolder = ensureFolder(plugin.getConfig().getString("paths.cosmetics-folder", "cosmetics"));
        this.kitsFolder = ensureFolder(plugin.getConfig().getString("paths.kits-folder", "kits"));
        this.snapshotsFolder = ensureFolder(plugin.getConfig().getString("paths.snapshots-folder", "snapshots"));
        this.arenaEditBackupsFolder = ensureFolder(plugin.getConfig().getString("paths.arena-edit-backups-folder", "arena-edit-backups"));
        this.defaultArenaSettings = ArenaSettings.defaults(
                plugin.getConfig().getInt("settings.default-countdown-seconds", 10),
                plugin.getConfig().getInt("settings.default-cage-seconds", 5),
                plugin.getConfig().getInt("settings.default-pre-border-delay-seconds", 30),
                plugin.getConfig().getInt("settings.default-border-shrink-seconds", 120),
                plugin.getConfig().getDouble("settings.default-end-border-diameter", 12.0),
                plugin.getConfig().getBoolean("settings.default-lava-enabled", false),
                plugin.getConfig().getInt("settings.default-lava-start-delay-seconds", 90),
                plugin.getConfig().getInt("settings.default-lava-rise-interval-seconds", 10),
                plugin.getConfig().getInt("settings.default-post-shrink-end-delay-seconds", 30),
                plugin.getConfig().getInt("settings.default-min-players", 2),
                plugin.getConfig().getInt("settings.default-min-filled-teams", 2),
                plugin.getConfig().getBoolean("settings.default-allow-place-blocks", true),
                plugin.getConfig().getBoolean("settings.default-allow-break-original-blocks", false),
                plugin.getConfig().getBoolean("settings.default-allow-break-player-blocks", true),
                plugin.getConfig().getBoolean("settings.default-allow-smooth-fall", false),
                plugin.getConfig().getBoolean("settings.default-friendly-fire", false),
                new ArrayList<String>(plugin.getConfig().getStringList("settings.default-allowed-commands"))
        );
        this.defaultArenaSettings.setLootEnabled(plugin.getConfig().getBoolean("settings.default-loot-enabled", true));
        this.defaultArenaSettings.setLootIntervalSeconds(plugin.getConfig().getInt("settings.default-loot-interval-seconds", 8));
        this.defaultArenaSettings.setBlacklistMode(plugin.getConfig().getBoolean("settings.default-loot-blacklist-mode", false));
        this.defaultArenaSettings.setLootWhitelist(new ArrayList<String>(plugin.getConfig().getStringList("settings.default-loot-whitelist")));
        this.defaultArenaSettings.setLootBlacklist(new ArrayList<String>(plugin.getConfig().getStringList("settings.default-loot-blacklist")));
        this.defaultArenaSettings.setSmoothFallSeconds(plugin.getConfig().getInt("settings.default-smooth-fall-seconds", 10));
        this.defaultArenaSettings.setCelebrationSeconds(plugin.getConfig().getInt("settings.default-celebration-seconds", 10));
        this.defaultArenaSettings.setVictoryEffectType(VictoryEffectType.parse(plugin.getConfig().getString("settings.default-victory-effect.type", "fireworks"), VictoryEffectType.FIREWORKS));
        this.defaultArenaSettings.setVictoryEffectShape(VictoryEffectShape.parse(plugin.getConfig().getString("settings.default-victory-effect.shape", "square"), VictoryEffectShape.SQUARE));
        this.defaultArenaSettings.setVictoryEffectRadius(plugin.getConfig().getDouble("settings.default-victory-effect.radius", 8.0D));
        this.defaultArenaSettings.setVictoryEffectIntervalTicks(plugin.getConfig().getInt("settings.default-victory-effect.interval-ticks", 20));
        this.defaultArenaSettings.setVictoryEffectSpawnHeight(plugin.getConfig().getDouble("settings.default-victory-effect.spawn-height", 14.0D));
        this.defaultArenaSettings.setVictoryEffectAmountPerWave(plugin.getConfig().getInt("settings.default-victory-effect.amount-per-wave", 2));
        this.defaultArenaSettings.setVictoryEffectBlockMaterial(parseVictoryBlockMaterial(plugin.getConfig().getString("settings.default-victory-effect.block-material", "DIAMOND_BLOCK")));
        this.defaultArenaSettings.setVictoryCommands(new ArrayList<String>(plugin.getConfig().getStringList("settings.default-victory-commands")));
        ConfigurationSection spawnSection = plugin.getConfig().getConfigurationSection("settings.global-spawn");
        this.defaultPostGameSpawn = SerializedLocation.fromSection(spawnSection);
        this.blockNaturalSpawnsInArenas = plugin.getConfig().getBoolean("settings.block-natural-spawns-in-arenas", true);
    }

    public boolean isBlockNaturalSpawnsInArenas() {
        return blockNaturalSpawnsInArenas;
    }

    public File getArenasFolder() {
        return arenasFolder;
    }

    public File getCagesFolder() {
        return cagesFolder;
    }

    public File getCosmeticsFolder() {
        return cosmeticsFolder;
    }

    public File getKitsFolder() {
        return kitsFolder;
    }

    public File getSnapshotsFolder() {
        return snapshotsFolder;
    }

    public File getArenaEditBackupsFolder() {
        return arenaEditBackupsFolder;
    }

    public ArenaSettings createDefaultArenaSettings() {
        List<String> commands = new ArrayList<String>(defaultArenaSettings.getAllowedCommands());
        ArenaSettings settings = ArenaSettings.defaults(
                defaultArenaSettings.getCountdownSeconds(),
                defaultArenaSettings.getCageSeconds(),
                defaultArenaSettings.getPreBorderDelaySeconds(),
                defaultArenaSettings.getBorderShrinkSeconds(),
                defaultArenaSettings.getEndBorderDiameter(),
                defaultArenaSettings.isLavaEnabled(),
                defaultArenaSettings.getLavaStartDelaySeconds(),
                defaultArenaSettings.getLavaRiseIntervalSeconds(),
                defaultArenaSettings.getPostShrinkEndDelaySeconds(),
                defaultArenaSettings.getMinPlayers(),
                defaultArenaSettings.getMinFilledTeams(),
                defaultArenaSettings.isAllowPlaceBlocks(),
                defaultArenaSettings.isAllowBreakOriginalBlocks(),
                defaultArenaSettings.isAllowBreakPlayerBlocks(),
                defaultArenaSettings.isAllowSmoothFall(),
                defaultArenaSettings.isFriendlyFire(),
                commands
        );
        settings.setBlacklistMode(defaultArenaSettings.isBlacklistMode());
        settings.setAllowEnchantedBooks(defaultArenaSettings.isAllowEnchantedBooks());
        settings.setAllowPotions(defaultArenaSettings.isAllowPotions());
        settings.setAllowTippedArrows(defaultArenaSettings.isAllowTippedArrows());
        settings.setAllowSpawnEggs(defaultArenaSettings.isAllowSpawnEggs());
        settings.setLootEnabled(defaultArenaSettings.isLootEnabled());
        settings.setLootIntervalSeconds(defaultArenaSettings.getLootIntervalSeconds());
        settings.setLootWhitelist(new ArrayList<String>(defaultArenaSettings.getLootWhitelist()));
        settings.setLootBlacklist(new ArrayList<String>(defaultArenaSettings.getLootBlacklist()));
        settings.setSmoothFallSeconds(defaultArenaSettings.getSmoothFallSeconds());
        settings.setCelebrationSeconds(defaultArenaSettings.getCelebrationSeconds());
        settings.setVictoryEffectType(defaultArenaSettings.getVictoryEffectType());
        settings.setVictoryEffectShape(defaultArenaSettings.getVictoryEffectShape());
        settings.setVictoryEffectRadius(defaultArenaSettings.getVictoryEffectRadius());
        settings.setVictoryEffectIntervalTicks(defaultArenaSettings.getVictoryEffectIntervalTicks());
        settings.setVictoryEffectSpawnHeight(defaultArenaSettings.getVictoryEffectSpawnHeight());
        settings.setVictoryEffectAmountPerWave(defaultArenaSettings.getVictoryEffectAmountPerWave());
        settings.setVictoryEffectBlockMaterial(defaultArenaSettings.getVictoryEffectBlockMaterial());
        settings.setVictoryCommands(new ArrayList<String>(defaultArenaSettings.getVictoryCommands()));
        return settings;
    }

    public SerializedLocation getDefaultPostGameSpawn() {
        return defaultPostGameSpawn;
    }

    private File ensureFolder(String relativePath) {
        File folder = new File(plugin.getDataFolder(), relativePath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    /**
     * Ensures default cage schematic exists in data/cages.
     * If admin replaced it, do nothing; if deleted, restore from plugin resources.
     */
    private void ensureDefaultCageSchematic() {
        File target = new File(cagesFolder, "default.schem");
        if (target.isFile()) {
            return;
        }
        InputStream resource = plugin.getResource("default.schem");
        if (resource == null) {
            plugin.getLogger().warning("Resource default.schem not found in plugin jar; cannot restore cages/default.schem.");
            return;
        }
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (InputStream in = resource; FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to restore cages/default.schem: " + exception.getMessage());
        }
    }

    private Material parseVictoryBlockMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Material.DIAMOND_BLOCK;
        }
        try {
            Material material = Material.valueOf(raw.trim().toUpperCase());
            return material == null ? Material.DIAMOND_BLOCK : material;
        } catch (IllegalArgumentException ignored) {
            return Material.DIAMOND_BLOCK;
        }
    }
}
