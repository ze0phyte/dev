package com.bingorace.game;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BingoTeam {

    private final String name;
    private final ChatColor color;
    private final List<UUID> members = new ArrayList<>();
    private BingoCard card;
    private int bingoCount = 0;

    public BingoTeam(String name, ChatColor color) {
        this.name = name;
        this.color = color;
    }

    public String getName() { return name; }
    public ChatColor getColor() { return color; }
    public String getDisplayName() { return color + name; }

    public List<UUID> getMembers() { return members; }
    public void addMember(UUID uuid) { members.add(uuid); }
    public boolean hasMember(UUID uuid) { return members.contains(uuid); }

    public BingoCard getCard() { return card; }
    public void setCard(BingoCard card) { this.card = card; }

    public int getBingoCount() { return bingoCount; }
    public void incrementBingo() { bingoCount++; }
}
