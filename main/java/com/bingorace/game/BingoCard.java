package com.bingorace.game;

import java.util.ArrayList;
import java.util.List;

public class BingoCard {

    private final Difficulty difficulty;
    private final BingoItem[] cells; // row-major: index = row * gridSize + col
    private final int size;

    public BingoCard(Difficulty difficulty, List<BingoItem> items) {
        this.difficulty = difficulty;
        this.size = difficulty.getGridSize();
        this.cells = new BingoItem[size * size];
        for (int i = 0; i < cells.length && i < items.size(); i++) {
            cells[i] = items.get(i);
        }
    }

    public BingoItem getCell(int row, int col) {
        return cells[row * size + col];
    }

    public BingoItem[] getCells() { return cells; }

    public int getSize() { return size; }

    public Difficulty getDifficulty() { return difficulty; }

    /**
     * Mark an item as complete if it exists on this card.
     * Returns true if something was newly completed.
     */
    public boolean markComplete(org.bukkit.Material material, String playerName) {
        for (BingoItem item : cells) {
            if (item != null && !item.isCompleted() && item.getMaterial() == material) {
                item.complete(playerName);
                return true;
            }
        }
        return false;
    }

    public boolean hasItem(org.bukkit.Material material) {
        for (BingoItem item : cells) {
            if (item != null && item.getMaterial() == material) return true;
        }
        return false;
    }

    // ── Win condition checks ───────────────────────────────────────────────

    public List<String> checkNewBingos(int prevCompleted) {
        List<String> bingos = new ArrayList<>();
        // Check rows
        for (int r = 0; r < size; r++) {
            if (isRowComplete(r)) bingos.add("row");
        }
        // Check columns
        for (int c = 0; c < size; c++) {
            if (isColComplete(c)) bingos.add("column");
        }
        // Check diagonals
        if (isDiag1Complete()) bingos.add("diagonal");
        if (isDiag2Complete()) bingos.add("diagonal");
        return bingos;
    }

    public boolean isRowComplete(int row) {
        for (int c = 0; c < size; c++) {
            BingoItem item = cells[row * size + c];
            if (item == null || !item.isCompleted()) return false;
        }
        return true;
    }

    public boolean isColComplete(int col) {
        for (int r = 0; r < size; r++) {
            BingoItem item = cells[r * size + col];
            if (item == null || !item.isCompleted()) return false;
        }
        return true;
    }

    public boolean isDiag1Complete() {
        for (int i = 0; i < size; i++) {
            BingoItem item = cells[i * size + i];
            if (item == null || !item.isCompleted()) return false;
        }
        return true;
    }

    public boolean isDiag2Complete() {
        for (int i = 0; i < size; i++) {
            BingoItem item = cells[i * size + (size - 1 - i)];
            if (item == null || !item.isCompleted()) return false;
        }
        return true;
    }

    public boolean isFullCard() {
        for (BingoItem item : cells) {
            if (item == null || !item.isCompleted()) return false;
        }
        return true;
    }

    public int getCompletedCount() {
        int count = 0;
        for (BingoItem item : cells) {
            if (item != null && item.isCompleted()) count++;
        }
        return count;
    }
}
