package com.parkourcraft.parkour.data.levels;

import com.parkourcraft.parkour.Parkour;
import com.parkourcraft.parkour.storage.mysql.DatabaseQueries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LevelsDB {

    static Map<String, LevelData> getDataCache() {
        Map<String, LevelData> levelData = new HashMap<>();

        List<Map<String, String>> levelsResults = DatabaseQueries.getResults(
                "levels",
                "level_id, level_name, reward, score_modifier",
                ""
        );

        for (Map<String, String> levelResult : levelsResults)
            levelData.put(
                    levelResult.get("level_name"),
                    new LevelData(
                            Integer.parseInt(levelResult.get("level_id")),
                            Integer.parseInt(levelResult.get("reward")),
                            Integer.parseInt(levelResult.get("score_modifier")),
                            RatingDB.getAverageRating(Integer.parseInt(levelResult.get("level_id"))),
                            RatingDB.getTotalRatings(Integer.parseInt(levelResult.get("level_id")))
                    )
            );

        Parkour.getPluginLogger().info("Levels in data cache: " + levelData.size());

        return levelData;
    }

    static void syncDataCache() {
        for (Map.Entry<String, Level> entry : Parkour.getLevelManager().getLevels().entrySet())
            syncDataCache(entry.getValue(), Parkour.getLevelManager().getLevelDataCache());
    }

    static void syncDataCache(Level level, Map<String, LevelData> levelDataCache) {
        LevelData levelData = levelDataCache.get(level.getName());

        if (levelData != null) {
            level.setID(levelData.getID());
            level.setReward(levelData.getReward());
            level.setScoreModifier(levelData.getScoreModifier());
            level.setRating(levelData.getRating());
            level.setRatingsCount(levelData.getRatingsCount());
        }
    }

    static boolean syncLevelData() {
        List<String> insertQueries = new ArrayList<>();

        for (Map.Entry<String, Level> entry : Parkour.getLevelManager().getLevels().entrySet())
            if (!Parkour.getLevelManager().getLevelDataCache().containsKey(entry.getValue().getName()))
                insertQueries.add(
                        "INSERT INTO levels " +
                                "(level_name)" +
                                " VALUES " +
                                "('" + entry.getValue().getName() + "')"
                );

        if (insertQueries.size() > 0) {
            String finalQuery = "";
            for (String sql : insertQueries)
                finalQuery = finalQuery + sql + "; ";

            Parkour.getDatabaseManager().run(finalQuery);

            return true;
        }

        return false;
    }

    public static void updateReward(Level level) {
        String query = "UPDATE levels SET " +
                "reward=" + level.getReward() + " " +
                "WHERE level_id=" + level.getID() + ""
                ;

        LevelData levelData = Parkour.getLevelManager().getLevelDataCache().get(level.getName());
        if (levelData != null)
            levelData.setReward(level.getReward());

        Parkour.getDatabaseManager().add(query);
    }

    public static void updateScoreModifier(Level level) {
        String query = "UPDATE levels SET " +
                "score_modifier=" + level.getScoreModifier() + " " +
                "WHERE level_id=" + level.getID() + "";

        LevelData levelData = Parkour.getLevelManager().getLevelDataCache().get(level.getName());
        if (levelData != null)
            levelData.setScoreModifier(level.getScoreModifier());

        Parkour.getDatabaseManager().add(query);
    }
}
