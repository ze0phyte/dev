package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class DropPickupListener implements Listener {

    private final ParasitePlugin plugin;

    public DropPickupListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning() && gm.getGamePlayer(event.getPlayer().getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning() && gm.getGamePlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
