package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.Difficulty;
import com.bingorace.game.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final BingoRacePlugin plugin;

    public GUIListener(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // ── Bingo Card GUI (read-only) ────────────────────────────────────
        if (title.contains("Bingo Card")) {
            event.setCancelled(true);
            return;
        }

        // ── Setup GUI ─────────────────────────────────────────────────────
        if (title.equals(SetupGUI.TITLE)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null) return;
            ItemStack clicked = event.getCurrentItem();
            String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
            GameManager gm = plugin.getGameManager();

            if (name.contains("EASY")) {
                gm.setDifficulty(Difficulty.EASY);
                player.sendMessage(GameManager.PREFIX + "§aDifficulty set to Easy (3x3)");
                SetupGUI.open(player, plugin); // refresh
            } else if (name.contains("MEDIUM")) {
                gm.setDifficulty(Difficulty.MEDIUM);
                player.sendMessage(GameManager.PREFIX + "§eDifficulty set to Medium (4x4)");
                SetupGUI.open(player, plugin);
            } else if (name.contains("HARD")) {
                gm.setDifficulty(Difficulty.HARD);
                player.sendMessage(GameManager.PREFIX + "§cDifficulty set to Hard (5x5)");
                SetupGUI.open(player, plugin);
            } else if (name.contains("Solo Mode")) {
                gm.setSoloMode(!gm.isSoloMode());
                player.sendMessage(GameManager.PREFIX + "§7Solo mode: " + (gm.isSoloMode() ? "§aON" : "§cOFF"));
                SetupGUI.open(player, plugin);
            } else if (name.contains("Team Size")) {
                if (event.isRightClick()) {
                    gm.setTeamSize(Math.max(1, gm.getTeamSize() - 1));
                } else {
                    gm.setTeamSize(Math.min(10, gm.getTeamSize() + 1));
                }
                player.sendMessage(GameManager.PREFIX + "§7Team size: §e" + gm.getTeamSize());
                SetupGUI.open(player, plugin);
            } else if (name.contains("Team Count")) {
                if (event.isRightClick()) {
                    gm.setTeamCount(Math.max(2, gm.getTeamCount() - 1));
                } else {
                    gm.setTeamCount(Math.min(8, gm.getTeamCount() + 1));
                }
                player.sendMessage(GameManager.PREFIX + "§7Team count: §e" + gm.getTeamCount());
                SetupGUI.open(player, plugin);
            } else if (name.contains("START GAME")) {
                player.closeInventory();
                gm.startGame(gm.getDifficulty(), gm.getTeamSize(), gm.getTeamCount(), gm.isSoloMode());
            } else if (name.contains("CANCEL")) {
                player.closeInventory();
                player.sendMessage(GameManager.PREFIX + "§7Setup cancelled.");
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            String title = event.getView().getTitle();
            if (title.contains("Bingo Card")) {
                BingoCardGUI.onClose(player.getUniqueId());
            }
        }
    }
}
