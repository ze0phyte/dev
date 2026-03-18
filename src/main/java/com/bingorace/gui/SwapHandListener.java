package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.BingoTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class SwapHandListener implements Listener {

    private final BingoRacePlugin plugin;

    public SwapHandListener(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        BingoTeam team = plugin.getGameManager().getTeamOf(player.getUniqueId());
        if (team == null || !plugin.getGameManager().isRunning()) return;

        // Cancel the actual swap and open the bingo card instead
        event.setCancelled(true);
        BingoCardGUI.open(player, team);
    }
}
