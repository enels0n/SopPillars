package net.enelson.soppillars.match;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Minimal pre-match player snapshot restored when the player exits SopPillars flow.
 */
final class SavedPlayerState {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final float exp;
    private final int level;
    private final int totalExp;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int fireTicks;
    private final GameMode gameMode;

    private SavedPlayerState(Player player) {
        PlayerInventory inv = player.getInventory();
        this.contents = copy(inv.getContents());
        this.armor = copy(inv.getArmorContents());
        this.offHand = cloneItem(inv.getItemInOffHand());
        this.exp = player.getExp();
        this.level = player.getLevel();
        this.totalExp = player.getTotalExperience();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.fireTicks = player.getFireTicks();
        this.gameMode = player.getGameMode();
    }

    static SavedPlayerState capture(Player player) {
        return new SavedPlayerState(player);
    }

    void restore(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setContents(copy(contents));
        inv.setArmorContents(copy(armor));
        inv.setItemInOffHand(cloneItem(offHand));
        player.setTotalExperience(0);
        player.setExp(0.0F);
        player.setLevel(0);
        player.setTotalExperience(Math.max(0, totalExp));
        player.setLevel(Math.max(0, level));
        player.setExp(Math.max(0.0F, Math.min(1.0F, exp)));
        player.setGameMode(gameMode);
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0D, health)));
        player.setFoodLevel(Math.max(0, Math.min(20, foodLevel)));
        player.setSaturation(Math.max(0.0F, saturation));
        player.setFireTicks(Math.max(0, fireTicks));
        player.updateInventory();
    }

    private static ItemStack[] copy(ItemStack[] source) {
        if (source == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            out[i] = cloneItem(source[i]);
        }
        return out;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
