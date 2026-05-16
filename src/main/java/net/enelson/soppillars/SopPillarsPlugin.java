package net.enelson.soppillars;

import net.enelson.soppillars.arena.ArenaManager;
import net.enelson.soppillars.cage.CageManager;
import net.enelson.soppillars.command.PillarsCommand;
import net.enelson.soppillars.config.PillarsConfig;
import net.enelson.soppillars.cosmetic.CosmeticManager;
import net.enelson.soppillars.edit.EditorManager;
import net.enelson.soppillars.edit.EditWizardManager;
import net.enelson.soppillars.kit.KitManager;
import net.enelson.soppillars.loot.LootListEditorManager;
import net.enelson.soppillars.listener.ArenaAdminMenuListener;
import net.enelson.soppillars.listener.ArenaProtectionListener;
import net.enelson.soppillars.listener.EditWizardListener;
import net.enelson.soppillars.listener.MatchArenaEnvironmentListener;
import net.enelson.soppillars.listener.MatchSpectatorListener;
import net.enelson.soppillars.listener.MatchBuildListener;
import net.enelson.soppillars.listener.KitMenuListener;
import net.enelson.soppillars.listener.MatchLobbyListener;
import net.enelson.soppillars.listener.MatchRuntimeListener;
import net.enelson.soppillars.listener.MatchStateListener;
import net.enelson.soppillars.match.MatchManager;
import net.enelson.sopparty.api.SopPartyApi;
import net.enelson.sopparty.api.SopPartyServices;
import net.enelson.soppillars.party.PartyBridge;
import net.enelson.soppillars.party.SoloPartyBridge;
import net.enelson.soppillars.party.SopPartyPartyBridgeAdapter;
import net.enelson.soppillars.placeholder.SopPillarsPlaceholderExpansion;
import net.enelson.soppillars.rollback.ArenaSnapshotManager;
import net.enelson.soppillars.stats.PlayerStatisticsManager;
import net.enelson.soppillars.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SopPillarsPlugin extends JavaPlugin implements Listener {

    private MessageService messageService;
    private PillarsConfig pillarsConfig;
    private ArenaManager arenaManager;
    private EditorManager editorManager;
    private MatchManager matchManager;
    private KitManager kitManager;
    private CageManager cageManager;
    private ArenaSnapshotManager arenaSnapshotManager;
    private PlayerStatisticsManager statisticsManager;
    private CosmeticManager cosmeticManager;
    private EditWizardManager editWizardManager;
    private LootListEditorManager lootListEditorManager;
    private PartyBridge partyBridge = new SoloPartyBridge();
    private SopPillarsPlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageService = new MessageService(this);
        this.pillarsConfig = new PillarsConfig(this);
        this.arenaManager = new ArenaManager(this);
        this.editorManager = new EditorManager();
        this.matchManager = new MatchManager(this);
        this.kitManager = new KitManager(this);
        this.cageManager = new CageManager(this);
        this.arenaSnapshotManager = new ArenaSnapshotManager(this);
        this.statisticsManager = new PlayerStatisticsManager(this);
        this.cosmeticManager = new CosmeticManager(this);
        this.editWizardManager = new EditWizardManager(this);
        this.lootListEditorManager = new LootListEditorManager(this);

        if (!reloadPlugin()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PillarsCommand pillarsCommand = new PillarsCommand(this);
        getCommand("pillars").setExecutor(pillarsCommand);
        getCommand("pillars").setTabCompleter(pillarsCommand);
        Bukkit.getPluginManager().registerEvents(new MatchLobbyListener(this), this);
        Bukkit.getPluginManager().registerEvents(new KitMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchRuntimeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchSpectatorListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchStateListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchArenaEnvironmentListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MatchBuildListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ArenaAdminMenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new EditWizardListener(this), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        attachSopPartyBridgeIfAvailable();
        registerPlaceholderExpansionIfAvailable();
    }

    @Override
    public void onDisable() {
        if (this.matchManager != null) {
            this.matchManager.shutdownAndEvacuate();
        }
        if (this.cageManager != null) {
            this.cageManager.clearAll();
        }
        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
            this.placeholderExpansion = null;
        }
        if (this.editWizardManager != null) {
            this.editWizardManager.stopAll();
        }
    }

    private void attachSopPartyBridgeIfAvailable() {
        SopPartyApi api = SopPartyServices.get();
        if (api != null) {
            setPartyBridge(new SopPartyPartyBridgeAdapter(api));
            getLogger().info("SopParty detected — using its API for party grouping.");
        }
    }

    @EventHandler
    public void onPluginEnableLate(PluginEnableEvent event) {
        if ("SopParty".equals(event.getPlugin().getName())) {
            attachSopPartyBridgeIfAvailable();
            return;
        }
        if ("PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
            registerPlaceholderExpansionIfAvailable();
        }
    }

    @EventHandler
    public void onPluginDisableEarly(PluginDisableEvent event) {
        if ("SopParty".equals(event.getPlugin().getName())) {
            setPartyBridge(null);
            getLogger().info("SopParty disabled — reverting to solo party grouping.");
            return;
        }
        if ("PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName()) && placeholderExpansion != null) {
            placeholderExpansion = null;
        }
    }

    private void registerPlaceholderExpansionIfAvailable() {
        if (placeholderExpansion != null) {
            return;
        }
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) {
            return;
        }
        placeholderExpansion = new SopPillarsPlaceholderExpansion(this);
        placeholderExpansion.register();
        getLogger().info("PlaceholderAPI detected — registered %soppillars_*% placeholders.");
    }

    public boolean reloadPlugin() {
        reloadConfig();
        try {
            this.pillarsConfig.reload();
            this.arenaManager.reload();
            this.kitManager.reload();
            this.statisticsManager.reload();
            this.cosmeticManager.reload();
            this.editWizardManager.stopAll();
            this.matchManager.reset();
            this.cageManager.clearAll();
            this.matchManager.startTicker();
            return true;
        } catch (Exception exception) {
            getLogger().severe("Failed to reload SopPillars: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public PillarsConfig getPillarsConfig() {
        return pillarsConfig;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public EditorManager getEditorManager() {
        return editorManager;
    }

    public MatchManager getMatchManager() {
        return matchManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public CageManager getCageManager() {
        return cageManager;
    }

    public ArenaSnapshotManager getArenaSnapshotManager() {
        return arenaSnapshotManager;
    }

    public PlayerStatisticsManager getStatistics() {
        return statisticsManager;
    }

    public CosmeticManager getCosmeticManager() {
        return cosmeticManager;
    }

    public EditWizardManager getEditWizardManager() {
        return editWizardManager;
    }

    public LootListEditorManager getLootListEditorManager() {
        return lootListEditorManager;
    }

    public PartyBridge getPartyBridge() {
        return partyBridge;
    }

    public void setPartyBridge(PartyBridge partyBridge) {
        this.partyBridge = partyBridge == null ? new SoloPartyBridge() : partyBridge;
    }
}
