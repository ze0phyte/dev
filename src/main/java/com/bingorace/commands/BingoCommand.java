package com.bingorace.commands;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.GameManager;
import com.bingorace.game.BingoTeam;
import com.bingorace.gui.BingoCardGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BingoCommand implements CommandExecutor {

    private final BingoRacePlugin plugin;

    public BingoCommand(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this.");
            return true;
        }

        GameManager gm = plugin.getGameManager();
        BingoTeam team = gm.getTeamOf(player.getUniqueId());

        if (team == null || !gm.isRunning()) {
            player.sendMessage(GameManager.PREFIX + "§cNo active bingo game.");
            return true;
        }

        BingoCardGUI.open(player, team);
        return true;
    }
}
