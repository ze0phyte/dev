package com.parasite.utils;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public static ItemStack pageButton(String label, int targetPage) {
        ItemStack item = new ItemStack(Material.ARROW, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(label);
        meta.setLore(Collections.singletonList("§8page:" + targetPage));
        item.setItemMeta(meta);
        return item;
    }

    public static Integer getPageTarget(ItemStack item) {
        if (item == null || item.getType() != Material.ARROW) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line.startsWith("§8page:")) {
                try { return Integer.parseInt(line.substring(7)); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    public static ItemStack skipPaper() {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7§lSKIP VOTE");
        meta.setLore(Collections.singletonList("§7Right-click to skip voting this round"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Iron axe — can break oak signs only (set via CanDestroy NBT in Adventure mode).
     * We use ItemMeta with the canDestroy flag so Adventure mode allows breaking OAK_WALL_SIGN
     * and OAK_SIGN specifically.
     */
    public static ItemStack crewAxe() {
        ItemStack item = new ItemStack(Material.IRON_AXE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Maintenance Tool");
        meta.setLore(Arrays.asList(
                "§8Standard crew equipment",
                "§7Can break §foak signs"
        ));
        item.setItemMeta(meta);
        // CanDestroy NBT — required for Adventure mode to allow breaking signs
        Bukkit.getUnsafe().modifyItemStack(item,
            "{CanDestroy:[\"minecraft:oak_sign\",\"minecraft:oak_wall_sign\"]}");

        // Apply CanDestroy NBT via Bukkit API — works in 1.20.1+
        // We do this by using the PersistentDataContainer approach
        // Actually in Spigot 1.20 the cleanest way is via ItemMeta directly with keys:
        // Unfortunately pure Bukkit doesn't expose CanDestroy cleanly.
        // The workaround: give the axe in SURVIVAL mode tick then back, OR
        // use the Spigot-specific approach of setting damage tags.
        // Best approach for Adventure mode: we allow axe to break signs via the BlockListener.
        return item;
    }

    /**
     * Identity Scanner — crossbow pre-loaded with one arrow.
     * Player gets the crossbow already charged so they can fire immediately.
     */
    public static ItemStack scanCrossbow() {
        ItemStack item = new ItemStack(Material.CROSSBOW, 1);
        CrossbowMeta meta = (CrossbowMeta) item.getItemMeta();
        meta.setDisplayName("§e§lIdentity Scanner");
        meta.setLore(Arrays.asList(
                "§7Shoot a player to scan their identity.",
                "§c1 use per round — arrow included."
        ));
        // Pre-load the crossbow with an arrow so it fires immediately
        ItemStack arrow = new ItemStack(Material.ARROW, 1);
        meta.setChargedProjectiles(Collections.singletonList(arrow));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /** Signs with CanPlaceOn NBT — placeable on smooth_stone and gray_concrete in Adventure mode */
    @SuppressWarnings("deprecation")
    public static ItemStack signStack(int amount) {
        ItemStack item = new ItemStack(Material.OAK_SIGN, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fCommunications Board");
        meta.setLore(Collections.singletonList("§7Place on smooth stone or gray concrete"));
        item.setItemMeta(meta);
        // CanPlaceOn NBT — required for Adventure mode to allow placing on these blocks
        Bukkit.getUnsafe().modifyItemStack(item,
            "{CanPlaceOn:[\"minecraft:smooth_stone\",\"minecraft:gray_concrete\"]}");
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
                "§7• Shoot the §eIdentity Scanner§7 at a player",
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
        if (item == null || item.getType() != Material.CROSSBOW) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        return item.getItemMeta().getDisplayName().contains("Identity Scanner");
    }
}