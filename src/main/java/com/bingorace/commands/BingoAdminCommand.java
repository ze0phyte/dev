package com.bingorace.commands;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.Difficulty;
import com.bingorace.game.GameManager;
import com.bingorace.gui.BingoCardGUI;
import com.bingorace.gui.SetupGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BingoAdminCommand implements CommandExecutor {

    private final BingoRacePlugin plugin;

    public BingoAdminCommand(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        GameManager gm = plugin.getGameManager();

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "start" -> {
                if (!sender.hasPermission("bingorace.admin")) {
                    sender.sendMessage(GameManager.PREFIX + "§cNo permission.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Must be a player to use the setup GUI.");
                    return true;
                }
                SetupGUI.open(player, plugin);
            }

            case "stop" -> {
                if (!sender.hasPermission("bingorace.admin")) {
                    sender.sendMessage(GameManager.PREFIX + "§cNo permission.");
                    return true;
                }
                gm.stopGame(sender);
            }

            case "card" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Must be a player.");
                    return true;
                }
                var team = gm.getTeamOf(player.getUniqueId());
                if (team == null) {
                    player.sendMessage(GameManager.PREFIX + "§cYou are not in a game.");
                    return true;
                }
                BingoCardGUI.open(player, team);
            }

            case "config" -> {
                if (!sender.hasPermission("bingorace.admin")) {
                    sender.sendMessage(GameManager.PREFIX + "§cNo permission.");
                    return true;
                }
                handleConfig(sender, args, gm);
            }

            case "status" -> {
                sender.sendMessage("§8§m──────────────────────────");
                sender.sendMessage("§a§lBINGO RACE STATUS");
                sender.sendMessage("§7State: §f" + gm.getState());
                sender.sendMessage("§7Difficulty: " + gm.getDifficulty().getDisplay());
                sender.sendMessage("§7Mode: §f" + (gm.isSoloMode() ? "Solo" : "Teams (" + gm.getTeamSize() + " per team, " + gm.getTeamCount() + " teams)"));
                sender.sendMessage("§7Lobby: §f" + gm.getLobby().size() + " players");
                sender.sendMessage("§7Teams: §f" + gm.getTeams().size());
                sender.sendMessage("§8§m──────────────────────────");
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleConfig(CommandSender sender, String[] args, GameManager gm) {
        if (args.length < 3) {
            sender.sendMessage(GameManager.PREFIX + "§7Config options:");
            sender.sendMessage("§e/br config difficulty <easy|medium|hard>");
            sender.sendMessage("§e/br config teamsize <1-10>");
            sender.sendMessage("§e/br config teams <2-8>");
            sender.sendMessage("§e/br config solo <true|false>");
            sender.sendMessage("§e/br config countdown <seconds>");
            sender.sendMessage("§e/br config timelimit <seconds> §7(0=no limit)");
            sender.sendMessage("§7Current: diff=§f" + gm.getDifficulty().name()
                + "§7 teamsize=§f" + gm.getTeamSize()
                + "§7 teams=§f" + gm.getTeamCount()
                + "§7 solo=§f" + gm.isSoloMode());
            return;
        }

        String key = args[1].toLowerCase();
        String val = args[2].toLowerCase();

        switch (key) {
            case "difficulty" -> {
                try {
                    Difficulty d = Difficulty.valueOf(val.toUpperCase());
                    gm.setDifficulty(d);
                    sender.sendMessage(GameManager.PREFIX + "§aDifficulty set to " + d.getDisplay());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(GameManager.PREFIX + "§cInvalid difficulty. Use: easy, medium, hard");
                }
            }
            case "teamsize" -> {
                try {
                    int s = Integer.parseInt(val);
                    if (s < 1 || s > 10) { sender.sendMessage(GameManager.PREFIX + "§cMust be 1-10."); return; }
                    gm.setTeamSize(s);
                    sender.sendMessage(GameManager.PREFIX + "§aTeam size set to §f" + s);
                } catch (NumberFormatException e) {
                    sender.sendMessage(GameManager.PREFIX + "§cMust be a number.");
                }
            }
            case "teams" -> {
                try {
                    int t = Integer.parseInt(val);
                    if (t < 2 || t > 8) { sender.sendMessage(GameManager.PREFIX + "§cMust be 2-8."); return; }
                    gm.setTeamCount(t);
                    sender.sendMessage(GameManager.PREFIX + "§aTeam count set to §f" + t);
                } catch (NumberFormatException e) {
                    sender.sendMessage(GameManager.PREFIX + "§cMust be a number.");
                }
            }
            case "solo" -> {
                boolean b = val.equals("true") || val.equals("on") || val.equals("yes");
                gm.setSoloMode(b);
                sender.sendMessage(GameManager.PREFIX + "§aSolo mode: " + (b ? "§aON" : "§cOFF"));
            }
            case "countdown" -> {
                try {
                    int c = Integer.parseInt(val);
                    if (c < 3 || c > 60) { sender.sendMessage(GameManager.PREFIX + "§cMust be 3-60 seconds."); return; }
                    plugin.getConfig().set("game.countdown", c);
                    plugin.saveConfig();
                    sender.sendMessage(GameManager.PREFIX + "§aCountdown set to §f" + c + "s");
                } catch (NumberFormatException e) {
                    sender.sendMessage(GameManager.PREFIX + "§cMust be a number.");
                }
            }
            case "timelimit", "time", "timer" -> {
                try {
                    int t = Integer.parseInt(val);
                    if (t < 0) { sender.sendMessage(GameManager.PREFIX + "§cMust be >= 0 (0 = no limit)."); return; }
                    gm.setTimeLimit(t);
                    String display = t == 0 ? "§aNo limit" : "§f" + (t / 60) + "m " + (t % 60) + "s";
                    sender.sendMessage(GameManager.PREFIX + "§aTime limit set to " + display);
                } catch (NumberFormatException e) {
                    sender.sendMessage(GameManager.PREFIX + "§cMust be a number in seconds. e.g. 3600 = 1 hour");
                }
            }
            default -> sender.sendMessage(GameManager.PREFIX + "§cUnknown config key: " + key);
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m──────────────────────────");
        sender.sendMessage("§a§lBINGO RACE COMMANDS");
        sender.sendMessage("§e/br start §7— Open setup GUI and start game");
        sender.sendMessage("§e/br stop §7— Stop and reset the game");
        sender.sendMessage("§e/br card §7— View your bingo card");
        sender.sendMessage("§e/br config <key> <val> §7— Change settings");
        sender.sendMessage("§e/br status §7— View current game info");
        sender.sendMessage("§e/bjoin §7— Join the lobby");
        sender.sendMessage("§8§m──────────────────────────");
    }
}