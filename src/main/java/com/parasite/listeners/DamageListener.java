package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
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

    // Cancel ALL damage to any game player (fall, void, fire, etc.)
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
        // Cancel if in lobby OR game running
        if (gp != null) event.setCancelled(true);
    }

    // Cancel PvP — if either the attacker or victim is in the game
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        GameManager gm = plugin.getGameManager();

        // Victim is a game player
        if (event.getEntity() instanceof Player victim) {
            if (gm.getGamePlayer(victim.getUniqueId()) != null) {
                event.setCancelled(true);
                return;
            }
        }

        // Attacker is a game player
        if (event.getDamager() instanceof Player attacker) {
            if (gm.getGamePlayer(attacker.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent hunger drain
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        GameManager gm = plugin.getGameManager();
        if (gm.getGamePlayer(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }
}