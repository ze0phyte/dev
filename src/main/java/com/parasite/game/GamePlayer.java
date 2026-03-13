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
    private boolean hasVoted = false;
    private boolean infectedThisRound = false; // Parasite can only infect ONE person per round

    // Stamina
    private int stamina = 20;           // 0-20, mirrors food bar
    private long lastStaminaUse = 0;

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

    // Reset per-round state (called each new day)
    public void resetRound() {
        infected = false;
        savedThisRound = false;
        usedCrossbow = false;
        hasVoted = false;
        votedFor = null;
        infectedThisRound = false;
        stamina = 20;
        lastStaminaUse = 0;
    }

    // Full reset for new game
    public void resetFull() {
        role = Role.CREWMATE;
        alive = true;
        lastSwapMillis = 0;
        forcedRole = null;
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
}