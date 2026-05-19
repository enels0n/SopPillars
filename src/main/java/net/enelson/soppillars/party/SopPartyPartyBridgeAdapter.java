package net.enelson.soppillars.party;

import net.enelson.sopparty.api.SopPartyApi;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Delegates {@link PartyBridge} to SopParty's public API when that plugin is present.
 */
public final class SopPartyPartyBridgeAdapter implements PartyBridge {

    private final SopPartyApi api;

    public SopPartyPartyBridgeAdapter(SopPartyApi api) {
        this.api = api;
    }

    @Override
    public List<UUID> getMemberUuids(Player player) {
        if (!api.isInParty(player)) {
            return Collections.singletonList(player.getUniqueId());
        }
        List<UUID> members = api.getMemberUuids(player);
        if (members == null || members.isEmpty()) {
            return Collections.singletonList(player.getUniqueId());
        }
        return members;
    }
}
