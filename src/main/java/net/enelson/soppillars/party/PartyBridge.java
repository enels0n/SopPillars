package net.enelson.soppillars.party;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Provides the set of players that should enter a minigame together. Replace with SopParty when available.
 */
public interface PartyBridge {

    /**
     * Players to place when {@code player} requests join (typically party members including {@code player}).
     */
    List<UUID> getMemberUuids(Player player);
}
