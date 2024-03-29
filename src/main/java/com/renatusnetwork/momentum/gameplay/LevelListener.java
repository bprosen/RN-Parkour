package com.renatusnetwork.momentum.gameplay;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.events.EventManager;
import com.renatusnetwork.momentum.data.events.EventType;
import com.renatusnetwork.momentum.data.infinite.InfinitePK;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.races.Race;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import com.renatusnetwork.momentum.utils.dependencies.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class LevelListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {

        Player player = event.getPlayer();
        // In water
        if (event.getTo().getBlock().isLiquid()) {
            PlayerStats playerStats = Momentum.getStatsManager().get(player);

            if (playerStats.getLevel() != null) {

                EventManager eventManager = Momentum.getEventManager();

                // if they are participant and fall into water, eliminate them
                if (eventManager.isEventRunning() &&
                    playerStats.isEventParticipant() &&
                    eventManager.getEventType() == EventType.RISING_WATER &&
                    eventManager.isStartCoveredInWater()) {

                    eventManager.doFireworkExplosion(player.getLocation());
                    eventManager.removeParticipant(player, false);
                    eventManager.addEliminated(player);
                    player.sendMessage(Utils.translate("&7You are &beliminated &7out of the event!"));

                } else if (playerStats.inRace()) {

                    Race race = Momentum.getRaceManager().get(player);
                    if (race != null) {
                        if (race.isPlayer1(player))
                            race.getPlayer1().teleport(race.getRaceLevel().getRaceLocation1());
                        // swap tp to loc 2 if player 2
                        else
                            race.getPlayer2().teleport(race.getRaceLevel().getRaceLocation2());
                    }
                // if they are not spectating anyone, continue
                } else if (!playerStats.isSpectating()) {
                    Level level = playerStats.getLevel();
                    if (level != null && !level.isDropperLevel() && level.doesLiquidResetPlayer()) {

                        // if is elytra level, set gliding to false
                        if (level.isElytraLevel())
                            player.setGliding(false);

                        if (playerStats.hasCurrentCheckpoint() || playerStats.inPracticeMode())
                            Momentum.getCheckpointManager().teleportToCP(playerStats);
                        else
                            LevelHandler.respawnPlayer(playerStats, level);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onStepOnPressurePlate(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        // Start timer
        if (event.getAction().equals(Action.PHYSICAL)) {
            // stone plate = timer start
            if (block.getType() == Material.STONE_PLATE) {

                PlayerStats playerStats = Momentum.getStatsManager().get(player);
                if (playerStats != null && playerStats.inLevel() &&
                    !playerStats.inPracticeMode() && !playerStats.isSpectating() &&
                    !playerStats.hasCurrentCheckpoint()) {

                    // cancel so no click sound and no hogging plate
                    event.setCancelled(true);
                    playerStats.startedLevel();
                }
            } else if (block.getType() == Material.GOLD_PLATE) {
                // gold plate = checkpoint
                PlayerStats playerStats = Momentum.getStatsManager().get(player);
                if (playerStats != null && playerStats.inLevel() && !playerStats.inPracticeMode() && !playerStats.isSpectating()) {
                    // cancel so no click sound and no hogging plate
                    event.setCancelled(true);

                    if (playerStats.hasCurrentCheckpoint())
                    {

                        int blockX = playerStats.getCurrentCheckpoint().getBlockX();
                        int blockZ = playerStats.getCurrentCheckpoint().getBlockZ();

                        if (!(blockX == block.getLocation().getBlockX() && blockZ == block.getLocation().getBlockZ()))
                            setCheckpoint(player, playerStats, block.getLocation());
                    }
                    else
                        setCheckpoint(player, playerStats, block.getLocation());
                }
            } else if (block.getType() == Material.IRON_PLATE) {
                // iron plate = infinite pk or race end
                PlayerStats playerStats = Momentum.getStatsManager().get(player);

                if (playerStats != null) {
                    // cancel so no click sound and no hogging plate
                    event.setCancelled(true);

                    // end if in race
                    if (playerStats.inRace())
                        Momentum.getRaceManager().endRace(player, false);
                    else if (playerStats.isInInfinitePK()) {

                        // prevent double clicking
                        InfinitePK infinitePK = Momentum.getInfinitePKManager().get(player.getName());

                        if (infinitePK.getPressutePlateLoc().getBlockX() == block.getLocation().getBlockX() &&
                                infinitePK.getPressutePlateLoc().getBlockZ() == block.getLocation().getBlockZ()) {

                            block.setType(Material.AIR);
                            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.35f, 2f);
                            Momentum.getInfinitePKManager().doNextJump(playerStats, false);
                        }
                    }
                }
            }
        }
    }

    private void setCheckpoint(Player player, PlayerStats playerStats, Location location) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.25f, 0f);

        // update if they have a cp
        if (playerStats.hasCurrentCheckpoint())
            Momentum.getDatabaseManager().runAsyncQuery("UPDATE checkpoints SET world=?, x=?, y=?, z=? WHERE level_name=? AND uuid=?",
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ(),
                    playerStats.getLevel().getName(),
                    playerStats.getUUID());
        else
            // add to async queue
            Momentum.getDatabaseManager().runAsyncQuery("INSERT INTO checkpoints " +
                    "(uuid, player_name, level_name, world, x, y, z)" +
                    " VALUES ('" +
                    playerStats.getUUID() + "','" +
                    playerStats.getPlayerName() + "','" +
                    playerStats.getLevel().getName() + "','" +
                    location.getWorld().getName() + "','" +
                    location.getBlockX() + "','" +
                    location.getBlockY() + "','" +
                    location.getBlockZ() +
                    "')"
            );

        playerStats.setCurrentCheckpoint(location);
        playerStats.removeCheckpoint(playerStats.getLevel().getName());

        // update if in ascendance realm
        if (location.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().ascendant_realm_world))
        {
            // check region null
            ProtectedRegion region = WorldGuard.getRegion(player.getLocation());
            if (region != null)
            {
                Level level = Momentum.getLevelManager().get(region.getId());

                // make sure the area they are spawning in is a level and not equal
                if (level != null && !level.getName().equalsIgnoreCase(playerStats.getLevel().getName()))
                    playerStats.setLevel(level);
            }
        }

        playerStats.addCheckpoint(playerStats.getLevel().getName(), location);

        String msgString = "&eYour checkpoint has been set";
        if (playerStats.getLevelStartTime() > 0)
        {
            double timeElapsed = System.currentTimeMillis() - playerStats.getLevelStartTime();
            msgString += " &6(" + (timeElapsed / 1000.0) + "s)";
        }

        player.sendMessage(Utils.translate(msgString));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignClick(PlayerInteractEvent event) {
        if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))
             && event.getClickedBlock().getType().equals(Material.WALL_SIGN)
             && !event.getClickedBlock().getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world)) {

            Sign sign = (Sign) event.getClickedBlock().getState();
            String[] signLines = sign.getLines();
            Player player = event.getPlayer();

            if (ChatColor.stripColor(signLines[0]).contains(Momentum.getSettingsManager().signs_first_line) &&
                ChatColor.stripColor(signLines[1]).contains(Momentum.getSettingsManager().signs_second_line_completion)) {

                PlayerStats playerStats = Momentum.getStatsManager().get(player);
                Level level = playerStats.getLevel();

                if (level != null)
                {
                    // check region null
                    ProtectedRegion region = WorldGuard.getRegion(event.getClickedBlock().getLocation());
                    if (region != null)
                    {
                        Level levelTo = Momentum.getLevelManager().get(region.getId());
                        // make sure the area they are spawning in is a level and not equal
                        if (levelTo != null && !levelTo.getName().equalsIgnoreCase(level.getName()))
                        {
                            // if they are glitching elytra -> !elytra, remove elytra!
                            if (level.isElytraLevel() && !levelTo.isElytraLevel())
                                Momentum.getStatsManager().toggleOffElytra(playerStats);

                            playerStats.setLevel(levelTo);
                        }
                    }
                    LevelHandler.levelCompletion(player, playerStats.getLevel().getName());
                }
            } else if (ChatColor.stripColor(signLines[1]).contains(Momentum.getSettingsManager().signs_second_line_spawn)) {
                Location lobby = Momentum.getLocationManager().getLobbyLocation();

                if (lobby != null) {
                    PlayerStats playerStats = Momentum.getStatsManager().get(player);

                    // toggle off elytra armor
                    Momentum.getStatsManager().toggleOffElytra(playerStats);

                    playerStats.resetCurrentCheckpoint();
                    PracticeHandler.resetDataOnly(playerStats);
                    playerStats.resetLevel();
                    playerStats.clearPotionEffects();

                    player.teleport(lobby);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        PlayerStats playerStats = Momentum.getStatsManager().get(player);

        // this is mainly QOL for staff!
        if (playerStats != null && !playerStats.isSpectating() &&
           !playerStats.isEventParticipant() && player.hasPermission("momentum.staff")) {

            // boolean for resetting level
            boolean resetLevel = false;
            ProtectedRegion region = WorldGuard.getRegion(event.getTo());
            if (region != null) {

                // make sure the area they are spawning in is a level
                Level levelTo = Momentum.getLevelManager().get(region.getId());

                if (levelTo != null) {
                    // if player has level and if not same level, then run level change
                    if (playerStats.inLevel() && levelTo.getName().equalsIgnoreCase(playerStats.getLevel().getName()))
                        return;

                    // if they are in a level and have a cp, continue
                    if (playerStats.inLevel() && playerStats.hasCurrentCheckpoint()) {
                        ProtectedRegion currentCPRegion = WorldGuard.getRegion(playerStats.getCurrentCheckpoint());

                        // if the cp region isnt null, continue and get level
                        if (currentCPRegion != null) {
                            Level currentLevel = Momentum.getLevelManager().get(currentCPRegion.getId());

                            // if they cp level isnt null and the cp level is NOT the same as the level theyre teleporting to, save the cp
                            if (currentLevel != null && !currentLevel.getName().equalsIgnoreCase(levelTo.getName()))
                            {
                                playerStats.resetCurrentCheckpoint();

                                // set cp if finds one
                                Location newCP = playerStats.getCheckpoint(levelTo.getName());
                                playerStats.setCurrentCheckpoint(newCP);
                            }
                        }
                    }
                    playerStats.setLevel(levelTo);

                    // enable tutorial if they tp to it and not in tutorial
                    if (levelTo.getName().equalsIgnoreCase(Momentum.getLevelManager().getTutorialLevel().getName()) && !playerStats.isInTutorial())
                        playerStats.setTutorial(true);

                } else if (playerStats.inLevel())
                    resetLevel = true;

            } else if (playerStats.inLevel())
                resetLevel = true;

            if (resetLevel)
            {
                // save checkpoint if had one
                playerStats.resetCurrentCheckpoint();
                playerStats.resetLevel();

                // disable tutorial
                if (playerStats.isInTutorial())
                    playerStats.setTutorial(false);
            }
        }
    }
}