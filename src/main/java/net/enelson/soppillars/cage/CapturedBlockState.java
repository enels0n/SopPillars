package net.enelson.soppillars.cage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public final class CapturedBlockState {

    private final Block block;
    private final Material material;
    private final String blockData;

    public CapturedBlockState(Block block) {
        this.block = block;
        this.material = block.getType();
        this.blockData = block.getBlockData().getAsString();
    }

    public void restore() {
        block.setType(material, false);
        try {
            block.setBlockData(Bukkit.createBlockData(blockData), false);
        } catch (Exception ignored) {
            // If the data string no longer parses, the material restore is still better than leaving cage blocks behind.
        }
    }

    public boolean isAt(Location location) {
        if (location == null || location.getWorld() == null || block.getWorld() == null) {
            return false;
        }
        return block.getWorld().equals(location.getWorld())
                && block.getX() == location.getBlockX()
                && block.getY() == location.getBlockY()
                && block.getZ() == location.getBlockZ();
    }
}
