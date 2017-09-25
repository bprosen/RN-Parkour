package com.parkourcraft.Parkour.listeners;

import com.parkourcraft.Parkour.data.MenuManager;
import com.parkourcraft.Parkour.data.StatsManager;
import com.parkourcraft.Parkour.data.menus.Menu;
import com.parkourcraft.Parkour.data.menus.MenuItem;
import com.parkourcraft.Parkour.data.menus.MenuItemAction;
import com.parkourcraft.Parkour.data.stats.PlayerStats;
import com.parkourcraft.Parkour.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Menu menu = MenuManager.getMenuFromTitle(event.getInventory().getTitle());

        if (menu != null) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null
                    && event.getCurrentItem().getType() != Material.AIR
                    && event.getCurrentItem().getItemMeta().getDisplayName() != null) {
                MenuItem menuItem = menu.getMenuItemFromTitle(
                        Utils.getTrailingInt(event.getInventory().getTitle()),
                        event.getCurrentItem().getItemMeta().getDisplayName()
                );

                if (menuItem != null)
                    MenuItemAction.perform((Player) event.getWhoClicked(), menuItem);
            }
        }
    }

    @EventHandler
    public void onMenuItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() == Action.RIGHT_CLICK_AIR
                || event.getAction() == Action.RIGHT_CLICK_BLOCK
                || event.getAction() == Action.LEFT_CLICK_AIR
                || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Menu menu = MenuManager.getMenuFromSelectItem(player.getInventory().getItemInMainHand());

            if (menu != null) {
                PlayerStats playerStats = StatsManager.get(player);

                player.openInventory(MenuManager.getInventory(menu.getName(), 1));
                MenuManager.updateInventory(playerStats, player.getOpenInventory(), menu.getName());
            }
        }

    }

}