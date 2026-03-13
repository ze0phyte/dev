package com.parasite.utils;

import com.parasite.ParasitePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * Skin changes: SkinsRestorer console command is "/skin set <player> <skin>"
 * The dispatchCommand runs it as console which has full permissions.
 *
 * Name hiding: We apply the hidden team to EVERY player's scoreboard individually
 * because ScoreboardUtils gives each player their own scoreboard object.
 * Teams on the main scoreboard don't carry over to custom scoreboards.
 */
public class SkinUtils {

    private static final String HIDDEN_TEAM = "parasite_hidden";
    private static boolean namesCurrentlyHidden = false;

    public static boolean areNamesHidden() { return namesCurrentlyHidden; }

    public static void hideAllNames() {
        namesCurrentlyHidden = true;
        applyToBoard(Bukkit.getScoreboardManager().getMainScoreboard(), true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            applyToBoard(p.getScoreboard(), true);
        }
    }

    /** Called by ScoreboardUtils every time it assigns a new scoreboard to a player. */
    public static void applyHiddenIfNeeded(Player player) {
        if (namesCurrentlyHidden) {
            applyToBoard(player.getScoreboard(), true);
        }
    }

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
     * Set Steve skin. Modern SkinsRestorer command: /skin set <player> <skin>
     * Called from console so it has full admin permissions.
     */
    /**
     * Sets player skin to Steve by running the command AS the player.
     * "skin set steve_disguise" works as a player command; the admin
     * variant with a target name does not work on this SR version.
     */
    public static void setCrewSkin(Player player, ParasitePlugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            player.performCommand("skin set steve_disguise");
        }
    }

    /**
     * Restore original skin. /skin clear <player> resets to their Mojang account skin.
     */
    public static void restoreOriginalSkin(Player player, ParasitePlugin plugin) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            player.performCommand("skin clear");
        }
    }

    public static void cleanup() {
        namesCurrentlyHidden = false;
        Team t = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(HIDDEN_TEAM);
        if (t != null) t.unregister();
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team pt = p.getScoreboard().getTeam(HIDDEN_TEAM);
            if (pt != null) pt.unregister();
        }
    }
}