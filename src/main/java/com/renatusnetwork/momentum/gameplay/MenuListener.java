package com.renatusnetwork.momentum.gameplay;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.data.levels.Level;
import com.renatusnetwork.momentum.data.levels.LevelManager;
import com.renatusnetwork.momentum.data.menus.CancelTasks;
import com.renatusnetwork.momentum.data.menus.Menu;
import com.renatusnetwork.momentum.data.menus.MenuItem;
import com.renatusnetwork.momentum.data.menus.MenuItemAction;
import com.renatusnetwork.momentum.data.plots.Plot;
import com.renatusnetwork.momentum.data.stats.PlayerStats;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Menu menu = Momentum.getMenuManager().getMenuFromTitle(event.getInventory().getTitle());

        Player player = (Player) event.getWhoClicked();

        if (menu != null) {
            event.setCancelled(true);
            ItemStack currentItem = event.getCurrentItem();

            if (currentItem != null
                && currentItem.getType() != Material.AIR
                && currentItem.hasItemMeta()
                && currentItem.getItemMeta().hasDisplayName()) {

                MenuItem menuItem = menu.getMenuItem(
                        Utils.getTrailingInt(event.getInventory().getTitle()),
                        event.getSlot()
                );

                if (menuItem != null && (menuItem.getItem().getType() == currentItem.getType() || Momentum.getLevelManager().isBuyingLevelMenu(player.getName()))) {
                    MenuItemAction.perform(player, menuItem);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.1f, 2f);
                } else {
                    // submitted plots section
                    String submittedPlotsTitle = Momentum.getMenuManager().getMenu("submitted-plots").getFormattedTitleBase();
                    String pickRaceLevelsTitle = Momentum.getMenuManager().getMenu("pick-race-levels").getFormattedTitleBase();

                    /*
                        Submitted Plots GUI
                     */
                    if (menu.getFormattedTitleBase().equalsIgnoreCase(submittedPlotsTitle)) {
                        if (currentItem.getType() == Material.SKULL_ITEM) {

                            String[] split = currentItem.getItemMeta().getDisplayName().split("'");
                            Plot plot = Momentum.getPlotsManager().get(ChatColor.stripColor(split[0]));

                            player.closeInventory();

                            if (plot != null) {
                                // set pitch and yaw for cleaner teleport
                                Location plotSpawn = plot.getSpawnLoc().clone();
                                plotSpawn.setPitch(player.getLocation().getPitch());
                                plotSpawn.setYaw(player.getLocation().getYaw());

                                player.teleport(plotSpawn);
                                player.sendMessage(Utils.translate("&cYou teleported to &4" + plot.getOwnerName() + "&c's Plot"));
                            } else {
                                player.sendMessage(Utils.translate("&cPlot does not exist"));
                            }
                        }
                    /*
                        Race Levels GUI, optimize by not including stained glass pane
                     */
                    } else if (menu.getFormattedTitleBase().equalsIgnoreCase(pickRaceLevelsTitle) && currentItem.getType() != Material.STAINED_GLASS_PANE) {
                        // conditions to determine if it is the right item, if it is a random level they selected, etc
                        boolean randomLevel = false;
                        boolean correctItem = false;
                        Level selectedLevel = null;

                        if (ChatColor.stripColor(currentItem.getItemMeta().getDisplayName()).equalsIgnoreCase("Random Level")) {
                            randomLevel = true;
                            correctItem = true;
                        } else {
                            Level level = Momentum.getLevelManager().getFromTitle(event.getCurrentItem().getItemMeta().getDisplayName());

                            // if they hit a selected level
                            if (level != null) {
                                selectedLevel = level;
                                correctItem = true;
                            }
                        }

                        // if it is an item that can be used for races, continue
                        if (correctItem) {
                            List<String> itemLore = currentItem.getItemMeta().getLore();

                            String lastString = ChatColor.stripColor(itemLore.get(itemLore.size() - 1));
                            boolean bet = false;
                            double betAmount = -1.0;
                            String opponentName = null;

                            if (lastString.toUpperCase().contains("bet amount".toUpperCase())) {
                                bet = true;
                                // get the right side of the ->
                                betAmount = Double.parseDouble(lastString.split("-> ")[1]);
                                // this means the against string is second last
                                opponentName = ChatColor.stripColor(itemLore.get(itemLore.size() - 2)).split("-> ")[1];

                            } else if (lastString.toUpperCase().contains("against".toUpperCase()))
                                opponentName = lastString.split("-> ")[1];

                            PlayerStats playerStats = Momentum.getStatsManager().get(player);
                            PlayerStats opponentStats = Momentum.getStatsManager().getByName(opponentName);

                            // close inventory
                            player.closeInventory();

                            if (opponentStats != null) {
                                // then use the boolean if to run the appropriate conditions
                                if (randomLevel)
                                    Momentum.getRaceManager().sendRequest(playerStats, opponentStats, true, null, bet, betAmount);
                                else
                                    Momentum.getRaceManager().sendRequest(playerStats, opponentStats, false, selectedLevel, bet, betAmount);
                            } else
                                player.sendMessage(Utils.translate("&4" + opponentName + " &cis not online anymore"));
                        }
                    }
                }
            }
        }
        else if (!player.isOp() && !player.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event)
    {
        LevelManager levelManager = Momentum.getLevelManager();
        String name = event.getPlayer().getName();

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Inventory openedInventory = event.getInventory();
                Inventory nextInventory = event.getPlayer().getOpenInventory().getTopInventory();

                if (!openedInventory.getName().equalsIgnoreCase(nextInventory.getName()))
                {
                    // remove buying
                    if (levelManager.isBuyingLevelMenu(name))
                        levelManager.removeBuyingLevel(name);

                    // cancelled tasks
                    CancelTasks cancelTasks = Momentum.getMenuManager().getCancelTasks(name);

                    // if not null and contains, we need to cancel remaining tasks!
                    if (cancelTasks != null && cancelTasks.getCancelledSlots() != null)
                    {
                        for (BukkitTask task : cancelTasks.getCancelledSlots())
                            task.cancel();

                        Momentum.getMenuManager().removeCancelTasks(name); // remove
                    }
                }
            }
        }.runTaskLater(Momentum.getPlugin(), 1);
    }

    @EventHandler
    public void onMenuItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().getName().equalsIgnoreCase(Momentum.getSettingsManager().player_submitted_world) &&
           (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK ||
            event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {

            Menu menu = Momentum.getMenuManager().getMenuFromSelectItem(player.getInventory().getItemInMainHand());

            if (menu != null)
            {
                PlayerStats playerStats = Momentum.getStatsManager().get(player);

                if (!playerStats.isInTutorial())
                {
                    player.openInventory(Momentum.getMenuManager().getInventory(menu.getName(), 1));
                    Momentum.getMenuManager().updateInventory(player, player.getOpenInventory(), menu.getName(), 1);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.1f, 2f);
                }
                else
                {
                    player.sendMessage(Utils.translate("&cYou cannot do this while in the tutorial"));
                }
            }
        }
    }
}
