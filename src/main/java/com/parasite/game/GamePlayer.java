package com.parasite.game;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GamePlayer {

    private final UUID uuid;
    private final String name;
    private Role role = Role.CREWMATE;
    private boolean alive = true;

    // Per-round state
    private boolean infected = false;       // Parasite infected this player
    private boolean savedThisRound = false; // Doctor saved this player
    private boolean usedCrossbow = false;   // Fired crossbow this round
    private boolean hasVoted = false;       // Cast a vote this voting phase

    // Parasite swap cooldown
    private long lastSwapMillis = 0;

    // Who this player voted for (null = skip)
    private UUID votedFor = null;

    // Force-assigned role by admin before game
    private Role forcedRole = null;

    // Stored skin data for restoration
    private String originalSkinTexture = null;
    private String originalSkinSignature = null;

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

    public String getOriginalSkinTexture() { return originalSkinTexture; }
    public void setOriginalSkinTexture(String t) { this.originalSkinTexture = t; }
    public String getOriginalSkinSignature() { return originalSkinSignature; }
    public void setOriginalSkinSignature(String s) { this.originalSkinSignature = s; }
}
