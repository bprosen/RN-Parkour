package com.renatusnetwork.momentum.gameplay;

import com.renatusnetwork.momentum.Momentum;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Location spawn = Momentum.getLocationManager().getLobbyLocation();

        if (spawn != null)
            event.setRespawnLocation(spawn);
    }
}
