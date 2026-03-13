package com.parasite.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class SkinUtils {

    private static final String HIDDEN_TEAM = "parasite_hidden";

    // Hide all name tags above heads during the round
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

    // Show name tags again (discussion / voting / game end)
    public static void showAllNames() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getMainScoreboard();

        Team team = board.getTeam(HIDDEN_TEAM);
        if (team == null) return;
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        for (Player p : Bukkit.getOnlinePlayers()) {
            team.removeEntry(p.getName());
        }
    }

    // Set player skin to Steve via SkinsRestorer
    public static void setCrewSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr setskin " + player.getName() + " MHF_Steve");
        }
    }

    // Restore original skin via SkinsRestorer
    public static void restoreOriginalSkin(Player player) {
        if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "sr applyskin " + player.getName());
        }
    }

    // Cleanup teams on reset
    public static void cleanup() {
        ScoreboardManager sbm = Bukkit.getScoreboardManager();
        Scoreboard board = sbm.getMainScoreboard();
        Team t = board.getTeam(HIDDEN_TEAM);
        if (t != null) t.unregister();
    }
}