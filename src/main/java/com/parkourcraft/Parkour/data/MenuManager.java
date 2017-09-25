package com.parkourcraft.Parkour.data;

import com.parkourcraft.Parkour.data.menus.Menu;
import com.parkourcraft.Parkour.data.menus.Menus_YAML;
import com.parkourcraft.Parkour.data.stats.PlayerStats;
import com.parkourcraft.Parkour.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuManager {

    private static Map<String, Menu> menuMap = new HashMap<>();

    public static void load(String menuName) {
        if (Menus_YAML.exists(menuName))
            menuMap.put(menuName, new Menu(menuName));
    }

    public static void loadMenus() {
        menuMap = new HashMap<>();

        for (String menuName : Menus_YAML.getNames())
            load(menuName);
    }

    public static boolean exists(String menuName) {
        return menuMap.containsKey(menuName);
    }

    public static Menu getMenu(String menuName) {
        return menuMap.get(menuName);
    }

    public static Menu getMenuFromStartingChars(String input) {
        for (Menu menu : menuMap.values())
            if (input.startsWith(menu.getName()))
                return menu;

        return null;
    }

    public static Menu getMenuFromTitle(String menuTitle) {
        for (Menu menu : menuMap.values())
            if (menuTitle.startsWith(menu.getFormattedTitle()))
                return menu;

        return null;
    }

    public static Menu getMenuFromSelectItem(ItemStack item) {
        if (item != null)
            for (Menu menu : menuMap.values())
                if (menu.getSelectItem().getType().equals(item.getType()))
                    return menu;

        return null;
    }

    public static List<String> getMenuNames() {
        return new ArrayList<>(menuMap.keySet());
    }

    public static Inventory getInventory(String menuName, int pageNumber) {
        if (exists(menuName))
            return menuMap.get(menuName).getInventory(pageNumber);

        return null;
    }

    public static void updateInventory(PlayerStats playerStats, InventoryView inventory) {
        Menu menu = getMenuFromTitle(inventory.getTitle());

        if (menu != null)
            menu.updateInventory(playerStats, inventory, Utils.getTrailingInt(inventory.getTitle()));
    }

    public static void updateInventory(PlayerStats playerStats, InventoryView inventory, String menuName) {
        if (exists(menuName))
            menuMap.get(menuName).updateInventory(playerStats, inventory, 1);
    }

    public static void updateOpenInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryView inventoryView = player.getOpenInventory();

            if (inventoryView != null) {
                Menu menu = getMenuFromTitle(inventoryView.getTitle());

                if (menu != null) {
                    PlayerStats playerStats = StatsManager.get(player);

                    updateInventory(playerStats, inventoryView, menu.getName());
                }
            }

        }
    }

}