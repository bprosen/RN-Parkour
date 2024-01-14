package com.renatusnetwork.parkour.data.menus;

import com.renatusnetwork.parkour.Parkour;
import com.renatusnetwork.parkour.data.levels.Level;
import com.renatusnetwork.parkour.data.levels.RaceLevel;
import com.renatusnetwork.parkour.data.plots.Plot;
import com.renatusnetwork.parkour.data.races.RaceManager;
import com.renatusnetwork.parkour.data.stats.PlayerStats;
import com.renatusnetwork.parkour.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class MenuManager {

    private HashMap<String, Menu> menus;
    private HashMap<String, CancelTasks> cancelTasks;

    public MenuManager()
    {
        this.menus = new HashMap<>();
        this.cancelTasks = new HashMap<>();
        load();
    }

    public void load()
    {
        for (String menuName : MenusYAML.getNames())
            load(menuName);

        Parkour.getPluginLogger().info("Menus loaded: " + menus.size());
    }

    public void load(String menuName)
    {
        if (MenusYAML.exists(menuName))
            menus.put(menuName, new Menu(menuName));
    }

    public List<Menu> getMenus()
    {
        List<Menu> menuArray = new ArrayList<>();

        // add menus to list
        for (Menu menu : menus.values())
            if (menu != null)
                menuArray.add(menu);

        return menuArray;
    }

    public boolean exists(String menuName) {
        return menus.containsKey(menuName);
    }

    public CancelTasks getCancelTasks(String playerName)
    {
        return cancelTasks.get(playerName);
    }

    public boolean hasCancelTasks(String playerName)
    {
        return cancelTasks.containsKey(playerName);
    }

    public void removeCancelTasks(String playerName)
    {
        cancelTasks.remove(playerName);
    }

    public boolean hasCancelledItem(String playerName, int slot)
    {
        boolean result = false;

        CancelTasks cancelTask = cancelTasks.get(playerName);

        if (cancelTask != null)
            result = cancelTask.hasItemInSlot(slot);

        return result;
    }

    public void addActiveCancel(String playerName, Inventory inventory, int slot, ItemStack itemStack, BukkitTask task) {

        CancelTasks menu = cancelTasks.get(playerName);

        // if non null, add task
        if (menu != null)
            menu.addSlot(slot, itemStack, task);
        else
        {
            CancelTasks cancelled = new CancelTasks(inventory);
            cancelled.addSlot(slot, itemStack, task);

            cancelTasks.put(playerName, cancelled);
        }
    }

    public Menu getMenu(String menuName) {
        return menus.get(menuName);
    }

    public Menu getMenuFromStartingChars(String input) {
        for (Menu menu : menus.values())
            if (input.startsWith(menu.getName()))
                return menu;

        return null;
    }

    public Menu getMenuFromTitle(String menuTitle) {
        // need to make exception for rate level menu as the title gets changed to replace a placeholder
        for (Menu menu : menus.values())
            if (menuTitle.startsWith(menu.getFormattedTitleBase()) ||
               (menu.getName().equalsIgnoreCase("rate_level") && ChatColor.stripColor(menuTitle).contains("Rate")))
                return menu;

        return null;
    }

    public Menu getMenuFromSelectItem(ItemStack item) {
        if (item != null)
            for (Menu menu : menus.values())
                if (menu.getSelectItem() != null && menu.getSelectItem().getType().equals(item.getType()))
                    return menu;

        return null;
    }

    public List<String> getMenuNames() {
        return new ArrayList<>(menus.keySet());
    }

    public Inventory getInventory(PlayerStats playerStats, String menuName, int pageNumber)
    {
        if (exists(menuName))
            return menus.get(menuName).getInventory(playerStats, pageNumber);

        return null;
    }

    public void updateInventory(PlayerStats playerStats, InventoryView inventory) {
        Menu menu = getMenuFromTitle(inventory.getTitle());

        if (menu != null)
            menu.updateInventory(playerStats, inventory, Utils.getTrailingInt(inventory.getTitle()));
    }

    public void updateInventory(PlayerStats playerStats, InventoryView inventory, String menuName, int pageNumber) {
        Menu menu = menus.get(menuName);

        if (menu != null)
            menu.updateInventory(playerStats, inventory, pageNumber);
    }

    public void renameLevel(String oldLevelName, String newLevelName) {

        // variables once it has been found
        Menu correctMenu = null;
        MenuPage correctMenuPage = null;
        MenuItem correctMenuItem = null;

        // outer loop for menus
        outer: for (Menu menu : menus.values())
            // outer loop for menu pages
            for (MenuPage menuPage : menu.getPages())
                // inner loop for menu items in pages
                for (MenuItem menuItem : menuPage.getItems())
                    // check if they are equal, if so, break outer loop
                    if (menuItem.getTypeValue().equalsIgnoreCase(oldLevelName)) {
                        correctMenu = menu;
                        correctMenuPage = menuPage;
                        correctMenuItem = menuItem;
                        break outer;
                    }

        // now null check them all
        if (correctMenu != null && correctMenuPage != null && correctMenuItem != null) {
            correctMenuItem.setTypeValue(newLevelName);
            MenusYAML.setItemType(correctMenu.getName(), correctMenuPage.getPageNumber(), correctMenuItem.getSlot(), oldLevelName, newLevelName);
        }
    }

    // in ONE of TWO cases where a different GUI gets auto-filled, we have to use this special method that goes around the normal OOP menus
    public void openSubmittedPlotsGUI(PlayerStats playerStats)
    {
        Player player = playerStats.getPlayer();
        Inventory inventory = getInventory(playerStats, "submitted-plots", 0);

        if (inventory != null)
        {
            List<Plot> submittedPlots = Parkour.getPlotsManager().getSubmittedPlots();
            for (int i = 0; i < submittedPlots.size() && i < inventory.getSize() - 9; i++)
            {
                Plot plot = submittedPlots.get(i);

                ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
                ItemMeta skullMeta = item.getItemMeta();

                String plotOwnerName = plot.getOwnerName();

                skullMeta.setDisplayName(Utils.translate("&4" + plotOwnerName + "&c's Plot Submission"));

                List<String> itemLore = new ArrayList<String>() {{
                    add("");
                    add(Utils.translate("&7Click to teleport to"));
                    add(Utils.translate("&4" + plotOwnerName + "&c's Plot"));
                    add("");
                    add(Utils.translate("&7Awaiting &aaccept &7or &cdeny"));
                    add("");
                }};

                skullMeta.setLore(itemLore);
                item.setItemMeta(skullMeta);

                // non null
                if (item != null)
                    inventory.setItem(i, item);
            }
            // make black glass at the bottom row
            for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++)
            {
                ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
                ItemMeta itemMeta = item.getItemMeta();
                itemMeta.setDisplayName(Utils.translate("&8Renatus Network"));
                item.setItemMeta(itemMeta);

                inventory.setItem(i, item);
            }
            player.openInventory(inventory);
        }
        else
        {
            player.sendMessage(Utils.translate("&cUnable to open inventory, null?"));
        }
    }

    // in ONE of TWO cases where a different GUI gets auto-filled, we have to use this special method that goes around the normal OOP menus
    public void openRaceLevelsGUI(PlayerStats playerStats, Player target, double betAmount)
    {
        Player player = playerStats.getPlayer();
        Inventory inventory = getInventory(playerStats, "pick-race-levels", 0);

        if (inventory != null)
        {
            RaceManager raceManager = Parkour.getRaceManager();
            List<RaceLevel> notInUseRaceLevels = raceManager.getNotInUseRaceLevels();

            for (int i = 0; i < notInUseRaceLevels.size() && i < inventory.getSize() - 9; i++) {

                RaceLevel level = notInUseRaceLevels.get(i);

                if (level != null)
                {
                    ItemStack item = new ItemStack(Material.QUARTZ_BLOCK);
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(level.getFormattedTitle());
                    List<String> itemLore = new ArrayList<String>() {{
                        add(Utils.translate("&7Click to select &c" + level.getFormattedTitle()));
                        add(Utils.translate("&7for your race!"));
                        add(Utils.translate(""));
                        add(Utils.translate("&7Wins this Race Level Has &6" + level.getTotalCompletionsCount()));
                        add(Utils.translate("&7Racing Against &e-> &6" + target.getName()));
                    }};

                    // this means they put a bet!
                    if (betAmount > 0.0)
                        itemLore.add(Utils.translate("&7Bet Amount &e-> &6" + betAmount));

                    itemMeta.setLore(itemLore);
                    item.setItemMeta(itemMeta);
                    inventory.setItem(i, item);
                }
            }

            // make black glass at the bottom row
            for (int i = inventory.getSize() - 9; i < inventory.getSize(); i++)
            {
                int middleSlot = inventory.getSize() - 5;
                ItemStack item;

                if (i == middleSlot)
                {
                    item = new ItemStack(Material.DIAMOND_BLOCK);
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(Utils.translate("&c&lRandom Level"));

                    List<String> itemLore = new ArrayList<String>() {{
                        add(Utils.translate("&7Click me to select Random Level"));
                        add(Utils.translate(""));
                        add(Utils.translate("&7Racing Against &e-> &6" + target.getName()));
                    }};

                    if (betAmount > 0.0)
                        itemLore.add(Utils.translate("&7Bet Amount &e-> &6" + betAmount));

                    itemMeta.setLore(itemLore);

                    item.setItemMeta(itemMeta);
                } else {
                    item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15);
                    ItemMeta itemMeta = item.getItemMeta();
                    itemMeta.setDisplayName(Utils.translate("&8Renatus Network"));
                    item.setItemMeta(itemMeta);
                }
                inventory.setItem(i, item);
            }
            player.openInventory(inventory);
        } else {
            player.sendMessage(Utils.translate("&cUnable to open inventory, null?"));
        }
    }
}
