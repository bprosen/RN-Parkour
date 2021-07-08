package com.parkourcraft.parkour.gameplay;

import com.parkourcraft.parkour.Parkour;
import com.parkourcraft.parkour.data.checkpoints.CheckpointDB;
import com.parkourcraft.parkour.data.clans.ClansManager;
import com.parkourcraft.parkour.data.events.EventManager;
import com.parkourcraft.parkour.data.infinite.InfinitePKManager;
import com.parkourcraft.parkour.data.levels.Level;
import com.parkourcraft.parkour.data.plots.Plot;
import com.parkourcraft.parkour.data.races.RaceManager;
import com.parkourcraft.parkour.data.stats.PlayerStats;
import com.parkourcraft.parkour.utils.PlayerHider;
import com.parkourcraft.parkour.utils.Utils;
import com.parkourcraft.parkour.utils.dependencies.WorldGuard;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;
import java.util.UUID;

public class JoinLeaveHandler implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            Location spawn = Parkour.getLocationManager().getLobbyLocation();
            if (spawn != null) {
                player.teleport(spawn);
                Bukkit.broadcastMessage(Utils.translate(
                        "&a&o" + player.getDisplayName() + "&7&o joined &b&l&oParkour &7&ofor the first time"
                ));
            }
        }
        PlayerHider.hideHiddenPlayersFromJoined(player);

        // send message to op people that there are undecided plots
        if (player.isOp()) {
            List<Plot> submittedPlotList = Parkour.getPlotsManager().getSubmittedPlots();

            if (!submittedPlotList.isEmpty())
                player.sendMessage(Utils.translate("&7There are &c&l" + submittedPlotList.size() + "" +
                        " &6Submitted Plots &7that still need to be checked! &a/plot submit list"));
        }

        // run most of this in async (region lookup, stat editing, etc)
        new BukkitRunnable() {
            @Override
            public void run() {
                Parkour.getStatsManager().add(player);
                List<String> regions = WorldGuard.getRegions(player.getLocation());
                if (!regions.isEmpty()) {

                    Level level = Parkour.getLevelManager().get(regions.get(0));

                    // make sure the area they are spawning in is a level
                    if (level != null) {
                        PlayerStats playerStats = Parkour.getStatsManager().get(player);
                        playerStats.setLevel(level);

                        UUID uuid = player.getUniqueId();
                        if (CheckpointDB.hasCheckpoint(uuid, regions.get(0)))
                            CheckpointDB.loadPlayer(uuid, regions.get(0));

                        // is elytra level, then set elytra in sync (player inventory changes)
                        if (level.isElytraLevel())
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    Parkour.getStatsManager().toggleOnElytra(playerStats);
                                }
                            }.runTask(Parkour.getPlugin());
                    }
                }
            }
        }.runTaskAsynchronously(Parkour.getPlugin());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        PlayerStats playerStats = Parkour.getStatsManager().get(player);
        RaceManager raceManager = Parkour.getRaceManager();
        EventManager eventManager = Parkour.getEventManager();
        InfinitePKManager infinitePKManager = Parkour.getInfinitePKManager();
        ClansManager clansManager = Parkour.getClansManager();

        // if left with checkpoint, save it
        if (playerStats.getCheckpoint() != null)
            CheckpointDB.savePlayerAsync(player);

        // if left in spectator, remove it
        if (playerStats.getPlayerToSpectate() != null)
            SpectatorHandler.removeSpectatorMode(playerStats);

        // if left in practice mode, reset it
        if (playerStats.getPracticeLocation() != null)
            PracticeHandler.resetPlayer(player, false);

        // if left in race, end it
        if (playerStats.inRace())
            raceManager.endRace(raceManager.get(player).getOpponent(player));

        // if left as hidden, remove them
        if (PlayerHider.containsPlayer(player))
            PlayerHider.removeHiddenPlayer(player);

        // if event is running and they are a participant, remove
        if (playerStats.isEventParticipant())
            eventManager.removeParticipant(player, true);

        if (playerStats.isInInfinitePK())
            infinitePKManager.endPK(player, true);

        // toggle off elytra armor
        Parkour.getStatsManager().toggleOffElytra(playerStats);

        // do not need to check, as method already checks
        clansManager.toggleClanChat(player.getName(), null);
        clansManager.toggleChatSpy(player.getName(), true);
    }
}
