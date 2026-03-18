package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.Difficulty;
import com.bingorace.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class SetupGUI {

    public static final String TITLE = "§8Setup Bingo Race";

    public static void open(Player player, BingoRacePlugin plugin) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);

        GameManager gm = plugin.getGameManager();

        // Row 1: Difficulty selection (slots 10, 12, 14 — left side)
        inv.setItem(2,  makeItem(Material.LIME_WOOL,   "§a§lEASY",   List.of("§73x3 grid — §f9 items", "§8Common overworld items", gm.getDifficulty() == Difficulty.EASY ? "§a✔ Selected" : "§7Click to select")));
        inv.setItem(11, makeItem(Material.YELLOW_WOOL, "§e§lMEDIUM", List.of("§74x4 grid — §f16 items", "§8Crafted & mob drop items", gm.getDifficulty() == Difficulty.MEDIUM ? "§a✔ Selected" : "§7Click to select")));
        inv.setItem(20, makeItem(Material.RED_WOOL,    "§c§lHARD",   List.of("§75x5 grid — §f25 items", "§8Rare & nether items", gm.getDifficulty() == Difficulty.HARD ? "§a✔ Selected" : "§7Click to select")));

        // Row 2: Team size (slots 29-33)
        inv.setItem(29, makeItem(Material.PLAYER_HEAD, "§fSolo Mode", List.of("§7Every player has their own card", gm.isSoloMode() ? "§a✔ Selected" : "§7Click to select")));
        inv.setItem(31, makeItem(Material.OAK_SIGN, "§fTeam Size: §e" + gm.getTeamSize(), List.of("§7Players per team", "§eLeft click §7to increase", "§eRight click §7to decrease")));
        inv.setItem(33, makeItem(Material.BOOKSHELF, "§fTeam Count: §e" + gm.getTeamCount(), List.of("§7Number of teams", "§eLeft click §7to increase", "§eRight click §7to decrease")));

        // Current settings summary (slot 4)
        inv.setItem(4, makeItem(Material.PAPER, "§f§lCurrent Settings", List.of(
            "§7Difficulty: " + gm.getDifficulty().getDisplay(),
            "§7Mode: " + (gm.isSoloMode() ? "§eSolo" : "§eTeams (" + gm.getTeamSize() + " per team, " + gm.getTeamCount() + " teams)"),
            "§7Lobby: §f" + gm.getLobby().size() + " players"
        )));

        // Confirm button (slot 49)
        inv.setItem(49, makeItem(Material.EMERALD_BLOCK, "§a§lSTART GAME", List.of("§7Click to confirm and start!", "§8World will be regenerated.")));

        // Cancel button (slot 45)
        inv.setItem(45, makeItem(Material.BARRIER, "§c§lCANCEL", List.of("§7Close without starting")));

        // Fill remaining with glass
        ItemStack glass = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, glass);
        }

        player.openInventory(inv);
    }

    private static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
