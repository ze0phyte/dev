package com.parasite.commands;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SeanceCommand implements CommandExecutor {

    private final ParasitePlugin plugin;

    public SeanceCommand(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());

        if (gp == null) {
            player.sendMessage(GameManager.PREFIX + "§cYou are not in the game.");
            return true;
        }
        if (gp.isAlive()) {
            player.sendMessage(GameManager.PREFIX + "§cOnly dead players can use the séance.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(GameManager.PREFIX + "§7Usage: §e/seance <player>");
            player.sendMessage(GameManager.PREFIX + "§7Vote to haunt a living player. Most votes wins.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(GameManager.PREFIX + "§cPlayer not found: §e" + args[0]);
            return true;
        }

        gm.handleSeanceVote(player, target);
        return true;
    }
}
