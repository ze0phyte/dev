package com.parasite.game;

public enum GameState {
    WAITING,      // Lobby - waiting for players / op to start
    STARTING,     // Countdown
    IN_ROUND,     // Active 5-minute play phase
    DISCUSSION,   // 2-minute talk phase
    VOTING,       // 15-second vote phase
    ROUND_END,    // Results shown briefly
    ENDED         // Game over
}
