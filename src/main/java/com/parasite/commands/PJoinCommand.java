package com.parasite.commands;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PJoinCommand implements CommandExecutor {

    private final ParasitePlugin plugin;

    public PJoinCommand(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can join.");
            return true;
        }
        plugin.getGameManager().joinLobby(player);
        return true;
    }
}
