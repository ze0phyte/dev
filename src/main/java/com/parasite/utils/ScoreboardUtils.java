package com.parasite.utils;

import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import com.parasite.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ScoreboardUtils {

    private static final Set<UUID> infoEnabled = new HashSet<>();

    public static void toggleInfo(Player player, GameManager gm) {
        if (infoEnabled.contains(player.getUniqueId())) {
            infoEnabled.remove(player.getUniqueId());
            clearSidebar(player);
            player.sendMessage(GameManager.PREFIX + "§7Info display §coff§7.");
        } else {
            infoEnabled.add(player.getUniqueId());
            showInfo(player, gm);
            player.sendMessage(GameManager.PREFIX + "§7Info display §aon§7. Run again to refresh or turn off.");
        }
    }

    public static void showInfo(Player player, GameManager gm) {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getNewScoreboard();

        // CRITICAL: re-apply hidden team to this new scoreboard if names should be hidden
        SkinUtils.applyHiddenIfNeeded(player);

        Objective obj = board.registerNewObjective("pinfo", Criteria.DUMMY, "§5§l☣ PARASITE ☣");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        GameState state = gm.getState();
        int line = 15;

        if (state == GameState.WAITING || state == GameState.STARTING) {
            setLine(board, obj, line--, "§r");
            setLine(board, obj, line--, "§7Status: " + (state == GameState.STARTING ? "§eStarting..." : "§7Waiting"));
            setLine(board, obj, line--, "§7Players: §f" + gm.getPlayerCount() + " §7/ §f" + gm.getMaxPlayers());
            setLine(board, obj, line--, "§7Min to start: §f" + gm.getMinPlayers());
            setLine(board, obj, line--, "§r§r");
            setLine(board, obj, line--, "§b/pjoin §7to join");
        } else {
            long alive = gm.getAlivePlayers().size();
            long total = gm.getAllGamePlayers().size();
            long dead = total - alive;

            String phase = switch (state) {
                case IN_ROUND   -> "§aIn Round";
                case DISCUSSION -> "§eDiscussion";
                case VOTING     -> "§cVoting";
                case ROUND_END  -> "§6Results";
                default         -> "§7-";
            };

            setLine(board, obj, line--, "§r");
            setLine(board, obj, line--, "§7Day: §f" + gm.getCurrentDay());
            setLine(board, obj, line--, "§7Phase: " + phase);
            setLine(board, obj, line--, "§7Time: §f" + formatTime(gm.getTimer()));
            setLine(board, obj, line--, "§r§r");
            setLine(board, obj, line--, "§7Alive: §a" + alive + "  §7Dead: §c" + dead);
            setLine(board, obj, line--, "§r§r§r");

            for (GamePlayer gp : gm.getAllGamePlayers()) {
                String status = gp.isAlive() ? "§a✔" : "§c✘";
                String roleTag = player.isOp()
                        ? " §8[" + gp.getRole().getColor() + stripColor(gp.getRole().getDisplay()) + "§8]"
                        : "";
                String entry = status + " §f" + gp.getName() + roleTag;
                if (line > 0) setLine(board, obj, line--, entry);
            }
        }

        player.setScoreboard(board);

        // Re-apply name hiding to the new scoreboard
        SkinUtils.applyHiddenIfNeeded(player);
    }

    public static void refreshAll(GameManager gm) {
        for (UUID id : infoEnabled) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) showInfo(p, gm);
        }
    }

    public static void clearSidebar(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        player.setScoreboard(board);
        // Re-apply name hiding to fresh scoreboard
        SkinUtils.applyHiddenIfNeeded(player);
    }

    public static void clearAll() {
        infoEnabled.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public static void updateTabList(GameManager gm) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOp()) updateTabList(p, gm);
        }
    }

    public static void updateTabList(Player player, GameManager gm) {
        if (!player.isOp()) return;
        GameState state = gm.getState();
        String header, footer;
        if (state == GameState.WAITING || state == GameState.STARTING) {
            header = "\n§5§l☣ PARASITE §8| §7Admin View\n";
            footer = "\n§7Lobby: §a" + gm.getPlayerCount() + " §7/ §f" + gm.getMaxPlayers() + "\n";
        } else {
            header = "\n§5§l☣ PARASITE §8| §7Day " + gm.getCurrentDay() + " — " + state.name() + "\n";
            footer = "\n§7Alive: §a" + gm.getAlivePlayers().size()
                    + "  §7Dead: §c" + (gm.getAllGamePlayers().stream().filter(g -> !g.isAlive()).count()) + "\n";
        }
        player.setPlayerListHeaderFooter(header, footer);
    }

    private static void setLine(Scoreboard board, Objective obj, int score, String text) {
        if (text.length() > 40) text = text.substring(0, 40);
        obj.getScore(text).setScore(score);
    }

    private static String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }
}