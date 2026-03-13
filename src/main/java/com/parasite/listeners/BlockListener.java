package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

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
     * Sign placement fix:
     * Adventure mode blocks block placement at the engine level even if you cancel the event
     * and try to allow it. The only reliable workaround without NMS is to temporarily switch
     * the player to SURVIVAL for the placement tick, then switch them back.
     * We only do this for sign blocks during IN_ROUND and DISCUSSION phases.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (gm.getGamePlayer(player.getUniqueId()) == null) return;

        GameState state = gm.getState();
        boolean isSign = event.getBlockPlaced().getType().name().contains("SIGN");

        if (isSign && (state == GameState.IN_ROUND || state == GameState.DISCUSSION)) {
            // Temporarily switch to survival so the sign can be placed,
            // then immediately switch back to adventure
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                player.setGameMode(GameMode.ADVENTURE);
            }, 1L);
            return; // allow the placement
        }

        // Block everything else
        event.setCancelled(true);
    }
}