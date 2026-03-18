package com.bingorace.game;

public enum Difficulty {
    EASY(3, "§aEasy", "§aEASY — 9 items"),
    MEDIUM(4, "§eMedium", "§eMEDIUM — 16 items"),
    HARD(5, "§cHard", "§cHARD — 25 items");

    private final int gridSize;   // 3, 4, or 5
    private final String display;
    private final String description;

    Difficulty(int gridSize, String display, String description) {
        this.gridSize = gridSize;
        this.display = display;
        this.description = description;
    }

    public int getGridSize() { return gridSize; }
    public int getCellCount() { return gridSize * gridSize; }
    public String getDisplay() { return display; }
    public String getDescription() { return description; }

    public String getItemFile() {
        return switch (this) {
            case EASY -> "items/easy.yml";
            case MEDIUM -> "items/medium.yml";
            case HARD -> "items/hard.yml";
        };
    }
}
