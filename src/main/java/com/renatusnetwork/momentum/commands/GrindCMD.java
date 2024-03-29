package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.data.stats.StatsDB;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GrindCMD implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a)
    {

        if (sender instanceof Player)
        {
            Player player = (Player) sender;

            if (a.length == 0)
            {
                PlayerStats playerStats = Momentum.getStatsManager().get(player);

                if (playerStats != null)
                {
                    // Update cache and DB
                    playerStats.toggleGrinding();
                    StatsDB.updatePlayerGrinding(playerStats);

                    player.sendMessage(Utils.translate("&7You have turned grind mode " + (playerStats.isGrinding() ? "&aOn" : "&cOff")));
                }
            }
        }
        return false;
    }
}
