package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpawnCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;
        PlayerStats playerStats = Momentum.getStatsManager().get(player);

        if (player.hasPermission("momentum.admin"))
        {

            if (a.length == 0)
                checkTutorial(playerStats);
            else if (a.length == 1)
            {

                String victim = a[0];
                Player victimPlayer = Bukkit.getPlayer(victim);

                if (victimPlayer == null) {
                    player.sendMessage(Utils.translate("&4" + victim + " &cis not online"));
                    return true;
                }

                PlayerStats victimStats = Momentum.getStatsManager().get(victimPlayer);

                checkTutorial(victimStats);
                player.sendMessage(Utils.translate("&cYou teleported &4" + victim + " &cto spawn"));
            }
        } else if (a.length == 0) {
            checkTutorial(playerStats);
        }
        return false;
    }

    private static void checkTutorial(PlayerStats playerStats)
    {
        // check for tutorial
        if (!playerStats.isInTutorial())
            Utils.teleportToSpawn(playerStats);
        else
            playerStats.getPlayer().sendMessage(Utils.translate("&cYou cannot do this while in the tutorial, use &a/tutorial skip &cif you wish to skip"));
    }
}
