package net.enelson.soppillars.match;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Picks a config {@code messages.*} key for arena death broadcasts from last damage context (SPEC: death cause hooks).
 */
public final class DeathBroadcastResolver {

    private DeathBroadcastResolver() {
    }

    public static DeathContext resolve(Player victim, Player killerFromEvent, EntityDamageEvent lastDamage) {
        Player attacking = killerFromEvent;
        if (attacking == null && lastDamage instanceof EntityDamageByEntityEvent) {
            attacking = DamageResolver.resolvePlayerAttacker(((EntityDamageByEntityEvent) lastDamage).getDamager());
        }

        EntityDamageEvent.DamageCause cause = lastDamage != null ? lastDamage.getCause() : null;

        if (attacking != null) {
            return new DeathContext(attacking, keyForPlayerKill(cause), null);
        }

        if (lastDamage instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) lastDamage).getDamager();
            return new DeathContext(null, "death-by-mob", mobLabel(damager));
        }

        return new DeathContext(null, keyForEnvironmental(cause), null);
    }

    private static String keyForPlayerKill(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return "death-pvp";
        }
        switch (cause) {
            case VOID:
                return "death-pvp-void";
            case FALL:
                return "death-pvp-fall";
            case LAVA:
                return "death-pvp-lava";
            case FIRE:
            case FIRE_TICK:
            case MELTING:
            case HOT_FLOOR:
                return "death-pvp-fire";
            case PROJECTILE:
                return "death-pvp-projectile";
            default:
                return "death-pvp";
        }
    }

    private static String keyForEnvironmental(EntityDamageEvent.DamageCause cause) {
        if (cause == null) {
            return "death-environment";
        }
        switch (cause) {
            case VOID:
                return "death-void";
            case FALL:
                return "death-fall";
            case LAVA:
                return "death-lava";
            case FIRE:
            case FIRE_TICK:
            case MELTING:
            case HOT_FLOOR:
                return "death-fire";
            case STARVATION:
                return "death-starvation";
            case DROWNING:
                return "death-drown";
            case SUFFOCATION:
                return "death-suffocate";
            default:
                return "death-environment";
        }
    }

    private static String mobLabel(Entity damager) {
        if (damager == null) {
            return "?";
        }
        Entity base = damager;
        if (damager instanceof Projectile) {
            Projectile projectile = (Projectile) damager;
            Object shooter = projectile.getShooter();
            if (shooter instanceof Entity) {
                base = (Entity) shooter;
            }
        }
        if (base instanceof Player) {
            return base.getName();
        }
        return base.getType().name();
    }

    public static final class DeathContext {
        private final Player attackingPlayer;
        private final String messageKey;
        private final String mobName;

        public DeathContext(Player attackingPlayer, String messageKey, String mobName) {
            this.attackingPlayer = attackingPlayer;
            this.messageKey = messageKey;
            this.mobName = mobName;
        }

        public Player getAttackingPlayer() {
            return attackingPlayer;
        }

        public String getMessageKey() {
            return messageKey;
        }

        public String getMobName() {
            return mobName;
        }
    }
}
