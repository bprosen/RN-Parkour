package com.parkourcraft.Parkour.storage.mysql;

import com.parkourcraft.Parkour.data.LevelManager;
import com.parkourcraft.Parkour.data.PerkManager;
import com.parkourcraft.Parkour.data.levels.LevelObject;
import com.parkourcraft.Parkour.data.perks.Perk;
import com.parkourcraft.Parkour.data.stats.LevelCompletion;
import com.parkourcraft.Parkour.data.stats.PlayerStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataQueries {

    private static Map<String, Integer> levelIDCache = new HashMap<>();
    private static Map<String, Integer> perkIDCache = new HashMap<>();

    public static void loadPlayerStats(PlayerStats playerStats) {
        loadPlayerID(playerStats);
        loadCompletions(playerStats);
        loadPerks(playerStats);
    }

    private static void loadPlayerID(PlayerStats playerStats) {
        List<Map<String, String>> playerResults = DatabaseQueries.getResults(
                "players",
                "player_id, player_name, spectatable",
                " WHERE uuid='" + playerStats.getUUID() + "'"
        );

        if (playerResults.size() > 0) {
            for (Map<String, String> playerResult : playerResults) {
                playerStats.setPlayerID(Integer.parseInt(playerResult.get("player_id")));

                if (!playerResult.get("player_name").equals(playerStats.getPlayerName()))
                    updatePlayerName(playerStats);

                int spectatable = Integer.parseInt(playerResult.get("spectatable"));
                if (spectatable == 1)
                    playerStats.isSpectatable(true);
                else
                    playerStats.isSpectatable(false);
            }
        } else {
            insertPlayerID(playerStats);
            loadPlayerStats(playerStats);
        }
    }

    private static void insertPlayerID(PlayerStats playerStats) {
        String query = "INSERT INTO players " +
                "(uuid, player_name)" +
                " VALUES " +
                "('" +
                playerStats.getUUID() + "', '" +
                playerStats.getPlayerName() +
                "')"
                ;

        DatabaseManager.runQuery(query);
    }

    private static void updatePlayerName(PlayerStats playerStats) {
        String query = "UPDATE players SET " +
                "player_name='" + playerStats.getPlayerName() + "' " +
                "WHERE player_id='" + playerStats.getPlayerID() + "'"
                ;

        DatabaseManager.addUpdateQuery(query);
    }

    public static void updatePlayerSpectatable(PlayerStats playerStats) {
        int spectatable = 0;

        if (playerStats.isSpectatable())
            spectatable = 1;

        String query = "UPDATE players SET " +
                "spectatable=" + spectatable + " " +
                "WHERE player_id='" + playerStats.getPlayerID() + "'"
                ;

        DatabaseManager.addUpdateQuery(query);
    }

    private static void loadCompletions(PlayerStats playerStats) {
        List<Map<String, String>> completionsResults = DatabaseQueries.getResults(
                "completions",
                "level.level_name, " +
                        "time_taken, " +
                        "(UNIX_TIMESTAMP(completion_date) * 1000) AS date",
                "JOIN levels level" +
                        " ON level.level_id=completions.level_id" +
                        " WHERE player_id=" + playerStats.getPlayerID()
        );

        for (Map<String, String> completionResult : completionsResults)
            playerStats.levelCompletion(
                    completionResult.get("level_name"),
                    Long.parseLong(completionResult.get("date")),
                    Long.parseLong(completionResult.get("time_taken"))
            );
    }

    private static void loadPerks(PlayerStats playerStats) {
        List<Map<String, String>> perksResults = DatabaseQueries.getResults(
                "ledger",
                "perk.perk_name, " +
                        "(UNIX_TIMESTAMP(date) * 1000) AS date",
                "JOIN perks perk" +
                        " on perk.perk_id=ledger.perk_id" +
                        " WHERE player_id=" + playerStats.getPlayerID()
        );

        for (Map<String, String> perkResult : perksResults)
            playerStats.addPerk(
                    perkResult.get("perk_name"),
                    Long.parseLong(perkResult.get("date"))
            );
    }

    public static void insertCompletion(PlayerStats playerStats, LevelObject level, LevelCompletion levelCompletion) {
        DatabaseManager.addUpdateQuery(
                "INSERT INTO completions " +
                "(player_id, level_id, time_taken, completion_date)" +
                " VALUES (" +
                playerStats.getPlayerID() + ", " +
                level.getID() + ", " +
                levelCompletion.getCompletionTimeElapsed() + ", " +
                "FROM_UNIXTIME(" + (levelCompletion.getTimeOfCompletion() / 1000) + ")" +
                ")"
        );
    }

    public static void insertPerk(PlayerStats playerStats, Perk perk, Long date) {
        DatabaseManager.addUpdateQuery(
                "INSERT INTO ledger (player_id, perk_id, date)"
                        + " VALUES "
                        + "(" + playerStats.getPlayerID()
                        + ", " + perk.getID()
                        + ", FROM_UNIXTIME(" + (date / 1000) + "))"
        );
    }

    public static void loadTotalCompletions() {
        List<Map<String, String>> levelsResults = DatabaseQueries.getResults(
                "completions",
                "level.level_name, " +
                        "COUNT(*) AS total_completions",
                "JOIN levels level" +
                        " ON level.level_id=completions.level_id" +
                        " GROUP BY level_name"
        );

        for (Map<String, String> levelResult : levelsResults) {
            LevelObject levelObject = LevelManager.get(levelResult.get("level_name"));

            if (levelObject != null)
                levelObject.setTotalCompletionsCount(Integer.parseInt(levelResult.get("total_completions")));
        }
    }

    public static void loadLeaderboards() {
        for (LevelObject levelObject : LevelManager.getLevels())
            loadLeaderboard(levelObject);
    }

    private static void loadLeaderboard(LevelObject levelObject) {
        List<Map<String, String>> levelsResults = DatabaseQueries.getResults(
                "completions",
                        "player.player_name, " +
                        "time_taken, " +
                        "(UNIX_TIMESTAMP(completion_date) * 1000) AS date",
                        "JOIN players player" +
                        " on completions.player_id=player.player_id" +
                        " WHERE completions.level_id=" + levelObject.getID() +
                        " AND time_taken > 0" +
                        " ORDER BY time_taken" +
                        " ASC LIMIT 10"

        );

        List<LevelCompletion> levelCompletions = new ArrayList<>();

        for (Map<String, String> levelResult : levelsResults) {
            LevelCompletion levelCompletion =  new LevelCompletion(
                    Long.parseLong(levelResult.get("date")),
                    Long.parseLong(levelResult.get("time_taken"))
            );

            levelCompletion.setPlayerName(levelResult.get("player_name"));

            levelCompletions.add(levelCompletion);
        }

        levelObject.setLeaderboardCache(levelCompletions);
    }

    public static void syncLevelID(LevelObject levelObject) {
        if (levelIDCache.containsKey(levelObject.getName()))
            levelObject.setID(levelIDCache.get(levelObject.getName()));
    }

    public static void syncLevelIDCache() {
        for (LevelObject levelObject : LevelManager.getLevels())
            syncLevelID(levelObject);
    }

    public static void loadLevelIDCache() {
        List<Map<String, String>> levelsResults = DatabaseQueries.getResults(
                "levels",
                "level_id, level_name",
                ""
        );

        for (Map<String, String> levelResult : levelsResults)
            levelIDCache.put(
                    levelResult.get("level_name"),
                    Integer.parseInt(levelResult.get("level_id"))
            );

        syncLevelIDCache();
    }

    public static void syncLevelIDs() {
        List<String> insertQueries = new ArrayList<>();

        for (LevelObject levelObject : LevelManager.getLevels())
            if (levelObject.getID() == -1)
                insertQueries.add(
                        "INSERT INTO levels " +
                        "(level_name)" +
                        " VALUES " +
                        "('" + levelObject.getName() + "')"
                );

        if (insertQueries.size() > 0) {
            String finalQuery = "";
            for (String sql : insertQueries)
                finalQuery = finalQuery + sql + "; ";

            DatabaseManager.runQuery(finalQuery);
            loadLevelIDCache();
        }
    }

    public static void syncPerkID(Perk perk) {
        if (perkIDCache.containsKey(perk.getName()))
            perk.setID(perkIDCache.get(perk.getName()));
    }

    public static void syncPerkIDCache() {
        for (Perk perk : PerkManager.getPerks())
            syncPerkID(perk);
    }

    public static void loadPerkIDCache() {
        List<Map<String, String>> perkResults = DatabaseQueries.getResults(
                "perks",
                "perk_id, perk_name",
                ""
        );

        for (Map<String, String> perkResult : perkResults)
            perkIDCache.put(
                    perkResult.get("perk_name"),
                    Integer.parseInt(perkResult.get("perk_id"))
            );

        syncPerkIDCache();
    }

    public static void syncPerkIDs() {
        List<String> insertQueries = new ArrayList<>();

        for (Perk perk : PerkManager.getPerks())
            if (perk.getID() == -1)
                insertQueries.add(
                        "INSERT INTO perks " +
                                "(perk_name)" +
                                " VALUES " +
                                "('" + perk.getName() + "')"
                );

        if (insertQueries.size() > 0) {
            String finalQuery = "";
            for (String sql : insertQueries)
                finalQuery = finalQuery + sql + "; ";

            DatabaseManager.runQuery(finalQuery);
            loadPerkIDCache();
        }
    }

}