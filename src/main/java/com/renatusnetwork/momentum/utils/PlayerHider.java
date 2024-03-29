package com.renatusnetwork.momentum.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class PlayerHider {

    private static Set<Player> hiddenPlayers = new HashSet<>();

    public static void hidePlayer(Player player) {
        hiddenPlayers.add(player);

        for (Player online : Bukkit.getOnlinePlayers())
            if (!online.isOp())
                player.hidePlayer(online);
    }

    public static void showPlayer(Player player) {
        hiddenPlayers.remove(player);

        for (Player online : Bukkit.getOnlinePlayers())
            if (!online.isOp())
                player.showPlayer(online);
    }

    public static boolean containsPlayer(Player player) {
        return hiddenPlayers.contains(player);
    }

    public static void hideHiddenPlayersFromJoined(Player playerJoined)
    {
        for (Player player : hiddenPlayers)
            player.hidePlayer(playerJoined);
    }
}