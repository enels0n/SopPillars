package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Blocks spectators in a running match from affecting gameplay (interactions, drops, projectiles, fishing, consumables, harvest, shear).
 */
public final class MatchSpectatorListener implements Listener {

    private final SopPillarsPlugin plugin;

    public MatchSpectatorListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorMelee(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player attacker = (Player) event.getDamager();
        if (!plugin.getMatchManager().isRunningSpectator(attacker)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Projectile)) {
            return;
        }
        Projectile projectile = (Projectile) event.getEntity();
        if (!(projectile.getShooter() instanceof Player)) {
            return;
        }
        Player shooter = (Player) projectile.getShooter();
        if (!plugin.getMatchManager().isRunningSpectator(shooter)) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(shooter, "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHarvest(PlayerHarvestBlockEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (!plugin.getMatchManager().isRunningSpectator(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        plugin.getMessageService().send(event.getPlayer(), "spectator-no-interact");
    }
}
