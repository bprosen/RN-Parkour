package com.renatusnetwork.momentum.gameplay;

import com.connorlinfoot.titleapi.TitleAPI;
import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.api.LevelCompletionEvent;
import com.renatusnetwork.momentum.data.events.EventManager;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.levels.LevelManager;
import com.renatusnetwork.momentum.data.stats.LevelCompletion;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.data.stats.StatsDB;
import com.renatusnetwork.momentum.utils.Utils;
import com.renatusnetwork.momentum.utils.dependencies.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

public class LevelHandler {

    static void levelCompletion(Player player, String levelName) {

        PlayerStats playerStats = Momentum.getStatsManager().get(player);
        EventManager eventManager = Momentum.getEventManager();
        Level level = Momentum.getLevelManager().get(levelName);

        // if playerstats and level exists
        if (playerStats != null && !playerStats.isSpectating() && level != null) {
            // if they do have the required level
            if (level.hasRequiredLevels(playerStats)) {
                // if does not have a practice location
                if (!playerStats.inPracticeMode()) {

                    int playerLevelCompletions = playerStats.getLevelCompletionsCount(levelName);

                    if (level.getMaxCompletions() == -1 || playerLevelCompletions < level.getMaxCompletions()) {
                        // if it is a race completion, end it
                        if (!playerStats.inRace()) {
                            // if level is not a rankup level, continue
                            if (!level.isRankUpLevel()) {
                                // if level is not an event level, it is guaranteed normal completion
                                if (!level.isEventLevel())
                                    dolevelCompletion(playerStats, player, level, levelName, false, false);
                                // otherwise, if there is an event running, end!
                                else if (eventManager.isEventRunning())
                                    eventManager.endEvent(player, false, false);
                                // otherwise, they are clicking the sign when the event is not running
                                else
                                    player.sendMessage(Utils.translate("&cYou cannot do this when an Event is not running!"));

                            } else if (playerStats.getRankUpStage() == 2)
                                dolevelCompletion(playerStats, player, level, levelName, true, false);
                        } else {
                            // if in race
                            Momentum.getRaceManager().endRace(player, false);
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou've reached the maximum number of completions"));
                    }
                } else {
                    player.sendMessage(Utils.translate("&cYou cannot complete a level in practice mode"));
                }
            } else {
                    player.sendMessage(Utils.translate("&cYou do not have the required levels to complete this level"));
            }
        }
    }

    public static void dolevelCompletion(PlayerStats playerStats, Player player, Level level, String levelName, boolean rankUpLevel, boolean forcedCompletion) {

        LevelCompletionEvent event = new LevelCompletionEvent(playerStats, level);
        Bukkit.getPluginManager().callEvent(event);

        // continue if not cancelled
        if (!event.isCancelled())
        {
            LevelManager levelManager = Momentum.getLevelManager();

            // if they have not completed this individual level, then add
            if (playerStats.getLevelCompletionsCount(levelName) < 1)
                playerStats.setIndividualLevelsBeaten(playerStats.getIndividualLevelsBeaten() + 1);

            Long elapsedTime = (System.currentTimeMillis() - playerStats.getLevelStartTime());
            String time = (((double) elapsedTime) / 1000) + "s";
            LevelCompletion levelCompletion = new LevelCompletion(
                    System.currentTimeMillis(),
                    elapsedTime
            );

            // disable when complete
            if (level.getName().equalsIgnoreCase(Momentum.getLevelManager().getTutorialLevel().getName()))
                playerStats.setTutorial(false);

            levelCompletion.setPlayerName(player.getName());
            playerStats.setTotalLevelCompletions(playerStats.getTotalLevelCompletions() + 1);

            // small microoptimization running it in async
            new BukkitRunnable() {
                @Override
                public void run() {
                    StatsDB.insertCompletion(playerStats, level, levelCompletion);
                }
            }.runTaskAsynchronously(Momentum.getPlugin());

            levelManager.addTotalLevelCompletion();

            level.addCompletion(player.getName(), levelCompletion); // Update totalLevelCompletionsCount

            // run commands if there is any
            if (level.hasCommands()) {
                for (String commandString : level.getCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            commandString.replace("%player%", player.getName()));
                }
            }

            // Update player information
            playerStats.levelCompletion(levelName, levelCompletion);

            // used for playing sound!
            int beforeClanLevel = -1;

            if (rankUpLevel)
                Momentum.getRanksManager().doRankUp(player);
            else
                // only broadcast and give xp/coins if it is not a forced completion
                if (!forcedCompletion) {

                    if (playerStats.getClan() != null) {
                        beforeClanLevel = playerStats.getClan().getLevel();

                        // do clan xp algorithm if they are in clan and level has higher reward than configurable amount
                        if (level.getReward() > Momentum.getSettingsManager().clan_calc_level_reward_needed)
                            Momentum.getClansManager().doClanXPCalc(playerStats.getClan(), player, level);

                        // do clan reward split algorithm if they are in clan and level has higher reward than configurable amount
                        if (level.getReward() > Momentum.getSettingsManager().clan_split_reward_min_needed)
                        {
                            // async for database querying
                            new BukkitRunnable()
                            {
                                @Override
                                public void run()
                                {
                                    Momentum.getClansManager().doSplitClanReward(playerStats.getClan(), player, level);
                                }
                            }.runTaskAsynchronously(Momentum.getPlugin());
                        }
                    }

                    // give higher reward if prestiged
                    int prestiges = playerStats.getPrestiges();
                    int reward = event.getReward(); // get from event
                    // if featured, set reward!
                    if (level.isFeaturedLevel())
                        reward *= Momentum.getSettingsManager().featured_level_reward_multiplier;
                    else if (prestiges > 0 && reward > 0)
                        reward *= playerStats.getPrestigeMultiplier();

                    Momentum.getStatsManager().addCoins(playerStats, reward);

                    String messageFormatted = level.getFormattedMessage(playerStats);
                    if (elapsedTime > 0L && elapsedTime < 8388607L)
                        messageFormatted = messageFormatted.replace("%time%", time);
                    else
                        messageFormatted = messageFormatted.replace("%time%", "-");

                    player.sendMessage(messageFormatted);
                    player.sendMessage(Utils.translate("&7Rate &e" + level.getFormattedTitle() + " &7with &6/rate "
                            + ChatColor.stripColor(level.getFormattedTitle())));

                    // broadcast completed if it the featured level
                    if (level.isFeaturedLevel()) {
                        Bukkit.broadcastMessage(Utils.translate(
                                "&c" + player.getDisplayName() + " &7has completed the &6Featured Level &4" + level.getFormattedTitle()
                        ));
                    } else if (level.getBroadcastCompletion()) {
                        String broadcastMessage = Utils.translate(Momentum.getSettingsManager().levels_message_broadcast);

                        broadcastMessage = broadcastMessage.replace("%player%", player.getDisplayName());
                        broadcastMessage = broadcastMessage.replace("%title%", level.getFormattedTitle());

                        Bukkit.broadcastMessage(broadcastMessage);
                    }

                    if (!playerStats.isGrinding())
                        Momentum.getStatsManager().toggleOffElytra(playerStats);

                    Momentum.getPluginLogger().info(playerStats.getPlayerName() + " beat " + ChatColor.stripColor(level.getFormattedTitle())); // log to console

                    // reset cp and saves before teleport
                    Momentum.getCheckpointManager().deleteCheckpoint(playerStats, level);
                    Momentum.getSavesManager().removeSave(playerStats, level); // safety removal (likely will never actually execute)

                    // clear potion effects
                    playerStats.clearPotionEffects();

                    // send title and sound if not rankup level
                    if (!rankUpLevel) {
                        String titleMessage = Utils.translate("&7You beat " + level.getFormattedTitle());
                        if (elapsedTime > 0L && elapsedTime < 8388607L)
                            titleMessage += Utils.translate("&7 in &2" + time);

                        String subTitleMessage = Utils.translate("&7Rate &e" + level.getFormattedTitle() + " &7with &6/rate "
                                + ChatColor.stripColor(level.getFormattedTitle()));

                        TitleAPI.sendTitle(
                                player, 10, 60, 10,
                                titleMessage,
                                subTitleMessage
                        );

                        // play sound if they did not level up their clan
                        if (!(beforeClanLevel > -1 && beforeClanLevel < playerStats.getClan().getLevel()))
                            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 0f);
                    }

                    Location locationTo = level.getRespawnLocation();

                    // If not rank up level or has a start location and is grinding, set to start loc
                    if (!rankUpLevel && level.getStartLocation() != Momentum.getLocationManager().get("spawn") && playerStats.isGrinding())
                    {
                        locationTo = level.getStartLocation();
                        playerStats.resetFails(); // reset fails in grinding
                    }

                    ProtectedRegion getToRegion = WorldGuard.getRegion(locationTo);
                    Level newLevel = Momentum.getLevelManager().get(getToRegion.getId());

                    // if area they are teleporting to is empty
                    // if not empty, make sure it is a level
                    // if not a level (like spawn), reset level
                    if (getToRegion == null)
                        playerStats.resetLevel();
                    else if (newLevel != null) {
                        playerStats.setLevel(newLevel);

                        // apply potion effects if any exist
                        if (!newLevel.getPotionEffects().isEmpty())
                            for (PotionEffect potionEffect : newLevel.getPotionEffects())
                                player.addPotionEffect(potionEffect);
                    } else
                        playerStats.resetLevel();

                    // teleport
                    player.teleport(locationTo);
                    playerStats.disableLevelStartTime();

                } else {
                    player.sendMessage(Utils.translate("&7You have been given a completion for &c" + level.getFormattedTitle()));

                    if (!level.hasRequiredLevels(playerStats) && !level.getRequiredLevels().isEmpty()) {

                        for (String requiredLevelName : level.getRequiredLevels()) {

                            if (playerStats.getLevelCompletionsCount(requiredLevelName) < 1) {
                                Level requiredLevel = Momentum.getLevelManager().get(requiredLevelName);

                                dolevelCompletion(playerStats, player, requiredLevel, requiredLevelName, false, true);
                            }
                        }
                    }
                }
        }
    }

    // Respawn player if checkpoint isn't there
    public static void respawnPlayer(PlayerStats playerStats, Level level) {
        // make sure the water reset is toggled on
        if (level != null) {
            Location loc = level.getStartLocation();

            if (loc != null)
            {
                playerStats.getPlayer().teleport(loc);
                playerStats.addFail(); // used in multiple areas
            }
        }
    }
}
