package com.renatusnetwork.momentum.data.perks;

import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class Perk {

    private String name;
    private String title;
    private HashMap<String, ItemStack> items;
    private Material infinitePKBlock;

    private List<String> setRequirementsLore;
    private List<String> requirements;
    private List<String> requiredPermissions;

    private int price;
    private int ID = -1;

    public Perk(String perkName) {
        name = perkName;

        load();
    }

    public void load() {
        if (PerksYAML.exists(name)) {
            title = PerksYAML.getTitle(name);
            items = PerksYAML.getItems(name);
            requirements = PerksYAML.getRequirements(name);
            requiredPermissions = PerksYAML.getRequiredPermissions(name);
            price = PerksYAML.getPrice(name);
            setRequirementsLore = PerksYAML.getSetRequirementsLore(name);
            infinitePKBlock = PerksYAML.getInfinitePKBlock(name);
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

    public boolean isInfinitePKBlock() { return infinitePKBlock != null; }

    public Material getInfinitePKBlock() { return infinitePKBlock; }

    public String getFormattedTitle() {
        return Utils.translate(title);
    }

    public HashMap<String, ItemStack> getItems() { return items; }

    public List<String> getRequirements() {
        return requirements;
    }

    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public List<String> getSetRequirementsLore() { return setRequirementsLore; }

    public boolean hasRequiredPermissions(Player player)
    {
        boolean perk = false;

        if (!requiredPermissions.isEmpty())
        {
            perk = true;

            for (String requiredPermission : requiredPermissions)
                if (!player.hasPermission(requiredPermission))
                {
                    perk = false;
                    break;
                }
        }
        return perk;
    }

    public boolean hasSetRequirementsLore() { return setRequirementsLore != null; }
    public int getPrice() {
        return price;
    }

    public int getID() {
        return ID;
    }

    public boolean hasRequirements(PlayerStats playerStats, Player player)
    {
        if (player.isOp())
            return true;

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
