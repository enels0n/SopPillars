package net.enelson.soppillars.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class SerializedCuboid {

    private final SerializedLocation min;
    private final SerializedLocation max;

    public SerializedCuboid(SerializedLocation min, SerializedLocation max) {
        this.min = min;
        this.max = max;
    }

    public static SerializedCuboid fromLocations(Location first, Location second) {
        String world = first.getWorld() == null ? "" : first.getWorld().getName();
        double minX = Math.min(first.getX(), second.getX());
        double minY = Math.min(first.getY(), second.getY());
        double minZ = Math.min(first.getZ(), second.getZ());
        double maxX = Math.max(first.getX(), second.getX());
        double maxY = Math.max(first.getY(), second.getY());
        double maxZ = Math.max(first.getZ(), second.getZ());
        return new SerializedCuboid(
                new SerializedLocation(world, minX, minY, minZ, 0.0F, 0.0F),
                new SerializedLocation(world, maxX, maxY, maxZ, 0.0F, 0.0F)
        );
    }

    public static SerializedCuboid fromSection(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        SerializedLocation min = SerializedLocation.fromSection(section.getConfigurationSection("min"));
        SerializedLocation max = SerializedLocation.fromSection(section.getConfigurationSection("max"));
        if (min == null || max == null) {
            return null;
        }
        return new SerializedCuboid(min, max);
    }

    public void save(ConfigurationSection section) {
        min.save(section.createSection("min"));
        max.save(section.createSection("max"));
    }

    public SerializedLocation getMin() {
        return min;
    }

    public SerializedLocation getMax() {
        return max;
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (!location.getWorld().getName().equalsIgnoreCase(min.getWorld())) {
            return false;
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= min.getX() && x <= max.getX()
                && y >= min.getY() && y <= max.getY()
                && z >= min.getZ() && z <= max.getZ();
    }

    public Location getCenter(World world) {
        return new Location(
                world,
                (min.getX() + max.getX()) / 2.0D + 0.5D,
                max.getY() + 0.1D,
                (min.getZ() + max.getZ()) / 2.0D + 0.5D
        );
    }

    /**
     * Inclusive block coordinates: x0,y0,z0,x1,y1,z1 (min corner then max corner).
     */
    public int[] getInclusiveBlockBounds() {
        int x0 = (int) Math.floor(Math.min(min.getX(), max.getX()));
        int x1 = (int) Math.floor(Math.max(min.getX(), max.getX()));
        int y0 = (int) Math.floor(Math.min(min.getY(), max.getY()));
        int y1 = (int) Math.floor(Math.max(min.getY(), max.getY()));
        int z0 = (int) Math.floor(Math.min(min.getZ(), max.getZ()));
        int z1 = (int) Math.floor(Math.max(min.getZ(), max.getZ()));
        return new int[] { x0, y0, z0, x1, y1, z1 };
    }
}
