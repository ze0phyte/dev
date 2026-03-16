package com.parasite.game;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private final String name;
    private Role role = Role.CREWMATE;
    private boolean alive = true;

    // Per-round state
    private boolean infected = false;
    private boolean savedThisRound = false;
    private boolean usedCrossbow = false;
    private int scannerShotsLeft = 1; // set at round start based on player count
    private boolean hasVoted = false;
    private boolean infectedThisRound = false;

    // Stamina (food bar)
    private int stamina = 20;
    private long lastStaminaUse = 0;

    // Nutrition tracking — how many times eaten this round
    private int nutritionCount = 0;
    private long lastFoodStationUse = 0; // per-player cooldown per station tracked in GameManager

    // Last will — written before death
    private String lastWill = null;

    // Researcher ability cooldown
    private int researchLastUsedDay = 0;


    // Séance haunt target (dead players vote on this)
    // Stored in GameManager, not here

    // Parasite swap cooldown
    private long lastSwapMillis = 0;

    // Who this player voted for (null = skip)
    private UUID votedFor = null;

    // Force-assigned role by admin before game
    private Role forcedRole = null;

    public GamePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void resetRound() {
        infected = false;
        savedThisRound = false;
        usedCrossbow = false;
        scannerShotsLeft = 1;
        hasVoted = false;
        votedFor = null;
        infectedThisRound = false;
        stamina = 20;
        lastStaminaUse = 0;
        nutritionCount = 0;
        lastWill = null;
    }

    public void resetFull() {
        role = Role.CREWMATE;
        alive = true;
        lastSwapMillis = 0;
        forcedRole = null;
        lastFoodStationUse = 0;
        researchLastUsedDay = 0;
        resetRound();
    }

    // ── Getters / Setters ──────────────────────

    public UUID getUUID() { return uuid; }
    public String getName() { return name; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public boolean isInfected() { return infected; }
    public void setInfected(boolean infected) { this.infected = infected; }

    public boolean isSavedThisRound() { return savedThisRound; }
    public void setSavedThisRound(boolean saved) { this.savedThisRound = saved; }

    public boolean hasUsedCrossbow() { return usedCrossbow; }
    public void setUsedCrossbow(boolean used) { this.usedCrossbow = used; }

    public int getScannerShotsLeft() { return scannerShotsLeft; }
    public void setScannerShotsLeft(int n) { this.scannerShotsLeft = n; }
    public void decrementScannerShots() { this.scannerShotsLeft = Math.max(0, scannerShotsLeft - 1); }

    public boolean hasVoted() { return hasVoted; }
    public void setHasVoted(boolean v) { this.hasVoted = v; }

    public UUID getVotedFor() { return votedFor; }
    public void setVotedFor(UUID id) { this.votedFor = id; hasVoted = true; }

    public long getLastSwapMillis() { return lastSwapMillis; }
    public void setLastSwapMillis(long t) { this.lastSwapMillis = t; }

    public Role getForcedRole() { return forcedRole; }
    public void setForcedRole(Role r) { this.forcedRole = r; }

    public boolean hasInfectedThisRound() { return infectedThisRound; }
    public void setInfectedThisRound(boolean b) { this.infectedThisRound = b; }

    public int getStamina() { return stamina; }
    public void setStamina(int s) { this.stamina = Math.max(0, Math.min(20, s)); }

    public long getLastStaminaUse() { return lastStaminaUse; }
    public void setLastStaminaUse(long t) { this.lastStaminaUse = t; }

    public int getNutritionCount() { return nutritionCount; }
    public void incrementNutrition() { this.nutritionCount++; }

    public long getLastFoodStationUse() { return lastFoodStationUse; }
    public void setLastFoodStationUse(long t) { this.lastFoodStationUse = t; }

    public String getLastWill() { return lastWill; }
    public void setLastWill(String will) { this.lastWill = will; }


    public int getResearchLastUsedDay() { return researchLastUsedDay; }
    public void setResearchLastUsedDay(int day) { this.researchLastUsedDay = day; }
}