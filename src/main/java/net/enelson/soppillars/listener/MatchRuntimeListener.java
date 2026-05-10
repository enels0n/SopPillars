package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.DamageResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class MatchRuntimeListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchRuntimeListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFriendlyFire(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        Player attacker = DamageResolver.resolvePlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }
        if (plugin.getMatchManager().isFriendlyFireBlocked(victim, attacker)) {
            event.setCancelled(true);
        }
    }

    /**
     * Cancels lethal damage and eliminates without {@link PlayerDeathEvent} (no death screen).
     * Runs after {@link #onFriendlyFire} (HIGHEST); skips cancelled hits and teammate damage.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLethalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        if (!plugin.getMatchManager().isArenaEliminationCandidate(victim)) {
            return;
        }
        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntity = (EntityDamageByEntityEvent) event;
            Player attacker = DamageResolver.resolvePlayerAttacker(byEntity.getDamager());
            if (attacker != null && plugin.getMatchManager().isFriendlyFireBlocked(victim, attacker)) {
                return;
            }
        }
        double damage = event.getFinalDamage();
        double effective = victim.getHealth() + victim.getAbsorptionAmount();
        if (effective - damage > 1e-6) {
            return;
        }
        event.setCancelled(true);
        victim.setHealth(victim.getMaxHealth());
        victim.setAbsorptionAmount(0.0D);
        plugin.getMatchManager().eliminateFromLethalDamage(victim, event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeathSuppressBroadcast(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (plugin.getMatchManager().isArenaEliminationCandidate(victim)) {
            event.setDeathMessage(null);
        }
    }

    /**
     * Rare fallback when the player still dies ({@code /kill}, plugin conflicts): vanilla drops stay as-is.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathFallbackElimination(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!plugin.getMatchManager().isArenaEliminationCandidate(victim)) {
            return;
        }
        plugin.getMatchManager().eliminateAfterVanillaDeath(victim);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();
        if (!plugin.getMatchManager().isRunningSpectator(player)) {
            if (!plugin.getMatchManager().isEndingWinnerProtected(player)) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getMatchManager().isCommandAllowed(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            plugin.getMessageService().send(event.getPlayer(), "command-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getMatchManager().handleRespawn(event);
    }
}
