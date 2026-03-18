package com.bingorace.commands;

import com.bingorace.BingoRacePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BJoinCommand implements CommandExecutor {

    private final BingoRacePlugin plugin;

    public BJoinCommand(BingoRacePlugin plugin) {
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
