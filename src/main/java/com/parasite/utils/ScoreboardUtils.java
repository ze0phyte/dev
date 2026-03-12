package com.parasite.utils;

import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import com.parasite.game.GameState;
import com.parasite.game.Role;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardUtils {

    // ── Sidebar scoreboard shown to all players ──────────────────────────────

    public static void updateSidebar(GameManager gm) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateSidebar(p, gm);
        }
    }

    public static void updateSidebar(Player player, GameManager gm) {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getNewScoreboard();

        Objective obj = board.registerNewObjective("parasite", Criteria.DUMMY, "§5§l☣ PARASITE ☣");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        GameState state = gm.getState();
        int line = 15;

        if (state == GameState.WAITING || state == GameState.STARTING) {
            setLine(board, obj, line--, "§r");
            setLine(board, obj, line--, "§ePlayers: §a" + gm.getPlayerCount() + "/" + gm.getMaxPlayers());
            setLine(board, obj, line--, "§r§r");
            if (state == GameState.STARTING) {
                setLine(board, obj, line--, "§eStarting in: §a" + gm.getTimer() + "s");
            } else {
                setLine(board, obj, line--, "§7Waiting for players...");
                setLine(board, obj, line--, "§8Min: " + gm.getMinPlayers() + " players");
            }
            setLine(board, obj, line--, "§r§r§r");
            setLine(board, obj, line--, "§b/pjoin §7to board ship");
        } else {
            GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
            setLine(board, obj, line--, "§r");
            setLine(board, obj, line--, "§eDay: §f" + gm.getCurrentDay());
            setLine(board, obj, line--, "§r§r");

            String phase;
            switch (state) {
                case IN_ROUND -> phase = "§a⚙ IN ROUND";
                case DISCUSSION -> phase = "§e☎ DISCUSSION";
                case VOTING -> phase = "§c🗳 VOTING";
                case ROUND_END -> phase = "§6§l RESULTS";
                default -> phase = "§7...";
            }
            setLine(board, obj, line--, "§7Phase: " + phase);
            setLine(board, obj, line--, "§7Time: §f" + formatTime(gm.getTimer()));
            setLine(board, obj, line--, "§r§r§r");

            // Show alive crew count
            long alive = gm.getAlivePlayers().size();
            long dead = gm.getAllGamePlayers().stream().filter(g -> !g.isAlive()).count();
            setLine(board, obj, line--, "§7Alive: §a" + alive + " §7| Dead: §c" + dead);
            setLine(board, obj, line--, "§r§r§r§r");

            if (gp != null && gp.isAlive()) {
                setLine(board, obj, line--, "§7Your role:");
                setLine(board, obj, line--, gp.getRole().getColor() + "§l" + stripColor(gp.getRole().getDisplay()));

                if (gp.getRole() == Role.PARASITE) {
                    setLine(board, obj, line--, "§r§r§r§r§r");
                    long cd = gm.getSwapCooldownRemaining(player.getUniqueId());
                    if (cd > 0) {
                        setLine(board, obj, line--, "§cSwap [G]: §f" + cd + "s");
                    } else {
                        setLine(board, obj, line--, "§aSwap [G]: §fREADY");
                    }
                    if (gp.isInfected()) {
                        setLine(board, obj, line--, "§4Infected someone this round");
                    }
                }
                if (gp.getRole() == Role.DOCTOR) {
                    setLine(board, obj, line--, "§r§r§r§r§r");
                    setLine(board, obj, line--, gp.isSavedThisRound() ? "§7Save: §aUsed" : "§7Save: §bAvailable");
                }
            } else if (gp != null && !gp.isAlive()) {
                setLine(board, obj, line--, "§r§r§r§r");
                setLine(board, obj, line--, "§8☠ You are dead");
                setLine(board, obj, line--, "§8Spectating...");
            }
        }

        player.setScoreboard(board);
    }

    private static void setLine(Scoreboard board, Objective obj, int score, String text) {
        // Truncate to 40 chars to be safe
        if (text.length() > 40) text = text.substring(0, 40);
        Score s = obj.getScore(text);
        s.setScore(score);
    }

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }

    // ── Tab list header/footer (shows online count in lobby) ─────────────────

    public static void updateTabList(GameManager gm) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateTabList(p, gm);
        }
    }

    public static void updateTabList(Player player, GameManager gm) {
        GameState state = gm.getState();
        String header, footer;
        if (state == GameState.WAITING || state == GameState.STARTING) {
            header = "\n§5§l☣ PARASITE — SPACE MAFIA ☣\n";
            footer = "\n§ePlayers in lobby: §a" + gm.getPlayerCount() + " §8/ §7" + gm.getMaxPlayers()
                    + "\n§7Type §b/pjoin §7to board the ship!\n";
        } else {
            header = "\n§5§l☣ PARASITE — DAY " + gm.getCurrentDay() + " ☣\n";
            footer = "\n§7Alive: §a" + gm.getAlivePlayers().size()
                    + "  §7Dead: §c" + (gm.getAllGamePlayers().stream().filter(g -> !g.isAlive()).count())
                    + "\n";
        }
        player.setPlayerListHeaderFooter(header, footer);
    }
}
