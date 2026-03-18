package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.BingoTeam;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class RespawnListener implements Listener {

    private final BingoRacePlugin plugin;

    public RespawnListener(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    /** Keep death message but don't clear drops — they're part of the race */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        // Nothing to cancel — items drop normally, that's fine for bingo
    }

    /** Force respawn back into the game world */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        BingoTeam team = plugin.getGameManager().getTeamOf(player.getUniqueId());
        if (team == null || !plugin.getGameManager().isRunning()) return;

        World gameWorld = plugin.getGameManager().getGameWorld();
        if (gameWorld == null) return;

        // Respawn at game world spawn
        Location spawn = gameWorld.getSpawnLocation().clone();
        spawn.setY(gameWorld.getHighestBlockYAt(spawn) + 1);
        event.setRespawnLocation(spawn);
    }
}
