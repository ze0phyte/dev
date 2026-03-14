package com.parasite.utils;

import com.parasite.ParasitePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Collection;
import java.util.Collections;

public class SkinUtils {

    private static final String HIDDEN_TEAM   = "parasite_hidden";
    private static final String PARASITE_TEAM = "parasite_peer";
    private static boolean namesCurrentlyHidden = false;

    public static boolean areNamesHidden() { return namesCurrentlyHidden; }

    /** Hide all nametags. Pass alive parasites so they can see each other. */
    public static void hideAllNames(Collection<Player> parasites) {
        namesCurrentlyHidden = true;
        applyHideToBoard(Bukkit.getScoreboardManager().getMainScoreboard(), parasites, false);
        for (Player p : Bukkit.getOnlinePlayers()) {
            boolean isParasite = parasites.contains(p);
            applyHideToBoard(p.getScoreboard(), parasites, isParasite);
        }
    }

    /** No-parasite overload for non-round phases. */
    public static void hideAllNames() {
        hideAllNames(Collections.emptyList());
    }

    /** Called by ScoreboardUtils every time it assigns a new scoreboard to a player. */
    public static void applyHiddenIfNeeded(Player player) {
        if (namesCurrentlyHidden) {
            applyHideToBoard(player.getScoreboard(), Collections.emptyList(), false);
        }
    }

    public static void showAllNames() {
        namesCurrentlyHidden = false;
        cleanBoard(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player p : Bukkit.getOnlinePlayers()) {
            cleanBoard(p.getScoreboard());
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private static void applyHideToBoard(Scoreboard board, Collection<Player> parasites, boolean isParasite) {
        // 1. Hidden team — nobody sees nametags
        Team hidden = board.getTeam(HIDDEN_TEAM);
        if (hidden == null) hidden = board.registerNewTeam(HIDDEN_TEAM);
        hidden.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        hidden.setCanSeeFriendlyInvisibles(false);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hidden.hasEntry(p.getName())) hidden.addEntry(p.getName());
        }

        // 2. Parasite peer team — only on parasites' scoreboards
        Team old = board.getTeam(PARASITE_TEAM);
        if (old != null) old.unregister();

        if (isParasite && !parasites.isEmpty()) {
            Team peerTeam = board.registerNewTeam(PARASITE_TEAM);
            peerTeam.setPrefix("§4☣ §r");
            peerTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
            peerTeam.setCanSeeFriendlyInvisibles(true);
            for (Player p : parasites) {
                // Remove from hidden team first so peer team overrides
                if (hidden.hasEntry(p.getName())) hidden.removeEntry(p.getName());
                peerTeam.addEntry(p.getName());
            }
        }
    }

    private static void cleanBoard(Scoreboard board) {
        Team ht = board.getTeam(HIDDEN_TEAM);
        if (ht != null) {
            ht.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
            for (Player p : Bukkit.getOnlinePlayers()) ht.removeEntry(p.getName());
        }
        Team pt = board.getTeam(PARASITE_TEAM);
        if (pt != null) pt.unregister();
    }

    /**
     * Sets player skin to Steve.
     * REQUIRES: Run once in console: sr createcustom steve_disguise <url>
     */
    public static void setCrewSkin(Player player, ParasitePlugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) return;
        org.bukkit.permissions.PermissionAttachment att = player.addAttachment(plugin);
        att.setPermission("skinsrestorer.command", true);
        att.setPermission("skinsrestorer.command.set", true);
        player.performCommand("skin set steve_disguise");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.removeAttachment(att), 40L);
    }

    /** Restores player's original Mojang skin. */
    public static void restoreOriginalSkin(Player player, ParasitePlugin plugin) {
        if (!Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) return;
        org.bukkit.permissions.PermissionAttachment att = player.addAttachment(plugin);
        att.setPermission("skinsrestorer.command", true);
        att.setPermission("skinsrestorer.command.clear", true);
        player.performCommand("skin clear");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> player.removeAttachment(att), 40L);
    }

    public static void cleanup() {
        namesCurrentlyHidden = false;
        cleanBoard(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player p : Bukkit.getOnlinePlayers()) cleanBoard(p.getScoreboard());
    }
}