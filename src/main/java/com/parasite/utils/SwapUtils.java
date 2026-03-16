package com.parasite.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.parasite.ParasitePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

/**
 * Uses ProtocolLib to perform a completely invisible position swap.
 *
 * How it works:
 * 1. Send every OBSERVER a fake entity teleport packet making parasite appear
 *    to still be at parasite's old location and target at target's old location.
 * 2. Actually teleport both players server-side.
 * 3. The observers' clients already have the "correct" position from step 1
 *    so there's no lerp — from their view nothing moved.
 *
 * The two swapped players themselves will just see a normal TP (their own screen).
 */
public class SwapUtils {

    /**
     * Silently swap two players with no visible lerp for observers.
     * @param parasite  the parasite player
     * @param target    the player being swapped with
     * @param observers all alive players (witnesses)
     * @param plugin    plugin instance for scheduler
     */
    public static void silentSwap(Player parasite, Player target,
                                   Collection<Player> observers, ParasitePlugin plugin) {

        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        Location parasiteLoc = parasite.getLocation().clone();
        Location targetLoc   = target.getLocation().clone();

        // Step 1: Send fake teleport packets to all observers BEFORE the real teleport.
        // This pre-positions their client's entity tracker at the destination,
        // so when the real teleport packet arrives it's a zero-distance move = no lerp.
        for (Player observer : observers) {
            if (observer.getUniqueId().equals(parasite.getUniqueId())) continue;
            if (observer.getUniqueId().equals(target.getUniqueId())) continue;

            try {
                // Tell observer: parasite is now at target's location
                PacketContainer parasiteTP = pm.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                parasiteTP.getIntegers().write(0, parasite.getEntityId());
                parasiteTP.getDoubles().write(0, targetLoc.getX());
                parasiteTP.getDoubles().write(1, targetLoc.getY());
                parasiteTP.getDoubles().write(2, targetLoc.getZ());
                parasiteTP.getBytes().write(0, (byte)(targetLoc.getYaw() * 256f / 360f));
                parasiteTP.getBytes().write(1, (byte)(targetLoc.getPitch() * 256f / 360f));
                parasiteTP.getBooleans().write(0, true); // on ground
                pm.sendServerPacket(observer, parasiteTP);

                // Tell observer: target is now at parasite's location
                PacketContainer targetTP = pm.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                targetTP.getIntegers().write(0, target.getEntityId());
                targetTP.getDoubles().write(0, parasiteLoc.getX());
                targetTP.getDoubles().write(1, parasiteLoc.getY());
                targetTP.getDoubles().write(2, parasiteLoc.getZ());
                targetTP.getBytes().write(0, (byte)(parasiteLoc.getYaw() * 256f / 360f));
                targetTP.getBytes().write(1, (byte)(parasiteLoc.getPitch() * 256f / 360f));
                targetTP.getBooleans().write(0, true);
                pm.sendServerPacket(observer, targetTP);

            } catch (Exception e) {
                plugin.getLogger().warning("SwapUtils packet error: " + e.getMessage());
            }
        }

        // Step 2: Actually teleport server-side (1 tick later so packets arrive first)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            parasite.teleport(targetLoc);
            target.teleport(parasiteLoc);
            parasite.setVelocity(new Vector(0, 0, 0));
            target.setVelocity(new Vector(0, 0, 0));
            // Zero velocity again next tick to catch server reapplication
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                parasite.setVelocity(new Vector(0, 0, 0));
                target.setVelocity(new Vector(0, 0, 0));
            }, 1L);
        }, 1L);
    }
}
