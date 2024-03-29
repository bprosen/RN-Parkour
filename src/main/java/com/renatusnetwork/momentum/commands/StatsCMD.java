package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.clans.Clan;
import com.renatusnetwork.momentum.data.events.EventLBPosition;
import com.renatusnetwork.momentum.data.infinite.InfinitePKLBPosition;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.races.RaceLBPosition;
import com.renatusnetwork.momentum.data.stats.*;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (a.length > 0) {
            // infinite pk lb
            if (a.length == 1 && a[0].equalsIgnoreCase("infinite")) {

                if (!Momentum.getInfinitePKManager().getLeaderboard().isEmpty()) {
                    sender.sendMessage(Utils.translate("&5Infinite Parkour &7Leaderboard"));

                    int position = 1;
                    for (InfinitePKLBPosition lbPosition : Momentum.getInfinitePKManager().getLeaderboard().values()) {
                        if (lbPosition != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    position + " &5" +
                                    Utils.formatNumber(lbPosition.getScore()) + " &d" +
                                    lbPosition.getName()));
                        }
                        position++;
                    }

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        PlayerStats playerStats = Momentum.getStatsManager().get(player.getUniqueId().toString());
                        sender.sendMessage(Utils.translate("&7Your best &d" + Utils.formatNumber(playerStats.getInfinitePKScore())));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cInfinite Parkour lb not loaded or no lb positions"));
                }
            } else if (a.length == 1 && a[0].equalsIgnoreCase("levels")) {

                Collection<Level> globalLevelCompletionsLB = Momentum.getLevelManager().getGlobalLevelCompletionsLB().values();

                if (!globalLevelCompletionsLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&4Level Completions &7Leaderboard"));

                    int lbPositionNum = 1;
                    for (Level lbPosition : globalLevelCompletionsLB) {

                        if (lbPosition != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &4" +
                                    Utils.formatNumber(lbPosition.getTotalCompletionsCount()) + " &c" +
                                    lbPosition.getFormattedTitle()));
                            lbPositionNum++;
                        }
                    }
                    sender.sendMessage(Utils.translate("&7Global Completions &c" + Utils.formatNumber(Momentum.getLevelManager().getTotalLevelCompletions())));
                } else {
                    sender.sendMessage(Utils.translate("&cLevels lb not loaded or no lb positions"));
                }
            // players
            } else if (a.length == 1 && a[0].equalsIgnoreCase("players")) {

                HashMap<Integer, GlobalPersonalLBPosition> globalPersonalCompletionsLB = Momentum.getStatsManager().getGlobalPersonalCompletionsLB();

                if (!globalPersonalCompletionsLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&3Player Completions &7Leaderboard"));

                    int lbPositionNum = 1;
                    for (GlobalPersonalLBPosition globalPersonalLBPosition : globalPersonalCompletionsLB.values()) {

                        if (globalPersonalLBPosition != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &3" +
                                    Utils.formatNumber(globalPersonalLBPosition.getCompletions()) + " &b" +
                                    globalPersonalLBPosition.getName()));
                            lbPositionNum++;
                        }
                    }
                    // if player, send personal total
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        sender.sendMessage(Utils.translate("&7Your total &b" + Utils.formatNumber(
                                Momentum.getStatsManager().get(player).getTotalLevelCompletions())));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cPlayers lb not loaded or no lb positions"));
                }
            // clans lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("clans")) {

                HashMap<Integer, Clan> clansLB = Momentum.getClansManager().getLeaderboard();

                if (!clansLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&6Clan Total XP &7Leaderboard"));
                    int lbPositionNum = 1;

                    for (Clan clan : clansLB.values()) {
                        if (clan != null && clan.getOwner() != null && clan.getOwner().getPlayerName() != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &6" +
                                    Utils.shortStyleNumber(clan.getTotalGainedXP()) + " &e" +
                                    clan.getTag() + " &6(" + clan.getOwner().getPlayerName() + ")"));
                            lbPositionNum++;
                        }
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cClans lb not loaded or no lb positions"));
                }
            // top rated lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("toprated")) {

                Collection<Level> topRatedLB = Momentum.getLevelManager().getTopRatedLevelsLB().values();

                if (!topRatedLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&9Rated Levels &7Leaderboard"));

                    int lbPositionNum = 1;
                    for (Level level : topRatedLB) {

                        if (level != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &9" +
                                    level.getRating() + " &1" +
                                    level.getFormattedTitle()));
                            lbPositionNum++;
                        }
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cTop Rated lb not loaded or no lb positions"));
                }
            // race lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("races")) {

                HashMap<Integer, RaceLBPosition> leaderboard = Momentum.getRaceManager().getLeaderboard();

                if (!leaderboard.isEmpty()) {

                    sender.sendMessage(Utils.translate("&8Race Wins &7Leaderboard"));

                    int position = 1;
                    for (RaceLBPosition lbPosition : leaderboard.values()) {
                        if (lbPosition != null) {

                            sender.sendMessage(Utils.translate(" &7" +
                                    position + " &8" +
                                    lbPosition.getWins() + " &7" +
                                    lbPosition.getName() + " &8(" +
                                    lbPosition.getWinRate() + ")"));
                        }
                        position++;
                    }

                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        PlayerStats playerStats = Momentum.getStatsManager().get(player.getUniqueId().toString());
                        sender.sendMessage(Utils.translate("&7Your Wins/Win Rate &8" +
                                Utils.formatNumber(playerStats.getRaceWins()) + "&7/&8" +
                                playerStats.getRaceWinRate()));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cRace lb not loaded or no lb positions"));
                }
            }
            else if (a.length == 1 && a[0].equalsIgnoreCase("events"))
            {

                HashMap<Integer, EventLBPosition> leaderboard = Momentum.getEventManager().getEventLeaderboard();

                if (!leaderboard.isEmpty()) {

                    sender.sendMessage(Utils.translate("&bEvent Wins &7Leaderboard"));

                    int position = 1;
                    for (EventLBPosition lbPosition : leaderboard.values())
                    {
                        if (lbPosition != null)
                        {
                            sender.sendMessage(Utils.translate(" &7" +
                                    position + " &9" +
                                    Utils.formatNumber(lbPosition.getWins()) + " &b" +
                                    lbPosition.getName()));
                        }
                        position++;
                    }

                    if (sender instanceof Player)
                    {
                        Player player = (Player) sender;
                        PlayerStats playerStats = Momentum.getStatsManager().get(player.getUniqueId().toString());

                        sender.sendMessage(Utils.translate("&7Your wins &b" + Utils.formatNumber(playerStats.getEventWins())));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cEvent lb not loaded or no lb positions"));
                }
                // level lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("coins")) {
                printCoinsLB(sender);
            } else if (a.length == 1 && a[0].equalsIgnoreCase("records")) {
                printRecordsLB(sender);
            } else {

                // allow ability to get from title or name
                String levelName = a[0].toLowerCase();
                if (a.length >= 1) {
                    String[] split = Arrays.copyOfRange(a, 0, a.length);
                    levelName = String.join(" ", split);
                }

                // if it does not get it from name, then attempt to get it from title
                Level level = Momentum.getLevelManager().get(levelName);
                if (level == null)
                    level = Momentum.getLevelManager().getFromTitle(levelName);

                // then check if it is still null
                if (level != null)
                {
                    if (!Momentum.getStatsManager().isLoadingLeaderboards())
                    {
                        sender.sendMessage(Utils.translate(
                                level.getFormattedTitle() + " &7Leaderboard &a(" + Utils.shortStyleNumber(level.getTotalCompletionsCount()) + ")"
                        ));

                        List<LevelCompletion> completions = level.getLeaderboard();
                        boolean onLB = false;

                        if (completions.size() > 0)
                            for (int i = 0; i <= completions.size() - 1; i++)
                            {
                                LevelCompletion levelCompletion = completions.get(i);
                                int rank = i + 1;
                                String lbName = levelCompletion.getPlayerName();

                                String lbString = " &7" + rank;

                                if (!onLB && sender instanceof Player && sender.getName().equalsIgnoreCase(lbName))
                                {
                                    // we want to show it as blue if they are on it
                                    onLB = true;
                                    lbString += " &3" + (((double) levelCompletion.getCompletionTimeElapsed()) / 1000) + "s &b" + levelCompletion.getPlayerName();
                                }
                                else
                                    lbString += " &2" + (((double) levelCompletion.getCompletionTimeElapsed()) / 1000) + "s &a" + levelCompletion.getPlayerName();

                                sender.sendMessage(Utils.translate(lbString));
                            }
                        else
                            sender.sendMessage(Utils.translate("&cNo timed completions to display"));

                        if (!onLB && sender instanceof Player)
                        {
                            Player player = (Player) sender;
                            PlayerStats playerStats = Momentum.getStatsManager().get(player);

                            if (playerStats != null && playerStats.getLevelCompletionsCount(level.getName()) > 0)
                            {
                                // send your best if not on it and have beaten it
                                LevelCompletion levelCompletion = playerStats.getQuickestCompletion(level.getName());
                                if (levelCompletion != null)
                                {
                                    sender.sendMessage(Utils.translate("&7Your best is &2" +
                                            (((double) levelCompletion.getCompletionTimeElapsed()) / 1000) + "s"));
                                }
                            }
                        }
                    }
                    else
                    {
                        sender.sendMessage(Utils.translate("&cLeaderboards are still loading... check back soon"));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&7No level named '&c" + levelName + "&7' exists"));
                }
            }
        } else {
            sender.sendMessage(Utils.translate("&6/stats <levelName>  &7Gets level's Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats infinite  &7Gets Infinite Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats races  &7Gets Races Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats toprated  &7Gets Top Rated Levels Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats clans  &7Gets Clan XP Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats players  &7Gets Players Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats levels  &7Gets Levels Leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats coins  &7Gets coins leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats records  &7Gets records leaderboard"));
            sender.sendMessage(Utils.translate("&6/stats events  &7Gets events leaderboard"));
        }
        return true;
    }

    public static void printCoinsLB(CommandSender sender)
    {
        HashMap<Integer, CoinsLBPosition> coinsLB = Momentum.getStatsManager().getCoinsLB();

        if (!coinsLB.isEmpty()) {

            sender.sendMessage(Utils.translate("&eCoins &7Leaderboard"));

            int lbPositionNum = 1;
            for (CoinsLBPosition coinsLBPosition : coinsLB.values()) {

                if (coinsLBPosition != null) {
                    sender.sendMessage(Utils.translate(" &7" +
                            lbPositionNum + " &6" +
                            Utils.formatNumber(coinsLBPosition.getCoins()) + " &7" +
                            coinsLBPosition.getName()));
                    lbPositionNum++;
                }
            }

            // if player, send personal total
            if (sender instanceof Player) {
                Player player = (Player) sender;
                sender.sendMessage(Utils.translate("&7You have &6" + Utils.formatNumber(
                        Momentum.getStatsManager().get(player).getCoins()) + " &e&lCoins"));
            }
        } else {
            sender.sendMessage(Utils.translate("&cCoins lb not loaded or no lb positions"));
        }
    }

    public static void printRecordsLB(CommandSender sender)
    {
        HashMap<Integer, RecordsLBPosition> recordsLB = Momentum.getStatsManager().getRecordsLB();

        if (!recordsLB.isEmpty()) {

            sender.sendMessage(Utils.translate("&9Records &7Leaderboard"));

            int lbPositionNum = 1;
            for (RecordsLBPosition recordsLBPosition : recordsLB.values()) {

                if (recordsLBPosition != null) {
                    sender.sendMessage(Utils.translate(" &7" +
                            lbPositionNum + " &9" +
                            Utils.formatNumber(recordsLBPosition.getRecords()) + " &3" +
                            recordsLBPosition.getName()));
                    lbPositionNum++;
                }
            }

            // if player, send personal total
            if (sender instanceof Player) {
                Player player = (Player) sender;
                sender.sendMessage(Utils.translate("&7You have &e✦ " + Utils.formatNumber(
                        Momentum.getStatsManager().get(player).getRecords()) + " &7Records"));
            }
        } else {
            sender.sendMessage(Utils.translate("&cRecords lb not loaded or no lb positions"));
        }
    }
}