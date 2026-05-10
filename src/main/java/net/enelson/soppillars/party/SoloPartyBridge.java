package net.enelson.soppillars.party;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class SoloPartyBridge implements PartyBridge {

    @Override
    public List<UUID> getMemberUuids(Player player) {
        return Collections.singletonList(player.getUniqueId());
    }
}
