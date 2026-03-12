package com.parasite.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;

public class ItemUtils {

    /** Vote paper with a player's name */
    public static ItemStack votePaper(String playerName) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lVOTE: §f" + playerName);
        meta.setLore(Arrays.asList(
                "§7Right-click to vote for",
                "§e" + playerName
        ));
        // Store player name in lore line 1 so we can read it back
        item.setItemMeta(meta);
        return item;
    }

    /** Skip vote paper */
    public static ItemStack skipPaper() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7§lSKIP VOTE");
        meta.setLore(Collections.singletonList("§7Right-click to skip voting this round"));
        item.setItemMeta(meta);
        return item;
    }

    /** Doctor save paper - given to doctor during voting */
    public static ItemStack savePaper(String playerName) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lSAVE: §f" + playerName);
        meta.setLore(Arrays.asList(
                "§7Right-click to protect",
                "§b" + playerName + " §7from infection tonight"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Iron axe for crewmates */
    public static ItemStack crewAxe() {
        ItemStack item = new ItemStack(Material.IRON_AXE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Maintenance Tool");
        meta.setLore(Collections.singletonList("§8Standard crew equipment"));
        item.setItemMeta(meta);
        return item;
    }

    /** Crossbow - one use per round to reveal a name */
    public static ItemStack scanCrossbow() {
        ItemStack item = new ItemStack(Material.CROSSBOW, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§eIdentity Scanner");
        meta.setLore(Arrays.asList(
                "§7Shoot a player to scan their identity.",
                "§c1 use per round."
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Signs for crewmates (2 stacks of 16) */
    public static ItemStack signStack(int amount) {
        ItemStack item = new ItemStack(Material.OAK_SIGN, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCommunications Board");
        meta.setLore(Collections.singletonList("§7Place to communicate with the crew"));
        item.setItemMeta(meta);
        return item;
    }

    /** Parasite infection indicator item (slot 8, cosmetic) */
    public static ItemStack parasiteIndicator() {
        ItemStack item = new ItemStack(Material.FERMENTED_SPIDER_EYE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4☣ Parasite Abilities");
        meta.setLore(Arrays.asList(
                "§c• Right-click a player to INFECT them",
                "§c• Press G to SWAP with a random crewmate",
                "§8  (2 min cooldown)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Doctor save indicator item (slot 8, cosmetic) */
    public static ItemStack doctorIndicator() {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b✚ Doctor Abilities");
        meta.setLore(Arrays.asList(
                "§b• Right-click a player to SAVE them",
                "§7  (one save per round)"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Extract target player name from a vote or save paper's display name.
     * Returns null if not a vote/save paper.
     */
    public static String extractPaperTarget(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return null;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        String name = item.getItemMeta().getDisplayName();
        // "§c§lVOTE: §fPlayerName"  or  "§b§lSAVE: §fPlayerName"
        if (name.contains("VOTE: ") && !name.contains("SKIP")) {
            return ChatColor.stripColor(name).replace("VOTE: ", "").trim();
        }
        if (name.contains("SAVE: ")) {
            return ChatColor.stripColor(name).replace("SAVE: ", "").trim();
        }
        return null;
    }

    public static boolean isSkipPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("SKIP VOTE");
    }
}
