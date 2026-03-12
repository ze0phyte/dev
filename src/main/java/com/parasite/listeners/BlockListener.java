package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BlockListener implements Listener {

    private final ParasitePlugin plugin;

    public BlockListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    /** Prevent block breaking entirely during game */
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning() && gm.getGamePlayer(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    /**
     * Allow sign placement (players communicate with signs) but block other block placing.
     * Signs are allowed in IN_ROUND and DISCUSSION.
     */
    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (gm.getGamePlayer(player.getUniqueId()) == null) return;

        GameState state = gm.getState();
        boolean isSign = event.getBlockPlaced().getType().name().contains("SIGN");

        // Allow signs only during round and discussion
        if (isSign && (state == GameState.IN_ROUND || state == GameState.DISCUSSION)) {
            return; // Allow sign placement
        }

        // Block everything else
        event.setCancelled(true);
    }
}
