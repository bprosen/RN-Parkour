package com.renatusnetwork.momentum.commands;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.utils.PlayerHider;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerToggleCMD implements CommandExecutor {

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {

        if (!(sender instanceof Player))
            return true;

        Player player = (Player) sender;

        if (a.length == 0)
            if (PlayerHider.containsPlayer(player)) {
                PlayerHider.showPlayer(player);
                player.sendMessage(Utils.translate("&aYou have turned on players"));
            } else if (!Momentum.getStatsManager().get(player.getUniqueId().toString()).isEventParticipant()) {
                PlayerHider.hidePlayer(player);
                player.sendMessage(Utils.translate("&cYou have turned off players"));
            } else {
                player.sendMessage(Utils.translate("&cYou cannot do this while in an event"));
            }
        return true;
    }
}
