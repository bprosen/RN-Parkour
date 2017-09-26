package com.parkourcraft.Parkour.data;

import com.parkourcraft.Parkour.Parkour;
import com.parkourcraft.Parkour.data.levels.LevelObject;
import com.parkourcraft.Parkour.storage.mysql.DatabaseManager;
import com.parkourcraft.Parkour.storage.mysql.DatabaseQueries;
import com.parkourcraft.Parkour.data.levels.Levels_YAML;

import java.util.*;

public class LevelManager {

    private static List<LevelObject> levels = new ArrayList<>();
    private static Map<String, Integer> levelIDCache = new HashMap<>();

    public static LevelObject get(String levelName) {
        for (LevelObject levelObject : levels)
            if (levelObject.getName().equals(levelName))
                return levelObject;

        return null;
    }

    public static LevelObject get(int levelID) {
        for (LevelObject levelObject : levels)
            if (levelObject.getID() == levelID)
                return levelObject;

        return null;
    }

    public static int getIDFromCache(String levelName) {
        return levelIDCache.get(levelName);
    }

    public static boolean exists(String levelName) {
        if (get(levelName) != null)
            return true;

        return false;
    }

    public static void remove(String levelName) {
        for (Iterator<LevelObject> iterator = levels.iterator(); iterator.hasNext();)
            if (iterator.next().getName().equals(levelName))
                iterator.remove();
    }

    public static void load(String levelName) {
        boolean exists = exists(levelName);

        if (!Levels_YAML.exists(levelName)
                && exists)
            remove(levelName);
        else {
            LevelObject levelObject = new LevelObject(levelName);

            if (levelIDCache.containsKey(levelName))
                levelObject.setID(levelIDCache.get(levelName));

            if (exists)
                remove(levelName);

            levels.add(levelObject);
        }
    }

    public static void loadAll() {
        levels = new ArrayList<>();

        for (String levelName : Levels_YAML.getNames())
            load(levelName);

        Parkour.getPluginLogger().info("Levels loaded: " + levels.size());

        syncIDCache();
    }

    public static void loadIDs() {
        List<Map<String, String>> levelsResults = DatabaseQueries.getResults(
                "levels",
                "*",
                ""
        );

        if (levelsResults.size() > 0) {
            levelIDCache = new HashMap<>();

            for (Map<String, String> levelsResult : levelsResults) {
                levelIDCache.put(
                        levelsResult.get("level_name"),
                        Integer.parseInt(levelsResult.get("level_id"))
                );
            }
        }
    }

    private static void syncIDCache() {
        for (String levelName : levelIDCache.keySet()) {
            LevelObject levelObject = get(levelName);

            if (levelObject != null)
                levelObject.setID(levelIDCache.get(levelName));
        }
    }

    public static void syncIDs() {
        syncIDCache();

        List<String> insertQueries = new ArrayList<>();

        for (LevelObject levelObject : levels)
            if (levelObject.getID() == -1) {
                String query = "INSERT INTO levels " +
                        "(level_name)" +
                        " VALUES " +
                        "('" + levelObject.getName() + "')";

                insertQueries.add(query);
            }


        if (insertQueries.size() > 0) {
            String finalQuery = "";
            for (String sql : insertQueries)
                finalQuery = finalQuery + sql + "; ";

            DatabaseManager.runQuery(finalQuery);
            loadIDs();
            syncIDs();
        }
    }

    public static void loadTotalCompletions() {
        List<Map<String, String>> results = DatabaseQueries.getResults(
                "completions",
                "level_id, COUNT(*) AS total_completions",
                "GROUP BY level_id"
        );

        for (Map<String, String> result : results) {
            int levelID = Integer.parseInt(result.get("level_id"));
            LevelObject levelObject = LevelManager.get(levelID);

            if (levelObject != null)
                levelObject.setTotalCompletionsCount(Integer.parseInt(result.get("total_completions")));
        }
    }

    public static void loadLeaderboards() {
        for (LevelObject levelObject : levels)
            levelObject.loadLeaderboard();
    }

    public static void create(String levelName) {
        Levels_YAML.create(levelName);
    }

    public static List<String> getNames() {
        List<String> names = new ArrayList<>();

        for (LevelObject levelObject : levels)
            names.add(levelObject.getName());

        return names;
    }

    public static Map<String, String> getNamesLower() {
        Map<String, String> levelNamesLower = new HashMap<>();

        for (String levelName : getNames())
            levelNamesLower.put(levelName.toLowerCase(), levelName);

        return levelNamesLower;
    }

}
