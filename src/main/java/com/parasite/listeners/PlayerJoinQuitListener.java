package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import com.parasite.utils.ScoreboardUtils;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinQuitListener implements Listener {

    private final ParasitePlugin plugin;

    public PlayerJoinQuitListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Update tab list and scoreboard for the new player
        ScoreboardUtils.updateTabList(plugin.getGameManager());
        ScoreboardUtils.updateSidebar(event.getPlayer(), plugin.getGameManager());

        // If game is running and this player is NOT in the game, put them in spectator
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning() && gm.getGamePlayer(event.getPlayer().getUniqueId()) == null) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            event.getPlayer().sendMessage(GameManager.PREFIX + "§7A game is in progress. You are spectating.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getGameManager().removePlayer(event.getPlayer());
        ScoreboardUtils.updateTabList(plugin.getGameManager());
    }
}
