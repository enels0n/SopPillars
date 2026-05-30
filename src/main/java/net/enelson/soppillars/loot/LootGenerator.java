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
 * Random item rolls for in-match loot.
 */
public final class LootGenerator {

    private LootGenerator() {
    }

    public static ItemStack roll(ArenaSettings settings, List<Material> blacklistPool, Random random) {
        if (!settings.isLootEnabled()) {
            return null;
        }
        LootMode mode = settings.getLootMode();
        switch (mode) {
            case BLACKLIST:
                return rollBlacklist(blacklistPool, random);
            case MIXED:
                return rollMixed(settings, blacklistPool, random);
            case WHITELIST:
            default:
                return rollWhitelist(settings, random);
        }
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
        List<ItemStack> customItems = collectWhitelistItems(settings);
        if (!customItems.isEmpty()) {
            return customItems.get(random.nextInt(customItems.size())).clone();
        }

        List<Material> dynamicPool = buildBlacklistPool(settings);
        if (dynamicPool.isEmpty()) {
            return new ItemStack(Material.BREAD, 1);
        }
        Material material = dynamicPool.get(random.nextInt(dynamicPool.size()));
        return new ItemStack(material, 1);
    }

    private static ItemStack rollMixed(ArenaSettings settings, List<Material> pool, Random random) {
        List<ItemStack> customItems = collectWhitelistItems(settings);
        List<Material> dynamicPool = (pool == null || pool.isEmpty()) ? buildBlacklistPool(settings) : pool;
        if (customItems.isEmpty()) {
            return rollBlacklist(dynamicPool, random);
        }
        if (dynamicPool.isEmpty()) {
            return customItems.get(random.nextInt(customItems.size())).clone();
        }

        double whitelistRollChance = settings.getLootWhitelistRollChance();
        if (whitelistRollChance > 0.0D) {
            if (random.nextDouble() < whitelistRollChance) {
                return customItems.get(random.nextInt(customItems.size())).clone();
            }
            return rollBlacklist(dynamicPool, random);
        }

        int totalEntries = dynamicPool.size() + customItems.size();
        int rolledIndex = random.nextInt(totalEntries);
        if (rolledIndex < dynamicPool.size()) {
            return new ItemStack(dynamicPool.get(rolledIndex), 1);
        }
        return customItems.get(rolledIndex - dynamicPool.size()).clone();
    }

    private static List<ItemStack> collectWhitelistItems(ArenaSettings settings) {
        List<ItemStack> customItems = new ArrayList<ItemStack>();
        for (ItemStack itemStack : settings.getLootWhitelistItems()) {
            if (itemStack != null && itemStack.getType().isItem()) {
                customItems.add(itemStack.clone());
            }
        }
        return customItems;
    }

    private static ItemStack rollBlacklist(List<Material> pool, Random random) {
        if (pool == null || pool.isEmpty()) {
            return new ItemStack(Material.BREAD, 1);
        }
        Material material = pool.get(random.nextInt(pool.size()));
        return new ItemStack(material, 1);
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
