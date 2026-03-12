package com.parasite.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

/**
 * Handles hiding/showing player name tags above heads and in tab list.
 *
 * During IN_ROUND: all players get the same "CREW" skin feel and names are hidden.
 * During DISCUSSION/VOTING/LOBBY: names are visible.
 *
 * Note: Actual skin texture swapping requires ProtocolLib or a NMS approach.
 * This implementation handles the name-hiding part natively through Scoreboards/Teams.
 * For the "blue shirt Steve" skin, players will need to have that skin set manually
 * OR you can integrate SkinsRestorer plugin commands via Bukkit.dispatchCommand.
 */
public class SkinUtils {

    private static final String HIDDEN_TEAM = "parasite_hidden";
    private static final String VISIBLE_TEAM = "parasite_visible";

    /**
     * Hide all name tags above players' heads during the round.
     */
    public static void hideAllNames() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getMainScoreboard();

        Team team = board.getTeam(HIDDEN_TEAM);
        if (team == null) team = board.registerNewTeam(HIDDEN_TEAM);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        team.setCanSeeFriendlyInvisibles(false);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!team.hasEntry(p.getName())) team.addEntry(p.getName());
        }
    }

    /**
     * Restore name tags for all players.
     */
    public static void showAllNames() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getMainScoreboard();

        Team team = board.getTeam(HIDDEN_TEAM);
        if (team != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                team.removeEntry(p.getName());
            }
        }
    }

    /**
     * Attempts to set a player's skin to the default Steve blue-shirt skin
     * by dispatching a SkinsRestorer command if that plugin is present.
     * Falls back gracefully if not installed.
     */
    public static void setCrewSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            // SkinsRestorer command: /sr set <player> <skin>
            // "MHF_Steve" is the default Steve skin in SkinsRestorer
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr set " + player.getName() + " MHF_Steve");
        }
        // Without SkinsRestorer, skin changes aren't doable in vanilla Spigot without NMS/ProtocolLib
    }

    /**
     * Restore player's original skin via SkinsRestorer.
     */
    public static void restoreOriginalSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr applyskin " + player.getName());
        }
    }

    /**
     * Remove all teams created by this plugin (cleanup).
     */
    public static void cleanup() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getMainScoreboard();
        Team t1 = board.getTeam(HIDDEN_TEAM);
        if (t1 != null) t1.unregister();
        Team t2 = board.getTeam(VISIBLE_TEAM);
        if (t2 != null) t2.unregister();
    }
}
