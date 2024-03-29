package com.renatusnetwork.momentum.data.infinite;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.api.InfiniteEndEvent;
import com.renatusnetwork.momentum.data.SettingsManager;
import com.renatusnetwork.momentum.data.locations.LocationManager;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.storage.mysql.DatabaseQueries;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class InfinitePKManager {

    private HashMap<String, InfinitePK> participants = new HashMap<>();
    private HashMap<Integer, InfinitePKLBPosition> leaderboard = new HashMap<>(Momentum.getSettingsManager().max_infinitepk_leaderboard_size);
    private LinkedHashMap<Integer, InfinitePKReward> rewards = new LinkedHashMap<>(); // linked so order stays

    public InfinitePKManager() {
        startScheduler();
    }

    public void startScheduler() {

        new BukkitRunnable() {
            @Override
            public void run() {
                loadLeaderboard();
                InfiniteRewardsYAML.loadRewards();
            }
        }.runTaskAsynchronously(Momentum.getPlugin());
    }

    public void startPK(PlayerStats playerStats, boolean fromPortal) {

        /*
            We force run this in sync for a few reasons:
            1. The packet listener executes this in async which cant be done as this creates blocks.
            2. Someone with bad ping can sometimes make this method run many times causing glitched blocks in the air
               if they have bad ping.
         */
        new BukkitRunnable() {
            @Override
            public void run() {

                // if from portal, prevent getting in via spectator
                if (playerStats.isInInfinitePK() || (fromPortal && playerStats.isSpectating()))
                    return;

                Player player = playerStats.getPlayer();

                // find loc
                Location startingLoc = findStartingLocation();
                startingLoc.setPitch(Momentum.getSettingsManager().infinitepk_starting_pitch);
                startingLoc.setYaw(Momentum.getSettingsManager().infinitepk_starting_yaw);

                // create cache
                InfinitePK infinitePK = new InfinitePK(player);
                participants.put(player.getName(), infinitePK);

                infinitePK.setCurrentBlockLoc(startingLoc);

                Location respawnLoc = Momentum.getSettingsManager().infinitepk_portal_respawn;

                // if they are at spawn prior to teleport, change original loc to setting
                if (fromPortal && respawnLoc != null)
                    infinitePK.setOriginalLoc(respawnLoc);

                // prepare block and teleport
                startingLoc.getBlock().setType(playerStats.getInfiniteBlock());
                playerStats.clearPotionEffects();
                player.teleport(startingLoc.clone().add(0.5, 1, 0.5));
                // set to true
                playerStats.setInfinitePK(true);

                // immediately get new loc
                doNextJump(playerStats, true);

            }
        }.runTask(Momentum.getPlugin());
    }

    public void endPK(Player player, boolean disconnected)
    {

        InfinitePK infinitePK = get(player.getName());
        if (infinitePK != null)
        {
            InfiniteEndEvent event = new InfiniteEndEvent(player, infinitePK.getScore());
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled())
            {
                // run in sync because of packet listener running in async, need to remove blocks in sync
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // tp in sync
                        player.teleport(infinitePK.getOriginalLoc());
                        // clear blocks and reset data
                        infinitePK.getLastBlockLoc().getBlock().setType(Material.AIR);
                        infinitePK.getPressutePlateLoc().getBlock().setType(Material.AIR);
                        infinitePK.getCurrentBlockLoc().getBlock().setType(Material.AIR);
                    }
                }.runTask(Momentum.getPlugin());

                int score = infinitePK.getScore();
                PlayerStats playerStats = Momentum.getStatsManager().get(player);

                boolean doRewardsMsg = false;
                List<InfinitePKReward> rewards = null;

                if (score > playerStats.getInfinitePKScore())
                {
                    rewards = getApplicableRewards(playerStats.getInfinitePKScore(), score);

                    // dispatch command if not null and they havent gotten this reward yet
                    if (!rewards.isEmpty()) {
                        doRewardsMsg = true;

                        // run in sync for safety
                        for (InfinitePKReward reward : rewards) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    // loop through and run commands of applicable rewards
                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.getCommand().replace("%player%", player.getName()));
                                }
                            }.runTask(Momentum.getPlugin());
                        }
                    }
                }

                if (isBestScore(player.getName(), score))
                {
                    // if they disconnected
                    if (!disconnected) {
                        player.sendMessage(Utils.translate(
                                "&7You have beaten your previous record of &d" +
                                        Utils.formatNumber(playerStats.getInfinitePKScore()) + " &7with &d" + Utils.formatNumber(score) + "\n" +
                                        "&7Awarded &6" + Utils.formatNumber(event.getReward()) + " &eCoins"
                        ));

                        if (doRewardsMsg) {
                            // we can safely assume since it will only be true if its not empty
                            for (InfinitePKReward reward : rewards)
                                player.sendMessage(Utils.translate(
                                        " &7You received &d" + reward.getName() + " &d(Score of " + Utils.formatNumber(reward.getScoreNeeded()) + ")"));
                        }
                    }

                    updateScore(player.getName(), score);

                    // load leaderboard if they have a lb position
                    if (scoreWillBeLB(score) || leaderboard.size() < Momentum.getSettingsManager().max_infinitepk_leaderboard_size) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                loadLeaderboard();
                            }
                        }.runTaskAsynchronously(Momentum.getPlugin());
                    }
                } else if (!disconnected) {
                    player.sendMessage(Utils.translate(
                            "&7You failed at &d" + Utils.formatNumber(score) + " &5(Best is " + Utils.formatNumber(playerStats.getInfinitePKScore()) + ")\n" +
                                    "&7Awarded &6" + Utils.formatNumber(event.getReward()) + " &eCoins"
                    ));
                }
                // deposit reward from listener (default = score)
                Momentum.getStatsManager().addCoins(playerStats, event.getReward());

                playerStats.setInfinitePK(false);
                participants.remove(player.getName());
            }
        }
    }

    public LinkedHashMap<Integer, InfinitePKReward> getRewards() { return rewards; }

    public InfinitePKReward getReward(int score) { return rewards.get(score); }

    public void addReward(InfinitePKReward infinitePKReward) { rewards.put(infinitePKReward.getScoreNeeded(), infinitePKReward); }

    public void clearRewards() { rewards.clear(); }

    public List<InfinitePKReward> getApplicableRewards(int oldBestScore, int newBestScore) {
        List<InfinitePKReward> tempRewards = new ArrayList<>();

        for (Map.Entry<Integer, InfinitePKReward> entry : rewards.entrySet()) {
            int score = entry.getKey();
            // if score is greater than old and less than or = to new, then add
            if (score > oldBestScore && score <= newBestScore)
                tempRewards.add(entry.getValue());
        }
        return tempRewards;
    }

    public InfinitePK get(String playerName) {
        return participants.get(playerName);
    }

    public void remove(String playerName) {
        participants.remove(playerName);
    }

    public boolean isBestScore(String playerName, int score) {

        boolean isBest = false;

        // it will first check if they have a score (exist), then it will check if their gotten score is better
        if (InfinitePKDB.hasScore(playerName)) {
            int currentBestScore = InfinitePKDB.getScoreFromName(playerName);

            if (score > currentBestScore)
                isBest = true;
        }
        return isBest;
    }

    // method to update their score in all 3 possible placed
    public void updateScore(String playerName, int score) {
        PlayerStats playerStats = Momentum.getStatsManager().getByName(playerName);

        if (playerStats != null)
            playerStats.setInfinitePKScore(score);

        if (isLBPosition(playerName))
            getLeaderboardPosition(playerName).setScore(score);

        Momentum.getDatabaseManager().runQuery(
                "UPDATE players SET infinitepk_score=? WHERE player_name=?",
                score, playerName
        );
    }

    public void doNextJump(PlayerStats playerStats, boolean startingJump)
    {
        Player player = playerStats.getPlayer();
        InfinitePK infinitePK = get(player.getName());

        if (infinitePK != null) {

            // go back if in any current blocks
            Location newLocation = findNextBlockSpawn(infinitePK.getCurrentBlockLoc(), infinitePK);
            if (isLocInCurrentBlocks(newLocation))
                doNextJump(playerStats, startingJump);
            else
            {
                // so they dont start at score of 1
                if (!startingJump)
                    infinitePK.addScore();

                // remove old loc and update to new loc if not null, otherwise it is starting location, just remove current block
                if (infinitePK.getLastBlockLoc() != null) {
                    infinitePK.getLastBlockLoc().clone().add(0, 1, 0).getBlock().setType(Material.AIR);
                    infinitePK.getLastBlockLoc().getBlock().setType(Material.AIR);
                }
                else if (!startingJump)
                    infinitePK.getCurrentBlockLoc().getBlock().setType(Material.AIR);

                infinitePK.updateBlockLoc(newLocation);

                // then set new
                infinitePK.getCurrentBlockLoc().getBlock().setType(playerStats.getInfiniteBlock());
                infinitePK.getPressutePlateLoc().getBlock().setType(Material.IRON_PLATE);

                if (!startingJump)
                    newLocation.getWorld().spawnParticle(Particle.CLOUD, newLocation.getX(), newLocation.getY(), newLocation.getZ(), 15);
            }
        }
    }

    public Location findNextBlockSpawn(Location oldLocation, InfinitePK infinitePK) {

        LocationManager locationManager = Momentum.getLocationManager();
        SettingsManager settingsManager = Momentum.getSettingsManager();
        InfinitePKDirection directionType = infinitePK.getDirectionType();

        int xMin = 2, zMin = 2, yMin = -1;
        int xMax = 5, zMax = 5, yMax = 2;

        /*
         flip max/min if backwards/forwards and it is not already in negatives/positives (simple < check)
         xMin becomes xMax, etc
         */
        if (directionType == InfinitePKDirection.BACKWARDS && xMin > 0) {
            // make a copy so we do not access already changed values (xMin and zMin)
            int copyMinX = xMin;
            int copyMinZ = zMin;

            xMin = xMax * -1;
            xMax = copyMinX * -1;
            zMin = zMax * -1;
            zMax = copyMinZ * -1;
        }

        // get random increase
        int zIncrease = ThreadLocalRandom.current().nextInt(zMin, zMax);
        int xIncrease = ThreadLocalRandom.current().nextInt(xMin, xMax);

        // since random does not go to bound (6 becomes 5, need to make these real variables to mimic depending on direction)
        int realMaxX, realMaxZ;
        if (directionType == InfinitePKDirection.FORWARDS) {
            realMaxX = xMax++;
            realMaxZ = zMax++;
        } else {
            realMaxX = xMin--;
            realMaxZ = zMin--;
        }

        /*
            below is a section that handles certain cases of impossible jumps, like 4 + 1, 4 by 4, etc
         */
        if (xIncrease == realMaxX) {
            // remove 1 from y
            if (yMax > 1)
                yMax--;

            // adjust z in special case of both being max
            if (zIncrease != realMaxZ) {
                if (directionType == InfinitePKDirection.BACKWARDS && zIncrease < -2)
                    zIncrease += 2;
                else if (zIncrease > 2)
                    zIncrease -= 2;
            } else {
                if (directionType == InfinitePKDirection.BACKWARDS)
                    zIncrease += 3;
                else
                    zIncrease -= 3;
            }
        } else if (zIncrease == realMaxZ) {
            // remove 1 from y
            if (yMax > 1)
                yMax--;

            // adjust x in special case of both being max
            if (xIncrease != realMaxX) {
                if (directionType == InfinitePKDirection.BACKWARDS && xIncrease < -2)
                    xIncrease += 2;
                else if (xIncrease > 2)
                    xIncrease -= 2;
            } else {
                if (directionType == InfinitePKDirection.BACKWARDS)
                    zIncrease += 3;
                else
                    zIncrease -= 3;
            }

        }

        // if too high, reduce max by 1 if it is already above 0
        if (oldLocation.getBlockY() > Momentum.getSettingsManager().max_infinitepk_y && yMax > 1)
            yMax--;

        // if they are going to be below the min, set to 0 instead
        if (yMin < 0 && (oldLocation.getBlockY() - yMin <= Momentum.getSettingsManager().min_infinitepk_y))
            yMin = 0;

        int yIncrease = ThreadLocalRandom.current().nextInt(yMin, yMax);

        // if the block will be 1 above, need to remove/add 1 from max/min depending on direction if x and z increase is 3
        if (yIncrease > 0) {
            if (directionType == InfinitePKDirection.BACKWARDS) {
                if (xIncrease < -2 && zIncrease < -2) {
                    xIncrease++;
                    zIncrease++;
                }
            } else if (xIncrease > 2 && zIncrease > 2) {
                xIncrease--;
                zIncrease--;
            }
        }

        // run through maxes, and flip if they hit it
        if ((directionType == InfinitePKDirection.FORWARDS &&
           (((oldLocation.getX() + xIncrease) >= (locationManager.getInfiniteMiddle().getBlockX() + settingsManager.max_infinitepk_x)) ||
           (oldLocation.getZ() + zIncrease) >= (locationManager.getInfiniteMiddle().getBlockZ() + settingsManager.max_infinitepk_z)))
           || (directionType == InfinitePKDirection.BACKWARDS &&
           (((oldLocation.getX() - xIncrease) <= (locationManager.getInfiniteMiddle().getBlockX() - settingsManager.max_infinitepk_x)) ||
           (oldLocation.getZ() - zIncrease) <= (locationManager.getInfiniteMiddle().getBlockZ() - settingsManager.max_infinitepk_z)))) {

            xIncrease *= -1;
            zIncrease *= -1;

            // flip enum
            infinitePK.flipDirectionType();
        }

        Location newLocation = new Location(oldLocation.getWorld(),
                                         oldLocation.getX() + xIncrease,
                                         oldLocation.getY() + yIncrease,
                                         oldLocation.getZ() + zIncrease);

        return newLocation;
    }

    public Location findStartingLocation() {

        SettingsManager settingsManager = Momentum.getSettingsManager();
        LocationManager locationManager = Momentum.getLocationManager();

        // make the box 6 less so it will never flip on the first one
        int minX = locationManager.getInfiniteMiddle().getBlockX() - (settingsManager.max_infinitepk_x - 6);
        int maxX = locationManager.getInfiniteMiddle().getBlockX() + (settingsManager.max_infinitepk_x - 6);
        int minZ = locationManager.getInfiniteMiddle().getBlockZ() - (settingsManager.max_infinitepk_z - 6);
        int maxZ = locationManager.getInfiniteMiddle().getBlockZ() + (settingsManager.max_infinitepk_z - 6);

        int foundX = ThreadLocalRandom.current().nextInt(minX, maxX + 1);
        int foundZ = ThreadLocalRandom.current().nextInt(minZ, maxZ + 1);

        Location foundLoc = new Location(
                locationManager.getInfiniteMiddle().getWorld(),
                foundX, settingsManager.infinitepk_starting_y, foundZ
        );

        // check if the location will be in the way, if so, go again
        if (isLocInCurrentBlocks(foundLoc))
            findStartingLocation();
        else
            return foundLoc;

        return null;
    }

    public void loadLeaderboard() {
        try {

            HashMap<Integer, InfinitePKLBPosition> leaderboard = getLeaderboard();
            leaderboard.clear();

            List<Map<String, String>> scoreResults = DatabaseQueries.getResults(
                    "players",
                    "uuid, player_name, infinitepk_score",
                    " WHERE infinitepk_score > 0" +
                            " ORDER BY infinitepk_score DESC" +
                            " LIMIT " + Momentum.getSettingsManager().max_infinitepk_leaderboard_size);

            int lbPos = 1;
            for (Map<String, String> scoreResult : scoreResults) {
                leaderboard.put(lbPos,
                        new InfinitePKLBPosition(
                                scoreResult.get("uuid"),
                                scoreResult.get("player_name"),
                                Integer.parseInt(scoreResult.get("infinitepk_score")))
                );
                lbPos++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLocInCurrentBlocks(Location loc) {
        for (InfinitePK infinitePK : participants.values())
            if ((infinitePK.getCurrentBlockLoc().getBlockX() == loc.getBlockX() &&
                infinitePK.getCurrentBlockLoc().getBlockZ() == loc.getBlockZ()) ||
                loc.getBlock().getType() != Material.AIR)
                return true;
        return false;
    }

    public InfinitePKLBPosition getLeaderboardPosition(String playerName) {
        for (InfinitePKLBPosition infinitePKLBPosition : leaderboard.values())
            if (infinitePKLBPosition.getName().equalsIgnoreCase(playerName))
                return infinitePKLBPosition;

        return null;
    }

    public boolean isLBPosition(String playerName) {
        for (InfinitePKLBPosition infinitePKLBPosition : leaderboard.values())
            if (infinitePKLBPosition.getName().equalsIgnoreCase(playerName))
                return true;

        return false;
    }

    public boolean scoreWillBeLB(int score) {
        int lowestScore = 0;
        // gets lowest score
        for (InfinitePKLBPosition infinitePKLBPosition : leaderboard.values())
            if (lowestScore == 0 || infinitePKLBPosition.getScore() < lowestScore)
                lowestScore = infinitePKLBPosition.getScore();

        if (lowestScore <= score)
            return true;
        return false;
    }

    public void shutdown() {
        for (InfinitePK infinitePK : participants.values())
            endPK(infinitePK.getPlayer(), true);
    }

    public HashMap<Integer, InfinitePKLBPosition> getLeaderboard() {
        return leaderboard;
    }

    public HashMap<String, InfinitePK> getParticipants() {
        return participants;
    }
}
