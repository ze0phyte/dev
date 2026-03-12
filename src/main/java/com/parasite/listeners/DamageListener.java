package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import com.parasite.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class DamageListener implements Listener {

    private final ParasitePlugin plugin;

    public DamageListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    /** Block all player damage during the game - no friendly fire, no fall damage */
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
        if (gp == null) return;

        // Cancel ALL damage to game players
        event.setCancelled(true);
    }

    /** Also cancel entity-by-entity so melee hits do nothing */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (gm.getGamePlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    /** Prevent hunger drain */
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (!gm.isRunning()) return;
        if (gm.getGamePlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}
