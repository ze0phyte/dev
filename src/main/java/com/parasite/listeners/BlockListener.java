package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

    private final ParasitePlugin plugin;

    public BlockListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    /** Iron axe can break oak signs during round/discussion. Everything else blocked. */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (gm.getGamePlayer(player.getUniqueId()) == null) return;

        GameState state = gm.getState();
        Material broken = event.getBlock().getType();
        ItemStack held = player.getInventory().getItemInMainHand();

        boolean isSign = broken == Material.OAK_SIGN || broken == Material.OAK_WALL_SIGN;
        boolean hasAxe = held != null && held.getType() == Material.IRON_AXE;
        boolean breakPhase = state == GameState.IN_ROUND || state == GameState.DISCUSSION;

        if (isSign && hasAxe && breakPhase) return;

        event.setCancelled(true);
    }

    /**
     * Sign placement: switch to SURVIVAL for 1 tick so Adventure mode allows the place,
     * then switch back. Signs only, round and discussion only.
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
            player.setGameMode(GameMode.SURVIVAL);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (gm.getGamePlayer(player.getUniqueId()) != null) {
                    player.setGameMode(GameMode.ADVENTURE);
                }
            }, 1L);
            return;
        }

        event.setCancelled(true);
    }
}