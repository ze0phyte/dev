package com.bingorace.utils;

import com.bingorace.BingoRacePlugin;
import com.bingorace.game.BingoItem;
import com.bingorace.game.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemLoader {

    public static List<BingoItem> loadAndShuffle(BingoRacePlugin plugin, Difficulty difficulty, int count) {
        String path = difficulty.getItemFile();
        var stream = plugin.getResource(path);
        if (stream == null) {
            plugin.getLogger().warning("Could not find item file: " + path);
            return fallback(count);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        List<String> raw = cfg.getStringList("items");

        List<BingoItem> items = new ArrayList<>();
        for (String s : raw) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                items.add(new BingoItem(mat));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in item list: " + s);
            }
        }

        Collections.shuffle(items);
        if (items.size() < count) {
            plugin.getLogger().warning("Not enough items in " + path + " for count=" + count + ", padding with STONE");
            while (items.size() < count) items.add(new BingoItem(Material.STONE));
        }
        return items.subList(0, count);
    }

    private static List<BingoItem> fallback(int count) {
        List<BingoItem> list = new ArrayList<>();
        for (int i = 0; i < count; i++) list.add(new BingoItem(Material.STONE));
        return list;
    }
}
