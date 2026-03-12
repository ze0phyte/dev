package com.parasite.game;

public enum Role {
    PARASITE("§4☣ PARASITE", "§4"),
    DOCTOR("§b✚ DOCTOR", "§b"),
    CREWMATE("§7⚙ CREWMATE", "§7");

    private final String display;
    private final String color;

    Role(String display, String color) {
        this.display = display;
        this.color = color;
    }

    public String getDisplay() { return display; }
    public String getColor() { return color; }
}
