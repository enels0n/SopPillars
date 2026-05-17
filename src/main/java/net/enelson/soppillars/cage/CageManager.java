package net.enelson.soppillars.cage;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import net.enelson.soppillars.SopPillarsPlugin;
import net.enelson.soppillars.match.WaitingMatch;
import net.enelson.soppillars.model.SerializedLocation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public final class CageManager {

    private final SopPillarsPlugin plugin;
    private final List<ActiveCage> activeCages = new ArrayList<ActiveCage>();
    private BukkitTask releaseTask;

    public CageManager(SopPillarsPlugin plugin) {
        this.plugin = plugin;
    }

    public void clearAll() {
        if (releaseTask != null) {
            releaseTask.cancel();
            releaseTask = null;
        }
        for (ActiveCage cage : new ArrayList<ActiveCage>(activeCages)) {
            cage.restore();
        }
        activeCages.clear();
    }

    public boolean isActiveCageBlock(Location location) {
        if (location == null) {
            return false;
        }
        for (ActiveCage cage : new ArrayList<ActiveCage>(activeCages)) {
            if (cage.contains(location)) {
                return true;
            }
        }
        return false;
    }

    public void spawnCages(WaitingMatch match) {
        clearAll();
        for (Player player : resolvePlayers(match)) {
            int rotationQuarterTurns = resolveRotationQuarterTurns(match, player);
            ActiveCage cage = createCage(player, rotationQuarterTurns);
            if (cage != null) {
                activeCages.add(cage);
            }
        }
        int delayTicks = Math.max(1, match.getArena().getSettings().getCageSeconds()) * 20;
        releaseTask = plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                releaseAll(match);
            }
        }, delayTicks);
    }

    private void releaseAll(WaitingMatch match) {
        for (ActiveCage cage : new ArrayList<ActiveCage>(activeCages)) {
            cage.restore();
            Player player = cage.getPlayer();
            if (player != null && player.isOnline()) {
                player.setFallDistance(0.0F);
                player.setNoDamageTicks(100);
                if (match.getArena().getSettings().isAllowSmoothFall()) {
                    int durationTicks = Math.max(1, match.getArena().getSettings().getSmoothFallSeconds()) * 20;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, durationTicks, 0, false, false, true));
                }
            }
        }
        activeCages.clear();
        releaseTask = null;
        for (Player player : resolvePlayers(match)) {
            plugin.getMessageService().send(player, "cages-opened");
        }
    }

    private ActiveCage createCage(Player player, int rotationQuarterTurns) {
        if (player == null || player.getWorld() == null) {
            return null;
        }
        ActiveCage schematic = createCageFromSchematic(player, rotationQuarterTurns);
        if (schematic != null) {
            return schematic;
        }
        List<CapturedBlockState> captured = new ArrayList<CapturedBlockState>();
        int baseX = player.getLocation().getBlockX();
        int baseY = player.getLocation().getBlockY();
        int baseZ = player.getLocation().getBlockZ();

        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean wall = Math.abs(x) == 1 || Math.abs(z) == 1 || y == 3 || y == 0;
                    boolean center = x == 0 && z == 0;
                    if (!wall || center && y > 0 && y < 3) {
                        continue;
                    }
                    Block block = player.getWorld().getBlockAt(baseX + x, baseY + y, baseZ + z);
                    captured.add(new CapturedBlockState(block));
                    block.setType(Material.GLASS, false);
                }
            }
        }
        return new ActiveCage(player, captured);
    }

    private ActiveCage createCageFromSchematic(Player player, int rotationQuarterTurns) {
        File schem = plugin.getCosmeticManager().resolveSelectedCageFile(player);
        if (!schem.isFile()) {
            return null;
        }
        ClipboardFormat format = ClipboardFormats.findByFile(schem);
        if (format == null) {
            plugin.getLogger().warning("Unsupported schematic format for " + schem.getName() + "; falling back to glass cage.");
            return null;
        }
        try (ClipboardReader reader = format.getReader(new FileInputStream(schem))) {
            Clipboard clipboard = reader.read();
            BlockVector3 origin = clipboard.getOrigin();
            List<CapturedBlockState> captured = new ArrayList<CapturedBlockState>();
            int baseX = player.getLocation().getBlockX();
            int baseY = player.getLocation().getBlockY();
            int baseZ = player.getLocation().getBlockZ();
            for (BlockVector3 pos : clipboard.getRegion()) {
                BaseBlock source = clipboard.getFullBlock(pos);
                BlockData data = BukkitAdapter.adapt(source);
                if (data == null || data.getMaterial() == Material.AIR) {
                    continue;
                }
                int dx = pos.x() - origin.x();
                int dy = pos.y() - origin.y();
                int dz = pos.z() - origin.z();
                int x = baseX + rotateOffsetX(dx, dz, rotationQuarterTurns);
                int y = baseY + dy;
                int z = baseZ + rotateOffsetZ(dx, dz, rotationQuarterTurns);
                Block block = player.getWorld().getBlockAt(x, y, z);
                captured.add(new CapturedBlockState(block));
                block.setBlockData(data, false);
            }
            if (captured.isEmpty()) {
                return null;
            }
            return new ActiveCage(player, captured);
        } catch (Throwable exception) {
            plugin.getLogger().warning("Failed to place cages/default.schem (" + exception.getMessage()
                    + "); falling back to glass cage.");
            return null;
        }
    }

    private int resolveRotationQuarterTurns(WaitingMatch match, Player player) {
        int team = match.getTeam(player.getUniqueId());
        if (team <= 0) {
            return 0;
        }
        List<java.util.UUID> teamPlayers = match.getPlayersInTeam(team);
        int slot = teamPlayers.indexOf(player.getUniqueId()) + 1;
        if (slot <= 0) {
            return 0;
        }
        SerializedLocation spawn = match.getArena().getSpawn(team, slot);
        if (spawn == null) {
            return 0;
        }
        return rotationQuarterTurnsFromYaw(spawn.getYaw());
    }

    private static int rotationQuarterTurnsFromYaw(float yaw) {
        int quarter = Math.round(yaw / 90.0F);
        return Math.floorMod(quarter, 4);
    }

    private static int rotateOffsetX(int x, int z, int quarterTurns) {
        switch (quarterTurns) {
            case 1:
                return -z;
            case 2:
                return -x;
            case 3:
                return z;
            default:
                return x;
        }
    }

    private static int rotateOffsetZ(int x, int z, int quarterTurns) {
        switch (quarterTurns) {
            case 1:
                return x;
            case 2:
                return -z;
            case 3:
                return -x;
            default:
                return z;
        }
    }

    private List<Player> resolvePlayers(WaitingMatch match) {
        List<Player> players = new ArrayList<Player>();
        for (java.util.UUID playerId : match.getPlayers()) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }
}
