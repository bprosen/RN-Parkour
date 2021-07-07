package com.parkourcraft.parkour.data.checkpoints;

import com.parkourcraft.parkour.Parkour;
import com.parkourcraft.parkour.data.stats.PlayerStats;
import com.parkourcraft.parkour.utils.Utils;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CheckpointManager {

    public void teleportPlayer(PlayerStats playerStats) {

        if (!playerStats.inRace()) {
            if (!playerStats.isEventParticipant()) {
                if (playerStats.getPlayerToSpectate() == null) {

                    Location loc = null;
                    /*
                     check if there is a practice location,
                     if not then check if the loc is checkpoint and
                     adjust the x and z to teleport to middle
                     */
                    if (playerStats.getPracticeLocation() != null)
                        loc = playerStats.getPracticeLocation().clone();
                    else if (playerStats.getCheckpoint() != null)
                        loc = playerStats.getCheckpoint().clone().add(0.5, 0, 0.5);

                    if (loc != null) {

                        loc.setPitch(playerStats.getPlayer().getLocation().getPitch());
                        loc.setYaw(playerStats.getPlayer().getLocation().getYaw());
                        playerStats.getPlayer().teleport(loc);
                    } else {
                        playerStats.getPlayer().sendMessage(Utils.translate("&cYou do not have a saved checkpoint"));
                    }
                } else {
                    playerStats.getPlayer().sendMessage(Utils.translate("&cYou cannot do this while spectating"));
                }
            } else {
                playerStats.getPlayer().sendMessage(Utils.translate("&cYou cannot do this while in an event"));
            }
        } else {
            playerStats.getPlayer().sendMessage(Utils.translate("&cYou cannot do this while in a race"));
        }
    }
}
