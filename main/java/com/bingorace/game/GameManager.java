package com.bingorace.game;

import com.bingorace.BingoRacePlugin;
import com.bingorace.gui.BingoCardGUI;
import com.bingorace.utils.ItemLoader;
import com.bingorace.world.WorldManager;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class GameManager {

    public static final String PREFIX = "§8[§aBINGO§8] §r";

    private final BingoRacePlugin plugin;
    private final WorldManager worldManager;

    private GameState state = GameState.IDLE;
    private Difficulty difficulty;
    private boolean soloMode;
    private int teamSize;
    private int teamCount;
    private int timeLimit;   // seconds, 0 = no limit
    private int borderSize = 600;
    private BukkitTask trackerTask;
    private boolean endOnFirstBingo = true;

    private final List<UUID> lobby = new ArrayList<>();
    private final List<BingoTeam> teams = new ArrayList<>();
    private final Map<UUID, BingoTeam> playerTeamMap = new HashMap<>();

    private World gameWorld;
    private BukkitTask countdownTask;
    private BukkitTask timerTask;
    private int timeRemaining = 0;

    private final Map<String, Set<String>> announcedBingos = new HashMap<>();

    public GameManager(BingoRacePlugin plugin) {
        this.plugin = plugin;
        this.worldManager = new WorldManager(plugin);
        loadConfig();
    }

    public void loadConfig() {
        var cfg = plugin.getConfig();
        try { difficulty = Difficulty.valueOf(cfg.getString("game.default-difficulty", "MEDIUM").toUpperCase()); }
        catch (Exception e) { difficulty = Difficulty.MEDIUM; }
        soloMode  = cfg.getBoolean("game.solo-mode", false);
        teamSize  = cfg.getInt("game.team-size", 2);
        teamCount = cfg.getInt("game.team-count", 4);
        timeLimit = cfg.getInt("game.time-limit", 0);
    }

    // ── Lobby ──────────────────────────────────────────────────────────────

    public void joinLobby(Player player) {
        if (state != GameState.IDLE && state != GameState.LOBBY) {
            player.sendMessage(PREFIX + "§cA game is already running!");
            return;
        }
        if (lobby.contains(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§cYou're already in the lobby.");
            return;
        }
        lobby.add(player.getUniqueId());
        state = GameState.LOBBY;
        broadcastAll(PREFIX + "§a" + player.getName() + " §7joined the lobby! §8[§f" + lobby.size() + "§8]");
        player.sendMessage(PREFIX + "§7You joined! Wait for the host to start.");
    }

    public void leaveLobby(Player player) {
        lobby.remove(player.getUniqueId());
        BingoTeam team = playerTeamMap.get(player.getUniqueId());
        if (team != null) {
            team.getMembers().remove(player.getUniqueId());
            playerTeamMap.remove(player.getUniqueId());
        }
        player.sendMessage(PREFIX + "§7You left the bingo lobby.");
        if (lobby.isEmpty() && state == GameState.LOBBY) state = GameState.IDLE;
    }

    // ── Start ──────────────────────────────────────────────────────────────

    public void startGame(Difficulty diff, int tSize, int tCount, boolean solo) {
        if (state == GameState.RUNNING || state == GameState.STARTING) {
            Bukkit.broadcastMessage(PREFIX + "§cA game is already in progress.");
            return;
        }
        this.difficulty = diff;
        this.teamSize   = tSize;
        this.teamCount  = tCount;
        this.soloMode   = solo;

        state = GameState.STARTING;
        broadcastAll(PREFIX + "§eGame starting! Generating world...");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String worldName = plugin.getConfig().getString("game.world-name", "bingo_world");
            Bukkit.getScheduler().runTask(plugin, () -> {
                gameWorld = worldManager.resetAndCreate(worldName);
                if (gameWorld == null) {
                    broadcastAll(PREFIX + "§cFailed to create game world!");
                    state = GameState.IDLE;
                    return;
                }
                assignTeams();
                buildCards();
                startCountdown();
            });
        });
    }

    // Fixed team names+colors — always first N used based on team count
    private static final String[] TEAM_NAMES   = {"Blue", "Red", "Green", "Yellow", "Aqua", "Purple", "White", "Orange"};
    private static final ChatColor[] TEAM_COLORS = {
        ChatColor.BLUE, ChatColor.RED, ChatColor.GREEN, ChatColor.YELLOW,
        ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.WHITE, ChatColor.GOLD
    };

    private void assignTeams() {
        teams.clear();
        playerTeamMap.clear();
        announcedBingos.clear();

        // Shuffle lobby players for random team assignment
        List<Player> players = lobby.stream()
            .map(Bukkit::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Collections.shuffle(players);

        if (soloMode) {
            for (int i = 0; i < players.size(); i++) {
                BingoTeam team = new BingoTeam(players.get(i).getName(), TEAM_COLORS[i % TEAM_COLORS.length]);
                team.addMember(players.get(i).getUniqueId());
                teams.add(team);
                playerTeamMap.put(players.get(i).getUniqueId(), team);
            }
        } else {
            int numTeams = Math.min(teamCount, (int) Math.ceil((double) players.size() / Math.max(1, teamSize)));
            numTeams = Math.max(1, numTeams);
            for (int t = 0; t < numTeams; t++) {
                teams.add(new BingoTeam(TEAM_NAMES[t % TEAM_NAMES.length], TEAM_COLORS[t % TEAM_COLORS.length]));
            }
            // Round-robin distribute shuffled players
            for (int i = 0; i < players.size(); i++) {
                BingoTeam team = teams.get(i % teams.size());
                team.addMember(players.get(i).getUniqueId());
                playerTeamMap.put(players.get(i).getUniqueId(), team);
            }
        }

        // Announce team assignments
        broadcastAll(PREFIX + "§7Teams have been randomized!");
        for (BingoTeam team : teams) {
            StringBuilder sb = new StringBuilder(PREFIX + team.getDisplayName() + "§7: ");
            for (UUID id : team.getMembers()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) sb.append("§f").append(p.getName()).append(" ");
            }
            broadcastAll(sb.toString().trim());
        }
    }

    private void buildCards() {
        int count = difficulty.getCellCount();
        // Every team always gets their own unique randomly shuffled card
        for (BingoTeam team : teams) {
            team.setCard(new BingoCard(difficulty, ItemLoader.loadAndShuffle(plugin, difficulty, count)));
            announcedBingos.put(team.getName(), new HashSet<>());
        }
    }

    private void startCountdown() {
        int seconds = plugin.getConfig().getInt("game.countdown", 10);
        if (countdownTask != null) countdownTask.cancel();
        countdownTask = new BukkitRunnable() {
            int t = seconds;
            @Override
            public void run() {
                if (t <= 0) { cancel(); launchGame(); return; }
                if (t <= 5 || t % 5 == 0) broadcastAll(PREFIX + "§eGame starts in §c" + t + "§e seconds!");
                t--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void launchGame() {
        state = GameState.RUNNING;

        Location spawn = gameWorld.getSpawnLocation();
        spawn.setY(spawn.getWorld().getHighestBlockYAt(spawn) + 1);

        for (BingoTeam team : teams) {
            for (UUID id : team.getMembers()) {
                Player p = Bukkit.getPlayer(id);
                if (p == null) continue;

                // Clear inventory
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);

                // Survival mode, PvP world
                p.setGameMode(GameMode.SURVIVAL);

                // Full health and hunger
                p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
                p.setFoodLevel(20);
                p.setSaturation(20);
                p.setExp(0);
                p.setLevel(0);

                // Clear effects
                p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));

                p.teleport(spawn);
                p.sendTitle("§a§lBINGO RACE!", team.getDisplayName(), 10, 60, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                p.sendMessage(PREFIX + "§aThe race has begun! Press §eF §ato open your card.");
            }
        }

        // Enable PvP in the game world
        gameWorld.setPVP(true);

        // World border
        borderSize = plugin.getConfig().getInt("game.border-size", 600);
        org.bukkit.WorldBorder wb = gameWorld.getWorldBorder();
        wb.setCenter(gameWorld.getSpawnLocation());
        wb.setSize(borderSize);
        wb.setDamageAmount(1.0);
        wb.setDamageBuffer(5.0);
        wb.setWarningDistance(20);
        wb.setWarningTime(15);

        broadcastAll(" ");
        broadcastAll("§a§l§m════════════════════════════════");
        broadcastAll("§a§l  GO! Collect items on your card.");
        broadcastAll("§7  Press §eF §7to open your bingo card at any time.");
        broadcastAll("§a§l§m════════════════════════════════");
        broadcastAll(" ");
        broadcastAll(PREFIX + "§7Border: §e" + borderSize + "x" + borderSize + "  §7Press §eF §7to open card.");

        // Start timer if configured
        timeLimit = plugin.getConfig().getInt("game.time-limit", 0);
        if (timeLimit > 0) startTimer();

        // Teammate tracker
        startTrackerTask();
    }

    // ── Timer ──────────────────────────────────────────────────────────────

    private void startTimer() {
        timeRemaining = timeLimit;
        if (timerTask != null) timerTask.cancel();
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.RUNNING) { cancel(); return; }
                timeRemaining--;

                // Warn at key moments
                if (timeRemaining == 300) broadcastAll(PREFIX + "§e5 minutes remaining!");
                else if (timeRemaining == 60) broadcastAll(PREFIX + "§e1 minute remaining!");
                else if (timeRemaining <= 10 && timeRemaining > 0) {
                    broadcastAll(PREFIX + "§c" + timeRemaining + " seconds!");
                } else if (timeRemaining <= 0) {
                    cancel();
                    handleTimeUp();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void handleTimeUp() {
        if (state != GameState.RUNNING) return;
        state = GameState.ENDED;

        // Find team with most completed items
        BingoTeam leader = null;
        int most = -1;
        for (BingoTeam team : teams) {
            int completed = team.getCard() != null ? team.getCard().getCompletedCount() : 0;
            if (completed > most) { most = completed; leader = team; }
        }

        broadcastAll(PREFIX + "§c§lTIME'S UP!");
        if (leader != null && most > 0) {
            broadcastAll(" ");
            broadcastAll("§6§l§m═══════════════════════════════");
            broadcastAll("§6§l  🏆 TIME'S UP — " + leader.getDisplayName() + " §6§lWINS!");
            broadcastAll("§7  Most items collected: §f" + most);
            broadcastAll("§6§l§m═══════════════════════════════");
            broadcastAll(" ");
            final BingoTeam winner = leader;
            for (UUID id : winner.getMembers()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.sendMessage("§6§l🏆 You won with " + most + " items!");
                    spawnFirework(p.getLocation(), winner.getColor());
                }
            }
        } else {
            broadcastAll(PREFIX + "§7No winner — nobody collected any items!");
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, 600L);
    }

    public int getTimeRemaining() { return timeRemaining; }
    public int getTimeLimit() { return timeLimit; }

    // ── Item Collection ────────────────────────────────────────────────────

    public void handleItemPickup(Player player, org.bukkit.Material material) {
        if (state != GameState.RUNNING) return;
        BingoTeam team = playerTeamMap.get(player.getUniqueId());
        if (team == null) return;
        BingoCard card = team.getCard();
        if (card == null || !card.hasItem(material)) return;

        boolean marked = card.markComplete(material, player.getName());
        if (!marked) return;

        int completed = card.getCompletedCount();
        int total = card.getCells().length;
        String itemName = formatMaterial(material);

        // Broadcast to EVERYONE — color coded by team
        broadcastAll(team.getColor() + "[" + team.getName() + "] §r§f" + player.getName()
            + " §7found §e" + itemName + " §8(" + completed + "/" + total + ")");

        // Play sound for team members
        for (UUID id : team.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.5f);
        }

        BingoCardGUI.refreshForTeam(team);
        checkBingo(team, card);
        if (card.isFullCard()) handleWin(team, "FULL CARD");
    }

    private void checkBingo(BingoTeam team, BingoCard card) {
        Set<String> announced = announcedBingos.getOrDefault(team.getName(), new HashSet<>());
        int size = card.getSize();
        for (int r = 0; r < size; r++) {
            String key = "row" + r;
            if (!announced.contains(key) && card.isRowComplete(r)) { announced.add(key); team.incrementBingo(); announceBingo(team, "row " + (r + 1)); }
        }
        for (int col = 0; col < size; col++) {
            String key = "col" + col;
            if (!announced.contains(key) && card.isColComplete(col)) { announced.add(key); team.incrementBingo(); announceBingo(team, "column " + (col + 1)); }
        }
        if (!announced.contains("diag1") && card.isDiag1Complete()) { announced.add("diag1"); team.incrementBingo(); announceBingo(team, "diagonal"); }
        if (!announced.contains("diag2") && card.isDiag2Complete()) { announced.add("diag2"); team.incrementBingo(); announceBingo(team, "diagonal"); }
        announcedBingos.put(team.getName(), announced);
    }

    private void announceBingo(BingoTeam team, String type) {
        broadcastAll(" ");
        broadcastAll(team.getColor() + "§l§m          §r " + team.getDisplayName() + team.getColor() + "§l§m          ");
        broadcastAll(team.getColor() + "§l  ★ BINGO! §r" + team.getDisplayName() + team.getColor() + " completed a " + type + "!");
        broadcastAll(team.getColor() + "  Bingo count: §f" + team.getBingoCount() + " §8line" + (team.getBingoCount() > 1 ? "s" : ""));
        broadcastAll(team.getColor() + "§l§m          §r§8§m                    §r " + team.getColor() + "§l§m          ");
        broadcastAll(" ");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (team.hasMember(online.getUniqueId())) {
                online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            } else {
                online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1f);
            }
        }
        if (endOnFirstBingo) {
            handleWin(team, "BINGO — " + type);
        }
    }

    private void handleWin(BingoTeam team, String type) {
        if (state != GameState.RUNNING) return;
        state = GameState.ENDED;
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }

        broadcastAll(" ");
        broadcastAll("§6§l§m═══════════════════════════════");
        broadcastAll("§6§l  🏆 " + team.getDisplayName() + " §6§lWINS THE RACE! 🏆");
        broadcastAll("§7  " + type);
        broadcastAll("§6§l§m═══════════════════════════════");
        broadcastAll(" ");
        for (UUID id : team.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.sendMessage("§6§l🏆 You won!");
                spawnFirework(p.getLocation(), team.getColor());
            }
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::resetGame, 600L);
    }

    // ── Stop / Reset ───────────────────────────────────────────────────────

    public void stopGame(org.bukkit.command.CommandSender sender) {
        if (state == GameState.IDLE) { sender.sendMessage(PREFIX + "§cNo game is running."); return; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (trackerTask != null) { trackerTask.cancel(); trackerTask = null; }
        broadcastAll(PREFIX + "§cGame stopped by admin.");
        resetGame();
        sender.sendMessage(PREFIX + "§aGame stopped and reset.");
    }

    private void resetGame() {
        if (timerTask != null) { timerTask.cancel(); timerTask = null; }
        if (trackerTask != null) { trackerTask.cancel(); trackerTask = null; }
        World main = Bukkit.getWorlds().get(0);
        teams.forEach(t -> t.getMembers().forEach(id -> {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.getInventory().clear();
                p.setGameMode(GameMode.SURVIVAL);
                p.teleport(main.getSpawnLocation());
            }
        }));
        teams.clear();
        playerTeamMap.clear();
        lobby.clear();
        announcedBingos.clear();
        gameWorld = null;
        timeRemaining = 0;
        state = GameState.IDLE;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void broadcastAll(String msg) { Bukkit.broadcastMessage(msg); }

    private String formatMaterial(org.bukkit.Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private void spawnFirework(Location loc, ChatColor color) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
            .withColor(chatColorToFirework(color)).withFade(Color.WHITE)
            .with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    private Color chatColorToFirework(ChatColor cc) {
        return switch (cc) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case YELLOW -> Color.YELLOW;
            case AQUA -> Color.AQUA;
            case LIGHT_PURPLE -> Color.FUCHSIA;
            case GOLD -> Color.ORANGE;
            default -> Color.WHITE;
        };
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  TEAMMATE TRACKER — action bar shows all teammates coords every 3 seconds
    // ══════════════════════════════════════════════════════════════════════════
    private void startTrackerTask() {
        if (trackerTask != null) { trackerTask.cancel(); }
        trackerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.RUNNING) { cancel(); trackerTask = null; return; }
                for (BingoTeam team : teams) {
                    if (team.getMembers().size() <= 1) continue;
                    for (UUID id : team.getMembers()) {
                        Player p = Bukkit.getPlayer(id);
                        if (p == null) continue;
                        StringBuilder sb = new StringBuilder();
                        for (UUID otherId : team.getMembers()) {
                            if (otherId.equals(id)) continue;
                            Player other = Bukkit.getPlayer(otherId);
                            if (other == null) { sb.append("§8[offline] "); continue; }
                            Location loc = other.getLocation();
                            // Direction arrow relative to this player
                            String arrow = getDirectionArrow(p.getLocation(), loc);
                            sb.append(team.getColor()).append("§l").append(other.getName())
                              .append(" §r§7").append(arrow).append(" §8(")
                              .append((int) loc.getX()).append(", ")
                              .append((int) loc.getY()).append(", ")
                              .append((int) loc.getZ()).append(")  ");
                        }
                        if (sb.length() > 0) {
                            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(sb.toString().trim()));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 60L); // every 3 seconds
    }

    private String getDirectionArrow(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // Convert to compass bearing (0=E, 90=S, 180=W, 270=N in atan2)
        // Minecraft yaw: 0=S, 90=W, 180=N, 270=E
        double playerYaw = ((from.getYaw() % 360) + 360) % 360;
        double targetBearing = ((Math.toDegrees(Math.atan2(dx, -dz)) % 360) + 360) % 360;
        double relative = ((targetBearing - playerYaw) % 360 + 360) % 360;
        // 8 directions
        if (relative < 22.5 || relative >= 337.5) return "↑";
        if (relative < 67.5)  return "↗";
        if (relative < 112.5) return "→";
        if (relative < 157.5) return "↘";
        if (relative < 202.5) return "↓";
        if (relative < 247.5) return "↙";
        if (relative < 292.5) return "←";
        return "↖";
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public GameState getState() { return state; }
    public Difficulty getDifficulty() { return difficulty; }
    public boolean isSoloMode() { return soloMode; }
    public int getTeamSize() { return teamSize; }
    public int getTeamCount() { return teamCount; }
    public List<BingoTeam> getTeams() { return teams; }
    public List<UUID> getLobby() { return lobby; }
    public BingoTeam getTeamOf(UUID uuid) { return playerTeamMap.get(uuid); }
    public World getGameWorld() { return gameWorld; }
    public boolean isRunning() { return state == GameState.RUNNING; }

    public void setDifficulty(Difficulty d) { this.difficulty = d; plugin.getConfig().set("game.default-difficulty", d.name()); plugin.saveConfig(); }
    public void setTeamSize(int s) { this.teamSize = s; plugin.getConfig().set("game.team-size", s); plugin.saveConfig(); }
    public void setTeamCount(int c) { this.teamCount = c; plugin.getConfig().set("game.team-count", c); plugin.saveConfig(); }
    public void setSoloMode(boolean b) { this.soloMode = b; plugin.getConfig().set("game.solo-mode", b); plugin.saveConfig(); }
    public void setTimeLimit(int t) { this.timeLimit = t; plugin.getConfig().set("game.time-limit", t); plugin.saveConfig(); }
    public void setBorderSize(int s) { this.borderSize = s; plugin.getConfig().set("game.border-size", s); plugin.saveConfig(); }
    public void setEndOnFirstBingo(boolean b) { this.endOnFirstBingo = b; plugin.getConfig().set("game.end-on-first-bingo", b); plugin.saveConfig(); }
}