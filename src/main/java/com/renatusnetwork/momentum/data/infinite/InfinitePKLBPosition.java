package com.renatusnetwork.momentum.data.infinite;

public class InfinitePKLBPosition {

    private String playerUUID;
    private String playerName;
    private int score;

    public InfinitePKLBPosition(String playerUUID, String playerName, int score) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.score = score;
    }

    public int getScore() { return score; }

    public void setScore(int score) { this.score = score; }

    public String getUUID() { return playerUUID; }

    public String getName() { return playerName; }
}
