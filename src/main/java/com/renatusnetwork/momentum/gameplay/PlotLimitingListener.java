package com.renatusnetwork.momentum.gameplay;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.plots.Plot;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class PlotLimitingListener implements Listener {

    /*
        these are all listeners meant for the plot world that disable entities and multiple types of redstone
     */

    /*
        Buckets dont like packets :(
     */
    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event)
    {
        Location loc = event.getBlockClicked().getRelative(event.getBlockFace()).getLocation();

        if (cancel(loc, event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent event)
    {
        if (cancel(event.getBlockClicked().getLocation(), event.getPlayer()))
            event.setCancelled(true);
    }

    private boolean cancel(Location loc, Player player)
    {
        boolean doCancel = false;

        if (loc.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
        {

            // Abrupt if found to be bypassing
            if (player.isOp() && Momentum.getStatsManager().get(player).isBypassingPlots())
                return false;

            Plot plot = Momentum.getPlotsManager().getPlotInLocation(loc);

            // check if they have a plot,
            // only way this does not get cancelled is if they are trusted or own it
            if (plot != null)
            {
                // check if their plot is submitted
                if (plot.isSubmitted())
                    doCancel = true;

                    // check if they are not trusted and not owner, then cancel
                else if (!plot.getOwnerName().equalsIgnoreCase(player.getName()) &&
                        !plot.getTrustedPlayers().contains(player.getName()))
                    doCancel = true;
                // this will only continue if the block they edited is in the x and y of the bedrock spawn
                // no nearest plot
            }
            else
                doCancel = true;
        }

        return doCancel;
    }
    @EventHandler
    public void onLiquidFlow(BlockFromToEvent event)
    {
        Location loc = event.getToBlock().getLocation();

        if (loc.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
        {
            Plot plot = Momentum.getPlotsManager().getPlotInLocation(loc);

            // If not in a plot
            if (plot == null)
                event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        // ZERO entity overloading
        if (event.getLocation().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
            event.setCancelled(true);
    }

    @EventHandler
    public void onDispenserDrop(BlockDispenseEvent event) {
        // ZERO dispenser overloading
        if (event.getBlock().getLocation().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
            event.setCancelled(true);
    }

    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        // ZERO redstone overloading
        if (event.getBlock().getLocation().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
            event.setNewCurrent(0);
    }
}
