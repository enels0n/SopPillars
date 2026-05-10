package net.enelson.soppillars.match;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;

/**
 * Resolves a {@link Player} responsible for damage from an entity (direct hit, projectile, owned wolf, etc.).
 */
public final class DamageResolver {

    private DamageResolver() {
    }

    public static Player resolvePlayerAttacker(Entity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof Player) {
            return (Player) entity;
        }
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            Object shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
            if (shooter instanceof Tameable) {
                return resolveTameableOwnerPlayer((Tameable) shooter);
            }
        }
        if (entity instanceof Tameable) {
            return resolveTameableOwnerPlayer((Tameable) entity);
        }
        return null;
    }

    private static Player resolveTameableOwnerPlayer(Tameable tameable) {
        if (tameable.getOwner() instanceof Player) {
            return (Player) tameable.getOwner();
        }
        return null;
    }
}
