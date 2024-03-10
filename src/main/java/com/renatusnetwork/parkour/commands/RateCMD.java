package com.renatusnetwork.parkour.commands;

import com.renatusnetwork.parkour.Parkour;
import com.renatusnetwork.parkour.data.levels.Level;
import com.renatusnetwork.parkour.data.levels.RatingDB;
import com.renatusnetwork.parkour.data.menus.MenuManager;
import com.renatusnetwork.parkour.data.stats.PlayerStats;
import com.renatusnetwork.parkour.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;

public class RateCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (!(sender instanceof Player))
            return true;

        Player player = (Player) sender;

        if (a.length >= 1)
        {
            // allow ability to get from title or name
            String[] split = Arrays.copyOfRange(a, 0, a.length);
            String levelName = String.join(" ", split);

            // if it does not get it from name, then attempt to get it from title
            Level level = Parkour.getLevelManager().getNameThenTitle(levelName);

            if (level != null)
            {
                PlayerStats playerStats = Parkour.getStatsManager().get(player);

                if (playerStats.hasCompleted(level))
                {
                    MenuManager menuManager = Parkour.getMenuManager();

                    menuManager.addChoosingRating(playerStats, level);
                    // menu
                    menuManager.openInventory(playerStats, "rate_level", true);
                }
                else
                    player.sendMessage(Utils.translate(
                            "&cYou have not completed &c" + level.getTitle() + "&c to be able to rate it"
                    ));
            }
            else
                player.sendMessage(Utils.translate("&cNo level named &4" + levelName + " &cexists"));

        }
        return false;
    }
}
