package com.renatusnetwork.momentum;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.renatusnetwork.momentum.commands.*;
import com.renatusnetwork.momentum.data.Placeholders;
import com.renatusnetwork.momentum.data.clans.ClansManager;
import com.renatusnetwork.momentum.data.checkpoints.CheckpointManager;
import com.renatusnetwork.momentum.data.events.EventManager;
import com.renatusnetwork.momentum.data.infinite.InfinitePKManager;
import com.renatusnetwork.momentum.data.levels.LevelManager;
import com.renatusnetwork.momentum.data.locations.LocationManager;
import com.renatusnetwork.momentum.data.menus.MenuManager;
import com.renatusnetwork.momentum.data.perks.PerkManager;
import com.renatusnetwork.momentum.data.plots.PlotsManager;
import com.renatusnetwork.momentum.data.races.RaceManager;
import com.renatusnetwork.momentum.data.ranks.RanksManager;
import com.renatusnetwork.momentum.data.saves.SavesManager;
import com.renatusnetwork.momentum.gameplay.*;
import com.renatusnetwork.momentum.data.stats.StatsManager;
import com.renatusnetwork.momentum.data.SettingsManager;
import com.renatusnetwork.momentum.storage.ConfigManager;
import com.renatusnetwork.momentum.storage.mysql.DatabaseManager;
import com.renatusnetwork.momentum.utils.dependencies.ProtocolLib;
import com.sk89q.worldedit.WorldEdit;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class Momentum extends JavaPlugin
{
    private static Plugin plugin;
    private static Logger logger;

    private static ConfigManager configs;
    private static DatabaseManager database;
    private static SettingsManager settings;
    private static LocationManager locations;
    private static LevelManager levels;
    private static PerkManager perks;
    private static StatsManager stats;
    private static ClansManager clans;
    private static MenuManager menus;
    private static CheckpointManager checkpoint;
    private static RaceManager races;
    private static RanksManager ranks;
    private static PlotsManager plots;
    private static ProtocolManager protocol;
    private static EventManager events;
    private static InfinitePKManager infinite;
    private static SavesManager saves;

    @Override
    public void onEnable() {
        plugin = this;
        logger = getLogger();

        registerEvents();
        registerCommands();

        // load all classes
        loadClasses();

        // check before loading classes
        if (!ProtocolLib.setupProtocol()) {
            getLogger().info("ProtocolLib not found or disabled");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // register placeholders
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)
            new Placeholders().register();
        else
            getLogger().info("Placeholder not found, not able to initialize placeholders");

        // initialize packet listeners
        PacketListener.loadListeners(this);

        // start schedulers and any settings
        Scoreboard.startScheduler(plugin);
        stats.addUnloadedPlayers();

        getLogger().info("Momentum Enabled");
    }

    @Override
    public void onDisable() {
        // save and do all shutdown methods
        PracticeHandler.shutdown();
        SpectatorHandler.shutdown();
        infinite.shutdown();
        events.shutdown();
        races.shutdown();
        levels.shutdown();

        // close database and unload classes
        database.close();
        unloadClasses();

        getLogger().info("Momentum Disabled");

        plugin = null;
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new LevelListener(), this);
        getServer().getPluginManager().registerEvents(new JoinLeaveHandler(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new TestChamberHandler(), this);
        getServer().getPluginManager().registerEvents(new InteractListener(), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(), this);
        getServer().getPluginManager().registerEvents(new PacketListener(), this);
        getServer().getPluginManager().registerEvents(new DamageListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new ItemSpawnListener(), this);
        getServer().getPluginManager().registerEvents(new PlotLimitingListener(), this);
        getServer().getPluginManager().registerEvents(new WorldChangeListener(), this);
        getServer().getPluginManager().registerEvents(new CropGrowthListener(), this);
        getServer().getPluginManager().registerEvents(new BowListener(), this);
        getServer().getPluginManager().registerEvents(new GlideListener(), this);

        WorldEdit.getInstance().getEventBus().register(new SelectionListener());

    }

    private void registerCommands() {
        getCommand("practicego").setExecutor(new PracticeGoCMD());
        getCommand("level").setExecutor(new LevelCMD());
        getCommand("race").setExecutor(new RaceCMD());
        getCommand("records").setExecutor(new RecordsCMD());
        getCommand("location").setExecutor(new LocationCMD());
        getCommand("stats").setExecutor(new StatsCMD());
        getCommand("menu").setExecutor(new MenuCMD());
        getCommand("perks").setExecutor(new PerksCMD());
        getCommand("spectate").setExecutor(new SpectateCMD());
        getCommand("clan").setExecutor(new ClanCMD());
        getCommand("momentum").setExecutor(new MomentumCMD());
        getCommand("toggleplayers").setExecutor(new PlayerToggleCMD());
        getCommand("checkpoint").setExecutor(new CheckpointCMD());
        getCommand("spawn").setExecutor(new SpawnCMD());
        getCommand("practice").setExecutor(new PracticeCMD());
        getCommand("ranks").setExecutor(new RankCMD());
        getCommand("rankup").setExecutor(new RankupCMD());
        getCommand("plot").setExecutor(new PlotCMD());
        getCommand("event").setExecutor(new EventCMD());
        getCommand("prestige").setExecutor(new PrestigeCMD());
        getCommand("rate").setExecutor(new RateCMD());
        getCommand("sword").setExecutor(new SwordCMD());
        getCommand("infinite").setExecutor(new InfiniteCMD());
        getCommand("profile").setExecutor(new ProfileCMD());
        getCommand("cosmetics").setExecutor(new CosmeticsCMD());
        getCommand("ragequit").setExecutor(new RageQuitCMD());
        getCommand("setspawn").setExecutor(new SetSpawnCMD());
        getCommand("nightvision").setExecutor(new NightVisionCMD());
        getCommand("grinding").setExecutor(new GrindCMD());
        getCommand("coins").setExecutor(new CoinsCMD());
        getCommand("tutorial").setExecutor(new TutorialCMD());
        getCommand("fails").setExecutor(new FailsCMD());
        getCommand("join").setExecutor(new JoinCMD());
        getCommand("play").setExecutor(new PlayCMD());
        getCommand("save").setExecutor(new SaveCMD());
        getCommand("pay").setExecutor(new PayCMD());
    }

    private static void loadClasses() {
        configs = new ConfigManager(plugin);
        settings = new SettingsManager(configs.get("settings"));
        locations = new LocationManager();
        checkpoint = new CheckpointManager();
        database = new DatabaseManager(plugin);
        stats = new StatsManager(plugin);
        ranks = new RanksManager();
        menus = new MenuManager();
        levels = new LevelManager(plugin);
        perks = new PerkManager(plugin);
        clans = new ClansManager(plugin);
        races = new RaceManager();
        infinite = new InfinitePKManager();
        plots = new PlotsManager();
        events = new EventManager();
        protocol = ProtocolLibrary.getProtocolManager();
        saves = new SavesManager();
    }

    private static void unloadClasses() {
        menus = null;
        checkpoint = null;
        clans = null;
        stats = null;
        perks = null;
        levels = null;
        ranks = null;
        races = null;
        infinite = null;
        locations = null;
        settings = null;
        configs = null;
        database = null;
        plots = null;
        protocol = null;
        events = null;
        saves = null;
    }

    public static Plugin getPlugin() { return plugin; }
    public static Logger getPluginLogger() {
        return logger;
    }

    // all manager methods
    public static SettingsManager getSettingsManager() { return settings; }
    public static DatabaseManager getDatabaseManager() {
        return database;
    }
    public static ConfigManager getConfigManager() {
        return configs;
    }
    public static LocationManager getLocationManager() { return locations; }
    public static LevelManager getLevelManager() {
        return levels;
    }
    public static PerkManager getPerkManager() {
        return perks;
    }
    public static MenuManager getMenuManager() {
        return menus;
    }
    public static StatsManager getStatsManager() { return stats; }
    public static ClansManager getClansManager() {
        return clans;
    }
    public static CheckpointManager getCheckpointManager() { return checkpoint; }
    public static RaceManager getRaceManager() { return races; }
    public static RanksManager getRanksManager() { return ranks; }
    public static PlotsManager getPlotsManager() { return plots; }
    public static EventManager getEventManager() { return events; }
    public static InfinitePKManager getInfinitePKManager() { return infinite; }
    public static ProtocolManager getProtocolManager() { return protocol; }
    public static SavesManager getSavesManager() { return saves; }
}