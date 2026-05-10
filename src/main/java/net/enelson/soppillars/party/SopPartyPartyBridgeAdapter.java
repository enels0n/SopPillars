package net.enelson.soppillars.party;

import net.enelson.sopparty.api.SopPartyApi;
import org.bukkit.entity.Player;

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
        return api.getMemberUuids(player);
    }
}
