package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.data.stats.StatsDB;
import com.renatusnetwork.momentum.gameplay.SpectatorHandler;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (sender.hasPermission("momentum.donator")) {

                PlayerStats spectatorStats = Momentum.getStatsManager().get(player);

                if (spectatorStats != null) {
                    if (a.length > 0) {
                        if (a.length == 1 && a[0].equalsIgnoreCase("toggle")) {
                            if (spectatorStats.isSpectatable())
                                sender.sendMessage(Utils.translate("&7You can no longer be spectated"));
                            else
                                sender.sendMessage(Utils.translate("&7You can now be spectated"));

                            spectatorStats.setSpectatable(!spectatorStats.isSpectatable());
                            StatsDB.updatePlayerSpectatable(spectatorStats);
                        } else if (a.length == 1 && a[0].equalsIgnoreCase("help")) {
                            sendHelp(sender);
                        // spectate to player
                        } else if (a.length == 1) {
                            PlayerStats playerStats = Momentum.getStatsManager().getByName(a[0]);

                            if (playerStats != null && playerStats.getPlayer().isOnline()) {
                                if (!player.getName().equalsIgnoreCase(playerStats.getPlayer().getName())) {
                                    if (playerStats.isSpectatable()) {
                                        if (!playerStats.getPlayer().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world)) {
                                            if (!spectatorStats.inPracticeMode()) {
                                                if (!spectatorStats.inRace()) {
                                                    if (!spectatorStats.isEventParticipant()) {
                                                        if (!player.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world)) {
                                                            if (!spectatorStats.isInInfinitePK()) {
                                                                if (player.isOnGround()) {

                                                                    Location location = player.getLocation().clone();
                                                                    if (location.add(0, 1, 0).getBlock().getType() == Material.AIR)
                                                                    {
                                                                        boolean initialSpectate = true;
                                                                        if (spectatorStats.isSpectating()) {
                                                                            initialSpectate = false;

                                                                            /*
                                                                             if they are already spectating and the person they
                                                                             are spectating are who they are trying to spectate again, cancel
                                                                             */
                                                                            if (spectatorStats.getPlayerToSpectate().getPlayerName()
                                                                                    .equalsIgnoreCase(playerStats.getPlayerName())) {

                                                                                player.sendMessage(Utils.translate(
                                                                                        "&cYou cannot spectate the same person you are spectating"));
                                                                                return true;
                                                                            }
                                                                        }

                                                                        // enable spectator mode
                                                                        SpectatorHandler.setSpectatorMode(spectatorStats, playerStats, initialSpectate);

                                                                        playerStats.getPlayer().sendMessage(Utils.translate("&2" +
                                                                                spectatorStats.getPlayerName() + " &7began to spectate you"));
                                                                    }
                                                                    else
                                                                        player.sendMessage(Utils.translate("&cYou cannot spectate while in a block"));
                                                                }
                                                                else
                                                                {
                                                                    player.sendMessage(Utils.translate("&cYou cannot use spectate while in the air"));
                                                                }
                                                            } else {
                                                                player.sendMessage(Utils.translate("&cYou cannot spectate while in infinite parkour"));
                                                            }
                                                        } else {
                                                            player.sendMessage(Utils.translate("&cYou cannot spectate in the plot world"));
                                                        }
                                                    } else {
                                                        player.sendMessage(Utils.translate("&cYou cannot do this while in an event"));
                                                    }
                                                } else {
                                                    player.sendMessage(Utils.translate("&cYou cannot do this while in a race"));
                                                }
                                            } else {
                                                player.sendMessage(Utils.translate("&cYou cannot enter spectator mode while in /prac"));
                                            }
                                        } else {
                                            sender.sendMessage(Utils.translate("&cYou cannot spectate players that are in the plot world"));
                                        }
                                    } else {
                                        sender.sendMessage(Utils.translate("&cThat player cannot be spectated"));
                                    }
                                } else {
                                    player.sendMessage(Utils.translate("&cYou cannot spectate yourself"));
                                }
                            } else {
                                sender.sendMessage(Utils.translate("&cThere is no player online named &4" + a[0]));
                            }
                        }
                    // if they just run /spectate
                    } else if (spectatorStats.isSpectating()) {
                        SpectatorHandler.removeSpectatorMode(spectatorStats);
                    } else {
                        sendHelp(sender);
                    }
                } else {
                    sender.sendMessage(Utils.translate("&cError loading your data"));
                }
            } else {
                sender.sendMessage(Utils.translate("&cOnly Donators can use spectate mode"));
            }
        } else {
            sender.sendMessage(Utils.translate("&cOnly players can run this command"));
        }
        return true;
    }

    private static void sendHelp(CommandSender sender) {
        sender.sendMessage(getHelp("player"));
        sender.sendMessage(getHelp("toggle"));
        sender.sendMessage(getHelp("help"));
    }

    private static String getHelp(String cmd) {
        if (cmd.equalsIgnoreCase("player"))
            return Utils.translate("&2/spectate <player>  &7Spectates a player");
        else if (cmd.equalsIgnoreCase("toggle"))
            return Utils.translate("&2/spectate toggle  &7Toggles if you can be spectated");
        else if (cmd.equalsIgnoreCase("help"))
            return Utils.translate("&2/spectate help  &7Display this page");
        return "";
    }
}
