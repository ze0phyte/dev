package com.bingorace.gui;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.GameManager;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

public class ItemPickupListener implements Listener {

    private final BingoRacePlugin plugin;

    public ItemPickupListener(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    /** Ground item pickup */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        org.bukkit.Material mat = event.getItem().getItemStack().getType();
        plugin.getGameManager().handleItemPickup(player, mat);
    }

    /** Crafting table output */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getRecipe() == null) return;
        org.bukkit.Material mat = event.getRecipe().getResult().getType();
        plugin.getGameManager().handleItemPickup(player, mat);
    }

    /** Furnace smelting output */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnace(FurnaceExtractEvent event) {
        plugin.getGameManager().handleItemPickup(event.getPlayer(), event.getItemType());
    }

    /** Fishing */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (!(event.getCaught() instanceof Item item)) return;
        plugin.getGameManager().handleItemPickup(event.getPlayer(), item.getItemStack().getType());
    }
}
