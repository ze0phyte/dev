package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.BingoCard;
import com.bingorace.game.BingoItem;
import com.bingorace.game.BingoTeam;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BingoCardGUI {

    // Track which players currently have their card open so we can refresh
    private static final Map<UUID, Inventory> openCards = new HashMap<>();

    /**
     * Open the bingo card GUI for a player.
     */
    public static void open(Player player, BingoTeam team) {
        if (team == null || team.getCard() == null) {
            player.sendMessage("§cYou don't have a bingo card yet!");
            return;
        }

        BingoCard card = team.getCard();
        int size = card.getSize();       // 3, 4, or 5
        int cells = size * size;          // 9, 16, 25

        // We use a 54-slot chest (6 rows of 9). We center the card grid.
        // For 3x3: use slots 10-12, 19-21, 28-30 (centered in a 54-slot chest)
        // For 4x4: use slots 10-13, 19-22, 28-31, 37-40
        // For 5x5: use slots 9-13, 18-22, 27-31, 36-40, 45-49
        String title = team.getDisplayName() + " §8— §fBingo Card §8(" + card.getCompletedCount() + "/" + cells + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill all slots with a dark border
        ItemStack border = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) inv.setItem(i, border);

        // Map card cells to inventory slots
        int[] slots = getSlots(size);
        BingoItem[] cardCells = card.getCells();

        for (int i = 0; i < cells && i < slots.length; i++) {
            BingoItem bingoItem = cardCells[i];
            if (bingoItem == null) continue;
            ItemStack display = buildCellItem(bingoItem);
            inv.setItem(slots[i], display);
        }

        openCards.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private static ItemStack buildCellItem(BingoItem bingoItem) {
        Material mat = bingoItem.getMaterial();
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (bingoItem.isCompleted()) {
            meta.setDisplayName("§a§l✔ " + formatMaterial(mat));
            meta.setLore(Collections.singletonList("§7Collected by §f" + bingoItem.getCompletedBy()));
            // Add glow effect
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.setDisplayName("§7" + formatMaterial(mat));
            meta.setLore(Collections.singletonList("§8Not yet collected"));
        }
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Refresh open card GUIs for all members of a team.
     */
    public static void refreshForTeam(BingoTeam team) {
        if (team.getCard() == null) return;
        BingoCard card = team.getCard();
        int size = card.getSize();
        int[] slots = getSlots(size);
        BingoItem[] cells = card.getCells();

        for (UUID id : team.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Inventory inv = openCards.get(id);
            if (inv == null) continue;
            // Check player actually has this inventory open
            if (p.getOpenInventory().getTopInventory() != inv) {
                openCards.remove(id);
                continue;
            }
            // Update each cell
            for (int i = 0; i < cells.length && i < slots.length; i++) {
                if (cells[i] != null) {
                    inv.setItem(slots[i], buildCellItem(cells[i]));
                }
            }
        }
    }

    public static void onClose(UUID uuid) {
        openCards.remove(uuid);
    }

    // ── Slot layouts ───────────────────────────────────────────────────────

    private static int[] getSlots(int size) {
        return switch (size) {
            case 3 -> new int[]{10, 11, 12, 19, 20, 21, 28, 29, 30};
            case 4 -> new int[]{10, 11, 12, 13, 19, 20, 21, 22, 28, 29, 30, 31, 37, 38, 39, 40};
            case 5 -> new int[]{9, 10, 11, 12, 13, 18, 19, 20, 21, 22, 27, 28, 29, 30, 31, 36, 37, 38, 39, 40, 45, 46, 47, 48, 49};
            default -> new int[]{};
        };
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private static ItemStack makeItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatMaterial(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }
}
