package com.parkourcraft.Parkour.data.perks;

import com.parkourcraft.Parkour.data.stats.PlayerStats;
import com.parkourcraft.Parkour.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Perk {

    private String name;
    private String title;
    private List<String> permissions;
    private List<String> requirements;
    private List<String> requiredPermissions;

    private int price;
    private int ID = -1;

    public Perk(String perkName) {
        name = perkName;

        load();
    }

    public void load() {
        if (Perks_YAML.exists(name)) {
            title = Perks_YAML.getTitle(name);
            permissions = Perks_YAML.getPermissions(name);
            requirements = Perks_YAML.getRequirements(name);
            requiredPermissions = Perks_YAML.getRequiredPermissions(name);
            price = Perks_YAML.getPrice(name);
        }
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getFormattedTitle() {
        return Utils.translate(title);
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getRequirements() {
        return requirements;
    }

    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public int getPrice() {
        return price;
    }

    public int getID() {
        return ID;
    }

    public boolean hasRequirements(PlayerStats playerStats, Player player) {
        for (String requiredPermission : requiredPermissions)
            if (!player.hasPermission(requiredPermission))
                return false;

        for (String levelRequirement : requirements)
            if (playerStats.getLevelCompletionsCount(levelRequirement) < 1)
                return false;

        if (price > 0)
            if (!playerStats.hasPerk(name))
                return false;

        return true;
    }

}
