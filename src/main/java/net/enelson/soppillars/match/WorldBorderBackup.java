package net.enelson.soppillars.match;

import org.bukkit.Location;
import org.bukkit.WorldBorder;

/**
 * Captures vanilla world border state so SopPillars can restore it after a match.
 */
public final class WorldBorderBackup {

    private final double size;
    private final double centerX;
    private final double centerZ;
    private final double damageAmount;
    private final double damageBuffer;
    private final int warningDistance;
    private final int warningTime;

    private WorldBorderBackup(double size,
                              double centerX,
                              double centerZ,
                              double damageAmount,
                              double damageBuffer,
                              int warningDistance,
                              int warningTime) {
        this.size = size;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.damageAmount = damageAmount;
        this.damageBuffer = damageBuffer;
        this.warningDistance = warningDistance;
        this.warningTime = warningTime;
    }

    public static WorldBorderBackup capture(WorldBorder border) {
        Location center = border.getCenter();
        return new WorldBorderBackup(
                border.getSize(),
                center.getX(),
                center.getZ(),
                border.getDamageAmount(),
                border.getDamageBuffer(),
                border.getWarningDistance(),
                border.getWarningTime()
        );
    }

    public void restore(WorldBorder border) {
        border.setCenter(centerX, centerZ);
        border.setSize(size);
        border.setDamageAmount(damageAmount);
        border.setDamageBuffer(damageBuffer);
        border.setWarningDistance(warningDistance);
        border.setWarningTime(warningTime);
    }
}
