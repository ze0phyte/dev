package com.parasite.commands;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GameState;
import com.parasite.game.Role;
import com.parasite.utils.ScoreboardUtils;
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

        if (args.length == 0) { sendHelp(sender); return true; }

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

            case "setdiscussion" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.setDiscussionLocation(p.getLocation());
                sender.sendMessage(GameManager.PREFIX + "§aDiscussion area set!");
            }

            case "setvoting" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.setVotingLocation(p.getLocation());
                sender.sendMessage(GameManager.PREFIX + "§aVoting area set!");
            }

            case "setrole" -> {
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
                if (args.length < 2) { sender.sendMessage(GameManager.PREFIX + "§cUsage: /parasite addplayer <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(GameManager.PREFIX + "§cPlayer not found."); return true; }
                gm.joinLobby(target);
                sender.sendMessage(GameManager.PREFIX + "§aAdded §f" + target.getName() + " §ato the lobby.");
            }

            case "skipday" -> {
                if (!gm.isRunning()) { sender.sendMessage(GameManager.PREFIX + "§cNo game running."); return true; }
                gm.skipToDiscussion();
                sender.sendMessage(GameManager.PREFIX + "§aSkipped to discussion!");
            }

            case "skipvote" -> {
                if (!gm.isRunning()) { sender.sendMessage(GameManager.PREFIX + "§cNo game running."); return true; }
                gm.skipToVoteEnd();
                sender.sendMessage(GameManager.PREFIX + "§aSkipped voting!");
            }

            case "info" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                ScoreboardUtils.toggleInfo(p, gm);
            }

            case "status" -> {
                sender.sendMessage("§8§m──────────────────────────");
                sender.sendMessage("§c§l☣ PARASITE STATUS");
                sender.sendMessage("§7State: §f" + gm.getState());
                sender.sendMessage("§7Day: §f" + gm.getCurrentDay());
                sender.sendMessage("§7Players in game: §f" + gm.getPlayerCount());
                sender.sendMessage("§7Alive: §f" + gm.getAlivePlayers().size());
                sender.sendMessage("§7Lobby: §f"      + (gm.getLobbyLocation()      != null ? "§aSet" : "§cNot set"));
                sender.sendMessage("§7Arena: §f"      + (gm.getArenaLocation()      != null ? "§aSet" : "§cNot set"));
                sender.sendMessage("§7Discussion: §f" + (gm.getDiscussionLocation() != null ? "§aSet" : "§cNot set"));
                sender.sendMessage("§7Voting: §f"     + (gm.getVotingLocation()     != null ? "§aSet" : "§cNot set"));
                sender.sendMessage("§8§m──────────────────────────");
            }

            case "addfoodstation" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.addFoodStationCmd(p, p.getLocation());
            }

            case "lastwill" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("§cMust be a player."); return true; }
                gm.giveLastWillBook(p);
            }

            case "config" -> {
                // /parasite config                 — show current values
                // /parasite config <key> <value>   — set a value
                if (args.length == 1) {
                    sender.sendMessage(GameManager.PREFIX + "§7Current config:");
                    sender.sendMessage(gm.getConfigSummary());
                } else if (args.length == 3) {
                    String key = args[1];
                    try {
                        int val = Integer.parseInt(args[2]);
                        String err = gm.setConfigValue(key, val);
                        if (err != null) {
                            sender.sendMessage(GameManager.PREFIX + err);
                        } else {
                            sender.sendMessage(GameManager.PREFIX + "§aSet §e" + key + " §ato §f" + val);
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(GameManager.PREFIX + "§cValue must be a number.");
                    }
                } else {
                    sender.sendMessage(GameManager.PREFIX + "§cUsage: §e/parasite config §7or §e/parasite config <key> <value>");
                    sender.sendMessage("§7Keys: §fday-duration, discussion-duration, voting-duration, swap-cooldown, min-players, max-players, lobby-countdown, parasite-count");
                }
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m──────────────────────────");
        sender.sendMessage("§c§l☣ PARASITE COMMANDS");
        sender.sendMessage("§e/parasite start §7— Start the game");
        sender.sendMessage("§e/parasite stop §7— Force stop");
        sender.sendMessage("§e/parasite setlobby §7— Set lobby spawn");
        sender.sendMessage("§e/parasite setarena §7— Set arena centre");
        sender.sendMessage("§e/parasite setdiscussion §7— Set discussion area");
        sender.sendMessage("§e/parasite setvoting §7— Set voting area");
        sender.sendMessage("§e/parasite setrole <player> <role> §7— Force a role");
        sender.sendMessage("§e/parasite addplayer <player> §7— Add to lobby");
        sender.sendMessage("§e/parasite skipday §7— Skip to discussion now");
        sender.sendMessage("§e/parasite skipvote §7— End voting now");
        sender.sendMessage("§e/parasite config [key] [val] §7— View/change config");
        sender.sendMessage("§e/parasite addfoodstation §7— Mark food station at your feet");
        sender.sendMessage("§e/parasite lastwill §7— Get a last will book");
        sender.sendMessage("§e/parasite info §7— Toggle info sidebar");
        sender.sendMessage("§e/parasite status §7— Show game info in chat");
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