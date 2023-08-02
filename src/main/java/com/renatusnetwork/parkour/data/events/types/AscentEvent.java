package com.renatusnetwork.parkour.data.events.types;

import com.renatusnetwork.parkour.data.levels.Level;
import com.renatusnetwork.parkour.data.levels.LevelsYAML;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class AscentEvent extends Event
{
    private HashMap<Player, Integer> levels;
    private HashMap<Integer, Location> locations;

    public AscentEvent(Level level)
    {
        super(level, "Ascent");

        this.levels = new HashMap<>();
        this.locations = LevelsYAML.getAscentLevelLocations(level.getName());
    }

    public void add(Player player)
    {
        levels.put(player, 1); // default
    }

    public void remove(Player player)
    {
        levels.remove(player);
    }

    public int getLevelID(Player player)
    {
        return levels.get(player);
    }

    public void levelUp(Player player)
    {
        if (levels.containsKey(player))
        {
            int newLevel = levels.get(player) + 1;

            levels.replace(player, newLevel);
            player.teleport(locations.get(newLevel));
        }
    }

    public void levelDown(Player player)
    {
        if (levels.containsKey(player) && levels.get(player) > 1) // min of 1
        {
            int newLevel = levels.get(player) - 1;

            levels.replace(player, newLevel);
            player.teleport(locations.get(newLevel));
        }
    }

    @Override
    public void end()
    {
        // clear
        levels.clear();
        locations.clear();
    }
}