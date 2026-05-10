package net.enelson.soppillars.match;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Drops inventory, armor, off-hand and experience like a death when elimination happens without {@link org.bukkit.event.entity.PlayerDeathEvent}.
 */
public final class MatchEliminationDrops {

    private static final int MAX_ORB_VALUE = 2477;

    private MatchEliminationDrops() {
    }

    public static void dropInventoryAndExperience(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        PlayerInventory inv = player.getInventory();
        for (ItemStack stack : inv.getStorageContents()) {
            dropIfPresent(world, loc, stack);
        }
        for (ItemStack stack : inv.getArmorContents()) {
            dropIfPresent(world, loc, stack);
        }
        dropIfPresent(world, loc, inv.getItemInOffHand());

        int xp = player.getTotalExperience();
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(0);

        spawnExperienceOrbs(world, loc, xp);

        inv.clear();
        inv.setArmorContents(new ItemStack[] { null, null, null, null });
        inv.setItemInOffHand(null);
    }

    private static void dropIfPresent(World world, Location loc, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        int amount = Math.max(1, stack.getAmount());
        ItemStack single = stack.clone();
        single.setAmount(1);
        for (int i = 0; i < amount; i++) {
            world.dropItemNaturally(loc, single.clone());
        }
    }

    private static void spawnExperienceOrbs(World world, Location loc, int total) {
        int remaining = total;
        while (remaining > 0) {
            int chunk = Math.min(remaining, MAX_ORB_VALUE);
            ExperienceOrb orb = world.spawn(loc, ExperienceOrb.class);
            orb.setExperience(chunk);
            remaining -= chunk;
        }
    }
}
