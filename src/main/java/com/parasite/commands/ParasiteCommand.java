package com.parasite.commands;
import com.parasite.utils.ScoreboardUtils;
import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import com.parasite.game.Role;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ParasiteCommand implements CommandExecutor {

    private final ParasitePlugin plugin;
    private final GameManager gm;

    public ParasiteCommand(ParasitePlugin plugin) {
        this.plugin = plugin;
        this.gm = plugin.getGameManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("parasite.admin")) {
            sender.sendMessage(GameManager.PREFIX + "§cYou don't have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "start" -> {
                Player admin = (sender instanceof Player p) ? p : null;
                boolean ok = gm.startGame(admin);
                if (ok) sender.sendMessage(GameManager.PREFIX + "§aGame started!");
            }

            case "stop" -> {
                gm.forceStop();
                sender.sendMessage(GameManager.PREFIX + "§cGame stopped.");
            }

            case "setlobby" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.setLobbyLocation(p.getLocation());
                sender.sendMessage(GameManager.PREFIX + "§aLobby location set!");
            }

            case "setarena" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.setArenaLocation(p.getLocation());
                sender.sendMessage(GameManager.PREFIX + "§aArena centre set!");
            }

            case "setrole" -> {
                // /parasite setrole <player> <parasite|doctor|crewmate>
                if (args.length < 3) { sender.sendMessage(GameManager.PREFIX + "§cUsage: /parasite setrole <player> <parasite|doctor|crewmate>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(GameManager.PREFIX + "§cPlayer not found: " + args[1]); return true; }
                Role role = parseRole(args[2]);
                if (role == null) { sender.sendMessage(GameManager.PREFIX + "§cUnknown role. Use: parasite, doctor, crewmate"); return true; }
                gm.forceRole(target, role);
                sender.sendMessage(GameManager.PREFIX + "§aForced §f" + target.getName() + " §ato role: " + role.getDisplay());
                target.sendMessage(GameManager.PREFIX + "§7Your role has been set to " + role.getDisplay() + " §7by an admin.");
            }

            case "addplayer" -> {
                // /parasite addplayer <player>  — force-add to lobby
                if (args.length < 2) { sender.sendMessage(GameManager.PREFIX + "§cUsage: /parasite addplayer <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(GameManager.PREFIX + "§cPlayer not found."); return true; }
                gm.joinLobby(target);
                sender.sendMessage(GameManager.PREFIX + "§aAdded §f" + target.getName() + " §ato the lobby.");
            }

            case "status" -> {
                sender.sendMessage("§8§m──────────────────────────");
                sender.sendMessage("§5§l☣ PARASITE STATUS");
                sender.sendMessage("§7State: §f" + gm.getState());
                sender.sendMessage("§7Day: §f" + gm.getCurrentDay());
                sender.sendMessage("§7Players in game: §f" + gm.getPlayerCount());
                sender.sendMessage("§7Alive: §f" + gm.getAlivePlayers().size());
                sender.sendMessage("§7Lobby: §f" + (gm.getLobbyLocation() != null ? "Set" : "§cNot set"));
                sender.sendMessage("§7Arena: §f" + (gm.getArenaLocation() != null ? "Set" : "§cNot set"));
                sender.sendMessage("§8§m──────────────────────────");
            }
           
            case "info" -> {
             if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
             ScoreboardUtils.toggleInfo(p, gm);
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m──────────────────────────");
        sender.sendMessage("§5§l☣ PARASITE COMMANDS");
        sender.sendMessage("§e/parasite start §7— Start the game");
        sender.sendMessage("§e/parasite stop §7— Force stop");
        sender.sendMessage("§e/parasite setlobby §7— Set lobby spawn");
        sender.sendMessage("§e/parasite setarena §7— Set arena centre");
        sender.sendMessage("§e/parasite setrole <player> <role> §7— Force a role");
        sender.sendMessage("§e/parasite addplayer <player> §7— Add to lobby");
        sender.sendMessage("§e/parasite status §7— Show game info");
        sender.sendMessage("§8§m──────────────────────────");
    }

    private Role parseRole(String s) {
        return switch (s.toLowerCase()) {
            case "parasite" -> Role.PARASITE;
            case "doctor", "medic" -> Role.DOCTOR;
            case "crewmate", "crew" -> Role.CREWMATE;
            default -> null;
        };
    }
}
