package net.enelson.soppillars.loot;

import net.enelson.soppillars.model.ArenaSettings;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random item rolls for in-match loot (whitelist vs blacklist modes per arena settings).
 * In blacklist mode, exclusions come only from arena {@code loot.blacklist} plus family toggles (potions, spawn eggs, etc.); there is no built-in block denylist.
 */
public final class LootGenerator {

    private LootGenerator() {
    }

    public static ItemStack roll(ArenaSettings settings, List<Material> blacklistPool, Random random) {
        if (!settings.isLootEnabled()) {
            return null;
        }
        if (settings.isBlacklistMode()) {
            return rollBlacklist(blacklistPool, random);
        }
        return rollWhitelist(settings, random);
    }

    /**
     * Builds the pool of materials allowed in blacklist mode (call once per tick, not per player).
     */
    public static List<Material> buildBlacklistPool(ArenaSettings settings) {
        Set<String> extraBlocked = new HashSet<String>();
        for (String name : settings.getLootBlacklist()) {
            if (name != null && !name.isEmpty()) {
                extraBlocked.add(name.trim().toUpperCase(Locale.ROOT));
            }
        }

        List<Material> pool = new ArrayList<Material>();
        for (Material material : Material.values()) {
            if (!material.isItem()) {
                continue;
            }
            if (extraBlocked.contains(material.name())) {
                continue;
            }
            if (isBlockedByFamily(material, settings)) {
                continue;
            }
            pool.add(material);
        }
        return pool;
    }

    private static ItemStack rollWhitelist(ArenaSettings settings, Random random) {
        List<String> names = settings.getLootWhitelist();
        if (names != null && !names.isEmpty()) {
            String pick = names.get(random.nextInt(names.size()));
            Material material = Material.matchMaterial(pick.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
            if (material == null || !material.isItem()) {
                material = Material.STICK;
            }
            return new ItemStack(material, 1);
        }

        List<Material> dynamicPool = buildDynamicWhitelistPool(settings);
        if (dynamicPool.isEmpty()) {
            return new ItemStack(Material.BREAD, 1);
        }
        Material material = dynamicPool.get(random.nextInt(dynamicPool.size()));
        return new ItemStack(material, 1);
    }

    private static ItemStack rollBlacklist(List<Material> pool, Random random) {
        if (pool == null || pool.isEmpty()) {
            return new ItemStack(Material.BREAD, 1);
        }
        Material material = pool.get(random.nextInt(pool.size()));
        return new ItemStack(material, 1);
    }

    private static List<Material> buildDynamicWhitelistPool(ArenaSettings settings) {
        List<Material> pool = new ArrayList<Material>();
        for (Material material : Material.values()) {
            if (!material.isItem()) {
                continue;
            }
            if (isBlockedByFamily(material, settings)) {
                continue;
            }
            pool.add(material);
        }
        return pool;
    }

    private static boolean isBlockedByFamily(Material material, ArenaSettings settings) {
        String name = material.name();
        if (material == Material.ENCHANTED_BOOK && !settings.isAllowEnchantedBooks()) {
            return true;
        }
        if (!settings.isAllowSpawnEggs() && name.endsWith("_SPAWN_EGG")) {
            return true;
        }
        if (!settings.isAllowTippedArrows() && material == Material.TIPPED_ARROW) {
            return true;
        }
        if (!settings.isAllowPotions()) {
            if (material == Material.POTION || material == Material.SPLASH_POTION || material == Material.LINGERING_POTION) {
                return true;
            }
            if (name.contains("POTION")) {
                return true;
            }
        }
        return false;
    }

    public static Random threadLocalRandom() {
        return ThreadLocalRandom.current();
    }
}
