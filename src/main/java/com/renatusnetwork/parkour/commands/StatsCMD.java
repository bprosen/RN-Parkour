package com.renatusnetwork.parkour.commands;

import com.renatusnetwork.parkour.Parkour;
import com.renatusnetwork.parkour.data.clans.Clan;
import com.renatusnetwork.parkour.data.leaderboards.EventLBPosition;
import com.renatusnetwork.parkour.data.leaderboards.InfiniteLBPosition;
import com.renatusnetwork.parkour.data.infinite.gamemode.InfiniteType;
import com.renatusnetwork.parkour.data.levels.Level;
import com.renatusnetwork.parkour.data.leaderboards.RaceLBPosition;
import com.renatusnetwork.parkour.data.levels.LevelCompletion;
import com.renatusnetwork.parkour.data.stats.*;
import com.renatusnetwork.parkour.data.leaderboards.CoinsLBPosition;
import com.renatusnetwork.parkour.data.leaderboards.GlobalPersonalLBPosition;
import com.renatusnetwork.parkour.data.leaderboards.RecordsLBPosition;
import com.renatusnetwork.parkour.utils.Utils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class StatsCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (a.length > 0) {
            // infinite pk lb
            if (a.length == 2 && a[0].equalsIgnoreCase("infinite"))
            {
                String typeString = a[1];
                boolean correct = false;

                // verify type
                for (InfiniteType type : InfiniteType.values())
                    if (type.toString().equalsIgnoreCase(typeString))
                    {
                        correct = true;
                        break;
                    }

                if (correct)
                {
                    InfiniteType type = InfiniteType.valueOf(typeString.toUpperCase());

                    if (!Parkour.getInfiniteManager().getLeaderboard(type).isEmpty())
                    {
                        sender.sendMessage(Utils.translate("&d" + StringUtils.capitalize(typeString.toLowerCase()) + " &5Infinite &7Leaderboard"));

                        int position = 1;
                        for (InfiniteLBPosition lbPosition : Parkour.getInfiniteManager().getLeaderboard(type).getLeaderboardPositions())
                        {
                            if (lbPosition != null)
                            {
                                sender.sendMessage(Utils.translate(" &7" +
                                        position + " &5" +
                                        Utils.formatNumber(lbPosition.getScore()) + " &d" +
                                        lbPosition.getName()));
                            }
                            position++;
                        }

                        if (sender instanceof Player)
                        {
                            Player player = (Player) sender;
                            PlayerStats playerStats = Parkour.getStatsManager().get(player.getUniqueId().toString());
                            sender.sendMessage(Utils.translate("&7Your best &d" + Utils.formatNumber(playerStats.getBestInfiniteScore(type))));
                        }
                    }
                    else
                        sender.sendMessage(Utils.translate("&c" + StringUtils.capitalize(typeString.toLowerCase()) + " Infinite Parkour lb not loaded or no lb positions"));
                }
                else
                {
                    sender.sendMessage(Utils.translate("&4'" + typeString + "' &cis not a infinite type!"));
                }
            } else if (a.length == 1 && a[0].equalsIgnoreCase("levels")) {

                Collection<Level> globalLevelCompletionsLB = Parkour.getLevelManager().getGlobalLevelCompletionsLB().values();

                if (!globalLevelCompletionsLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&4Level Completions &7Leaderboard"));

                    int lbPositionNum = 1;
                    for (Level lbPosition : globalLevelCompletionsLB) {

                        if (lbPosition != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &4" +
                                    Utils.formatNumber(lbPosition.getTotalCompletionsCount()) + " &c" +
                                    lbPosition.getTitle()));
                            lbPositionNum++;
                        }
                    }
                    sender.sendMessage(Utils.translate("&7Global Completions &c" + Utils.formatNumber(Parkour.getLevelManager().getTotalLevelCompletions())));
                } else {
                    sender.sendMessage(Utils.translate("&cLevels lb not loaded or no lb positions"));
                }
            // players
            } else if (a.length == 1 && a[0].equalsIgnoreCase("players")) {

                HashMap<Integer, GlobalPersonalLBPosition> globalPersonalCompletionsLB = Parkour.getStatsManager().getGlobalPersonalCompletionsLB();

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
                                Parkour.getStatsManager().get(player).getTotalLevelCompletions())));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cPlayers lb not loaded or no lb positions"));
                }
            // clans lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("clans")) {

                HashMap<Integer, Clan> clansLB = Parkour.getClansManager().getLeaderboard();

                if (!clansLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&6Clan Total XP &7Leaderboard"));
                    int lbPositionNum = 1;

                    for (Clan clan : clansLB.values()) {
                        if (clan != null && clan.getOwner() != null && clan.getOwner().getName() != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &6" +
                                    Utils.shortStyleNumber(clan.getTotalXP()) + " &e" +
                                    clan.getTag() + " &6(" + clan.getOwner().getName() + ")"));
                            lbPositionNum++;
                        }
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cClans lb not loaded or no lb positions"));
                }
            // top rated lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("toprated")) {

                Collection<Level> topRatedLB = Parkour.getLevelManager().getTopRatedLevelsLB().values();

                if (!topRatedLB.isEmpty()) {

                    sender.sendMessage(Utils.translate("&9Rated Levels &7Leaderboard"));

                    int lbPositionNum = 1;
                    for (Level level : topRatedLB) {

                        if (level != null) {
                            sender.sendMessage(Utils.translate(" &7" +
                                    lbPositionNum + " &9" +
                                    level.getRating() + " &1" +
                                    level.getTitle()));
                            lbPositionNum++;
                        }
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cTop Rated lb not loaded or no lb positions"));
                }
            // race lb
            } else if (a.length == 1 && a[0].equalsIgnoreCase("races")) {

                HashMap<Integer, RaceLBPosition> leaderboard = Parkour.getRaceManager().getLeaderboard();

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
                        PlayerStats playerStats = Parkour.getStatsManager().get(player.getUniqueId().toString());
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

                HashMap<Integer, EventLBPosition> leaderboard = Parkour.getEventManager().getEventLeaderboard();

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
                        PlayerStats playerStats = Parkour.getStatsManager().get(player.getUniqueId().toString());

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
                if (a.length >= 1)
                {
                    String[] split = Arrays.copyOfRange(a, 0, a.length);
                    levelName = String.join(" ", split);
                }

                // if it does not get it from name, then attempt to get it from title
                Level level = Parkour.getLevelManager().getNameThenTitle(levelName);

                // then check if it is still null
                if (level != null)
                {
                    if (!Parkour.getLevelManager().isLoadingLeaderboards())
                    {
                        sender.sendMessage(Utils.translate(
                                level.getTitle() + "&7 Leaderboard &a(" + Utils.shortStyleNumber(level.getTotalCompletionsCount()) + ")"
                        ));

                        List<LevelCompletion> leaderboard = level.getLeaderboard();
                        boolean onLB = false;

                        if (!leaderboard.isEmpty())
                            for (int i = 0; i < leaderboard.size(); i++)
                            {
                                LevelCompletion levelCompletion = leaderboard.get(i);
                                String lbName = levelCompletion.getName();
                                double time = levelCompletion.getCompletionTimeElapsedSeconds();
                                String lbString = " &7" + (i + 1);

                                if (!onLB && sender instanceof Player && sender.getName().equalsIgnoreCase(levelCompletion.getName()))
                                {
                                    // we want to show it as blue if they are on it
                                    onLB = true;
                                    lbString += " &3" + time + "s &b" + lbName;
                                }
                                else
                                    lbString += " &2" + time + "s &a" + lbName;

                                sender.sendMessage(Utils.translate(lbString));
                            }
                        else
                            sender.sendMessage(Utils.translate("&cNo timed completions to display"));

                        if (!onLB && sender instanceof Player)
                        {
                            Player player = (Player) sender;
                            PlayerStats playerStats = Parkour.getStatsManager().get(player);

                            if (playerStats != null && playerStats.hasCompleted(level))
                            {
                                // send your best if not on it and have beaten it
                                LevelCompletion levelCompletion = playerStats.getQuickestCompletion(level);
                                if (levelCompletion != null)
                                {
                                    sender.sendMessage(Utils.translate("&7Your best is &2" + levelCompletion.getCompletionTimeElapsedSeconds() + "s"));
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
            sender.sendMessage(Utils.translate("&6/stats infinite <type>  &7Gets Infinite Type's Leaderboard"));
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
        HashMap<Integer, CoinsLBPosition> coinsLB = Parkour.getStatsManager().getCoinsLB();

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
                        Parkour.getStatsManager().get(player).getCoins()) + " &eCoins"));
            }
        } else {
            sender.sendMessage(Utils.translate("&cCoins lb not loaded or no lb positions"));
        }
    }

    public static void printRecordsLB(CommandSender sender)
    {
        HashMap<Integer, RecordsLBPosition> recordsLB = Parkour.getLevelManager().getRecordsLB();

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
            if (sender instanceof Player)
            {
                Player player = (Player) sender;
                sender.sendMessage(Utils.translate("&7You have &e✦ " + Utils.formatNumber(
                        Parkour.getStatsManager().get(player).getNumRecords()) + " &7Records"));
            }
        } else {
            sender.sendMessage(Utils.translate("&cRecords lb not loaded or no lb positions"));
        }
    }
}