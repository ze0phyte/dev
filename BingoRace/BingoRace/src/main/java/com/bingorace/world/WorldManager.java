package com.bingorace.world;

import com.bingorace.BingoRacePlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Random;

public class WorldManager {

    private final BingoRacePlugin plugin;

    public WorldManager(BingoRacePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Unload and delete the existing bingo world, then generate a fresh one.
     * Returns the new World, or null on failure.
     */
    public World resetAndCreate(String worldName) {
        // 1. Teleport anyone in the world out first
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            World safe = Bukkit.getWorlds().get(0); // main world
            existing.getPlayers().forEach(p -> p.teleport(safe.getSpawnLocation()));
            Bukkit.unloadWorld(existing, false);
        }

        // 2. Delete the world folder
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            deleteDirectory(worldFolder);
            plugin.getLogger().info("Deleted old world: " + worldName);
        }

        // Also delete nether and end
        deleteDirectory(new File(Bukkit.getWorldContainer(), worldName + "_nether"));
        deleteDirectory(new File(Bukkit.getWorldContainer(), worldName + "_the_end"));

        // 3. Create fresh world with random seed
        long seed = new Random().nextLong();
        WorldCreator creator = new WorldCreator(worldName);
        creator.seed(seed);
        creator.type(WorldType.NORMAL);

        World world = Bukkit.createWorld(creator);
        if (world != null) {
            world.setGameRuleValue("doMobSpawning", "true");
            world.setGameRuleValue("keepInventory", "false");
            world.setGameRuleValue("doImmediateRespawn", "true");
            plugin.getLogger().info("Created bingo world: " + worldName + " (seed: " + seed + ")");
        }
        return world;
    }

    private void deleteDirectory(File dir) {
        if (!dir.exists()) return;
        try {
            Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to delete directory: " + dir.getName() + " — " + e.getMessage());
        }
    }
}
