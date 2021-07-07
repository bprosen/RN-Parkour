package com.parkourcraft.parkour.gameplay;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLib;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.parkourcraft.parkour.Parkour;
import com.parkourcraft.parkour.data.levels.Level;
import com.parkourcraft.parkour.data.plots.Plot;
import com.parkourcraft.parkour.data.stats.PlayerStats;
import com.parkourcraft.parkour.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class PacketListener implements Listener {

    public static void loadListeners(Plugin plugin) {
        registerBlockListener(plugin);
        registerMoveListener(plugin);
    }

    private static void registerBlockListener(Plugin plugin) {

        // listen to block dig/place asynchronously
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.BLOCK_DIG, PacketType.Play.Client.USE_ITEM) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                // use block change for right click, left click, etc

                if (packet.getType() == PacketType.Play.Client.BLOCK_DIG ||
                    packet.getType() == PacketType.Play.Client.USE_ITEM) {

                    Player player = event.getPlayer();

                    // make sure they are in the right world and not opped
                    if (player.getWorld().getName().equalsIgnoreCase(Parkour.getSettingsManager().player_submitted_world)
                        && !player.isOp()) {

                        /*
                            This section of the code runs checks to find the location of the block packet sent
                         */
                        BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
                        Location loc = blockPosition.toVector().toLocation(
                                Bukkit.getWorld(Parkour.getSettingsManager().player_submitted_world));

                        // if it is a block place, adjust location to the block being placed, not what it is placed on
                        if (packet.getType() == PacketType.Play.Client.USE_ITEM) {
                            String direction = packet.getDirections().read(0).toString();

                            // adjust location
                            switch (direction) {
                                case "UP":
                                    loc.add(0, 1, 0);
                                    break;
                                case "DOWN":
                                    loc.subtract(0, 1, 0);
                                    break;
                                case "NORTH":
                                    loc.subtract(0, 0, 1);
                                    break;
                                case "EAST":
                                    loc.add(1, 0, 0);
                                    break;
                                case "SOUTH":
                                    loc.add(0, 0, 1);
                                    break;
                                case "WEST":
                                    loc.subtract(1, 0, 0);
                                    break;
                            }
                        }

                        // get nearest plot from location
                        Plot plot = Parkour.getPlotsManager().getNearestPlot(loc);

                        /*
                            This section of the code runs then uses the found location to do conditional checks on whether
                            it will cancel the packet
                         */
                        boolean doCancel = false;
                        String reason = "";

                        // check if they have a plot,
                        // only way this does not get cancelled is if they are trusted or own it
                        if (plot != null) {
                            // check if their plot is submitted
                            if (plot.isSubmitted()) {
                                doCancel = true;
                                reason = "&cYou cannot edit your plot when it has been submitted";
                            // check if they are not trusted and not owner, then cancel
                            } else if (!plot.getOwnerName().equalsIgnoreCase(player.getName()) &&
                                       !plot.getTrustedPlayers().contains(player.getName())) {

                                doCancel = true;
                                reason = "&cYou cannot do this here";
                            // this will only continue if the block they edited is in the x and y of the bedrock spawn
                            } else if (loc.getBlockX() == plot.getSpawnLoc().getBlockX() && loc.getBlockZ() == plot.getSpawnLoc().getBlockZ()) {

                                // this means they edited a block within 2 above the spawn bedrock
                                if (packet.getType() == PacketType.Play.Client.USE_ITEM &&
                                   (loc.getBlockY() <= plot.getSpawnLoc().getBlockY() + 1 &&
                                    loc.getBlockY() >= plot.getSpawnLoc().getBlockY())) {
                                    doCancel = true;
                                    reason = "&cYou cannot build that close on the top of the spawn block!";

                                // this means cancel if they are trying to break the spawn block
                                } else if (packet.getType() == PacketType.Play.Client.BLOCK_DIG &&
                                           loc.getBlockY() == plot.getSpawnLoc().clone().subtract(0, 1, 0).getBlockY()) {
                                    doCancel = true;
                                    reason = "&cYou cannot break the spawn bedrock";
                                }
                            }
                        // no nearest plot
                        } else {
                            doCancel = true;
                            reason = "&cYou cannot do this here";
                        }

                        if (doCancel) {
                            player.sendMessage(Utils.translate(reason));
                            event.setCancelled(true);
                            // send block update back to player from server that intercepted the packet
                            player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
                        }
                    }
                }
            }
        });
    }

    private static void registerMoveListener(Plugin plugin) {

        // listen to move event asynchronously
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
                plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.POSITION) {

            @Override
            public void onPacketReceiving(PacketEvent event) {
                PacketContainer packet = event.getPacket();

                double playerY = packet.getDoubles().read(1);
                PlayerStats playerStats = Parkour.getStatsManager().get(event.getPlayer());

                // null check jic
                if (playerStats != null) {
                    Level level = Parkour.getLevelManager().get(playerStats.getLevel());

                    // if level is not null, has a respawn y, and the y is greater than or equal to player y, respawn
                    if (level != null && level.hasRespawnY() && level.getRespawnY() >= playerY) {
                        // teleport
                        if (playerStats.getCheckpoint() != null || playerStats.getPracticeLocation() != null)
                            Parkour.getCheckpointManager().teleportPlayer(event.getPlayer());
                        else
                            LevelHandler.respawnPlayer(event.getPlayer(), level);
                    }
                }
            }
        });
    }
}