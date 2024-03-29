package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.levels.*;
import com.renatusnetwork.momentum.data.locations.LocationManager;
import com.renatusnetwork.momentum.data.locations.LocationsYAML;
import com.renatusnetwork.momentum.data.stats.LevelCompletion;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.data.stats.StatsDB;
import com.renatusnetwork.momentum.gameplay.LevelHandler;
import com.renatusnetwork.momentum.storage.mysql.DatabaseQueries;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LevelCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (sender.isOp() || !(sender instanceof Player)) {

            LevelManager levelManager = Momentum.getLevelManager();

            if (a.length == 0) {
                sendHelp(sender);
            } else {
                if (a[0].equalsIgnoreCase("show")) { // subcommand: show
                    if (sender instanceof Player) {

                        Player player = (Player) sender;
                        if (a.length == 1) {
                            PlayerStats playerStats = Momentum.getStatsManager().get(player);

                            if (playerStats.inLevel())
                                sender.sendMessage(Utils.translate("&7You are in &c" + playerStats.getLevel().getFormattedTitle()));
                            else
                                sender.sendMessage(Utils.translate("&7You are not in a level"));
                        } else {
                            sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                        }
                    }
                } else if (a[0].equalsIgnoreCase("list")) { // subcommand: list
                    sender.sendMessage(Utils.translate("&7Levels loaded in: &2" + String.join("&7, &2",
                            levelManager.getNames())));
                } else if (a[0].equalsIgnoreCase("create")) { // subcommand: create
                    if (a.length == 2) {
                        String levelName = a[1].toLowerCase();

                        if (levelName.contains("'"))
                            sender.sendMessage(Utils.translate("&7Please do not use ' ..."));
                        else if (levelManager.exists(levelName))
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7already exists"));
                        else {
                            LevelsYAML.create(levelName);
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // sync level data now
                                    if (LevelsDB.syncLevelData()) {
                                        levelManager.setLevelDataCache(LevelsDB.getDataCache());
                                        LevelsDB.syncDataCache();
                                    }
                                }
                            }.runTaskAsynchronously(Momentum.getPlugin());
                            sender.sendMessage(Utils.translate("&7Created level &2" + levelName));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("delete")) { // subcommand: delete
                    if (a.length == 2) {
                        String levelName = a[1];

                        if (!levelManager.exists(levelName))
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                        else {
                            levelManager.remove(levelName);
                            sender.sendMessage(Utils.translate("&7Deleted level &2" + levelName));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("load")) { // subcommand: load
                    Momentum.getConfigManager().load("levels");
                    sender.sendMessage(Utils.translate("&7Loaded &2levels.yml &7from disk"));
                    levelManager.load();
                    sender.sendMessage(Utils.translate("&7Loaded levels from &2levels.yml&7, &a" +
                                       Momentum.getLevelManager().getNames().size() + " &7total"));

                    // load total completions and leaderboards in async
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            StatsDB.loadTotalCompletions();
                            StatsDB.loadLeaderboards();
                            Momentum.getLevelManager().loadGlobalLevelCompletions();
                        }
                    }.runTaskAsynchronously(Momentum.getPlugin());

                } else if (a[0].equalsIgnoreCase("title")) { //subcommand: title
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();

                        if (levelManager.exists(levelName)) {
                            if (a.length > 2) {
                                String title = "";
                                for (int i = 2; i < a.length; i++)
                                    title = title + " " + a[i];
                                title = title.trim();

                                LevelsYAML.setTitle(levelName, title);
                                sender.sendMessage(Utils.translate("&7Set &2" + levelName + "&7's title to &2" + title));
                            } else {
                                sender.sendMessage(Utils.translate("&2" + levelName + "&7's current title: &2" +
                                                   LevelsYAML.getTitle(levelName)));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("reward")) { //subcommand: reward
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();
                        Level level = levelManager.get(levelName);

                        if (level != null) {
                            if (a.length == 3) {
                                if (Utils.isInteger(a[2])) {
                                    level.setReward(Integer.parseInt(a[2]));
                                    LevelsDB.updateReward(level);
                                    sender.sendMessage(Utils.translate("&7Set &2" + levelName + "&7's reward to &6" + a[2]));
                                } else {
                                    sender.sendMessage(Utils.translate("&cIncorrect parameters, must enter integer"));
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&2" + levelName + "&7's current reward: &6" + level.getReward()));
                            }
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("startloc")) { //subcommand: startloc
                    if (a.length < 2) {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    } else {
                        String levelName = a[1].toLowerCase();

                        if (Momentum.getLevelManager().exists(levelName)) {
                            if (sender instanceof Player) {
                                Player player = ((Player) sender).getPlayer();

                                Location playerLocation = player.getLocation();
                                String spawnPositionName = levelName + "-spawn";

                                Momentum.getLocationManager().save(spawnPositionName, playerLocation);
                                LevelsYAML.commit(levelName);

                                sender.sendMessage(Utils.translate("&7Location saved as &2" + spawnPositionName +
                                                   " &7for &2" + levelName));
                            } else {
                                sender.sendMessage(Utils.translate("&4This command can only be run in-game"));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                        }
                    }
                } else if (a[0].equalsIgnoreCase("completionloc")) { //subcommand: completionloc
                    if (a.length < 2) {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    } else {
                        String levelName = a[1].toLowerCase();

                        if (Momentum.getLevelManager().exists(levelName)) {
                            if (a.length > 2) {
                                if (a[2].equalsIgnoreCase("default")) {
                                    String completionLocationName = levelName + "-completion";

                                    Momentum.getLocationManager().remove(completionLocationName);
                                    Momentum.getLevelManager().load(levelName);

                                    sender.sendMessage(Utils.translate("&7The completion location for &2" +
                                                       levelName + " &7has been reset to default"));
                                } else {
                                    sender.sendMessage(Utils.translate("&cThe only parameter that can be used is &4default"));
                                    sender.sendMessage(Utils.translate("&7In order to reset the completion " +
                                                       "location, use &/levels completionloc " + levelName + " default"));
                                }
                            } else {
                                if (sender instanceof Player) {
                                    Player player = ((Player) sender).getPlayer();

                                    Location playerLocation = player.getLocation();
                                    String completionLocationName = levelName + "-completion";

                                    Momentum.getLocationManager().save(completionLocationName, playerLocation);
                                    levelManager.load(levelName);

                                    sender.sendMessage(Utils.translate("&7Location saved as &2" +
                                                       completionLocationName + " &7for &2" + levelName));
                                    sender.sendMessage(Utils.translate("&7In order to reset the completion " +
                                                       "location, use &2/levels completionloc " + levelName + "default"));
                                } else {
                                    sender.sendMessage(Utils.translate("&cThis command can only be run in-game"));
                                }
                            }
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                    }
                } else if (a[0].equalsIgnoreCase("message")) { //subcommand: message
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();

                        if (Momentum.getLocationManager().exists(levelName)) {
                            if (a.length > 2) {
                                String message = "";
                                for (int i = 2; i < a.length; i++)
                                    message = message + " " + a[i];
                                message = message.trim();

                                LevelsYAML.setMessage(levelName, message);
                                sender.sendMessage(Utils.translate("&7Set &2" + levelName + "&7's completion message" +
                                                   " to &2" + message));
                            } else {
                                sender.sendMessage(Utils.translate("&2" + levelName + "&7's current message: "
                                                  + LevelsYAML.getMessage(levelName)));
                            }
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + "&7does not exist"));
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("completions")) { //subcommand: completions
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();

                        if (levelManager.exists(levelName)) {
                            if (a.length == 3) {
                                if (Utils.isInteger(a[2])) {
                                    LevelsYAML.setMaxCompletions(levelName, Integer.parseInt(a[2]));
                                    sender.sendMessage(Utils.translate("&7Set &2" + levelName + "&7's max completions to &2" + a[2]));
                                } else {
                                    sender.sendMessage(Utils.translate("&cIncorrect parameters, must enter integer"));
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&2" + levelName + "&7's current max " +
                                                   "completions: &2" + LevelsYAML.getMaxCompletions(levelName)));
                            }
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + "&7 does not exist"));
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("broadcast")) { //subcommand: broadcast
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();

                        if (levelManager.exists(levelName)) {
                            boolean broadcastSetting = LevelsYAML.getBroadcastSetting(levelName);

                            LevelsYAML.setBroadcast(levelName, !broadcastSetting);

                            sender.sendMessage(Utils.translate("&7Broadcast completion for &2" + levelName +
                                               " &7was set to " + (!broadcastSetting)));
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a[0].equalsIgnoreCase("requires")) { //subcommand: requires
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();
                        Level level = Momentum.getLevelManager().get(levelName);

                        if (level != null) {
                            if (a.length == 3) {
                                List<String> requiredLevels = level.getRequiredLevels();

                                if (requiredLevels.contains(a[2])) {
                                    requiredLevels.remove(a[2]);
                                    LevelsYAML.setRequiredLevels(levelName, requiredLevels);

                                    sender.sendMessage(Utils.translate("&7Removed &c" + a[2] + " &7from required" +
                                                       " levels"));
                                } else {
                                    requiredLevels.add(a[2]);
                                    LevelsYAML.setRequiredLevels(levelName, requiredLevels);

                                    sender.sendMessage(Utils.translate("&7Added &2" + a[2] + " &7to required levels"));
                                }
                            }
                            level = levelManager.get(levelName);
                            sender.sendMessage(Utils.translate("&2" + levelName + "&7's required levels: &2" +
                                               String.join("&7, &2", level.getRequiredLevels())));
                        } else {
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                        }
                    }
                } else if (a[0].equalsIgnoreCase("removetime") && a.length == 3) {

                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (Utils.isInteger(a[2])) {

                        int row = Integer.parseInt(a[2]);

                        if (level != null) {

                            List<LevelCompletion> completions = level.getLeaderboard();

                            if (row <= completions.size() && row > 0) {

                                int name = row - 1;
                                LevelCompletion levelCompletion = completions.get(name);
                                int time = (int) levelCompletion.getCompletionTimeElapsed();
                                // get player ID for specific deletion on no timer based levels
                                int playerID = StatsDB.getPlayerID(levelCompletion.getPlayerName());

                                // if deleting record
                                if (name == 0)
                                {
                                    // update records
                                    PlayerStats playerStats = Momentum.getStatsManager().getByName(levelCompletion.getPlayerName());

                                    if (playerStats != null)
                                        Momentum.getStatsManager().removeRecord(playerStats, playerStats.getRecords());
                                    else
                                        StatsDB.removeRecordsName(levelCompletion.getPlayerName());
                                }

                                Momentum.getDatabaseManager().runAsyncQuery("DELETE FROM completions WHERE level_id=" +
                                        level.getID() + " AND time_taken=" + time + " AND player_id=" + playerID);
                                completions.remove(levelCompletion);
                                level.setTotalCompletionsCount(level.getTotalCompletionsCount() - 1);
                                sender.sendMessage(Utils.translate("&4" + levelCompletion.getPlayerName() + "'s" +
                                                   " &ctime has been removed succesfully from &4" + levelName));
                                // run it async 0.25 seconds later so it does it when database has updated
                                new BukkitRunnable() {
                                    public void run() {
                                        StatsDB.loadLeaderboard(level);

                                        if (!level.getLeaderboard().isEmpty())
                                        {
                                            String newFirstPlace = level.getLeaderboard().get(0).getPlayerName();
                                            PlayerStats playerStats = Momentum.getStatsManager().getByName(newFirstPlace);

                                            // if not null, use stats manager
                                            if (playerStats != null)
                                                playerStats.setRecords(playerStats.getRecords() + 1);

                                            StatsDB.addRecordsName(newFirstPlace);
                                        }
                                    }
                                }.runTaskLaterAsynchronously(Momentum.getPlugin(), 5);
                            } else {
                                sender.sendMessage(Utils.translate("&cYou are entering an integer above 9"));
                            }
                        } else {
                                sender.sendMessage(Utils.translate("&7No level named '&c" + levelName + "&7' exists"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&c" + a[2] + " &7is not an integer!"));
                    }
                } else if (a[0].equalsIgnoreCase("modifier")) { //subcommand: modifier
                    if (a.length > 1) {
                        String levelName = a[1].toLowerCase();
                        Level level = Momentum.getLevelManager().get(levelName);

                        if (level != null) {
                            if (a.length == 3) {
                                if (Utils.isInteger(a[2])) {
                                    level.setScoreModifier(Integer.parseInt(a[2]));
                                    LevelsDB.updateScoreModifier(level);

                                    sender.sendMessage(Utils.translate("&7Set &2" + levelName + "&7's " +
                                                       "score modifier to &6" + a[2]));
                                } else {
                                    sender.sendMessage(Utils.translate("&cIncorrect parameters, must enter integer"));
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&2" + levelName + "&7's current score modifier:" +
                                                   " &6" + level.getScoreModifier()));
                            }
                        } else
                            sender.sendMessage(Utils.translate("&7Level &2" + levelName + " &7does not exist"));
                    } else {
                        sender.sendMessage(Utils.translate("&cIncorrect parameters"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("raceset")) {

                    if (!(sender instanceof Player)) {
                        return true;
                    }

                    Player player = (Player) sender;

                    if (a[2].equalsIgnoreCase("player1") || a[2].equalsIgnoreCase("player2")) {
                        String levelName = a[1].toLowerCase();

                        if (levelManager.exists(levelName)) {
                            LevelsYAML.setPlayerRaceLocation(a[2], levelName, player.getLocation());
                            player.sendMessage(Utils.translate("&cYou set the location for &4" + a[2] + " &con level &4" + levelName));
                        } else {
                            sender.sendMessage(Utils.translate("&7The level &c" + levelName + " &7does not exist"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cPlayer must be player1 or player2"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("forcecompletion")) {
                    Player target = Bukkit.getPlayer(a[1]);
                    String levelName = a[2].toLowerCase();

                    if (target == null) {
                        sender.sendMessage(Utils.translate("&4" + a[1] + " &cis not online!"));
                        return true;
                    }

                    PlayerStats playerStats = Momentum.getStatsManager().get(target);

                    if (levelManager.exists(levelName)) {

                        Level level = levelManager.get(levelName);
                        boolean isRankUpLevel = false;

                        if (level.isRankUpLevel())
                            isRankUpLevel = true;

                        LevelHandler.dolevelCompletion(playerStats, target, level, levelName, isRankUpLevel, true);
                        sender.sendMessage(Utils.translate("&7You forced a &c" + level.getFormattedTitle() + " &7Completion for &a" + target.getName()));
                    } else {
                        sender.sendMessage(Utils.translate("&cLevel &4" + levelName + " &cdoes not exist"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("setrespawny")) {
                    String levelName = a[1].toLowerCase();

                    if (Utils.isInteger(a[2])) {
                        // get new y
                        int newY = Integer.parseInt(a[2]);

                        if (levelManager.exists(levelName)) {
                            LevelsYAML.setRespawnY(levelName, newY);
                            sender.sendMessage(Utils.translate("&7You have set &c" +
                                    Momentum.getLevelManager().get(levelName).getFormattedTitle() +
                                    "&7's respawn y to &c" + newY));
                        } else {
                            sender.sendMessage(Utils.translate("&cLevel &4" + levelName + " &cdoes not exist"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + a[2] + " &cis not an integer"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("addrating")) {

                    if (!(sender instanceof Player)) {
                        return true;
                    }

                    Player player = (Player) sender;

                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        if (Utils.isInteger(a[2])) {

                            int rating = Integer.parseInt(a[2]);

                            if (rating <= 5 && rating >= 0) {
                                level.addRatingAndCalc(rating);
                                RatingDB.addRating(player, level, rating);
                                sender.sendMessage(Utils.translate("&7You added a rating of &4" + rating
                                        + " &7to &cLevel &a" + level.getFormattedTitle()));
                            } else {
                                sender.sendMessage(Utils.translate("&cRating has to be between &40-5"));
                            }
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("removerating")) {

                    String levelName = a[1].toLowerCase();
                    String playerName = a[2];
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        if (RatingDB.hasRatedLevelFromName(playerName, level.getID())) {

                            List<Map<String, String>> ratingResults = DatabaseQueries.getResults(
                                    "ratings",
                                    "rating",
                                    " WHERE player_name=? AND level_id=" + level.getID(), playerName
                            );

                            // loop through and remove, then remove all from database
                            for (Map<String, String> ratingResult : ratingResults)
                                level.removeRatingAndCalc(Integer.parseInt(ratingResult.get("rating")));

                            Momentum.getDatabaseManager().runAsyncQuery("DELETE FROM ratings WHERE level_id=" + level.getID()
                                    + " AND player_name='" + playerName + "'");

                            sender.sendMessage(Utils.translate("&cYou removed &4" + playerName + " &crating from &7" + level.getFormattedTitle()));

                        } else {
                            sender.sendMessage(Utils.translate("&4" + playerName + " &chas not rated &4" + levelName));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("hasrated")) {
                    String levelName = a[1].toLowerCase();
                    String playerName = a[2];
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        if (RatingDB.hasRatedLevelFromName(playerName, level.getID()))

                            sender.sendMessage(Utils.translate("&c" + playerName + " &7has rated &c" +
                                    level.getFormattedTitle() + " &7with a &6" +
                                    RatingDB.getRatingFromName(playerName, level.getID())));

                        else

                            sender.sendMessage(Utils.translate("&c" + playerName + " &7has not rated &c" +
                                    level.getFormattedTitle()));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length >= 2 && a[0].equalsIgnoreCase("listratings")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        if (a.length == 2) {
                            HashMap<Integer, List<String>> ratings = RatingDB.getAllLevelRaters(level.getID());

                            if (!ratings.isEmpty()) {

                                sender.sendMessage(Utils.translate("&2" + level.getFormattedTitle() + "&7's Ratings"));
                                int totalRatings = 0;

                                // loop through list
                                for (Map.Entry<Integer, List<String>> entry : ratings.entrySet()) {
                                    String msg = " &2" + entry.getKey() + " &7-";
                                    // loop through names
                                    for (String playerName : entry.getValue()) {
                                        if (!entry.getValue().get(entry.getValue().size() - 1).equalsIgnoreCase(playerName))
                                            msg += " &a" + playerName + "&7,";
                                        else
                                            // no comma if last one
                                            msg += " &a" + playerName;

                                        totalRatings++;
                                    }
                                    sender.sendMessage(Utils.translate(msg));
                                }
                                sender.sendMessage(Utils.translate("&a" + totalRatings + " &7Ratings"));
                            } else {
                                sender.sendMessage(Utils.translate("&cNobody has rated this level"));
                            }
                        // if they put the optional specification arg
                        } else if (a.length == 3) {
                            if (Utils.isInteger(a[2])) {
                                int rating = Integer.parseInt(a[2]);
                                // make sure it is between 0 and 5
                                if (rating >= 0 && rating <= 5) {

                                    List<String> ratings = RatingDB.getSpecificLevelRaters(level.getID(), rating);
                                    // if it is empty
                                    if (!ratings.isEmpty()) {

                                        sender.sendMessage(Utils.translate("&7Users who rated &2" +
                                                                level.getFormattedTitle() + " &7a &a" + rating));

                                        String msg = " &2" + rating + " &7-";

                                        for (String playerName : ratings) {
                                            if (!ratings.get(ratings.size() - 1).equalsIgnoreCase(playerName))
                                                msg += " &a" + playerName + "&7,";
                                            else
                                                // no comma if last one
                                                msg += " &a" + playerName;
                                        }
                                        sender.sendMessage(Utils.translate(msg));
                                        sender.sendMessage(Utils.translate("&a" + ratings.size() + " &7Ratings"));
                                    } else {
                                        sender.sendMessage(Utils.translate("&cNobody has rated this level a " + rating));
                                    }
                                } else {
                                    sender.sendMessage(Utils.translate("&cYour rating has to be anywhere from 0 to 5!"));
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&4" + a[2] + " &cis not an Integer"));
                            }
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("togglewater")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {

                        if (level.doesLiquidResetPlayer())
                            sender.sendMessage(Utils.translate("&7You toggled off liquid resetting players for level &c" + level.getFormattedTitle()));
                        else
                            sender.sendMessage(Utils.translate("&7You have toggled on liquid resetting players for level &c" + level.getFormattedTitle()));

                        LevelsYAML.toggleWaterReset(levelName);
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("rename")) {
                    String levelName = a[1];
                    String newLevelName = a[2].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        if (!levelManager.exists(newLevelName)) {
                            if (!newLevelName.contains("'")) {
                                // update in yaml and db
                                LevelsDB.updateName(levelName, newLevelName);
                                LevelsYAML.renameLevel(levelName, newLevelName);
                                LocationsYAML.renameLocation(levelName, newLevelName);

                                // update in level cache
                                level.setName(newLevelName);
                                levelManager.getLevels().remove(levelName);
                                levelManager.getLevels().put(newLevelName, level);

                                // update in leveldata cache
                                LevelData levelData = levelManager.getLevelDataCache().get(levelName);
                                levelManager.getLevelDataCache().remove(levelName);
                                levelManager.getLevelDataCache().put(newLevelName, levelData);

                                // remove and add from location cache
                                LocationManager locationManager = Momentum.getLocationManager();
                                if (locationManager.hasCompletionLocation(levelName)) {
                                    locationManager.getLocations().remove(levelName + "-completion");
                                    locationManager.load(newLevelName + "-completion");
                                }
                                if (locationManager.hasSpawnLocation(levelName)) {
                                    locationManager.getLocations().remove(levelName + "-spawn");
                                    locationManager.load(newLevelName + "-spawn");
                                }

                                if (locationManager.hasPortalLocation(levelName))
                                {
                                    locationManager.getLocations().remove(levelName + "-portal");
                                    locationManager.load(newLevelName + "-portal");
                                }
                                // run this in async, heavy task and can be in async
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        Momentum.getMenuManager().renameLevel(levelName, newLevelName);
                                    }
                                }.runTaskAsynchronously(Momentum.getPlugin());

                                sender.sendMessage(Utils.translate("&cYou have renamed &4" + levelName + " &cto &4" + newLevelName));
                            } else {
                                sender.sendMessage(Utils.translate("&7Please do not use ' ..."));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&4" + newLevelName + " &calready exists"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("toggleelytra")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        LevelsYAML.toggleElytraLevel(levelName);
                        sender.sendMessage(Utils.translate("&7You have set &c" + level.getFormattedTitle() +
                                "&7's Elytra to " + LevelsYAML.isElytraLevel(levelName)));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("toggledropper")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        LevelsYAML.toggleDropperLevel(levelName);
                        sender.sendMessage(Utils.translate("&7You have set &c" + level.getFormattedTitle() +
                                "&7 as a dropper level to &c" + LevelsYAML.isDropperLevel(levelName)));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("toggletc")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        LevelsYAML.toggleTCLevel(levelName);
                        sender.sendMessage(Utils.translate("&7You have set &c" + level.getFormattedTitle() +
                                "&7 as a tc level to &c" + LevelsYAML.isTCLevel(levelName)));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("toggleascendance")) {
                    String levelName = a[1].toLowerCase();
                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        LevelsYAML.toggleAscendanceLevel(levelName);
                        sender.sendMessage(Utils.translate("&7You have set &c" + level.getFormattedTitle() +
                                "&7 as a ascendance level to &c" + LevelsYAML.isAscendanceLevel(levelName)));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("delcompletions")) {
                    String playerName = a[1];
                    String levelName = a[2].toLowerCase();

                    Level level = levelManager.get(levelName);

                    if (level != null) {
                        int playerID = StatsDB.getPlayerID(playerName);
                        if (playerID > -1) {
                            if (StatsDB.hasCompleted(playerID, level.getID())) {

                                List<LevelCompletion> leaderboard = level.getLeaderboard();

                                // if deleting record
                                if (!leaderboard.isEmpty() && leaderboard.get(0).getPlayerName().equalsIgnoreCase(playerName))
                                {
                                    // update records
                                    PlayerStats playerStats = Momentum.getStatsManager().getByName(playerName);

                                    if (playerStats != null)
                                        Momentum.getStatsManager().removeRecord(playerStats, playerStats.getRecords());
                                    else
                                        StatsDB.removeRecordsName(playerName);
                                }

                                int totalCompletions = StatsDB.getTotalCompletions(playerName);
                                int levelCompletions = StatsDB.getCompletionsFromLevel(playerName, level.getID());

                                int newTotal = totalCompletions - levelCompletions;

                                Momentum.getDatabaseManager().runAsyncQuery(
                                        "UPDATE players SET level_completions=" + newTotal +
                                                " WHERE player_name=?", playerName);

                                StatsDB.removeCompletions(playerID, level.getID());

                                PlayerStats playerStats = Momentum.getStatsManager().getByName(playerName);
                                if (playerStats != null) {
                                    // remove completion from stats and other personalized stat
                                    playerStats.getLevelCompletionsMap().remove(levelName);
                                    playerStats.setTotalLevelCompletions(playerStats.getTotalLevelCompletions() - 1);
                                }

                                // remove completion on level basis
                                levelManager.removeTotalLevelCompletion();
                                level.setTotalCompletionsCount(level.getTotalCompletionsCount() - 1);

                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        StatsDB.loadLeaderboard(level);

                                        if (!level.getLeaderboard().isEmpty())
                                        {
                                            String newFirstPlace = level.getLeaderboard().get(0).getPlayerName();
                                            PlayerStats playerStats = Momentum.getStatsManager().getByName(newFirstPlace);

                                            // if not null, use stats manager
                                            if (playerStats != null)
                                                playerStats.setRecords(playerStats.getRecords() + 1);

                                            StatsDB.addRecordsName(newFirstPlace);
                                        }
                                    }
                                }.runTaskLaterAsynchronously(Momentum.getPlugin(), 5);

                                sender.sendMessage(Utils.translate("&cYou removed all of &4" + playerName + "&c's completions for &4" + levelName));
                            } else {
                                sender.sendMessage(Utils.translate("&4" + playerName + " &chas yet to complete &4" + levelName));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&4" + playerName + " &chas not joined the server"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a valid level name"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("resetcheckpoints")) {
                    String levelName = a[1].toLowerCase();
                    Level level = Momentum.getLevelManager().get(levelName);

                    if (level != null) {
                        // reset from cache
                        for (PlayerStats playerStats : Momentum.getStatsManager().getPlayerStats().values())
                            if (playerStats.hasCurrentCheckpoint() && playerStats.getLevel() != null &&
                                    playerStats.getLevel().getName().equalsIgnoreCase(levelName))
                            {
                                playerStats.resetCurrentCheckpoint();
                                playerStats.removeCheckpoint(levelName);
                            }

                        // delete from db
                        Momentum.getDatabaseManager().runAsyncQuery("DELETE FROM checkpoints WHERE level_name=?", levelName);

                        sender.sendMessage(Utils.translate("&cYou have deleted all checkpoints for level &4" + levelName));
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a level"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("resetcheckpoint")) {
                    String levelName = a[1].toLowerCase();
                    Level level = Momentum.getLevelManager().get(levelName);

                    if (level != null) {
                        String playerName = a[2].toLowerCase();
                        Player target = Bukkit.getPlayer(playerName);

                        if (target != null) {
                            PlayerStats playerStats = Momentum.getStatsManager().get(target);

                            // if they have checkpoint loaded
                            if (playerStats.hasCurrentCheckpoint()) {
                                // if they are in level and their level is the same as the target level
                                if (playerStats.getLevel() != null && playerStats.getLevel().getName().equalsIgnoreCase(levelName)) {
                                    Momentum.getCheckpointManager().deleteCheckpoint(playerStats, level);
                                    sender.sendMessage(Utils.translate("&cYou deleted &4" + playerName + "&c's checkpoint for &4" + levelName));
                                } else {
                                    sender.sendMessage(Utils.translate("&4" + playerName + " &cis not in " + levelName));
                                }
                            } else {
                                Momentum.getDatabaseManager().runAsyncQuery("DELETE FROM checkpoints WHERE level_name=? AND player_name=?", levelName, playerName);
                                sender.sendMessage(Utils.translate("&4" + playerName + " &cdoes not have a loaded checkpoint, but any database record was deleted"));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&4" + playerName + " &cis not online"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a level"));
                    }
                } else if (a.length == 1 && a[0].equalsIgnoreCase("cleanleveldatadb")) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            HashMap<String, LevelData> levelCache = levelManager.getLevelDataCache();
                            HashMap<String, Level> levels = levelManager.getLevels();

                            Set<String> levelsToRemove = new HashSet<>();

                            for (String levelName : levelCache.keySet())
                                if (!levels.containsKey(levelName))
                                    levelsToRemove.add(levelName); // add to levels to remove

                            // now remove from level cache and db
                            for (String levelName : levelsToRemove) {
                                levelCache.remove(levelName);
                                Momentum.getDatabaseManager().runQuery("DELETE FROM levels WHERE level_name='" + levelName + "'");
                            }

                            sender.sendMessage(Utils.translate("&2" + levelsToRemove.size() + " &7levels cleaned from the database (already removed levels, etc)"));
                            Momentum.getPluginLogger().info("Levels in data cache: " + levelCache.size());
                        }
                    }.runTaskAsynchronously(Momentum.getPlugin());
                }
                else if (a.length == 4 && a[0].equalsIgnoreCase("totalcompletions"))
                {
                    // asyncify since big db searching
                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            String levelName = a[1];
                            String startDate = a[2];
                            String endDate = a[3];

                            Level level = Momentum.getLevelManager().get(levelName);

                            if (level != null)
                            {
                                long totalCompletions = LevelsDB.getCompletionsBetweenDates(level.getID(), startDate, endDate);

                                sender.sendMessage(Utils.translate(
                                        "&c" + level.getFormattedTitle() + " &7between &a" + startDate + " &7and &a" + endDate +
                                                " &7has &a" + Utils.formatNumber(totalCompletions) + " &7Completions"));
                            }
                            else
                            {
                                sender.sendMessage(Utils.translate("&cThe level &4" + levelName + " &cdoes not exist"));
                            }
                        }
                    }.runTaskAsynchronously(Momentum.getPlugin());
                }
                else if (a.length == 1 && a[0].equalsIgnoreCase("syncrecords"))
                {
                    // asyncify since big iteration
                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            StatsDB.syncRecords();
                            sender.sendMessage(Utils.translate("&7Synced level records"));
                        }
                    }.runTaskAsynchronously(Momentum.getPlugin());
                }
                else if (a.length == 3 && a[0].equalsIgnoreCase("setprice"))
                {
                    String levelName = a[1];

                    if (Utils.isInteger(a[2]))
                    {
                        int price = Integer.parseInt(a[2]);
                        Level level = levelManager.get(levelName);

                        if (level != null)
                        {
                            level.setPrice(price);
                            LevelsYAML.setPrice(levelName, price);
                            sender.sendMessage(Utils.translate("&7You set the price of &c" + level.getFormattedTitle() + " &7to &6" + Utils.formatNumber(price) + " &e&lCoins"));
                        }
                        else
                        {
                            sender.sendMessage(Utils.translate("&c'&4" + levelName + "&c' is not a valid level"));
                        }
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&c'&4" + a[2] + "&c' is not a valid integer"));
                    }
                }
                else if (a.length == 3 && a[0].equalsIgnoreCase("addboughtlevel"))
                {
                    Level level = levelManager.get(a[2]);

                    if (level != null)
                    {
                        if (level.getPrice() > 0)
                        {
                            Player target = Bukkit.getPlayer(a[1]);

                            if (target != null)
                            {
                                PlayerStats targetStats = Momentum.getStatsManager().get(target);

                                if (!targetStats.hasBoughtLevel(level.getName()))
                                {
                                    targetStats.buyLevel(level.getName());
                                    StatsDB.addBoughtLevel(targetStats, level.getName());

                                    sender.sendMessage(Utils.translate("&7You have added &c" + level.getFormattedTitle() + " &7to &4" + a[1] + "&c's &7bought levels"));
                                }
                                else
                                {
                                    sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' has already bought &4" + level.getFormattedTitle() + "&c!"));
                                }
                            }
                            else
                            {
                                sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' is not online"));
                            }
                        }
                        else
                        {
                            sender.sendMessage(Utils.translate("&c'&4" + level.getFormattedTitle() + "&c' is not a buyable level"));
                        }
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&c'&4" + a[2] + "&c' is not a valid level"));
                    }
                }
                else if (a.length == 3 && a[0].equalsIgnoreCase("removeboughtlevel"))
                {
                    Level level = levelManager.get(a[2]);

                    if (level != null)
                    {
                        new BukkitRunnable()
                        {
                            @Override
                            public void run()
                            {
                                if (StatsDB.isPlayerInDatabase(a[1]))
                                {
                                    if (StatsDB.hasBoughtLevel(a[1], level.getName()))
                                    {
                                        PlayerStats targetStats = Momentum.getStatsManager().getByName(a[1]);

                                        // if non null, remove bought level
                                        if (targetStats != null)
                                            targetStats.removeBoughtLevel(level.getName());

                                        StatsDB.removeBoughtLevel(a[1], level.getName());
                                        sender.sendMessage(Utils.translate("&7You have removed &c" + level.getFormattedTitle() + " &7from &4" + a[1] + "&c's &7bought levels"));
                                    }
                                    else
                                    {
                                        sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' has not bought &4" + level.getFormattedTitle() + "&c!"));
                                    }
                                }
                                else
                                {
                                    sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' has not joined the server!"));
                                }
                            }
                        }.runTaskAsynchronously(Momentum.getPlugin());
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&c'&4" + a[2] + "&c' is not a valid level"));
                    }
                }
                else if (a.length == 2 && a[0].equalsIgnoreCase("togglenew"))
                {
                    Level level = levelManager.get(a[1]);

                    if (level != null)
                    {
                        level.toggleNewLevel();
                        LevelsYAML.setNewLevel(level.getName(), level.isNewLevel());
                        sender.sendMessage(Utils.translate("&7You have set the new level value of &c" + level.getFormattedTitle() + " &7to: &c" + level.isNewLevel()));
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' is not a valid level"));
                    }
                }
                else if (a.length == 3 && a[0].equalsIgnoreCase("setdifficulty"))
                {
                    Level level = levelManager.get(a[1]);

                    if (level != null)
                    {
                        if (Utils.isInteger(a[2]))
                        {
                            int difficulty = Integer.parseInt(a[2]);

                            if (difficulty > 10)
                                difficulty = 10;

                            if (difficulty < 1)
                                difficulty = 1;

                            LevelsYAML.setDifficulty(level.getName(), difficulty);
                            sender.sendMessage(Utils.translate("&7You have set the difficulty of &c" + level.getFormattedTitle() + " &7to &c" + difficulty));
                        }
                        else
                        {
                            sender.sendMessage(Utils.translate("&c'&4" + a[2] + "&c' is not a valid integer"));
                        }
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&c'&4" + a[1] + "&c' is not a valid level"));
                    }
                }
                else if (a.length == 1 && a[0].equalsIgnoreCase("pickfeatured"))
                {
                   levelManager.pickFeatured();
                   sender.sendMessage(Utils.translate("&7You have set the new featured to &c" + levelManager.getFeaturedLevel().getFormattedTitle()));
                }
                else if (a.length == 3 && a[0].equalsIgnoreCase("resetsave"))
                {
                    String playerName = a[1];
                    Player target = Bukkit.getPlayer(playerName);
                    String levelName = a[2].toLowerCase();
                    Level level = Momentum.getLevelManager().get(levelName);

                    if (level != null)
                    {
                        PlayerStats playerStats = Momentum.getStatsManager().getByName(playerName);

                        if (playerStats != null)
                        {
                            // if they have save
                            if (playerStats.hasSave(levelName))
                            {
                                Momentum.getSavesManager().removeSave(playerStats, level);
                                sender.sendMessage(Utils.translate("&7You have reset &c" + playerName + "'s &7save on &a" + level.getFormattedTitle()));
                            }
                            else
                                sender.sendMessage(Utils.translate("&4" + playerName + " &cdoes not have a save for " + level.getFormattedTitle()));
                        }
                        else
                        {
                            Momentum.getDatabaseManager().runAsyncQuery("DELETE FROM saves WHERE player_name=? AND level_name=?", playerName, levelName);
                            sender.sendMessage(Utils.translate("&4" + playerName + " &cis not online but any record has been deleted from the database"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&4" + levelName + " &cis not a level"));
                    }
                }
                else
                {
                    sender.sendMessage(Utils.translate("&c'&4" + a[0] + "&c' is not a valid parameter"));
                    sendHelp(sender);
                }
            }
        } else {
            sender.sendMessage(Utils.translate("&cYou do not have permission to run that command"));
        }
        return true;
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(Utils.translate("&aTo apply changes use &2/level load"));
        sender.sendMessage(Utils.translate("&7Level names are all lowercase"));
        sender.sendMessage(Utils.translate("&a/level show  &7Show level information"));
        sender.sendMessage(Utils.translate("&a/level list  &7List levels loaded in memory"));
        sender.sendMessage(Utils.translate("&a/level create <level>  &7Create a level"));
        sender.sendMessage(Utils.translate("&a/level load  &7Loads levels.yml then levels"));
        sender.sendMessage(Utils.translate("&a/level delete <level>  &7Delete a level"));
        sender.sendMessage(Utils.translate("&a/level title <level> [title]  &7View/Set a level's title"));
        sender.sendMessage(Utils.translate("&a/level reward <level> [reward]  &7View/Set a level's reward"));
        sender.sendMessage(Utils.translate("&a/level startloc <level>  &7Sets the start to your location"));
        sender.sendMessage(Utils.translate("&a/level completionloc <leveL>  &7Sets the completion to your location"));
        sender.sendMessage(Utils.translate("&a/level message <level> [message]  &7View/Set completion message"));
        sender.sendMessage(Utils.translate("&a/level completions <level> [completions]  &7View/Set max completions"));
        sender.sendMessage(Utils.translate("&a/level broadcast <level>  &7Toggled broadcast completion"));
        sender.sendMessage(Utils.translate("&a/level requires <level> <levelTheyNeed>  &7Add/Remove required level"));
        sender.sendMessage(Utils.translate("&a/level modifier <level> [modifier]  &7View/Set Score Modifier"));
        sender.sendMessage(Utils.translate("&a/level removetime <level> <leaderboardPlace>  &7Removes a player's time from a level's leaderboard"));
        sender.sendMessage(Utils.translate("&a/level raceset <level> <player1/player2>  &7Sets the race location for player 1 or 2"));
        sender.sendMessage(Utils.translate("&a/level forcecompletion <player> <level>  &7Force completion for player"));
        sender.sendMessage(Utils.translate("&a/level addrating <level> <rating (0-5)>  &7Adds a rating to a level (ADMIN WAY NOT /rate)"));
        sender.sendMessage(Utils.translate("&a/level removerating <level> <playerName>  &7Removes a rating from a level by player name"));
        sender.sendMessage(Utils.translate("&a/level hasrated <level> <playerName>  &7Tells you if someone has rated it and with what rating"));
        sender.sendMessage(Utils.translate("&a/level listratings <level> [rating (0-5)] &7Tells you all the ratings for a level with optional 0-5 specification"));
        sender.sendMessage(Utils.translate("&a/level togglewater <level>  &7Toggles the water from respawning you in a level"));
        sender.sendMessage(Utils.translate("&a/level rename <level> <newLevelName>  &7Renames a level's name to a new name"));
        sender.sendMessage(Utils.translate("&a/level delcompletions <player> <levelName>  &7Deletes ALL the completions of a player for a level"));
        sender.sendMessage(Utils.translate("&a/level setrespawny <level> <respawnY>  &7Sets level respawn y"));
        sender.sendMessage(Utils.translate("&a/level toggleelytra <level>  &7Sets level elytra"));
        sender.sendMessage(Utils.translate("&a/level toggledropper <level>  &7Sets level as dropper"));
        sender.sendMessage(Utils.translate("&a/level toggleascendance <level>  &7Sets level as ascendance level"));
        sender.sendMessage(Utils.translate("&a/level resetcheckpoint <level> <player>  &7Resets level checkpoint for single player"));
        sender.sendMessage(Utils.translate("&a/level resetcheckpoints <level>  &7Resets ALL checkpoints for specific level"));
        sender.sendMessage(Utils.translate("&a/level cleanleveldatadb  &7Cleans invalid data"));
        sender.sendMessage(Utils.translate("&a/level totalcompletions <level> <startDate> <endDate>  &7Gets total number of completions between two dates (YYYY-MM-DD)"));
        sender.sendMessage(Utils.translate("&a/level setprice <level> <price>  &7Sets the level's price"));
        sender.sendMessage(Utils.translate("&a/level addboughtlevel <player> <level>  &7Add bought level to player"));
        sender.sendMessage(Utils.translate("&a/level removeboughtlevel <player> <level>  &7Remove bought level from player"));
        sender.sendMessage(Utils.translate("&a/level togglenew <level>  &7Toggles if the level is new (for menu and future updates)"));
        sender.sendMessage(Utils.translate("&a/level setdifficulty <level> <difficulty>  &7Sets the difficulty of the level"));
        sender.sendMessage(Utils.translate("&a/level pickfeatured  &7Picks a new featured level"));
        sender.sendMessage(Utils.translate("&a/level resetsave <player> <level>  &7Resets a player's save for a specific level"));
        sender.sendMessage(Utils.translate("&a/level toggletc <level>  &7Sets level as tc"));
    }
}
