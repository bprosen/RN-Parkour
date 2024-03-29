package com.renatusnetwork.momentum.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class InfiniteEndEvent extends Event implements Cancellable
{

    private Player player;
    private int reward;
    private boolean cancelled;

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList()
    {
        return HANDLERS;
    }

    public InfiniteEndEvent(Player player, int score)
    {
        this.player = player;
        this.reward = score;
        this.cancelled = false;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void setReward(int reward)
    {
        this.reward = reward;
    }

    public int getReward()
    {
        return reward;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
