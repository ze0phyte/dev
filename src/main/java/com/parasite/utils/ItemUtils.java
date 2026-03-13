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
        meta.setLore(Arrays.asList("§7Right-click to vote for", "§e" + playerName));
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

    /** Iron axe for crewmates */
    public static ItemStack crewAxe() {
        ItemStack item = new ItemStack(Material.IRON_AXE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Maintenance Tool");
        meta.setLore(Collections.singletonList("§8Standard crew equipment"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Identity Scanner — Nether Star, right-click ON a player to reveal their name.
     * No arrow needed. Works via PlayerInteractAtEntityEvent.
     */
    public static ItemStack scannerItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§lIdentity Scanner");
        meta.setLore(Arrays.asList(
                "§7Right-click directly on a player",
                "§7to scan their identity.",
                "§c1 use per round."
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Signs for crewmates */
    public static ItemStack signStack(int amount) {
        ItemStack item = new ItemStack(Material.OAK_SIGN, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCommunications Board");
        meta.setLore(Collections.singletonList("§7Place anywhere to communicate"));
        item.setItemMeta(meta);
        return item;
    }

    /** Parasite role card — Nether Star */
    public static ItemStack parasiteIndicator() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§4§l☣ YOU ARE THE PARASITE");
        meta.setLore(Arrays.asList(
                "§c• Right-click a player §4(empty hand)§c to INFECT",
                "§c• Press §lF §r§cto SWAP positions with someone",
                "§8  2 minute cooldown on swap",
                "§7Infected players die before discussion",
                "§7unless the Doctor saves them."
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Doctor role card — Nether Star */
    public static ItemStack doctorIndicator() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l✚ YOU ARE THE DOCTOR");
        meta.setLore(Arrays.asList(
                "§b• Right-click a player §3(empty hand)§b to SAVE",
                "§7  One save per round only.",
                "§7Save before the round ends to protect",
                "§7someone from the parasite."
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Crewmate role card — Nether Star */
    public static ItemStack crewmateIndicator() {
        ItemStack item = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7§l⚙ YOU ARE A CREWMATE");
        meta.setLore(Arrays.asList(
                "§7• Place §fSigns§7 to communicate with others",
                "§7• Use §eIdentity Scanner§7 — right-click a player",
                "§7  to reveal their name (1 use per round)",
                "§7• Vote out the parasite to win!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    /** Extract target player name from a vote paper display name. */
    public static String extractPaperTarget(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return null;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        String name = item.getItemMeta().getDisplayName();
        if (name.contains("VOTE: ") && !name.contains("SKIP")) {
            return ChatColor.stripColor(name).replace("VOTE: ", "").trim();
        }
        return null;
    }

    public static boolean isSkipPaper(ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("SKIP VOTE");
    }

    public static boolean isScannerItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("Identity Scanner");
    }
}