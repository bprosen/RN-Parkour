package com.parkourcraft.Parkour.data.menus;

import com.connorlinfoot.titleapi.TitleAPI;
import com.parkourcraft.Parkour.Parkour;
import com.parkourcraft.Parkour.data.levels.LevelObject;
import com.parkourcraft.Parkour.data.perks.Perk;
import com.parkourcraft.Parkour.data.stats.PlayerStats;
import com.parkourcraft.Parkour.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class MenuItemAction {

    private static void runCommands(Player player, List<String> commands, List<String> consoleCommands) {
        for (String command : commands)
            player.performCommand(command.replace("%player%", player.getName()));

        for (String command : consoleCommands)
            Bukkit.dispatchCommand(
                    Parkour.getPlugin().getServer().getConsoleSender(),
                    command.replace("%player%", player.getName())
            );
    }

    public static void perform(Player player, MenuItem menuItem) {
        String itemType = menuItem.getType();

        if (itemType.equals("perk"))
            performPerkItem(player, menuItem);
        else {
            if (itemType.equals("level"))
                performLevelItem(player, menuItem);
            else if (itemType.equals("teleport"))
                performTeleportItem(player, menuItem);
            else if (itemType.equals("open"))
                performOpenItem(player, menuItem);

            if (menuItem.hasCommands())
                runCommands(player, menuItem.getCommands(), menuItem.getConsoleCommands());
        }
    }

    private static void performPerkItem(Player player, MenuItem menuItem) {
        Perk perk = Parkour.perks.get(menuItem.getTypeValue());

        if (perk != null) {
            PlayerStats playerStas = Parkour.stats.get(player);

            if (menuItem.hasCommands()
                    && perk.hasRequirements(playerStas, player)) {
                player.closeInventory();
                runCommands(player, menuItem.getCommands(), menuItem.getConsoleCommands());
            } else if (!playerStas.hasPerk(perk.getName())
                    && perk.getPrice() > 0) {
                int playerBalance = (int) Parkour.economy.getBalance(player);

                if (playerBalance > perk.getPrice()) {
                    Parkour.economy.withdrawPlayer(player, perk.getPrice());
                    Parkour.perks.bought(playerStas, perk);
                    Parkour.menus.updateInventory(player, player.getOpenInventory());
                }
            }
        }
    }

    private static void performLevelItem(Player player, MenuItem menuItem) {
        PlayerStats playerStats = Parkour.stats.get(player);
        LevelObject level = Parkour.levels.get(menuItem.getTypeValue());

        if (level.hasRequiredLevels(playerStats)) {
            player.closeInventory();
            player.teleport(level.getStartLocation());

            player.sendMessage(
                    ChatColor.GRAY + "You were teleported to the beginning of "
                            + level.getFormattedTitle()
            );

            TitleAPI.sendTitle(
                    player, 10, 40, 10,
                    "",
                    level.getFormattedTitle()
            );
        }
    }

    private static void performTeleportItem(Player player, MenuItem menuItem) {
        Location location = Parkour.locations.get(menuItem.getTypeValue());

        if (location != null) {
            player.closeInventory();
            player.teleport(location);
        }
    }

    private static void performOpenItem(Player player, MenuItem menuItem) {
        Menu menu = Parkour.menus.getMenuFromStartingChars(menuItem.getTypeValue());

        if (menu != null) {
            int pageeNumber = Utils.getTrailingInt(menuItem.getTypeValue());

            Inventory inventory = Parkour.menus.getInventory(menu.getName(), pageeNumber);

            if (inventory != null) {
                player.closeInventory();
                player.openInventory(inventory);
                Parkour.menus.updateInventory(player, player.getOpenInventory(), menu.getName(), pageeNumber);
            }
        }
    }

































}
