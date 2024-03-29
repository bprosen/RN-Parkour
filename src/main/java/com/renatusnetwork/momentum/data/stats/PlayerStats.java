package com.renatusnetwork.momentum.data.stats;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.clans.Clan;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.ranks.Rank;
import com.renatusnetwork.momentum.utils.Utils;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PlayerStats {

    private Player player;
    private String UUID;
    private String playerName;
    private double coins;
    private Level level = null;
    private int playerID = -1;
    private long levelStartTime = 0;
    private boolean spectatable;
    private PlayerStats playerToSpectate;
    private Clan clan;
    private Location currentCheckpoint = null;
    private Location practiceSpawn = null;
    private Location spectateSpawn = null;
    private boolean inRace = false;
    private Rank rank;
    private ItemStack chestplateSavedFromElytra = null;
    private int prestiges = 0;
    private int raceWins = 0;
    private int raceLosses = 0;
    private int ratedLevelsCount;
    private int records;
    private int gainedPerksCount = 0;
    private float raceWinRate = 0.00f;
    private float prestigeMultiplier = 1.00f;
    private int rankUpStage;
    private int individualLevelsBeaten;
    private int infinitePKScore = 0;
    private boolean inInfinitePK = false;
    private boolean eventParticipant = false;
    private boolean bypassingPlots = false;
    private int totalLevelCompletions = 0;
    private boolean nightVision = false;
    private boolean grinding = false;
    private int eventWins;
    private Material infiniteBlock;
    private boolean inTutorial = false;
    private boolean failsToggled;
    private int fails;

    private FastBoard board;

    private HashMap<String, Set<LevelCompletion>> levelCompletionsMap = new HashMap<>();
    private HashMap<String, Long> perks = new HashMap<>();
    private HashMap<String, Location> checkpoints = new HashMap<>();
    private HashSet<String> boughtLevels = new HashSet<>();
    private HashMap<String, Location> saves = new HashMap<>();

    public PlayerStats(Player player) {
        this.player = player;
        this.UUID = player.getUniqueId().toString();
        this.playerName = player.getName();

        board = new FastBoard(player); // load board
        board.updateTitle(Utils.translate("&c&lRenatus Network"));
    }

    //
    // Player Info Section
    //
    public boolean isLoaded() {
        return playerID > 0;
    }

    public Player getPlayer() {
        return player;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getUUID() {
        return UUID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    public FastBoard getBoard() { return board; }

    public int getPlayerID() {
        return playerID;
    }

    public boolean hasNVStatus() {return nightVision; }

    public void setNVStatus(boolean nightVision) { this.nightVision = nightVision; }

    public boolean isInTutorial() { return inTutorial; }

    public void setTutorial(boolean tutorial) { inTutorial = tutorial; }

    //
    // Coins Sections
    //
    public double getCoins()
    {
        return coins;
    }

    public void setCoins(double coins)
    {
        if (coins < 0)
            coins = 0;

        this.coins = coins;
    }

    public void addCoins(double coins)
    {
        this.coins += coins;
    }

    public void removeCoins(double coins)
    {
        this.coins -= coins;

        // no allowing negative numbers, NO DEBT
        if (this.coins < 0)
            this.coins = 0;
    }

    //
    // Race Section
    //
    public void startedRace() {
        inRace = true;
    }

    public void endedRace() {
        inRace = false;
    }

    public boolean inRace() {
        return inRace;
    }

    public int getRaceWins() { return raceWins; }

    public void setRaceWins(int raceWins) { this.raceWins = raceWins; }

    public int getRaceLosses() { return raceLosses; }

    public void setRaceLosses(int raceLosses) { this.raceLosses = raceLosses; }

    public float getRaceWinRate() { return raceWinRate; }

    public void setRaceWinRate(float raceWinRate) { this.raceWinRate = raceWinRate; }

    //
    // Level Section
    //
    public void setLevel(Level level) {
        this.level = level;
    }

    public void resetLevel() {
        level = null;
    }

    public Level getLevel() {
        return level;
    }

    public boolean inLevel() {
        return level != null;
    }

    public void startedLevel() {
        levelStartTime = System.currentTimeMillis();
    }

    public void disableLevelStartTime() {
        levelStartTime = 0;
    }

    public long getLevelStartTime() {
        return levelStartTime;
    }

    public int getTotalLevelCompletions() { return totalLevelCompletions; }

    public void setTotalLevelCompletions(int totalLevelCompletions) { this.totalLevelCompletions = totalLevelCompletions; }

    public void setIndividualLevelsBeaten(int individualLevelsBeaten) { this.individualLevelsBeaten = individualLevelsBeaten; }

    public int getIndividualLevelsBeaten() { return individualLevelsBeaten; }

    public ItemStack getChestplateSavedFromElytra() { return chestplateSavedFromElytra; }

    public void setChestplateSavedFromElytra(ItemStack chestplate) { chestplateSavedFromElytra = chestplate; }

    public int getRecords() { return records; }

    public void setRecords(int records) { this.records = records; }

    public boolean hasBoughtLevel(String levelName)
    {
        return boughtLevels.contains(levelName);
    }

    public void buyLevel(String levelName)
    {
        boughtLevels.add(levelName);
    }

    public void removeBoughtLevel(String levelName) { boughtLevels.remove(levelName); }

    public void setBoughtLevels(HashSet<String> levels) { boughtLevels = levels ; }

    //
    // Spectator Section
    //
    public void setSpectateSpawn(Location spectateSpawn) {
        this.spectateSpawn = spectateSpawn;
    }

    public void resetSpectateSpawn() { spectateSpawn = null; }

    public Location getSpectateSpawn() {
        return spectateSpawn;
    }

    public void setSpectatable(boolean setting) {
        spectatable = setting;
    }

    public boolean isSpectatable() {
        if (playerToSpectate != null)
            return false;
        return spectatable;
    }

    public void setPlayerToSpectate(PlayerStats playerStats) {
        playerToSpectate = playerStats;
    }

    public PlayerStats getPlayerToSpectate() {
        return playerToSpectate;
    }

    public boolean isSpectating() { return playerToSpectate != null; }

    //
    // InfinitePK Section
    //
    public void setInfinitePKScore(int infinitePKScore) {
        this.infinitePKScore = infinitePKScore;
    }

    public int getInfinitePKScore() {
        return infinitePKScore;
    }

    public void setInfinitePK(boolean inInfinitePK) { this.inInfinitePK = inInfinitePK; }

    public boolean isInInfinitePK() {
        return inInfinitePK;
    }

    public Material getInfiniteBlock() { return infiniteBlock; }

    public void setInfiniteBlock(Material infiniteBlock) { this.infiniteBlock = infiniteBlock; }

    //
    // Rank Section
    //
    public void setRank(Rank rank) {
        this.rank = rank;
    }

    public Rank getRank() {
        return rank;
    }

    public boolean isLastRank() {
        // get if they are at last rank
        return rank != null && rank.getRankId() == Momentum.getRanksManager().getRankList().size();
    }

    public void setRankUpStage(int rankUpStage) {
        this.rankUpStage = rankUpStage;
    }

    public int getRankUpStage() {
        return rankUpStage;
    }

    public int getPrestiges() { return prestiges; }

    public void addPrestige() { prestiges++; }

    public void setPrestiges(int prestiges) { this.prestiges = prestiges; }

    public float getPrestigeMultiplier() { return prestigeMultiplier; }

    public void setPrestigeMultiplier(float prestigeMultiplier) { this.prestigeMultiplier = prestigeMultiplier; }

    //
    // Fails Section
    //
    public void setFailMode(boolean failsToggled) { this.failsToggled = failsToggled; }
    public boolean inFailMode() { return failsToggled; }

    public int getFails() { return fails; }

    public void addFail()
    {
        if (failsToggled && !inInfinitePK && !inTutorial && !inRace && !eventParticipant)
            fails++;
    }

    public void resetFails() { fails = 0; }

    //
    // Practice Mode Section
    //
    public void setPracticeMode(Location loc) {
        practiceSpawn = loc;
    }

    public void resetPracticeMode() {
        practiceSpawn = null;
    }

    public Location getPracticeLocation() {
        return practiceSpawn;
    }

    public boolean inPracticeMode() { return practiceSpawn != null; }

    //
    // Clan Section
    //
    public void setClan(Clan clan) {
        this.clan = clan;
    }

    public Clan getClan() {
        return clan;
    }

    public void resetClan() { clan = null; }

    //
    // Checkpoint Section
    //
    public void setCurrentCheckpoint(Location location)
    {
        currentCheckpoint = location;
    }

    public Location getCurrentCheckpoint()
    {
        return currentCheckpoint;
    }

    public boolean hasCurrentCheckpoint()
    {
        return currentCheckpoint != null;
    }

    public void resetCurrentCheckpoint()
    {
        currentCheckpoint = null;
    }

    public void addCheckpoint(String levelName, Location location)
    {
        checkpoints.put(levelName, location);
    }

    public void removeCheckpoint(String levelName)
    {
        checkpoints.remove(levelName);
    }

    public Location getCheckpoint(String levelName)
    {
        return checkpoints.get(levelName);
    }

    public HashMap<String, Location> getCheckpoints() { return checkpoints; }

    //
    // Saves Section
    //
    public Location getSave(String levelName)
    {
        return saves.get(levelName);
    }

    public boolean hasSave(String levelName)
    {
        return saves.containsKey(levelName);
    }

    public void removeSave(String levelName)
    {
        saves.remove(levelName);
    }

    public void addSave(String levelName, Location location)
    {
        saves.put(levelName, location);
    }

    //
    // Completions Section
    //
    public String getMostCompletedLevel() {
        int mostCompletions = -1;
        String mostCompletedLevel = null;

        for (Map.Entry<String, Set<LevelCompletion>> entry : levelCompletionsMap.entrySet())
            if (entry.getValue().size() > mostCompletions) {
                mostCompletions = entry.getValue().size();
                mostCompletedLevel = entry.getKey();
            }

        if (mostCompletions > 0)
            return mostCompletedLevel;

        return null;
    }

    public void levelCompletion(String levelName, LevelCompletion levelCompletion) {
        if (levelName != null && levelCompletion != null) {
            if (!levelCompletionsMap.containsKey(levelName))
                levelCompletionsMap.put(levelName, new HashSet<>());

            if (levelCompletionsMap.get(levelName) != null)
                levelCompletionsMap.get(levelName).add(levelCompletion);
        }
    }

    public void levelCompletion(String levelName, long timeOfCompletion, long completionTimeElapsed) {
        this.levelCompletion(levelName, new LevelCompletion(timeOfCompletion, completionTimeElapsed));
    }

    public HashMap<String, Set<LevelCompletion>> getLevelCompletionsMap() {
        return levelCompletionsMap;
    }

    public int getLevelCompletionsCount(String levelName) {
        if (levelCompletionsMap.containsKey(levelName))
            return levelCompletionsMap.get(levelName).size();

        return 0;
    }

    // fastest completion
    public LevelCompletion getQuickestCompletion(String levelName) {
        LevelCompletion fastestCompletion = null;

        if (levelCompletionsMap.containsKey(levelName)) {
            // loop through to find fastest completion
            for (LevelCompletion levelCompletion : levelCompletionsMap.get(levelName))
                // if not null and not including not timed levels, continue
                if (levelCompletion != null && levelCompletion.getCompletionTimeElapsed() > 0)
                    // if null or faster than already fastest completion, set to new completion
                    if (fastestCompletion == null || (levelCompletion.getCompletionTimeElapsed() < fastestCompletion.getCompletionTimeElapsed()))
                        fastestCompletion = levelCompletion;
        }
        return fastestCompletion;
    }

    //
    // Perks Section
    //
    public void addPerk(String perkName, Long time) {
        perks.put(perkName, time);
    }

    public boolean hasPerk(String perkName) {
        return perks.containsKey(perkName);
    }

    public int getGainedPerksCount() { return gainedPerksCount; }

    public void setGainedPerksCount(int gainedPerksCount) { this.gainedPerksCount = gainedPerksCount; }

    public HashMap<String, Long> getPerks() { return perks; }

    //
    // Event Section
    //
    public boolean isEventParticipant() {
        return eventParticipant;
    }

    public void joinedEvent() {
        eventParticipant = true;
    }

    public void leftEvent() {
        eventParticipant = false;
    }

    public void setEventWins(int eventWins) { this.eventWins = eventWins; }

    public void addEventWin() { eventWins++; }

    public int getEventWins() { return eventWins; }

    //
    // Rated Levels Section
    //
    public void setRatedLevelsCount(int ratedLevelsCount) { this.ratedLevelsCount = ratedLevelsCount; }

    public int getRatedLevelsCount() { return ratedLevelsCount; }

    //
    // Plots Sections
    //
    public void setPlotBypassing(boolean bypassingPlots) {
        this.bypassingPlots = bypassingPlots;
    }

    public boolean isBypassingPlots() { return bypassingPlots; }

    //
    // Grinding Section
    //
    public boolean isGrinding() {
        return grinding;
    }

    public void toggleGrinding() {
        grinding = !grinding;
    }

    //
    // Misc
    //
    public void clearPotionEffects() {

        for (PotionEffect potionEffect : player.getActivePotionEffects())

            // dont remove effect if its night vision, and they have it enabled
            if (!(potionEffect.getType().equals(PotionEffectType.NIGHT_VISION) && nightVision))
                player.removePotionEffect(potionEffect.getType());
    }
}