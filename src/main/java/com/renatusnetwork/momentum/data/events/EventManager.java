package com.renatusnetwork.momentum.data.events;

import com.connorlinfoot.titleapi.TitleAPI;
import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.api.ParkourEventEndEvent;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.storage.mysql.DatabaseQueries;
import com.renatusnetwork.momentum.utils.Utils;
import com.renatusnetwork.momentum.utils.dependencies.WorldGuard;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private Event runningEvent = null;
    private Player winner = null;
    private BukkitTask maxRunTimer;
    private BukkitTask reminderTimer;
    private HashMap<String, EventParticipant> participants = new HashMap<>();
    private Set<String> eliminated = new HashSet<>();
    // start millis according to system
    private long startTime = 0L;
    private HashMap<Integer, EventLBPosition> eventLeaderboard = new HashMap<>(Momentum.getSettingsManager().max_event_leaderboard_size);

    public EventManager() {
        startScheduler();
    }

    // scheduler to handle the next event and reminder for running event
    public void startScheduler() {
        // run a timer scheduler for next event
        new BukkitRunnable() {
            @Override
            public void run() {
                // check if there is enough people online and an event isnt running
                if (runningEvent == null &&
                    Bukkit.getOnlinePlayers().size() >= Momentum.getSettingsManager().min_players_online) {

                    // get random type from list
                    EventType[] eventTypes = EventType.values();
                    Random ran = new Random();
                    EventType eventType = eventTypes[ran.nextInt(eventTypes.length)];

                    startEvent(eventType);
                }
            }
        }.runTaskTimer(Momentum.getPlugin(), 20 * Momentum.getSettingsManager().check_next_event_delay,
                                           20 * Momentum.getSettingsManager().check_next_event_delay);

        // update global event wins every 3 mins
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                loadLeaderboard();
            }
        }.runTaskTimerAsynchronously(Momentum.getPlugin(), 20 * 60, 20 * 180);
    }

    public void loadLeaderboard() {
        try {

            HashMap<Integer, EventLBPosition> leaderboard = eventLeaderboard;
            leaderboard.clear();

            List<Map<String, String>> winResults = DatabaseQueries.getResults(
                    "players",
                    "uuid, player_name, event_wins",
                    " WHERE event_wins > 0" +
                            " ORDER BY event_wins DESC" +
                            " LIMIT " + Momentum.getSettingsManager().max_event_leaderboard_size);

            int leaderboardPos = 1;

            for (Map<String, String> winResult : winResults) {
                leaderboard.put(leaderboardPos,
                        new EventLBPosition(
                                winResult.get("uuid"), winResult.get("player_name"), Integer.parseInt(winResult.get("event_wins"))
                        ));

                leaderboardPos++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // method to start event
    public void startEvent(EventType eventType) {
        runningEvent = new Event(eventType);
        startTime = System.currentTimeMillis();

        Bukkit.broadcastMessage("");
        String joinEvent = Utils.translate("&7A &b" + formatName(runningEvent.getEventType()) +
                        " Event &7has begun! &cClick here to join!");

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(joinEvent));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Utils.translate("&bClick to join!"))));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/event join"));

        Bukkit.spigot().broadcast(component);
        Bukkit.broadcastMessage("");

        // start max time timer
        startTimers();
    }

    // method to start the timer
    private void startTimers() {
        maxRunTimer = new BukkitRunnable() {
            @Override
            public void run() {

                if (runningEvent != null)
                    endEvent(null,false, true);
            }
        }.runTaskLater(Momentum.getPlugin(), 20 * Momentum.getSettingsManager().max_event_run_time);

        // run a timer scheduler for reminder to join running event
        reminderTimer = new BukkitRunnable() {
            @Override
            public void run() {
                long endTimeMillis = startTime + (Momentum.getSettingsManager().max_event_run_time * 1000);

                // if the event is running and the end time will be in sync to when the reminder broadcast is, dont do it
                if (runningEvent != null &&
                   (endTimeMillis - (20 * 1000 * Momentum.getSettingsManager().event_reminder_delay)) < System.currentTimeMillis()) {

                    Bukkit.broadcastMessage("");
                    String stillRunning = Utils.translate("&7A &b" + formatName(runningEvent.getEventType()) +
                            " Event &7is still running! &cClick here to join!");

                    TextComponent component = new TextComponent(TextComponent.fromLegacyText(stillRunning));
                    component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Utils.translate("&bClick to join!"))));
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/event join"));

                    Bukkit.spigot().broadcast(component);

                    Bukkit.broadcastMessage("");
                }
            }
        }.runTaskTimer(Momentum.getPlugin(), 20 * Momentum.getSettingsManager().event_reminder_delay,
                                           20 * Momentum.getSettingsManager().event_reminder_delay);
    }

    // method to end event
    public void endEvent(Player winner, boolean forceEnded, boolean ranOutOfTime)
    {
        ParkourEventEndEvent parkourEventEndEvent = new ParkourEventEndEvent(winner, runningEvent.getLevel().getReward());
        Bukkit.getPluginManager().callEvent(parkourEventEndEvent);

        if (!parkourEventEndEvent.isCancelled())
        {
            this.winner = winner;

            // cancel schedulers first
            runningEvent.getScheduler().cancel();
            maxRunTimer.cancel();
            reminderTimer.cancel();

            // then remove all participants
            removeAllParticipants(false);
            // clear eliminated list
            eliminated.clear();
            // clear all water if it was the rising water event
            clearWater();

            if (winner != null)
            {
                PlayerStats playerStats = Momentum.getStatsManager().get(winner);

                // give higher reward if prestiged
                int prestiges = playerStats.getPrestiges();
                int reward = parkourEventEndEvent.getReward();

                if (prestiges > 0 && reward > 0)
                    reward *= playerStats.getPrestigeMultiplier();

                Momentum.getStatsManager().addCoins(playerStats, reward);
                Momentum.getStatsManager().runGGTimer();

                // update wins
                playerStats.addEventWin();
                Momentum.getDatabaseManager().runAsyncQuery("UPDATE players SET event_wins=" + playerStats.getEventWins() + " WHERE uuid='" + playerStats.getUUID() + "'");
            }

            if (forceEnded)
                Bukkit.broadcastMessage(Utils.translate("&7A &b" + formatName(runningEvent.getEventType())
                        + " &7Event has been force ended!"));
            else if (ranOutOfTime)
                Bukkit.broadcastMessage(Utils.translate("&7A &b" + formatName(runningEvent.getEventType())
                        + " &7Event has gone on too long! Nobody beat it in time :("));
            else {
                Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(Utils.translate("&7A &b" + formatName(runningEvent.getEventType())
                        + " &7Event has ended! &b&l" + winner.getDisplayName() + " &7has won!"));
                Bukkit.broadcastMessage("");
            }

            // null the running event last
            runningEvent = null;
        }
    }

    public Event getRunningEvent() {
        return runningEvent;
    }

    public EventType getEventType() {
        return runningEvent.getEventType();
    }

    public boolean isEventRunning() {
        return runningEvent != null;
    }

    /*
        Event Participant Section
     */
    public EventParticipant get(Player player) {
        return participants.get(player.getName());
    }

    public boolean isParticipant(Player player) {
        return participants.containsKey(player.getName());
    }

    public void addParticipant(Player player) {

        PlayerStats playerStats = Momentum.getStatsManager().get(player);

        // save checkpoint
        playerStats.resetCurrentCheckpoint();

        // toggle off if saved
        Momentum.getStatsManager().toggleOffElytra(playerStats);

        EventParticipant eventParticipant = new EventParticipant(player, playerStats.getLevel());
        participants.put(player.getName(), eventParticipant);
        playerStats.setLevel(runningEvent.getLevel());
        playerStats.disableLevelStartTime();
        playerStats.joinedEvent();
        player.teleport(runningEvent.getLevel().getStartLocation());

        // remove active effects
        playerStats.clearPotionEffects();
    }

    public void removeParticipant(Player player, boolean disconnected) {
        EventParticipant eventParticipant = get(player);

        PlayerStats playerStats = Momentum.getStatsManager().get(player);

        if (!disconnected && eventParticipant.getOriginalLevel() != null)
        {
            Location location = playerStats.getCheckpoint(eventParticipant.getOriginalLevel().getName());

            // reset the cache and teleport player back
            if (location != null)
                playerStats.setCurrentCheckpoint(location);
        }

        // if their original level is not null, then set it, if it is, do region lookup of their original location jic
        if (eventParticipant.getOriginalLevel() != null)
            playerStats.setLevel(eventParticipant.getOriginalLevel());
        else {
            // region lookup here
            ProtectedRegion region = WorldGuard.getRegion(eventParticipant.getOriginalLocation());
            if (region != null) {

                // level lookup here
                Level level = Momentum.getLevelManager().get(region.getId());

                // make sure the area they are spawning in is a level
                if (level != null)
                    playerStats.setLevel(level);
                else
                    playerStats.resetLevel();
            } else {
                playerStats.resetLevel();
            }
        }

        playerStats.leftEvent();
        player.teleport(eventParticipant.getOriginalLocation());
        player.setHealth(20.0);

        if (!disconnected && winner != null)
            TitleAPI.sendTitle(eventParticipant.getPlayer(), 10, 80, 10,
                    Utils.translate("&c" + winner.getDisplayName() + " &7has won the &2&l" +
                                         formatName(runningEvent.getEventType()) + " &7Event"));

        // set back if they came from elytra level
        if (playerStats.inLevel() && playerStats.getLevel().isElytraLevel())
            Momentum.getStatsManager().toggleOnElytra(playerStats);

        participants.remove(player.getName());
    }

    public void removeAllParticipants(boolean shutdown) {

        Set<EventParticipant> tempList = new HashSet<>();

        // create a DEEP copy of the list so no concurrent errors
        for (EventParticipant eventParticipant : participants.values())
            tempList.add(eventParticipant);

        // now remove so theres no concurrency problem
        for (EventParticipant participant : tempList)
            removeParticipant(participant.getPlayer(), shutdown);

        // null winner once all participants have been handled
        winner = null;
    }

    public HashMap<String, EventParticipant> getParticipants() {
        return participants;
    }

    public Set<String> getEliminated() {
        return eliminated;
    }

    public boolean isEliminated(Player player) {
        return eliminated.contains(player.getName());
    }

    public void addEliminated(Player player) {
        eliminated.add(player.getName());
    }

    public void removeEliminated(Player player) {
        eliminated.remove(player.getName());
    }

    public void doFireworkExplosion(Location location) {

        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.clearEffects();

        // build the firework and then set the new one
        FireworkEffect effect = FireworkEffect.builder()
                .flicker(false)
                .trail(false)
                .with(FireworkEffect.Type.BURST)
                .withColor(Color.RED)
                .withFade(Color.RED)
                .build();

        meta.addEffect(effect);
        firework.setFireworkMeta(meta);

        new BukkitRunnable() {
            public void run() {
                firework.detonate();
            }
        }.runTaskLater(Momentum.getPlugin(), 1);
    }

    private void clearWater() {

        if (isEventRunning() &&
            runningEvent.getLevel() != null &&
            runningEvent.getLevelRegion() != null &&
            runningEvent.getEventType() == EventType.RISING_WATER) {

            BlockVector maxPoint = runningEvent.getLevelRegion().getMaximumPoint().toBlockPoint();
            BlockVector minPoint = runningEvent.getLevelRegion().getMinimumPoint().toBlockPoint();
            int minX = minPoint.getBlockX();
            int maxX = maxPoint.getBlockX();
            int minZ = minPoint.getBlockZ();
            int maxZ = maxPoint.getBlockZ();

            WorldEdit api = WorldEdit.getInstance();

            if (api != null) {
                LocalWorld world = new BukkitWorld(runningEvent.getLevel().getStartLocation().getWorld());
                Vector pos1 = new Vector(minX, 0, minZ);
                Vector pos2 = new Vector(maxX, 255, maxZ);
                CuboidRegion selection = new CuboidRegion(world, pos1, pos2);

                try {
                    // enable fast mode to do it w/o lag, then quickly disable fast mode once queue flushed
                    EditSession editSession = api.getEditSessionFactory().getEditSession(world, -1);
                    editSession.setFastMode(true);

                    // create single base block set for replace
                    Set<BaseBlock> baseBlockSet = new HashSet<BaseBlock>() {{ add(new BaseBlock(Material.WATER.getId())); }};

                    editSession.replaceBlocks(selection, baseBlockSet, new BaseBlock(Material.AIR.getId()));
                    editSession.flushQueue();
                    editSession.setFastMode(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Momentum.getPluginLogger().info("WorldEdit API found null in Event clearWater()");
            }
        }
    }

    public boolean isStartCoveredInWater() {

        boolean isCovered = false;

        if (isEventRunning() &&
            runningEvent.getLevel() != null &&
            runningEvent.getEventType() == EventType.RISING_WATER) {

            Location startLoc = getRunningEvent().getLevel().getStartLocation();

            if (startLoc.clone().getBlock().getType() == Material.WATER ||
                startLoc.clone().add(0, 1, 0).getBlock().getType() == Material.WATER)
                isCovered = true;
        }
        return isCovered;
    }

    /*
        Misc Utilities
     */
    public String formatName(EventType eventType) {
        if (eventType == EventType.PVP)
            return "PvP";
        else if (eventType == EventType.FALLING_ANVIL)
            return "Falling Anvil";
        else if (eventType == EventType.RISING_WATER)
            return "Rising Water";

        return null;
    }

    public long getTimeLeftMillis() {
        long futureEndTime = startTime + (Momentum.getSettingsManager().max_event_run_time * 1000);
        return futureEndTime - System.currentTimeMillis();
    }

    public void shutdown() {
        if (isEventRunning()) {
            runningEvent.getScheduler().cancel();
            removeAllParticipants(true);
            clearWater();
            runningEvent = null;
        }
    }

    public HashMap<Integer, EventLBPosition> getEventLeaderboard()
    {
        return eventLeaderboard;
    }
}
