package net.enelson.soppillars.rollback;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.arena.PillarsArena;
import net.enelson.soppillars.match.RunningMatch;
import net.enelson.soppillars.model.SerializedCuboid;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Baseline block snapshot taken on {@code /pillars save} and restored after each match (gameplay region).
 * Format v1: sequential block data strings in X → Z → Y order for a cuboid.
 */
public final class ArenaSnapshotManager {

    private static final byte[] MAGIC = new byte[] { 'S', 'P', 'S', '1' };
    private static final int VERSION = 1;

    private final SopPillarsPlugin plugin;

    public ArenaSnapshotManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public File snapshotFile(String arenaName) {
        String safe = arenaName == null ? "unknown" : arenaName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
        return new File(plugin.getPillarsConfig().getSnapshotsFolder(), safe + ".sps");
    }

    /**
     * Writes gameplay cuboid blocks to disk. Call from main thread.
     */
    public boolean captureSnapshot(PillarsArena arena) {
        SerializedCuboid cuboid = arena.getGameplayArea();
        if (cuboid == null) {
            plugin.getLogger().warning("Cannot snapshot arena " + arena.getName() + ": no gameplay area.");
            return false;
        }
        World world = Bukkit.getWorld(cuboid.getMin().getWorld());
        if (world == null) {
            plugin.getLogger().warning("Cannot snapshot arena " + arena.getName() + ": world not loaded.");
            return false;
        }

        int[] b = cuboid.getInclusiveBlockBounds();
        int x0 = b[0];
        int y0 = b[1];
        int z0 = b[2];
        int x1 = b[3];
        int y1 = b[4];
        int z1 = b[5];

        long total = (long) (x1 - x0 + 1) * (long) (z1 - z0 + 1) * (long) (y1 - y0 + 1);
        if (total <= 0L || total > Integer.MAX_VALUE) {
            plugin.getLogger().warning("Cannot snapshot arena " + arena.getName() + ": invalid bounds.");
            return false;
        }

        File file = snapshotFile(arena.getName());
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.write(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(world.getName());
            out.writeInt(x0);
            out.writeInt(y0);
            out.writeInt(z0);
            out.writeInt(x1);
            out.writeInt(y1);
            out.writeInt(z1);
            out.writeLong(total);

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        String data = block.getBlockData().getAsString();
                        out.writeUTF(data);
                    }
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to write snapshot for " + arena.getName() + ": " + exception.getMessage());
            return false;
        }

        plugin.getLogger().info("Arena baseline snapshot saved for " + arena.getName() + " (" + total + " blocks).");
        return true;
    }

    /**
     * Restores gameplay cuboid from last snapshot. Safe if file missing (logs only).
     */
    public void restoreArenaBaseline(PillarsArena arena) {
        File file = snapshotFile(arena.getName());
        if (!file.isFile()) {
            plugin.getLogger().warning("No baseline snapshot for arena " + arena.getName()
                    + "; skipping rollback (save the arena after editing to create one).");
            return;
        }

        SerializedCuboid cuboid = arena.getGameplayArea();
        if (cuboid == null) {
            return;
        }
        World world = Bukkit.getWorld(cuboid.getMin().getWorld());
        if (world == null) {
            plugin.getLogger().warning("Cannot restore arena " + arena.getName() + ": world not loaded.");
            return;
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] magic = new byte[4];
            if (in.read(magic) != 4 || magic[0] != MAGIC[0] || magic[1] != MAGIC[1] || magic[2] != MAGIC[2] || magic[3] != MAGIC[3]) {
                plugin.getLogger().severe("Invalid snapshot file for arena " + arena.getName());
                return;
            }
            int version = in.readInt();
            if (version != VERSION) {
                plugin.getLogger().severe("Unsupported snapshot version " + version + " for arena " + arena.getName());
                return;
            }
            String worldName = in.readUTF();
            if (!world.getName().equalsIgnoreCase(worldName)) {
                plugin.getLogger().warning("Snapshot world mismatch for " + arena.getName() + ": expected " + worldName);
            }
            int x0 = in.readInt();
            int y0 = in.readInt();
            int z0 = in.readInt();
            int x1 = in.readInt();
            int y1 = in.readInt();
            int z1 = in.readInt();
            long expected = in.readLong();
            long count = (long) (x1 - x0 + 1) * (long) (z1 - z0 + 1) * (long) (y1 - y0 + 1);
            if (count != expected) {
                plugin.getLogger().severe("Snapshot block count mismatch for " + arena.getName());
                return;
            }

            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = y0; y <= y1; y++) {
                        String dataString = in.readUTF();
                        try {
                            BlockData data = Bukkit.createBlockData(dataString);
                            Block block = world.getBlockAt(x, y, z);
                            block.setBlockData(data, false);
                        } catch (IllegalArgumentException ignored) {
                            world.getBlockAt(x, y, z).setType(Material.AIR, false);
                        }
                    }
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to restore snapshot for " + arena.getName() + ": " + exception.getMessage());
        }
    }

    /**
     * Removes dropped items, projectiles, and non-player entities inside the gameplay cuboid after rollback.
     */
    public void clearForeignEntities(PillarsArena arena) {
        SerializedCuboid cuboid = arena.getGameplayArea();
        if (cuboid == null) {
            return;
        }
        World world = Bukkit.getWorld(cuboid.getMin().getWorld());
        if (world == null) {
            return;
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            if (!cuboid.contains(entity.getLocation())) {
                continue;
            }
            entity.remove();
        }
    }

    public void clearTrackedResidualFluids(RunningMatch match) {
        if (match == null) {
            return;
        }
        for (RunningMatch.TrackedBlock tracked : match.getTrackedFluidBlocks()) {
            World world = Bukkit.getWorld(tracked.getWorldName());
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(tracked.getX(), tracked.getY(), tracked.getZ());
            if (isResidualFluidBlock(block.getType())) {
                block.setType(Material.AIR, true);
            }
        }
    }

    private boolean isResidualFluidBlock(Material material) {
        switch (material) {
            case WATER:
            case LAVA:
            case BUBBLE_COLUMN:
            case KELP:
            case KELP_PLANT:
            case SEAGRASS:
            case TALL_SEAGRASS:
                return true;
            default:
                return false;
        }
    }

    public void deleteSnapshotIfExists(String arenaName) {
        File file = snapshotFile(arenaName);
        if (file.isFile()) {
            file.delete();
        }
    }
}
