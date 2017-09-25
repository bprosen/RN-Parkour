package com.parkourcraft.Parkour.data;

import com.parkourcraft.Parkour.data.levels.LevelObject;
import com.parkourcraft.Parkour.data.stats.PlayerStats;
import com.parkourcraft.Parkour.storage.mysql.DatabaseQueries;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsManager {

    private static List<PlayerStats> playerStatsList = new ArrayList<>();
    private static Map<Integer, String> playerIDtoNameCache = new HashMap<>();

    public static PlayerStats get(String UUID) {
        for (PlayerStats playerStats : playerStatsList)
            if (playerStats.getUUID().equals(UUID))
                return playerStats;

        return null;
    }

    public static PlayerStats getByName(String playerName) {
        for (PlayerStats playerStats : playerStatsList)
            if (playerStats.getPlayerName().equals(playerName))
                return playerStats;

        return null;
    }

    public static PlayerStats get(Player player) {
        return get(player.getUniqueId().toString());
    }

    public static boolean exists(String UUID) {
        if (get(UUID) != null)
            return true;

        return false;
    }

    public static void add(String UUID, String playerName) {
        if (!exists(UUID))
            playerStatsList.add(new PlayerStats(UUID, playerName));
    }

    public static void remove(String UUID) {
        for (Iterator<PlayerStats> iterator = playerStatsList.iterator(); iterator.hasNext();)
            if (iterator.next().getUUID().equals(UUID))
                iterator.remove();
    }

    public static void update(String UUID) {
        PlayerStats playerStats = get(UUID);

        if (playerStats != null)
            playerStats.updateIntoDatabase();
    }

    public static void updateAll() {
        List<String> removeList = new ArrayList<>();

        for (PlayerStats playerStats : playerStatsList) {
            playerStats.updateIntoDatabase();

            if (Bukkit.getPlayer(UUID.fromString(playerStats.getUUID())) == null)
                removeList.add(playerStats.getUUID());
        }

        for (String UUID : removeList)
            remove(UUID);
    }

    public static void syncTotalCompletions() {
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

    public static void requiredID(int ID) {
        if (!playerIDtoNameCache.containsKey(ID))
            playerIDtoNameCache.put(ID, "");
    }

    public static String getNameFromCache(int ID) {
        if (playerIDtoNameCache.containsKey(ID))
            return playerIDtoNameCache.get(ID);

        return "";
    }

    public static void syncIDtoNameCache() {
        List<String> unknownIDs = new ArrayList<>();

        for (int ID : playerIDtoNameCache.keySet())
            if (playerIDtoNameCache.get(ID).equals(""))
                unknownIDs.add(Integer.toString(ID));


        if (unknownIDs.size() > 0) {
            String selection = String.join(", ", unknownIDs);

            List<Map<String, String>> results = DatabaseQueries.getResults(
                    "players",
                    "player_id, player_name",
                    "WHERE player_id IN (" + selection + ")"
            );

            for (Map<String, String> result : results)
                playerIDtoNameCache.put(
                        Integer.parseInt(result.get("player_id")),
                        result.get("player_name")
                );
        }
    }
























}