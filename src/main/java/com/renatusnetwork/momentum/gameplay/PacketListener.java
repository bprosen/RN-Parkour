package com.renatusnetwork.momentum.gameplay;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.infinite.InfinitePK;
import com.renatusnetwork.momentum.data.infinite.InfinitePKManager;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.locations.LocationManager;
import com.renatusnetwork.momentum.data.locations.PortalType;
import com.renatusnetwork.momentum.data.menus.MenuItemAction;
import com.renatusnetwork.momentum.data.plots.Plot;
import com.renatusnetwork.momentum.data.races.Race;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

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
                if (packet.getType() == PacketType.Play.Client.BLOCK_DIG || packet.getType() == PacketType.Play.Client.USE_ITEM) {

                    Player player = event.getPlayer();

                    // make sure they are not temporary and in the right world
                    if (!event.isPlayerTemporary() &&
                        player.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world)) {

                        // if they are opped, and if they are bypassing plots, then ignore
                        if (player.isOp() && Momentum.getStatsManager().get(player).isBypassingPlots())
                            return;

                        /*
                            This section of the code runs checks to find the location of the block packet sent
                         */
                        BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
                        Location loc = blockPosition.toVector().toLocation(
                                Bukkit.getWorld(Momentum.getSettingsManager().player_submitted_world));

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
                        Plot plot = Momentum.getPlotsManager().getPlotInLocation(loc);

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
                            }
                        // no nearest plot
                        } else {
                            doCancel = true;
                            reason = "&cYou cannot do this here";
                        }

                        if (doCancel) {
                            // if they are opped, tell them on being able to bypass
                            if (player.isOp())
                                reason += " &7You can bypass this with &c/plot bypass";

                            player.sendMessage(Utils.translate(reason));
                            event.setCancelled(true);

                            // send block update back to player from server that intercepted the packet, run in sync so no async async chunk load
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    player.sendBlockChange(loc, loc.getBlock().getType(), loc.getBlock().getData());
                                }
                            }.runTask(plugin);
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
                Player player = event.getPlayer();

                double playerX = packet.getDoubles().read(0);
                double playerY = packet.getDoubles().read(1);
                double playerZ = packet.getDoubles().read(2);

                PlayerStats playerStats = Momentum.getStatsManager().get(player);

                // null check jic
                if (playerStats != null) {

                    InfinitePKManager infinitePKManager = Momentum.getInfinitePKManager();
                    LocationManager locationManager = Momentum.getLocationManager();

                    // if spectating
                    if (playerStats.isSpectating())
                    {
                        PlayerStats beingSpectated = playerStats.getPlayerToSpectate();

                        if (beingSpectated != null && beingSpectated.getPlayer().isOnline() && beingSpectated.isSpectatable() &&
                                !beingSpectated.getPlayer().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
                        {

                            if (!beingSpectated.getPlayer().getWorld().getName().equalsIgnoreCase(playerStats.getPlayer().getWorld().getName()) ||
                                    playerStats.getPlayer().getLocation().distance(beingSpectated.getPlayer().getLocation()) > 30)

                                // run in sync due to teleporting
                                new BukkitRunnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        SpectatorHandler.spectateToPlayer(playerStats.getPlayer(), beingSpectated.getPlayer(), false);
                                    }
                                }.runTask(Momentum.getPlugin());
                        }
                        else
                        {
                            // run in sync due to teleporting
                            new BukkitRunnable()
                            {
                                @Override
                                public void run()
                                {
                                    SpectatorHandler.removeSpectatorMode(playerStats);
                                }
                            }.runTask(Momentum.getPlugin());
                        }
                    }
                    // if in infinite parkour
                    else if (playerStats.isInInfinitePK())
                    {
                        InfinitePK infinitePK = infinitePKManager.get(player.getName());

                        // end infinite pk if below current block
                        if ((infinitePK.getCurrentBlockLoc().getBlockY() - 2) > player.getLocation().getBlockY())
                            infinitePKManager.endPK(infinitePK.getPlayer(), false);
                        // if their loc
                    }
                    else if (!playerStats.inLevel())
                    {
                        if (locationManager.isNearPortal(playerX, playerY, playerZ, 1, PortalType.INFINITE))
                            infinitePKManager.startPK(playerStats, true);
                        else if (locationManager.isNearPortal(playerX, playerY, playerZ, 1, PortalType.ASCENDANCE))
                        {
                            Level level = Momentum.getLevelManager().get(Momentum.getSettingsManager().ascendance_hub_level);
                            if (level != null) {
                                // force sync
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        MenuItemAction.performLevelTeleport(playerStats, player, level); // Tp to ascendance hub
                                    }
                                }.runTask(Momentum.getPlugin());
                            }
                        }
                    }
                    else
                    {
                        Level level = playerStats.getLevel();

                        // run in sync due to teleporting
                        new BukkitRunnable() {
                            @Override
                            public void run() {

                                // if level is not null, it has a respawn y, and the y is greater than or equal to player y, respawn
                                if (level != null && level.hasRespawnY() && level.getRespawnY() >= player.getLocation().getY()) {
                                    // teleport
                                    if (playerStats.hasCurrentCheckpoint() || playerStats.inPracticeMode())
                                        Momentum.getCheckpointManager().teleportToCP(playerStats);
                                    else if (playerStats.inRace()) {
                                        Race race = Momentum.getRaceManager().get(player);
                                        if (race != null) {
                                            if (race.isPlayer1(player))
                                                race.getPlayer1().teleport(race.getRaceLevel().getRaceLocation1());
                                                // swap tp to loc 2 if player 2
                                            else
                                                race.getPlayer2().teleport(race.getRaceLevel().getRaceLocation2());
                                        }
                                    } else
                                        LevelHandler.respawnPlayer(playerStats, level);
                                }
                            }
                        }.runTask(plugin);
                    }
                }
            }
        });
    }
}