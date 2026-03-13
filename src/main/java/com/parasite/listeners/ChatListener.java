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

        // Not in game at all — let chat through normally
        if (!gm.isRunning() || gp == null) return;

        GameState state = gm.getState();

        // Dead players only talk to other dead players + ops
        if (!gp.isAlive()) {
            event.setCancelled(true);
            String msg = "§8[☠ DEAD] §7" + player.getName() + ": §8" + event.getMessage();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                GamePlayer tp = gm.getGamePlayer(p.getUniqueId());
                if ((tp != null && !tp.isAlive()) || p.isOp()) {
                    p.sendMessage(msg);
                }
            }
            return;
        }

        // IN_ROUND — no talking at all
        if (state == GameState.IN_ROUND) {
            event.setCancelled(true);
            player.sendMessage(GameManager.PREFIX + "§cYou can't talk during the round. Use signs!");
            return;
        }

        // VOTING — no talking
        if (state == GameState.VOTING) {
            event.setCancelled(true);
            player.sendMessage(GameManager.PREFIX + "§cNo talking during the vote!");
            return;
        }

        // DISCUSSION — talking allowed
        if (state == GameState.DISCUSSION) {
            event.setFormat("§e[☎] §f" + player.getName() + "§8: §e%2$s");
            return;
        }

        // Any other state (STARTING, ROUND_END etc) — cancel to be safe
        event.setCancelled(true);
    }
}