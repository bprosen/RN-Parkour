package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.clans.*;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.UUID;

public class ClanCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (a.length > 0) {
            if (a[0].equalsIgnoreCase("stats")) {

                Clan targetClan;

                // this is what allows "/clan stats" to do self check
                if (a.length == 1) {
                    // make sure it is a player, not console
                    if (sender instanceof Player) {

                        Player player = (Player) sender;
                        Clan selfClan = Momentum.getStatsManager().get(player).getClan();
                        // check if they are in a clan
                        if (selfClan != null)
                            targetClan = selfClan;
                        else {
                            player.sendMessage(Utils.translate("&cYou are not in a clan!"));
                            return true;
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cConsole cannot execute this"));
                        return true;
                    }
                } else {
                    targetClan = Momentum.getClansManager().getIgnoreCase(a[1]);
                }

                if (targetClan != null) {

                    // send stats
                    sender.sendMessage(Utils.translate("&6&l" + targetClan.getTag() + "&e's Stats"));
                    sender.sendMessage(Utils.translate("  &cClan Level &4" + targetClan.getLevel()));
                    sender.sendMessage(Utils.translate("  &cTotal Clan XP &4" + Utils.shortStyleNumber(targetClan.getTotalGainedXP())));

                    // if max level, dont send needed to level up
                    if (!targetClan.isMaxLevel()) {
                        long clanXPNeeded = ClansYAML.getLevelUpPrice(targetClan) - targetClan.getXP();

                        sender.sendMessage(Utils.translate("  &cClan XP for Level &4" + Utils.formatNumber(targetClan.getXP())));
                        sender.sendMessage(Utils.translate("  &cXP to Level Up"));
                        sender.sendMessage(Utils.translate("    &4" + Utils.formatNumber(clanXPNeeded)));
                    }

                    sender.sendMessage("");
                    sender.sendMessage(Utils.translate( "&6Members &e" + targetClan.getMembers().size()));

                    for (ClanMember clanMember : targetClan.getMembers()) {

                        Player player = Bukkit.getPlayer(UUID.fromString(clanMember.getUUID()));
                        String onlineString = "  &c" + clanMember.getPlayerName() + " ";
                        // change string based on if they are online
                        if (player == null)
                            onlineString += " &4Offline";
                        else
                            onlineString += " &aOnline";

                        sender.sendMessage(Utils.translate(onlineString));
                    }
                } else {
                    sender.sendMessage(Utils.translate("&4" + a[1] + " &cis not a clan!"));
                }
            } else if (sender instanceof Player) {
                // Sub commands here cannot be ran by non-players
                Player player = (Player) sender;
                PlayerStats playerStats = Momentum.getStatsManager().get(player);
                Clan clan = playerStats.getClan();

                if (a.length == 3 && a[0].equalsIgnoreCase("setxp")) {

                    if (player.hasPermission("momentum.admin")) {
                        String clanName = a[1];
                        if (Utils.isInteger(a[2])) {
                            int newXp = Integer.parseInt(a[2]);

                            Clan targetClan = Momentum.getClansManager().get(clanName);
                            if (targetClan != null) {
                                // make sure it is not negative xp
                                if (newXp > 0) {
                                    targetClan.setXP(newXp);
                                    // update in db
                                    ClansDB.setClanXP(newXp, targetClan.getID());
                                    player.sendMessage(Utils.translate("&eYou set &6&l" + targetClan.getTag() + "&e's" +
                                            " XP to &6" + newXp));
                                } else {
                                    player.sendMessage(Utils.translate("&cYou cannot set negative xp!"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&4" + clanName + " &cis not a clan"));
                            }
                        } else {
                            player.sendMessage(Utils.translate(""));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cYou do not have permission to do this"));
                    }
                // set total gained xp
                } else if (a.length == 3 && a[0].equalsIgnoreCase("settotalxp")) {

                    if (player.hasPermission("momentum.admin")) {
                        String clanName = a[1];
                        if (Utils.isLong(a[2])) {
                            long newXP = Long.parseLong(a[2]);

                            Clan targetClan = Momentum.getClansManager().get(clanName);
                            if (targetClan != null) {
                                // make sure it is not negative xp
                                if (newXP > 0) {
                                    targetClan.setTotalGainedXP(newXP);
                                    // update in db
                                    ClansDB.setTotalGainedClanXP(newXP, targetClan.getID());
                                    player.sendMessage(Utils.translate("&eYou set &6&l" + targetClan.getTag() + "&e's" +
                                            " Total XP to &6" + newXP));
                                } else {
                                    player.sendMessage(Utils.translate("&cYou cannot set negative xp!"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&4" + clanName + " &cis not a clan"));
                            }
                        } else {
                            player.sendMessage(Utils.translate(""));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cYou do not have permission to do this"));
                    }
                } else if (a.length == 3 && a[0].equalsIgnoreCase("setlevel")) {

                    if (player.hasPermission("momentum.admin")) {
                        String clanName = a[1];

                        if (Utils.isInteger(a[2])) {
                            int newLevel = Integer.parseInt(a[2]);

                            Clan targetClan = Momentum.getClansManager().get(clanName);
                            if (targetClan != null) {
                                // make sure it is actually a level
                                if (ClansYAML.isSection("clans." + newLevel)) {
                                    targetClan.setLevel(newLevel);
                                    // update in db
                                    ClansDB.setClanLevel(newLevel, targetClan.getID());
                                    player.sendMessage(Utils.translate("&eYou set &6&l" + clan.getTag() + "&e's" +
                                            " level to &6" + newLevel));
                                } else {
                                    player.sendMessage(Utils.translate("&cThat level does not exist!"));
                                }
                            }
                        } else {
                            player.sendMessage(Utils.translate("&4" + clanName + " &cis not a clan"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cYou do not have permission to do this"));
                    }
                } else if (a.length == 1 && a[0].equalsIgnoreCase("chatspy")) {
                    if (player.hasPermission("momentum.staff")) {
                        ClansManager clansManager = Momentum.getClansManager();

                        clansManager.toggleChatSpy(player.getName(), false);

                        boolean chatSpy = clansManager.isInChatSpy(player.getName());
                        String isCSOn = "&aOn";
                        if (!chatSpy)
                            isCSOn = "&cOff";

                        player.sendMessage(Utils.translate("&7You have turned &6&lClan ChatSpy " + isCSOn));
                    } else {
                        sender.sendMessage(Utils.translate("&cYou do not have permission to do this"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("create")) {
                    // Creates a clan at the set price
                    if (clan == null) {

                        int playerBalance = (int) playerStats.getCoins();

                        if (playerBalance >= Momentum.getSettingsManager().clans_price_create) {
                            if (a.length > 1) {
                                String clanTag = ChatColor.stripColor(a[1]);

                                if (clanTagRequirements(clanTag, sender)) {
                                    Momentum.getStatsManager().removeCoins(playerStats, Momentum.getSettingsManager().clans_price_create);
                                    Clan newClan = new Clan(-1, clanTag, playerStats.getPlayerID(), 1, 0, 0);
                                    Momentum.getClansManager().add(newClan);
                                    ClansDB.newClan(newClan);
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&cNo clan tag specified"));
                                sender.sendMessage(getHelp("create"));
                            }
                        } else {
                            sender.sendMessage(Utils.translate("&cYou need &6" +
                                    Momentum.getSettingsManager().clans_price_create + " Coins &cto create a clan"));
                        }
                    } else {
                        sender.sendMessage(Utils.translate("&cYou cannot create a clan while in one"));
                    }
                } else if (a.length == 1 && a[0].equalsIgnoreCase("chat")) {
                    if (clan != null) {
                        ClansManager clansManager = Momentum.getClansManager();

                        clansManager.toggleClanChat(player.getName(), clan);

                        boolean clanChat = clansManager.isInClanChat(player.getName());
                        String isCCOn = "&aOn";
                        if (!clanChat)
                            isCCOn = "&cOff";

                        player.sendMessage(Utils.translate("&7You have turned &6&lClan Chat " + isCCOn));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("changetag")) {
                    // Changes clan tag

                    if (sender instanceof Player) {

                        // if they are in a clan
                        if (clan != null) {
                            if (clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {

                                if (a.length > 1) {
                                    String clanTag = ChatColor.stripColor(a[1]);

                                    if (clanTagRequirements(clanTag, sender)) {
                                        // update in data
                                        Momentum.getClansManager().remove(clan.getTag());
                                        Momentum.getClansManager().getClans().put(clanTag, clan);
                                        clan.setTag(clanTag);
                                        Momentum.getStatsManager().removeCoins(playerStats, Momentum.getSettingsManager().clans_price_tag);
                                        ClansDB.updateClanTag(clan);
                                        sendClanMessage(clan, "&6&lClan Owner " +
                                                player.getName() + " &ehas changed your clan's tag to &c" + clanTag, true, player);
                                    }
                                } else {
                                    sender.sendMessage(Utils.translate("&cNo clan tag specified"));
                                    sender.sendMessage(getHelp("changetag"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&cYou are not the owner of your clan"));
                            }
                        } else {
                            player.sendMessage(Utils.translate("&cYou are not in a clan"));
                        }
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("setowner")) {
                    // sets new owner

                    Clan targetClan = Momentum.getClansManager().getFromMember(a[1]);

                    if (clan != null) {
                        if (targetClan != null) {
                            // if in same clan
                            if (targetClan.getID() == clan.getID()) {
                                if (clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {
                                    // update data
                                    clan.promoteOwnerFromName(a[1]);
                                    ClansDB.updateClanOwnerID(clan);
                                    sendClanMessage(clan, "&6" + a[1] + " &ehas been promoted to" +
                                            " &6&lClan Owner &eby &6" + player.getName(), true, player);
                                } else {
                                    player.sendMessage(Utils.translate("&cYou cannot switch clan owners if you are not owner"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&cYou are not in the same clan as &4" + a[1]));
                            }
                        } else {
                            player.sendMessage(Utils.translate("&4" + a[1] + " &cis not in a clan"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou are not in a clan"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("invite")) {
                    // invite player
                    if (clan != null) {
                        if (clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {

                            Player victim = Bukkit.getPlayer(a[1]);

                            if (victim == null) {
                                player.sendMessage(Utils.translate("&4" + a[1] + " &cis not online"));
                                return true;
                            }

                            // if they are not in a clan
                            if (Momentum.getStatsManager().get(victim).getClan() == null) {

                                // if they already have an invite or not
                                if (!clan.isInvited(victim.getUniqueId().toString())) {

                                    int maxMembers = Momentum.getSettingsManager().clans_max_members;
                                    if (clan.getMembers().size() < maxMembers) {
                                        // add an invite
                                        victim.sendMessage(Utils.translate("&6&l" + player.getName() + " &ehas sent you an" +
                                                " invitation to their &6&lClan &c" + clan.getTag()));
                                        victim.sendMessage(Utils.translate("   &7Type &e/clan accept " + player.getName() +
                                                " &7within &c30 seconds &7to accept"));
                                        player.sendMessage(Utils.translate("&eYou sent a &6&lClan Invite &eto &6" + victim.getName()
                                                + " &ethey have 30 seconds to accept"));

                                        clan.addInvite(victim.getUniqueId().toString());
                                        new BukkitRunnable() {
                                            public void run() {
                                                // ran out of time
                                                if (clan.isInvited(victim.getUniqueId().toString())) {

                                                    player.sendMessage(Utils.translate("&6" + victim.getName() +
                                                            " &edid not accept your &6&lClan Invite &ein time"));
                                                    victim.sendMessage(Utils.translate("&6You did not accept &6" +
                                                            player.getName() + "&e's &6&lClan Invite &ein time"));

                                                    // remove old invite
                                                    clan.removeInvite(victim.getUniqueId().toString());
                                                }
                                            }
                                            // 20 seconds to accept invite
                                        }.runTaskLater(Momentum.getPlugin(), 20 * 30);
                                    } else {
                                        player.sendMessage(Utils.translate("&cYou cannot invite anymore people to your" +
                                                " clan! Max - " + maxMembers));
                                    }
                                } else {
                                    player.sendMessage(Utils.translate("&cYou have already sent an invite to &4" + victim.getName()));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&4" + victim.getName() + " &cis already in a clan"));
                            }
                        } else {
                            player.sendMessage(Utils.translate("&cOnly the owner can invite others"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou have to be in a clan to invite someone!"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("accept")) {
                    // accept invite if they have one

                    // make sure they are not in a clan
                    if (clan == null) {

                        Player victim = Bukkit.getPlayer(a[1]);
                        if (victim == null) {
                            player.sendMessage(Utils.translate("&4" + victim.getName() + " &cis not online"));
                            return true;
                        }

                        Clan targetClan = Momentum.getStatsManager().get(victim).getClan();

                        if (targetClan != null) {
                            // if they are invited
                            if (targetClan.isInvited(player.getUniqueId().toString())) {

                                if (targetClan.getMembers().size() < Momentum.getSettingsManager().clans_max_members) {
                                    // add to clan and remove invite
                                    targetClan.addMember(new ClanMember(playerStats.getPlayerID(), playerStats.getUUID(), player.getName()));
                                    playerStats.setClan(targetClan);
                                    ClansDB.updatePlayerClanID(playerStats);
                                    targetClan.removeInvite(player.getUniqueId().toString());
                                    sendClanMessage(targetClan, "&6" + player.getName() + " &ehas joined your clan!", false, player);
                                    player.sendMessage(Utils.translate("&eYou joined the &6&lClan &c" + targetClan.getTag()));

                                } else {
                                    player.sendMessage(Utils.translate("&cThat clan is full!"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&eYou do not have an invite from &6Clan &c" + targetClan.getTag()));
                            }
                        } else {
                            player.sendMessage(Utils.translate("&6" + victim.getName() + " &eis not in a &6&lClan"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou cannot join a clan if you are in one"));
                    }
                } else if (a.length == 2 && a[0].equalsIgnoreCase("kick")) {
                    // kick if in clan

                    // make sure they are in a clan
                    if (clan != null) {
                        // make sure they are owner of the clan
                        if (clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {

                            // make sure they are not trying to kick themselves
                            if (!a[1].equalsIgnoreCase(player.getName())) {

                                Clan targetClan = Momentum.getClansManager().getFromMember(a[1]);

                                // if they do not have a clan stored in cache
                                if (targetClan != null) {

                                    // make sure they are kicking from the same clan
                                    if (targetClan.getID() == clan.getID()) {

                                        sendClanMessage(clan, "&6" + a[1] + " &ehas been removed from the clan", true, player);

                                        targetClan.removeMemberFromName(a[1]);

                                        PlayerStats victimStats = Momentum.getStatsManager().get(a[1]);
                                        if (victimStats != null)
                                            victimStats.resetClan();

                                        ClansDB.updatePlayerClanID(a[1], -1);
                                    }
                                } else {
                                    player.sendMessage(Utils.translate("&4" + a[1] + " &cis not in your clan"));
                                }
                            } else {
                                player.sendMessage(Utils.translate("&cYou cannot kick yourself"));
                            }
                        } else {
                            player.sendMessage(Utils.translate("&cYou cannot kick a member from a clan you do not own!"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou are not in a clan!"));
                    }
                } else if (a.length == 1 && a[0].equalsIgnoreCase("disband")) {
                    // disband if in a clan

                    // make sure they are in a clan
                    if (clan != null) {
                        if (clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {
                            // delete clan
                            Momentum.getClansManager().deleteClan(clan.getID(), true);
                        } else {
                            player.sendMessage(Utils.translate("&cYou cannot disband a clan you are not owner of"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou cannot disband a clan if you are not in one"));
                    }
                } else if (a.length == 1 && a[0].equalsIgnoreCase("leave")) {
                    // leave if in a clan

                    if (clan != null) {
                        // if they are only one in their clan
                        if (clan.getMembers().size() == 1) {

                            ClansDB.resetClanMember(player.getName());
                            ClansDB.removeClan(clan.getID());
                            Momentum.getClansManager().remove(clan.getTag());
                            playerStats.resetClan();
                            player.sendMessage(Utils.translate("&eYou have left your clan"));

                            // if not only one, make sure they are not the owner
                        } else if (!clan.getOwner().getPlayerName().equalsIgnoreCase(player.getName())) {

                            ClansDB.resetClanMember(player.getName());
                            clan.removeMemberFromName(player.getName());
                            playerStats.resetClan();
                            player.sendMessage(Utils.translate("&eYou have left your clan"));
                            sendClanMessage(clan, "&6" + player.getName() + " &ehas left your clan", false, player);
                        } else {
                            player.sendMessage(Utils.translate("&cYou cannot leave a clan you own"));
                        }
                    } else {
                        player.sendMessage(Utils.translate("&cYou are not in a clan"));
                    }
                } else if (player.hasPermission("momentum.admin") && (a.length == 2 && a[0].equalsIgnoreCase("delete"))) {

                    Clan targetClan = Momentum.getClansManager().get(a[1]);

                    if (targetClan != null) {
                        // remove from cache and db
                        Momentum.getClansManager().deleteClan(targetClan.getID(), false);
                        player.sendMessage(Utils.translate("&7You have deleted &c" + a[1] + " &7from the database"));
                    } else {
                        player.sendMessage(Utils.translate("&7Clan &c" + a[1] + " &7does not exist"));
                    }
                } else {
                    // Unknown argument
                    sendHelp(sender);
                }
            } else {
                sender.sendMessage(Utils.translate("&cMost clan commands can only be run in-game"));
            }
        } else {
            sendHelp(sender);
        }
        return true;
    }

    private static boolean clanTagRequirements(String clanTag, CommandSender sender) {
        // Clan Tag has improper length
        if (clanTag.length() < Momentum.getSettingsManager().clans_tag_length_min
            || clanTag.length() > Momentum.getSettingsManager().clans_tag_length_max) {

            sender.sendMessage(Utils.translate("&c'&4" + clanTag + "&c' does not fit Clan Tag requirements"));
            sender.sendMessage(Utils.translate("&cClan Tags must be &4" + Momentum.getSettingsManager().clans_tag_length_min + "-"
                                               + Momentum.getSettingsManager().clans_tag_length_max + " &ccharacters"));
            return false;
        } else if (Momentum.getClansManager().get(clanTag) != null) {
            sender.sendMessage(Utils.translate("&cThe Clan Tag '&4" + clanTag + "&c' is already taken"));
            return false;
        } else if (Momentum.getSettingsManager().blocked_clan_names.contains(clanTag)) {
            sender.sendMessage(Utils.translate("&cYou cannot use '&4" + clanTag + "&c' as a tag"));
            return false;
        } else {
            return true;
        }
    }

    private static void sendClanMessage(Clan targetClan, String message, boolean sendToSelf, Player self) {
        for (ClanMember clanMember : targetClan.getMembers()) {
            // make sure they are online
            Player clanPlayer = Bukkit.getPlayer(UUID.fromString(clanMember.getUUID()));

            if (clanPlayer != null) {
                // if it will send to self, then do everyone
                if (sendToSelf)
                    clanPlayer.sendMessage(Utils.translate(message));
                // otherwise make sure it is not the same person
                else if (self != null && clanPlayer.getName() != self.getName())
                    clanPlayer.sendMessage(Utils.translate(message));
            }
        }
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(getHelp("stats")); // console friendly
        sender.sendMessage(getHelp("create"));
        sender.sendMessage(getHelp("invite"));
        sender.sendMessage(getHelp("accept"));
        sender.sendMessage(getHelp("leave"));
        sender.sendMessage(getHelp("kick"));
        sender.sendMessage(getHelp("disband"));
        sender.sendMessage(getHelp("changetag"));
        sender.sendMessage(getHelp("setowner"));
        sender.sendMessage(getHelp("chat"));

        // send admin section
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("momentum.admin")) {
                sender.sendMessage(getHelp("setlevel"));
                sender.sendMessage(getHelp("setxp"));
                sender.sendMessage(getHelp("settotalxp"));
                sender.sendMessage(getHelp("delete"));
                sender.sendMessage(getHelp("chatspy"));
            }
        }
    }

    private static String getHelp(String cmd) {
        switch (cmd.toLowerCase()) {
            case "stats":
                return Utils.translate("&3/clan stats <clan>  &7Display clan statistics");
            case "create":
                return Utils.translate("&3/clan create <tag>  &7Create a clan &6" + Momentum.getSettingsManager().clans_price_create + " Coins");
            case "changetag":
                return Utils.translate("&3/clan changetag <tag>  &7Change clan tag &6" + Momentum.getSettingsManager().clans_price_tag + " Coins");
            case "setowner":
                return Utils.translate("&3/clan setowner <player>  &7Give your clan ownership");
            case "kick":
                return Utils.translate("&3/clan kick <player>  &7Kick player from your clan");
            case "invite":
                return Utils.translate("&3/clan invite <player>  &7Invite player to your clan");
            case "accept":
                return Utils.translate("&3/clan accept <player>  &7Accept invite from player");
            case "disband":
                return Utils.translate("&3/clan disband  &7Disband your clan");
            case "leave":
                return Utils.translate("&3/clan leave  &7Leave your clan");
            case "setxp":
                return Utils.translate("&3/clan setxp <clan> <xp>  &7Sets clan XP");
            case "setlevel":
                return Utils.translate("&3/clan setlevel <clan> <level>  &7Sets clan level");
            case "delete":
                return Utils.translate("&3/clan delete <clan>  &7Deletes the clan");
            case "settotalxp":
                return Utils.translate("&3/clan settotalxp <clan>  &7Sets total XP of a clan");
            case "chat":
                return Utils.translate("&3/clan chat  &7Toggles clan chat");
            case "chatspy":
                return Utils.translate("&3/clan chatspy  &7Toggle clan chatspy");
        }
        return "";
    }
}
