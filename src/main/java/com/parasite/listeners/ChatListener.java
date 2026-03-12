package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import com.parasite.game.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final ParasitePlugin plugin;

    public ChatListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());

        if (!gm.isRunning()) return;
        if (gp == null) return; // spectator, let chat through normally

        // Dead players can only talk to each other (dead chat)
        if (!gp.isAlive()) {
            event.setCancelled(true);
            // Send only to other dead players and ops
            String msg = "§8[☠ DEAD] §7" + player.getName() + ": §8" + event.getMessage();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                GamePlayer tp = gm.getGamePlayer(p.getUniqueId());
                if ((tp != null && !tp.isAlive()) || p.isOp()) {
                    p.sendMessage(msg);
                }
            }
            return;
        }

        // During IN_ROUND - don't show names (anonymous signs are for communication, but chat allowed with name)
        // Format: no special restriction, but keep it themed
        GameState state = gm.getState();
        if (state == GameState.IN_ROUND) {
            // Chat is allowed, name shown since they're talking
            event.setFormat("§7[§fCrew§7] §f" + player.getName() + "§8: §f%2$s");
        } else if (state == GameState.DISCUSSION) {
            event.setFormat("§e[☎] §f" + player.getName() + "§8: §e%2$s");
        } else if (state == GameState.VOTING) {
            // Mute chat during voting - focus on the vote
            event.setCancelled(true);
            player.sendMessage(GameManager.PREFIX + "§cNo talking during the vote!");
        }
    }
}
