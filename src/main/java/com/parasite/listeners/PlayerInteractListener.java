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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final ParasitePlugin plugin;

    public PlayerInteractListener(ParasitePlugin plugin) {
        this.plugin = plugin;
    }

    // ── Right-click on paper (voting only — doctor saves during round by right-clicking player) ──
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.PAPER) return;

        GameManager gm = plugin.getGameManager();

        // Vote paper
        String target = ItemUtils.extractPaperTarget(item);
        if (target != null && item.getItemMeta().getDisplayName().startsWith("§c§lVOTE:")) {
            gm.handleVote(player, target);
            event.setCancelled(true);
            return;
        }

        // Skip paper
        if (ItemUtils.isSkipPaper(item)) {
            gm.handleVote(player, "SKIP");
            event.setCancelled(true);
        }
    }

    // ── Right-click ON a player (parasite infect or doctor save during round) ─
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

        // DOCTOR: right-click empty hand = save (round only, NOT voting)
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

    // ── Crossbow bolt hits player (scanner — no damage, just reveals name) ────
    @EventHandler
    public void onProjectileHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Projectile proj)) return;
        if (!(proj.getShooter() instanceof Player shooter)) return;

        GameManager gm = plugin.getGameManager();
        GamePlayer sgp = gm.getGamePlayer(shooter.getUniqueId());
        if (sgp == null) return;

        event.setCancelled(true);
        gm.handleCrossbowHit(shooter, target);
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