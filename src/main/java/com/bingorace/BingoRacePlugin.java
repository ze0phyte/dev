package com.bingorace;

import com.bingorace.commands.BingoAdminCommand;
import com.bingorace.commands.BingoCommand;
import com.bingorace.commands.BJoinCommand;
import com.bingorace.game.GameManager;
import com.bingorace.gui.GUIListener;
import com.bingorace.gui.ItemPickupListener;
import org.bukkit.plugin.java.JavaPlugin;

public class BingoRacePlugin extends JavaPlugin {

    private static BingoRacePlugin instance;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Save default item files if not present
        saveResource("items/easy.yml", false);
        saveResource("items/medium.yml", false);
        saveResource("items/hard.yml", false);

        this.gameManager = new GameManager(this);

        // Commands
        getCommand("br").setExecutor(new BingoAdminCommand(this));
        getCommand("bingo").setExecutor(new BingoCommand(this));
        getCommand("bjoin").setExecutor(new BJoinCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemPickupListener(this), this);

        getLogger().info("BingoRace enabled! Good luck, racers.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.stopGame(getServer().getConsoleSender());
        getLogger().info("BingoRace disabled.");
    }

    public static BingoRacePlugin getInstance() { return instance; }
    public GameManager getGameManager() { return gameManager; }
}
