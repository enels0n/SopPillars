package net.enelson.soppillars.listener;

import net.enelson.soppillars.SopPillarsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EditWizardListener implements Listener {

    private final SopPillarsPlugin plugin;
    private final Map<UUID, Long> lastWizardInteractAt = new HashMap<UUID, Long>();

    public EditWizardListener(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.getEditWizardManager().isActive(player.getUniqueId())) {
            return;
        }
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return;
        }
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (heldSlot >= 0 && heldSlot <= 2 && isDuplicateWizardInteract(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (heldSlot == 0) {
            event.setCancelled(true);
            plugin.getEditWizardManager().handleSet(player);
        } else if (heldSlot == 1) {
            event.setCancelled(true);
            plugin.getEditWizardManager().handleBack(player);
        } else if (heldSlot == 2) {
            event.setCancelled(true);
            plugin.getEditWizardManager().handleSkip(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getEditWizardManager().isActive(player.getUniqueId())) {
            return;
        }
        if (plugin.getLootListEditorManager().isLootListEditorTitle(event.getView().getTitle())) {
            if (isControlSlotClick(event) || isControlHotbarSwap(event)) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (!plugin.getEditWizardManager().isActive(player.getUniqueId())) {
            return;
        }
        if (!plugin.getLootListEditorManager().isLootListEditorTitle(event.getView().getTitle())) {
            event.setCancelled(true);
            return;
        }
        int topSize = event.getView().getTopInventory().getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (isControlRawSlot(rawSlot, topSize)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getEditWizardManager().isActive(player.getUniqueId())) {
            return;
        }
        int heldSlot = player.getInventory().getHeldItemSlot();
        if (heldSlot >= 0 && heldSlot <= 2) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (plugin.getEditWizardManager().isActive(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        lastWizardInteractAt.remove(player.getUniqueId());
        if (!plugin.getEditWizardManager().isActive(player.getUniqueId())) {
            return;
        }
        plugin.getEditWizardManager().stop(player, true);
        plugin.getEditorManager().leaveEditor(player.getUniqueId());
    }

    private boolean isDuplicateWizardInteract(UUID playerId) {
        long now = System.currentTimeMillis();
        Long previous = lastWizardInteractAt.get(playerId);
        lastWizardInteractAt.put(playerId, now);
        return previous != null && (now - previous.longValue()) < 125L;
    }

    private boolean isControlSlotClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return false;
        }
        if (event.getClickedInventory().equals(event.getWhoClicked().getInventory())) {
            int slot = event.getSlot();
            return slot >= 0 && slot <= 2;
        }
        return false;
    }

    private boolean isControlHotbarSwap(InventoryClickEvent event) {
        return event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0 && event.getHotbarButton() <= 2;
    }

    private boolean isControlRawSlot(int rawSlot, int topSize) {
        return rawSlot >= (topSize + 36) && rawSlot <= (topSize + 38);
    }
}
