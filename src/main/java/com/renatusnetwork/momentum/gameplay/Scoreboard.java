package com.renatusnetwork.momentum.gameplay;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.events.EventManager;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.ranks.Rank;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Time;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class Scoreboard {

    private static int boardWidth = 23;

    public static void startScheduler(Plugin plugin) {
        /*
            we can run the heaviest part when under stress in async,
            the stats display, but have to still run scoreboard creation in sync
         */
        new BukkitRunnable() {
            @Override
            public void run() {
                displayScoreboards();
            }
        }.runTaskTimerAsynchronously(plugin, 20, 2);
    }

    private static String getSpaces(int length) {
        String spaces = "";

        for (int i = 1; i <= length; i++)
            spaces += " ";

        return spaces;
    }

    private static String formatSpacing(String input) {
        int padding = boardWidth - input.length();

        if (padding > 0)
            return getSpaces(padding / 2) + input;

        return input;
    }

    public static void displayScoreboards()
    {
        HashMap<String, PlayerStats> stats = Momentum.getStatsManager().getPlayerStats();

        // ensure thread safety
        synchronized (stats)
        {
            for (PlayerStats playerStats : stats.values())
                displayScoreboard(playerStats);
        }
    }

    private static void displayScoreboard(PlayerStats playerStats) {
        if (playerStats != null && playerStats.getPlayer() != null) {

            List<String> board = new ArrayList<>();
            Level level = playerStats.getLevel();
            EventManager eventManager = Momentum.getEventManager();

            board.add(Utils.translate("&7"));

            String coinBalance = Utils.translate("  &e&lCoins &6" + Utils.formatNumber(playerStats.getCoins()));
            board.add(coinBalance);

            // if they have a rank, show it
            if (playerStats.getRank() != null) {
                String rankString = Utils.translate("  &e&lRank &6" + playerStats.getRank().getShortRankTitle());
                board.add(rankString);
            }

            // if they have a clan, show it
            if (playerStats.getClan() != null) {
                String clanString = Utils.translate("  &e&lClan &6" + playerStats.getClan().getTag());
                board.add(clanString);
            }

            int fails = playerStats.getFails();
            if (!playerStats.isInInfinitePK() && !playerStats.isEventParticipant() && !playerStats.inRace() &&
                !playerStats.isSpectating() && playerStats.inLevel() && !playerStats.getLevel().isAscendanceLevel() &&
                playerStats.inFailMode() && !playerStats.isInTutorial() && fails > 0)
                board.add(Utils.translate("  &e&lFails &6" + fails));

            // spectator section of scoreboard
            if (playerStats.isSpectating()) {

                board.add(Utils.translate("&7"));
                board.add(formatSpacing(Utils.translate("&c&lSpectating &6" + playerStats.getPlayerToSpectate().getPlayerName())));
                board.add(formatSpacing(Utils.translate("&c/spectate &7to exit")));

                // practice section of scoreboard
            } else if (playerStats.inPracticeMode()) {

                board.add(Utils.translate("&7"));
                board.add(formatSpacing(Utils.translate("&6Practice &a&lOn")));
                board.add(formatSpacing(Utils.translate("&c/prac &7to exit")));

                // race section of scoreboard
            } else if (playerStats.inRace()) {

                board.add(Utils.translate("&7"));
                Player opponent = Momentum.getRaceManager().get(playerStats.getPlayer()).getOpponent(playerStats.getPlayer());
                PlayerStats opponentStats = Momentum.getStatsManager().get(opponent);

                board.add(formatSpacing(Utils.translate("&6You are in a race!")));
                board.add(formatSpacing(Utils.translate("&7vs. &c" + opponent.getName())));
                // add timer
                if (playerStats.getLevelStartTime() > 0) {
                    double timeElapsed = System.currentTimeMillis() - playerStats.getLevelStartTime();

                    String timing = Utils.translate("&7" + Math.round((timeElapsed / 1000) * 10) / 10.0) + "s";
                    board.add(formatSpacing(timing));
                }
                board.add(Utils.translate("&7"));

                // add wins, losses, winrate
                board.add(formatSpacing(Utils.translate("&6Your W/L &e" +
                        Utils.shortStyleNumber(playerStats.getRaceWins()) + "/" + Utils.shortStyleNumber(playerStats.getRaceLosses()))));
                board.add(formatSpacing(Utils.translate("&6Their W/L &e" +
                        Utils.shortStyleNumber(opponentStats.getRaceWins()) + "/" + Utils.shortStyleNumber(opponentStats.getRaceLosses()))));


                // event section of scoreboard
            } else if (playerStats.isEventParticipant()) {

                board.add(Utils.translate("&7"));
                board.add(Utils.translate("  &e&lEvent &2&l" + eventManager.formatName(eventManager.getEventType())));
                board.add(Utils.translate("  &e&lPlayers &6" + eventManager.getParticipants().size()));
                board.add(Utils.translate("&7"));
                board.add(formatSpacing(Utils.translate("&6&lTime Left")));
                board.add(formatSpacing(Utils.translate("&7" + Time.elapsedShortened(eventManager.getTimeLeftMillis(), true))));

                // infinite parkour section of scoreboard
            } else if (playerStats.isInInfinitePK()) {

                board.add(Utils.translate("&7"));
                board.add(formatSpacing(Utils.translate("&5Infinite Parkour")));

                // add best if they have one
                String scoreString = "&7Score &d" + Momentum.getInfinitePKManager().get(playerStats.getPlayerName()).getScore();
                if (playerStats.getInfinitePKScore() > 0)
                    scoreString += " &7(&dBest " + playerStats.getInfinitePKScore() + "&7)";

                board.add(formatSpacing(Utils.translate(scoreString)));

                // level section of scoreboard
            } else if (level != null) {

                board.add(Utils.translate("&7"));

                // change the entire scoreboard if it is a rankup level
                if (level.isRankUpLevel()) {
                    Rank rank = playerStats.getRank();

                    // null check their rank to avoid NPE and same with next rank
                    if (rank != null) {
                        Rank nextRank = Momentum.getRanksManager().get(rank.getRankId() + 1);
                        if (nextRank != null) {
                            board.add(formatSpacing(Utils.translate("&cRankup Level")));
                            board.add(formatSpacing(Utils.translate("&7To &a" + nextRank.getRankTitle())));
                        }
                    }
                } else {

                    if (level.isAscendanceLevel()) {

                        // add scoreboard
                        board.add(formatSpacing(Utils.translate("&8&lAscendance")));
                        board.add(formatSpacing(Utils.translate("&7Exploring")));

                        // do time if in a timed level
                        if (playerStats.getLevelStartTime() > 0)
                        {
                            double timeElapsed = System.currentTimeMillis() - playerStats.getLevelStartTime();

                            String timing = Utils.translate("&7" + Math.round((timeElapsed / 1000) * 10) / 10.0) + "s";
                            board.add(formatSpacing(timing));
                        }
                    } else {

                        // normal scoreboard
                        String rewardString;

                        // add title and adjust rewardstring if it is a featured level
                        if (level.isFeaturedLevel()) {
                            board.add(formatSpacing(Utils.translate("&dFeatured Level")));

                            // proper cast
                            rewardString = Utils.translate("&c&m" + Utils.formatNumber(level.getReward()) + "&6 " +
                                    (Utils.formatNumber(level.getReward() * Momentum.getSettingsManager().featured_level_reward_multiplier)));

                        } else if (playerStats.getPrestiges() > 0 && level.getReward() > 0)
                            rewardString = Utils.translate("&c&m" + Utils.formatNumber(level.getReward()) + "&6 " + (Utils.formatNumber(level.getReward() * playerStats.getPrestigeMultiplier())));
                        else
                            rewardString = Utils.translate("&6" + Utils.formatNumber(level.getReward()));

                        String title = level.getFormattedTitle();
                        board.add(formatSpacing(title));
                        board.add(formatSpacing(rewardString));

                        if (playerStats.getLevelStartTime() > 0) {
                            double timeElapsed = System.currentTimeMillis() - playerStats.getLevelStartTime();

                            String timing = Utils.translate("&7" + Math.round((timeElapsed / 1000) * 10) / 10.0) + "s";
                            board.add(formatSpacing(timing));
                        } else {
                            board.add(formatSpacing(Utils.translate("&7-")));
                        }
                    }

                    // grind mode on scoreboard
                    if (playerStats.isGrinding())
                        board.add(formatSpacing(Utils.translate("&aGrinding")));
                }
            }
            // add ip
            board.add(Utils.translate("&7"));
            board.add(formatSpacing(Utils.translate("&6renatus.cc")));

            playerStats.getBoard().updateLines(board); // update board lines
        }
    }
}