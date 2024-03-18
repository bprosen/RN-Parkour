package com.renatusnetwork.momentum.storage.mysql;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DatabaseManager {

    private DatabaseConnection connection;

    // constants for all table names
    public static final String PLAYERS_TABLE = "players";
    public static final String LEVELS_TABLE = "levels";
    public static final String PERKS_TABLE = "perks";
    public static final String PLOTS_TABLE = "plots";
    public static final String LOCATIONS_TABLE = "locations";
    public static final String LEVEL_RATINGS_TABLE = "level_ratings";
    public static final String LEVEL_CHECKPOINTS_TABLE = "level_checkpoints";
    public static final String LEVEL_SAVES_TABLE = "level_saves";
    public static final String LEVEL_PURCHASES_TABLE = "level_purchases";
    public static final String LEVEL_COMPLETIONS_TABLE = "level_completions";
    public static final String CLANS_TABLE = "clans";
    public static final String RANKS_TABLE = "ranks";
    public static final String PLOTS_TRUSTED_PLAYERS_TABLE = "plot_trusted_players";
    public static final String MODIFIERS_TABLE = "modifiers";
    public static final String PLAYER_MODIFIERS_TABLE = "player_modifiers";
    public static final String PERKS_BOUGHT_TABLE = "perks_bought";
    public static final String PERKS_LEVEL_REQUIREMENTS_TABLE = "perks_level_requirements";
    public static final String PERKS_ARMOR_TABLE = "perks_armor";
    public static final String PERKS_COMMANDS_TABLE = "perks_commands";
    public static final String LEVEL_COMPLETION_COMMANDS_TABLE = "level_completion_commands";
    public static final String LEVEL_POTION_EFFECTS_TABLE = "level_potion_effects";
    public static final String LEVEL_REQUIRED_LEVELS_TABLE = "level_required_levels";
    public static final String FAVORITE_LEVELS = "favorite_levels";
    public static final String ELO_TIERS = "elo_tiers";
    public static final String BANK_ITEMS = "bank_items";
    public static final String BANK_WEEKS = "bank_weeks";
    public static final String BANK_BIDS = "bank_bids";

    public DatabaseManager(Plugin plugin)
    {
        connection = new DatabaseConnection();
        startScheduler(plugin);
    }

    public void close() {
        connection.close();
    }

    private void startScheduler(Plugin plugin) {

        // run async random queue every 10 minutes to keep connection alive if nobody is online and no database activity
        new BukkitRunnable() {
            public void run() {
                try (Connection connection = getConnection())
                {
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT * FROM " + DatabaseManager.PLAYERS_TABLE + " WHERE UUID='s'");
                    statement.execute();
                    statement.close();
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 20 * 60 * 10, 20 * 60 * 10);
    }

    public Connection getConnection() throws SQLException {
        return connection.get();
    }

    public DatabaseMetaData getMeta()
    {
        DatabaseMetaData meta = null;

        try (Connection connection = getConnection())
        {
            meta = connection.getMetaData();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

        return meta;
    }
}
