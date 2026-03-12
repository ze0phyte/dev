package com.parasite;

import com.parasite.commands.ParasiteCommand;
import com.parasite.commands.PJoinCommand;
import com.parasite.game.GameManager;
import com.parasite.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;

public class ParasitePlugin extends JavaPlugin {

    private static ParasitePlugin instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gameManager = new GameManager(this);

        // Commands
        getCommand("parasite").setExecutor(new ParasiteCommand(this));
        getCommand("pjoin").setExecutor(new PJoinCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new DropPickupListener(this), this);

        getLogger().info("ParasiteGame enabled! Space crew, beware...");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.forceStop();
    }

    public static ParasitePlugin getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
}
