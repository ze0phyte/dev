package com.bingorace.game;

import org.bukkit.Material;

public class BingoItem {

    private final Material material;
    private boolean completed = false;
    private String completedBy = null; // player name who got it

    public BingoItem(Material material) {
        this.material = material;
    }

    public Material getMaterial() { return material; }

    public boolean isCompleted() { return completed; }

    public String getCompletedBy() { return completedBy; }

    public void complete(String playerName) {
        this.completed = true;
        this.completedBy = playerName;
    }
}
