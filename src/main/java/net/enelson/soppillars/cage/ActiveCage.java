package net.enelson.soppillars.cage;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ActiveCage {

    private final Player player;
    private final List<CapturedBlockState> capturedBlocks;

    public ActiveCage(Player player, List<CapturedBlockState> capturedBlocks) {
        this.player = player;
        this.capturedBlocks = new ArrayList<CapturedBlockState>(capturedBlocks);
    }

    public Player getPlayer() {
        return player;
    }

    public List<CapturedBlockState> getCapturedBlocks() {
        return Collections.unmodifiableList(capturedBlocks);
    }

    public void restore() {
        for (CapturedBlockState capturedBlock : capturedBlocks) {
            capturedBlock.restore();
        }
    }
}
