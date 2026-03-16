package com.parasite.listeners;

import com.parasite.ParasitePlugin;
import com.parasite.game.GameManager;
import com.parasite.game.GamePlayer;
import com.parasite.game.GameState;
import com.parasite.game.Role;
import com.parasite.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final ParasitePlugin plugin;

    public PlayerInteractListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    // ── Right-click on paper (voting only) ───────────────────────────────────
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;
        // Only fire on right-click, not physical (stepping on pressure plates etc)
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;

        GameManager gm = plugin.getGameManager();

        // Page navigation feather — only during voting
        if (item.getType() == Material.FEATHER) {
            if (gm.getState() != GameState.VOTING) return;
            Integer targetPage = ItemUtils.getPageTarget(item);
            if (targetPage != null) {
                gm.giveVotingItemsPage(player, targetPage);
                event.setCancelled(true);
            }
            return;
        }

        if (item.getType() != Material.PAPER) return;

        String target = ItemUtils.extractPaperTarget(item);
        if (target != null && item.getItemMeta().getDisplayName().startsWith("§c§lVOTE:")) {
            gm.handleVote(player, target);
            event.setCancelled(true);
            return;
        }

        if (ItemUtils.isSkipPaper(item)) {
            gm.handleVote(player, "SKIP");
            event.setCancelled(true);
        }
    }

    // ── Crossbow bolt hits player = identity scan ─────────────────────────────
    // Crossbow is pre-loaded so player just right-clicks to fire, no arrow needed in inv
    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (!(proj.getShooter() instanceof Player shooter)) return;

        GameManager gm = plugin.getGameManager();
        GamePlayer sgp = gm.getGamePlayer(shooter.getUniqueId());
        if (sgp == null) return;

        // Cancel damage — scanner doesn't hurt
        event.setCancelled(true);
        gm.handleCrossbowHit(shooter, target);
    }

    // ── Right-click ON a player (parasite infect or doctor save) ─────────────
    @EventHandler
    public void onInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
        if (gp == null || !gp.isAlive()) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        boolean emptyHand = held == null || held.getType() == Material.AIR;

        // PARASITE: right-click empty hand = infect
        if (gp.getRole() == Role.PARASITE && emptyHand && gm.getState() == GameState.IN_ROUND) {
            gm.handleInfect(player, target);
            event.setCancelled(true);
            return;
        }

        // RESEARCHER: right-click empty hand = scan role
        if (gp.getRole() == Role.RESEARCHER && emptyHand && gm.getState() == GameState.IN_ROUND) {
            gm.handleResearchScan(player, target);
            event.setCancelled(true);
            return;
        }

        // DOCTOR: right-click empty hand = save (round only)
        if (gp.getRole() == Role.DOCTOR && emptyHand && gm.getState() == GameState.IN_ROUND) {
            if (gp.isSavedThisRound()) {
                player.sendMessage(GameManager.PREFIX + "§cYou've already used your save this round!");
            } else {
                GamePlayer tgp = gm.getGamePlayer(target.getUniqueId());
                if (tgp != null && tgp.isAlive()) {
                    tgp.setSavedThisRound(true);
                    gp.setSavedThisRound(true);
                    player.sendMessage(GameManager.PREFIX + "§b✚ You saved §f" + target.getName() + "§b!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                }
            }
            event.setCancelled(true);
        }
    }

    // ── Last Will book close ────────────────────────────────────────────────
    @EventHandler
    public void onBookEdit(PlayerEditBookEvent event) {
        if (!event.isSigning()) return;
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (gm.getGamePlayer(player.getUniqueId()) == null) return;
        gm.handleLastWillClose(player, event.getNewBookMeta());
    }

    // ── Séance: dead player right-clicks a living player to vote haunt ───────
    @EventHandler
    public void onSeanceInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
        if (gp == null || gp.isAlive()) return; // only dead players
        ItemStack held = player.getInventory().getItemInMainHand();
        // Dead players hold a bone as their séance item (given on death)
        if (held != null && held.getType() == org.bukkit.Material.BONE) {
            gm.handleSeanceVote(player, target);
            event.setCancelled(true);
        }
    }

    // ── F key = parasite position swap ───────────────────────────────────────
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        GamePlayer gp = gm.getGamePlayer(player.getUniqueId());
        if (gp == null || gp.getRole() != Role.PARASITE || !gp.isAlive()) return;
        if (gm.getState() != GameState.IN_ROUND) return;

        event.setCancelled(true);
        gm.handleParasiteSwap(player);
    }
}