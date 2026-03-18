package com.bingorace.game;

public enum Difficulty {
    EASY(      3, "§aEasy",       "§a3x3  — 9 items"),
    MEDIUM(    4, "§eMedium",     "§e4x4  — 16 items"),
    HARD(      5, "§cHard",       "§c5x5  — 25 items"),
    IMPOSSIBLE(6, "§4Impossible", "§46x6  — 36 items");

    private final int gridSize;
    private final String display;
    private final String description;

    Difficulty(int gridSize, String display, String description) {
        this.gridSize    = gridSize;
        this.display     = display;
        this.description = description;
    }

    public int getGridSize()    { return gridSize; }
    public int getCellCount()   { return gridSize * gridSize; }
    public String getDisplay()  { return display; }
    public String getDescription() { return description; }

    public String getItemFile() {
        return switch (this) {
            case EASY       -> "items/easy.yml";
            case MEDIUM     -> "items/medium.yml";
            case HARD       -> "items/hard.yml";
            case IMPOSSIBLE -> "items/impossible.yml";
        };
    }
}