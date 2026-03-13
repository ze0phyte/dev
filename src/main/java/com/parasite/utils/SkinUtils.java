package com.parasite.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * Name hiding strategy:
 * The problem is ScoreboardUtils gives each player their own Scoreboard object.
 * Teams on the MAIN scoreboard don't apply to players using a custom scoreboard.
 * Fix: we apply the hidden team to EVERY player's individual scoreboard whenever
 * names need to be hidden, and re-apply it every time a scoreboard is swapped.
 */
public class SkinUtils {

    private static final String HIDDEN_TEAM = "parasite_hidden";
    private static boolean namesCurrentlyHidden = false;

    public static boolean areNamesHidden() {
        return namesCurrentlyHidden;
    }

    /** Hide all name tags. Must be called after players have their scoreboards set. */
    public static void hideAllNames() {
        namesCurrentlyHidden = true;
        // Apply to main scoreboard
        applyToBoard(Bukkit.getScoreboardManager().getMainScoreboard(), true);
        // Apply to every player's individual scoreboard
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyToBoard(p.getScoreboard(), true);
        }
    }

    /**
     * Call this whenever a player's scoreboard changes so name hiding carries over.
     * Called by ScoreboardUtils every time it sets a new scoreboard.
     */
    public static void applyHiddenIfNeeded(Player player) {
        if (namesCurrentlyHidden) {
            applyToBoard(player.getScoreboard(), true);
        }
    }

    /** Show name tags again. */
    public static void showAllNames() {
        namesCurrentlyHidden = false;
        applyToBoard(Bukkit.getScoreboardManager().getMainScoreboard(), false);
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyToBoard(p.getScoreboard(), false);
        }
    }

    private static void applyToBoard(Scoreboard board, boolean hide) {
        Team team = board.getTeam(HIDDEN_TEAM);
        if (hide) {
            if (team == null) team = board.registerNewTeam(HIDDEN_TEAM);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setCanSeeFriendlyInvisibles(false);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!team.hasEntry(p.getName())) team.addEntry(p.getName());
            }
        } else {
            if (team != null) {
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                for (Player p : Bukkit.getOnlinePlayers()) team.removeEntry(p.getName());
            }
        }
    }

    /**
     * Set Steve skin via SkinsRestorer.
     * Correct command is "sr setskin <player> <skin>"
     */
    public static void setCrewSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "skin set " + player.getName() + " MHF_Steve");
        }
    }

    /** Restore original skin via SkinsRestorer. */
    public static void restoreOriginalSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "skin clear " + player.getName());
        }
    }

    /** Cleanup on game reset. */
    public static void cleanup() {
        namesCurrentlyHidden = false;
        // Clean main scoreboard
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(HIDDEN_TEAM);
        if (t != null) t.unregister();
        // Clean each player's scoreboard
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team pt = p.getScoreboard().getTeam(HIDDEN_TEAM);
            if (pt != null) pt.unregister();
        }
    }
}