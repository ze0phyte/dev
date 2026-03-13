package com.parasite.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * Name tag hiding works by putting every player into a team with NAME_TAG_VISIBILITY=NEVER.
 * CRITICAL: We must apply this team to EVERY player's individual scoreboard, not just the
 * main scoreboard. ScoreboardUtils gives each player their own scoreboard for the info
 * display, which means the main scoreboard team is invisible to them.
 * Solution: we track hidden state ourselves and apply it on every scoreboard set.
 */
public class SkinUtils {

    private static final String HIDDEN_TEAM = "parasite_hidden";
    private static boolean namesHidden = false;

    public static boolean areNamesHidden() { return namesHidden; }

    /**
     * Hide all name tags. Applies the hidden team to EVERY player's current scoreboard
     * so it works regardless of whether they have a custom scoreboard or the main one.
     */
    public static void hideAllNames() {
        namesHidden = true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyHiddenTeam(p.getScoreboard(), p);
        }
    }

    /**
     * Call this whenever a player's scoreboard is changed (e.g. when /parasite info is toggled).
     * Ensures the hidden team carries over to the new scoreboard.
     */
    public static void applyHiddenIfNeeded(Player player) {
        if (namesHidden) {
            applyHiddenTeam(player.getScoreboard(), player);
        }
    }

    private static void applyHiddenTeam(Scoreboard board, Player player) {
        Team team = board.getTeam(HIDDEN_TEAM);
        if (team == null) {
            team = board.registerNewTeam(HIDDEN_TEAM);
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setCanSeeFriendlyInvisibles(false);
        // Add ALL online players to this team on this board so everyone is hidden
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!team.hasEntry(p.getName())) team.addEntry(p.getName());
        }
    }

    /** Show name tags again (discussion / voting / game end). */
    public static void showAllNames() {
        namesHidden = false;
        // Apply to main scoreboard
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team mainTeam = main.getTeam(HIDDEN_TEAM);
        if (mainTeam != null) {
            mainTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            for (Player p : Bukkit.getOnlinePlayers()) mainTeam.removeEntry(p.getName());
        }
        // Apply to each player's individual scoreboard
        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            Team team = board.getTeam(HIDDEN_TEAM);
            if (team != null) {
                team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
                for (Player other : Bukkit.getOnlinePlayers()) team.removeEntry(other.getName());
            }
        }
    }

    /** Set Steve skin via SkinsRestorer. Correct command is "sr setskin". */
    public static void setCrewSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr setskin " + player.getName() + " MHF_Steve");
        }
    }

    /** Restore original skin via SkinsRestorer. */
    public static void restoreOriginalSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr applyskin " + player.getName());
        }
    }

    /** Cleanup teams on game reset. */
    public static void cleanup() {
        namesHidden = false;
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        Team t = main.getTeam(HIDDEN_TEAM);
        if (t != null) t.unregister();
        // Also clean from each player's individual scoreboard
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team pt = p.getScoreboard().getTeam(HIDDEN_TEAM);
            if (pt != null) pt.unregister();
        }
    }
}