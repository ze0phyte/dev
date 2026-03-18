package com.bingorace.game;

public enum GameState {
    IDLE,       // no game running
    LOBBY,      // players joining with /bjoin
    STARTING,   // countdown after OP confirmed setup
    RUNNING,    // active game
    ENDED       // game over, resetting
}
