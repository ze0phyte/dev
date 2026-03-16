package com.parasite.game;

import com.parasite.ParasitePlugin;
import com.parasite.utils.ItemUtils;
import com.parasite.utils.LocationUtils;
import com.parasite.utils.ScoreboardUtils;
import com.parasite.utils.SkinUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;
import java.util.Collection;

public class GameManager {

    private final ParasitePlugin plugin;

    private GameState state = GameState.WAITING;
    private int currentDay = 0;
    private int timer = 0;
    private BukkitTask phaseTask;
    private BukkitTask scoreboardTask;
    private BukkitTask staminaTask;
    private final java.util.Map<UUID, Integer> votePages = new java.util.HashMap<>();

    // ── New feature state ──────────────────────────────────────────────────
    // Round replay log
    private final java.util.List<String> roundLog = new java.util.ArrayList<>();
    // Blackout — fired once per game
    private boolean blackoutUsed = false;
    private BukkitTask blackoutTask = null;
    // Weather shift
    private String currentWeather = "clear";
    // Sample collection — tracks who completed samples and when
    private final java.util.Map<UUID, Integer> sampleRoundCompleted = new java.util.HashMap<>();
    // Food stations — list of locations set by OP, per-player cooldown 60s
    private final java.util.List<Location> foodStations = new java.util.ArrayList<>();
    // Séance — dead players vote to haunt someone
    private final java.util.Map<UUID, UUID> seanceVotes = new java.util.HashMap<>(); // voter->target
    private UUID hauntedPlayer = null;
    private BukkitTask hauntTask = null;
    // Vital monitor task
    private BukkitTask vitalTask = null;
    // Nutrition required per round (scales with day duration)
    private int nutritionRequired = 2;

    private final Map<UUID, GamePlayer> gamePlayers = new LinkedHashMap<>();
    private final Map<UUID, Role> forcedRoles = new HashMap<>();

    private Location lobbyLocation;
    private Location arenaLocation;
    private Location discussionLocation;
    private Location votingLocation;

    private int cfgDayDuration;
    private int cfgDiscussionDuration;
    private int cfgVotingDuration;
    private int cfgSwapCooldown;
    private int cfgMinPlayers;
    private int cfgMaxPlayers;
    private int cfgLobbyCountdown;
    private int cfgParasiteCount;

    public static final String PREFIX = "§8[§c☣§8] §r";

    public GameManager(ParasitePlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        loadLocations();
        startScoreboardUpdater();
    }

    private void reloadConfig() {
        cfgDayDuration        = plugin.getConfig().getInt("game.day-duration", 180);
        cfgDiscussionDuration = plugin.getConfig().getInt("game.discussion-duration", 120);
        cfgVotingDuration     = plugin.getConfig().getInt("game.voting-duration", 15);
        cfgSwapCooldown       = plugin.getConfig().getInt("game.parasite-swap-cooldown", 120);
        cfgMinPlayers         = plugin.getConfig().getInt("game.min-players", 4);
        cfgMaxPlayers         = plugin.getConfig().getInt("game.max-players", 16);
        cfgLobbyCountdown     = plugin.getConfig().getInt("game.lobby-countdown", 30);
        cfgParasiteCount      = plugin.getConfig().getInt("game.parasite-count", 1);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOBBY
    // ══════════════════════════════════════════════════════════════════════════

    public void joinLobby(Player player) {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            player.sendMessage(PREFIX + "§cA game is already running. Wait for the next round!");
            return;
        }
        if (gamePlayers.containsKey(player.getUniqueId())) {
            player.sendMessage(PREFIX + "§eYou're already in the lobby.");
            return;
        }
        if (gamePlayers.size() >= cfgMaxPlayers) {
            player.sendMessage(PREFIX + "§cThe ship is full!");
            return;
        }

        gamePlayers.put(player.getUniqueId(), new GamePlayer(player.getUniqueId(), player.getName()));

        if (lobbyLocation != null) teleportBlind(player, lobbyLocation);

        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        for (PotionEffect e : player.getActivePotionEffects()) player.removePotionEffect(e.getType());

        broadcastAll(PREFIX + "§a" + player.getName() + " §7boarded the ship! "
                + "§8[§f" + gamePlayers.size() + "§8/§f" + cfgMaxPlayers + "§8]");

        ScoreboardUtils.updateTabList(this);
    }

    public void removePlayer(Player player) {
        UUID id = player.getUniqueId();
        if (!gamePlayers.containsKey(id)) return;
        gamePlayers.remove(id);
        broadcastAll(PREFIX + "§c" + player.getName() + " §7left. §8[§f" + gamePlayers.size() + "§8/§f" + cfgMaxPlayers + "§8]");
        if (state == GameState.WAITING || state == GameState.STARTING) {
            ScoreboardUtils.updateTabList(this);
        } else if (state != GameState.ENDED) {
            checkWinCondition();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  GAME START
    // ══════════════════════════════════════════════════════════════════════════

    public boolean startGame(Player admin) {
        if (state != GameState.WAITING && state != GameState.STARTING) {
            if (admin != null) admin.sendMessage(PREFIX + "§cGame already running.");
            return false;
        }
        if (gamePlayers.size() < cfgMinPlayers) {
            if (admin != null) admin.sendMessage(PREFIX + "§cNeed at least " + cfgMinPlayers + " players. Currently: " + gamePlayers.size());
            return false;
        }
        if (arenaLocation == null) {
            if (admin != null) admin.sendMessage(PREFIX + "§cNo arena location set! Use §e/parasite setarena");
            return false;
        }

        if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
        state = GameState.STARTING;
        timer = cfgLobbyCountdown;

        broadcastAll(PREFIX + "§aGame starting in §e" + cfgLobbyCountdown + " §aseconds!");

        phaseTask = new BukkitRunnable() {
            int t = cfgLobbyCountdown;
            @Override
            public void run() {
                if (t <= 0) { cancel(); launchGame(); return; }
                if (t <= 5 || t == 10 || t == 20 || t == 30) {
                    broadcastAll(PREFIX + "§eLaunching in §a" + t + "§e seconds!");
                }
                timer = t--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
        return true;
    }

    private void launchGame() {
        currentDay = 0;
        assignRoles();

        // Clear all placed signs from previous round
        clearArenaSigns();

        List<Player> active = getAlivePlayers();
        for (Player p : active) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        }
        // Stagger skin changes — SR rate-limits rapid consecutive commands
        for (int si = 0; si < active.size(); si++) {
            final Player sp = active.get(si);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> SkinUtils.setCrewSkin(sp, plugin), si * 5L);
        }

        List<Location> spawnPoints = generateSpawnRing(arenaLocation, active.size(), 24.0);
        int i = 0;
        for (Player p : active) {
            teleportBlind(p, spawnPoints.get(i++));
        }

        // Build parasite player list for peer-visibility nametags
        List<Player> parasitePlayers = getAlivePlayers().stream()
            .filter(p -> { GamePlayer gp = gamePlayers.get(p.getUniqueId()); return gp != null && gp.getRole() == Role.PARASITE; })
            .collect(Collectors.toList());
        SkinUtils.hideAllNames(parasitePlayers);

        new BukkitRunnable() {
            @Override
            public void run() {
                broadcastAll(PREFIX + "§a§lThe ship has launched! A parasite lurks among the crew...");
                for (Player p : getAlivePlayers()) {
                    GamePlayer gp = gamePlayers.get(p.getUniqueId());
                    revealRoleToPlayer(p, gp.getRole());
                }
                startDay();
            }
        }.runTaskLater(plugin, 60L);
    }

    private void assignRoles() {
        List<UUID> ids = new ArrayList<>(gamePlayers.keySet());
        Collections.shuffle(ids);

        for (GamePlayer gp : gamePlayers.values()) gp.resetFull();

        Set<UUID> assigned = new HashSet<>();
        for (Map.Entry<UUID, Role> entry : forcedRoles.entrySet()) {
            if (gamePlayers.containsKey(entry.getKey())) {
                gamePlayers.get(entry.getKey()).setRole(entry.getValue());
                assigned.add(entry.getKey());
            }
        }
        forcedRoles.clear();

        long forcedParasites = gamePlayers.values().stream().filter(gp -> gp.getRole() == Role.PARASITE).count();
        boolean hasDoctor    = gamePlayers.values().stream().anyMatch(gp -> gp.getRole() == Role.DOCTOR);

        List<UUID> unassigned = ids.stream().filter(id -> !assigned.contains(id)).collect(Collectors.toList());
        int idx = 0;
        // Assign parasites up to cfgParasiteCount
        int parasitesNeeded = (int) Math.max(0, cfgParasiteCount - forcedParasites);
        while (parasitesNeeded-- > 0 && idx < unassigned.size()) {
            gamePlayers.get(unassigned.get(idx++)).setRole(Role.PARASITE);
        }
        if (!hasDoctor && gamePlayers.size() >= 4 && idx < unassigned.size()) {
            gamePlayers.get(unassigned.get(idx)).setRole(Role.DOCTOR);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DAY PHASE
    // ══════════════════════════════════════════════════════════════════════════

    private void startDay() {
        currentDay++;
        state = GameState.IN_ROUND;
        timer = cfgDayDuration;
        nutritionRequired = cfgDayDuration >= 240 ? 3 : 2;

        // Round log entry
        roundLog.add("§8--- Day " + currentDay + " ---");

        // Weather shift — 40% chance of special weather
        applyWeatherShift();

        // Blackout — schedule once per game at random time between day 2 and day 4
        if (!blackoutUsed && currentDay == 2) scheduleBlackout();

        // Reset séance
        seanceVotes.clear();
        cancelHaunt();

        // Reset sample results from last round
        for (GamePlayer gp : gamePlayers.values()) {
            gp.setSampleResultReady(false);
            gp.setSamplesCollected(0);
        }

        // Re-apply Steve skins every day (SR clears them at discussion)
        List<Player> dayAlive = getAlivePlayers();
        for (int si = 0; si < dayAlive.size(); si++) {
            final Player sp = dayAlive.get(si);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> SkinUtils.setCrewSkin(sp, plugin), si * 8L);
        }

        // Re-apply parasite peer visibility each day
        List<Player> dayParasites = dayAlive.stream()
            .filter(p -> { GamePlayer gp = gamePlayers.get(p.getUniqueId()); return gp != null && gp.getRole() == Role.PARASITE; })
            .collect(Collectors.toList());
        SkinUtils.hideAllNames(dayParasites);

        for (GamePlayer gp : gamePlayers.values()) gp.resetRound();

        for (Player p : getAlivePlayers()) {
            giveRoundItems(p, gamePlayers.get(p.getUniqueId()).getRole());
        }

        broadcastAll(PREFIX + "§e§lDAY " + currentDay + " — §7" + cfgDayDuration / 60 + " minutes. Stay alert!");
        for (Player p : getAlivePlayers()) {
            p.sendTitle("§e§lDAY " + currentDay, "§7Find the parasite!", 10, 50, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
        }

        if (phaseTask != null) phaseTask.cancel();
        phaseTask = new BukkitRunnable() {
            int t = cfgDayDuration;
            @Override
            public void run() {
                if (t <= 0) { cancel(); startDiscussion(); return; }
                if (t == 60) broadcastAll(PREFIX + "§e1 minute left in this round!");
                if (t == 30) broadcastAll(PREFIX + "§e30 seconds left!");
                timer = t--;
            }
        }.runTaskTimer(plugin, 20L, 20L);

        startStaminaTask();
        startVitalTask();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  DISCUSSION PHASE
    // ══════════════════════════════════════════════════════════════════════════

    private void startDiscussion() {
        state = GameState.DISCUSSION;
        timer = cfgDiscussionDuration;

        // ── Kill infected players BEFORE discussion starts ────────────────────
        List<String> infectedDied = new ArrayList<>();
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isAlive()) continue;
            if (gp.isInfected() && !gp.isSavedThisRound()) {
                gp.setAlive(false);
                infectedDied.add(gp.getName());
                Player dp = Bukkit.getPlayer(gp.getUUID());
                if (dp != null) {
                    dp.setGameMode(GameMode.SPECTATOR);
                    dp.sendTitle("§4§l☠ INFECTED", "§7The parasite consumed you...", 10, 80, 20);
                }
            }
        }

        // Announce infection deaths before discussion
        if (!infectedDied.isEmpty()) {
            for (String dn : infectedDied) {
                broadcastAll(PREFIX + "§4☣ " + dn + " §7was consumed by the parasite overnight!");
            }
        } else {
            broadcastAll(PREFIX + "§7No infection deaths this round.");
        }

        // ── Teleport to discussion area if set ────────────────────────────────
        if (discussionLocation != null) {
            for (Player p : Bukkit.getOnlinePlayers()) teleportBlind(p, discussionLocation);
        }

        // Show names and restore skins during discussion
        SkinUtils.showAllNames();
        List<Player> aliveAtDisc = getAlivePlayers();
        for (int si = 0; si < aliveAtDisc.size(); si++) {
            final Player sp = aliveAtDisc.get(si);
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> SkinUtils.restoreOriginalSkin(sp, plugin), si * 5L);
        }

        // Remove combat items, keep signs
        for (Player p : aliveAtDisc) {
            stripCombatItems(p);
        }

        broadcastAll(PREFIX + "§e§l☎ DISCUSSION TIME! §7" + cfgDiscussionDuration + "s to discuss.");

        // Deliver sample results from last round
        deliverSampleResults();

        // Broadcast last wills of players who died this round
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isAlive() && gp.getLastWill() != null) {
                broadcastAll("§8[§c✉ Last Will§8] §f" + gp.getName() + "§7: §f" + gp.getLastWill());
                gp.setLastWill(null);
            }
        }

        // Announce séance haunt result
        if (hauntedPlayer != null) {
            Player hp = Bukkit.getPlayer(hauntedPlayer);
            if (hp != null) broadcastAll(PREFIX + "§8The dead are restless... §7" + hp.getName() + " §8feels watched.");
        }
        for (Player p : getAlivePlayers()) {
            p.sendTitle("§e§lDISCUSSION", "§7Talk! Who's the parasite?", 10, 50, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }

        if (phaseTask != null) phaseTask.cancel();
        phaseTask = new BukkitRunnable() {
            int t = cfgDiscussionDuration;
            @Override
            public void run() {
                if (t <= 0) { cancel(); startVoting(); return; }
                if (t == 30) broadcastAll(PREFIX + "§e30 seconds of discussion remaining!");
                timer = t--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  VOTING PHASE
    // ══════════════════════════════════════════════════════════════════════════

    private void startVoting() {
        state = GameState.VOTING;
        timer = cfgVotingDuration;

        // ── Teleport to voting area if set ────────────────────────────────────
        if (votingLocation != null) {
            for (Player p : getAlivePlayers()) teleportBlind(p, votingLocation);
        }

        for (Player p : getAlivePlayers()) {
            addBlindness(p, 3);
            giveVotingItems(p);
            p.sendTitle("§c§lVOTING", "§7Right-click a paper to vote!", 10, 40, 20);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
        }

        broadcastAll(PREFIX + "§c§lVOTING PHASE! §7" + cfgVotingDuration + "s — right-click your vote paper!");

        if (phaseTask != null) phaseTask.cancel();
        phaseTask = new BukkitRunnable() {
            int t = cfgVotingDuration;
            @Override
            public void run() {
                if (t <= 0) { cancel(); resolveVoting(); return; }
                timer = t--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void handleVote(Player voter, String targetName) {
        if (state != GameState.VOTING) return;
        GamePlayer gp = gamePlayers.get(voter.getUniqueId());
        if (gp == null || !gp.isAlive() || gp.hasVoted()) return;

        if (targetName.equals("SKIP")) {
            gp.setVotedFor(null);
            gp.setHasVoted(true);
            voter.sendMessage(PREFIX + "§7You skipped your vote.");
        } else {
            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) { voter.sendMessage(PREFIX + "§cThat player isn't online."); return; }
            GamePlayer tgp = gamePlayers.get(target.getUniqueId());
            if (tgp == null || !tgp.isAlive()) { voter.sendMessage(PREFIX + "§cThat player is already dead."); return; }
            gp.setVotedFor(target.getUniqueId());
            voter.sendMessage(PREFIX + "§7You voted for §c" + targetName + "§7.");
        }

        voter.getInventory().clear();
        voter.sendTitle("§a§l✓ VOTED", "§7Waiting for others...", 5, 40, 10);
        voter.playSound(voter.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        long aliveCount = gamePlayers.values().stream().filter(GamePlayer::isAlive).count();
        long votedCount = gamePlayers.values().stream().filter(g -> g.isAlive() && g.hasVoted()).count();
        if (votedCount >= aliveCount) {
            if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
            resolveVoting();
        }
    }

    private void resolveVoting() {
        state = GameState.ROUND_END;

        // ── Tally votes ──────────────────────────────────────────────────────
        Map<UUID, Integer> tally = new HashMap<>();
        int skipCount = 0;
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isAlive()) continue;
            if (!gp.hasVoted() || gp.getVotedFor() == null) {
                skipCount++;
            } else {
                tally.merge(gp.getVotedFor(), 1, Integer::sum);
            }
        }

        UUID ejected = null;
        int maxVotes = 0;
        boolean tie = false;
        for (Map.Entry<UUID, Integer> e : tally.entrySet()) {
            if (e.getValue() > maxVotes) { maxVotes = e.getValue(); ejected = e.getKey(); tie = false; }
            else if (e.getValue() == maxVotes) { tie = true; }
        }
        if (skipCount > maxVotes) { ejected = null; tie = false; }

        // ── Ejection ─────────────────────────────────────────────────────────
        final UUID finalEjected = (!tie && ejected != null) ? ejected : null;
        Role ejectedRole = null;
        if (finalEjected != null) {
            GamePlayer egp = gamePlayers.get(finalEjected);
            if (egp != null && egp.isAlive()) {
                ejectedRole = egp.getRole();
                egp.setAlive(false);
                Player ep = Bukkit.getPlayer(finalEjected);
                if (ep != null) {
                    ep.setGameMode(GameMode.SPECTATOR);
                    ep.sendTitle("§c§lEJECTED", "§7You were thrown into the void.", 10, 80, 20);
                }
            }
        }

        final Role finalEjectedRole = ejectedRole;
        final Map<UUID, Integer> finalTally = tally;
        final int finalSkip = skipCount;
        final boolean finalTie = tie;

        for (Player p : Bukkit.getOnlinePlayers()) addBlindness(p, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("\n§8§m══════════════════════════§r\n");
                sb.append("§e§l       DAY ").append(currentDay).append(" RESULTS\n\n");

                for (Map.Entry<UUID, Integer> e : finalTally.entrySet()) {
                    GamePlayer vgp = gamePlayers.get(e.getKey());
                    String nm = vgp != null ? vgp.getName() : "?";
                    sb.append("  §f").append(nm).append(" §8— §c").append(e.getValue()).append(" vote").append(e.getValue() != 1 ? "s" : "").append("\n");
                }
                if (finalSkip > 0) sb.append("  §7Skip §8— §7").append(finalSkip).append(" vote").append(finalSkip != 1 ? "s" : "").append("\n");
                sb.append("\n");

                if (finalEjected != null) {
                    GamePlayer egp = gamePlayers.get(finalEjected);
                    String nm = egp != null ? egp.getName() : "?";
                    sb.append("§c  ").append(nm).append(" §7was ejected into space!\n");
                    if (finalEjectedRole != null) {
                        sb.append("  §8They were: ").append(finalEjectedRole.getDisplay()).append("\n");
                    }
                } else if (finalTie) {
                    sb.append("§7  It was a tie — nobody was ejected.\n");
                } else {
                    sb.append("§7  The crew skipped — nobody was ejected.\n");
                }

                sb.append("§8§m══════════════════════════§r");
                broadcastAll(sb.toString());

                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4f, 0.5f);
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!checkWinCondition()) {
                            scatterAlivePlayers();
                            // hideAllNames called inside startDay() now
                            new BukkitRunnable() {
                                @Override
                                public void run() { startDay(); }
                            }.runTaskLater(plugin, 60L);
                        }
                    }
                }.runTaskLater(plugin, 120L);
            }
        }.runTaskLater(plugin, 60L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SKIP COMMANDS
    // ══════════════════════════════════════════════════════════════════════════

    public void skipToDiscussion() {
        if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
        startDiscussion();
    }

    public void skipToVoteEnd() {
        if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
        resolveVoting();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PARASITE ABILITIES
    // ══════════════════════════════════════════════════════════════════════════

    public void handleInfect(Player parasite, Player target) {
        if (state != GameState.IN_ROUND) return;
        GamePlayer pgp = gamePlayers.get(parasite.getUniqueId());
        GamePlayer tgp = gamePlayers.get(target.getUniqueId());
        if (pgp == null || tgp == null) return;
        if (!pgp.isAlive() || !tgp.isAlive()) return;
        if (tgp.getRole() == Role.PARASITE) return;
        // Parasite can only infect ONE person per round
        if (pgp.hasInfectedThisRound()) {
            parasite.sendMessage(PREFIX + "§cYou already infected someone this round!");
            return;
        }
        if (tgp.isInfected()) { parasite.sendMessage(PREFIX + "§cThat crewmate is already infected."); return; }

        tgp.setInfected(true);
        pgp.setInfectedThisRound(true);
        roundLog.add("§8Day " + currentDay + " — §c" + parasite.getName() + " §8infected §c" + target.getName());
        parasite.sendMessage(PREFIX + "§4☣ §7You infected §f" + target.getName() + "§7! They will die before discussion unless the Doctor saves them.");
        parasite.playSound(parasite.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.5f);
    }

        public void handleParasiteSwap(Player parasite) {
        if (state != GameState.IN_ROUND) return;
        GamePlayer pgp = gamePlayers.get(parasite.getUniqueId());
        if (pgp == null || pgp.getRole() != Role.PARASITE || !pgp.isAlive()) return;

        long now = System.currentTimeMillis();
        long elapsed = (now - pgp.getLastSwapMillis()) / 1000;
        if (pgp.getLastSwapMillis() != 0 && elapsed < cfgSwapCooldown) {
            long remaining = cfgSwapCooldown - elapsed;
            parasite.sendMessage(PREFIX + "§cSwap on cooldown! §e" + remaining + "s §cremaining.");
            return;
        }

        List<Player> candidates = getAlivePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(parasite.getUniqueId()))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) { parasite.sendMessage(PREFIX + "§cNo one to swap with!"); return; }

        Player target = candidates.get(new Random().nextInt(candidates.size()));
        Location parasiteLoc = parasite.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        // Silent swap — no blindness, no sound, completely unnoticeable to observers
        // Use velocity trick to smooth out MC's teleport lerp
        parasite.teleport(targetLoc);
        target.teleport(parasiteLoc);
        // Zero out velocity so there's no position lerp artifact
        parasite.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        target.setVelocity(new org.bukkit.util.Vector(0, 0, 0));

        pgp.setLastSwapMillis(now);
        roundLog.add("§8Day " + currentDay + " — §cParasite §8swapped with §c" + target.getName());
        // Only the parasite gets a private confirm — no sound, no broadcast
        parasite.sendMessage(PREFIX + "§c☣ Swap complete.");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CROSSBOW SCAN
    // ══════════════════════════════════════════════════════════════════════════

    public void handleCrossbowHit(Player shooter, Player target) {
        if (state != GameState.IN_ROUND && state != GameState.DISCUSSION) return;
        GamePlayer sgp = gamePlayers.get(shooter.getUniqueId());
        if (sgp == null || !sgp.isAlive()) return;
        if (sgp.hasUsedCrossbow()) { shooter.sendMessage(PREFIX + "§cYou've already used your scanner this round."); return; }

        sgp.setUsedCrossbow(true);
        shooter.sendMessage(PREFIX + "§eScanner result: §f" + target.getName());
        shooter.sendTitle("§e§lSCANNED", "§f" + target.getName(), 5, 40, 10);
        shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);

        for (int i = 0; i < shooter.getInventory().getSize(); i++) {
            ItemStack it = shooter.getInventory().getItem(i);
            if (it != null && it.getType() == Material.CROSSBOW) {
                shooter.getInventory().setItem(i, null);
                break;
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  WIN CONDITION
    // ══════════════════════════════════════════════════════════════════════════

    private boolean checkWinCondition() {
        long parasites = gamePlayers.values().stream().filter(gp -> gp.isAlive() && gp.getRole() == Role.PARASITE).count();
        long crew = gamePlayers.values().stream().filter(gp -> gp.isAlive() && gp.getRole() != Role.PARASITE).count();
        if (parasites == 0) { endGame(false); return true; }
        if (parasites >= crew) { endGame(true); return true; }
        return false;
    }

    private void endGame(boolean parasiteWon) {
        state = GameState.ENDED;
        if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
        SkinUtils.showAllNames();

        String title = parasiteWon ? "§4§l☣ PARASITE WINS ☣" : "§a§lCREW WINS!";
        String sub   = parasiteWon ? "§cThe parasite consumed the crew..." : "§7The parasite was eliminated!";

        // ── Round Replay ──────────────────────────────────────────────────
        if (!roundLog.isEmpty()) {
            broadcastAll("§8§m──────────────────────────");
            broadcastAll("§c§lROUND REPLAY");
            for (String entry : roundLog) broadcastAll(entry);
            broadcastAll("§8§m──────────────────────────");
        }
        roundLog.clear();

        StringBuilder reveal = new StringBuilder("\n§8§m══════════════════════════§r\n§e§l         GAME OVER\n\n");
        for (GamePlayer gp : gamePlayers.values()) {
            String status = gp.isAlive() ? "§a(Alive)" : "§c(Dead)";
            reveal.append("  §f").append(gp.getName()).append(" §8— ")
                    .append(gp.getRole().getDisplay()).append(" ").append(status).append("\n");
        }
        reveal.append("§8§m══════════════════════════§r");
        broadcastAll(reveal.toString());

        List<Player> allOnline = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int si = 0; si < allOnline.size(); si++) {
            final Player sp = allOnline.get(si);
            sp.sendTitle(title, sub, 10, 100, 30);
            addBlindness(sp, 5);
            if (parasiteWon) sp.playSound(sp.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
            else sp.playSound(sp.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            // Stagger skin restores — SR rate-limits rapid consecutive commands
            final int delay = si * 8;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> SkinUtils.restoreOriginalSkin(sp, plugin), delay);
        }

        new BukkitRunnable() {
            @Override
            public void run() { resetGame(); }
        }.runTaskLater(plugin, 200L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private void giveRoundItems(Player p, Role role) {
        p.getInventory().clear();
        p.getInventory().setItem(0, ItemUtils.signStack(16));
        p.getInventory().setItem(1, ItemUtils.signStack(16));
        p.getInventory().setItem(2, ItemUtils.crewAxe());
        // Last will book in slot 7
        org.bukkit.inventory.ItemStack willBook = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITABLE_BOOK);
        org.bukkit.inventory.meta.BookMeta willMeta = (org.bukkit.inventory.meta.BookMeta) willBook.getItemMeta();
        willMeta.setDisplayName("§fLast Will");
        willMeta.addPage("");
        willBook.setItemMeta(willMeta);
        p.getInventory().setItem(7, willBook);

        // Scanner count scales with alive players
        int alive = getAlivePlayers().size();
        int scanners = alive >= 13 ? 4 : alive >= 8 ? 3 : alive >= 5 ? 2 : 1;
        for (int s = 0; s < scanners; s++) {
            p.getInventory().setItem(3 + s, ItemUtils.scanCrossbow());
        }

        // Role card in slot 8
        if (role == Role.PARASITE) {
            p.getInventory().setItem(8, ItemUtils.parasiteIndicator());
        } else if (role == Role.DOCTOR) {
            p.getInventory().setItem(8, ItemUtils.doctorIndicator());
        } else {
            p.getInventory().setItem(8, ItemUtils.crewmateIndicator());
        }
    }

    private void giveVotingItems(Player p) {
        giveVotingItemsPage(p, 0);
    }

    public void giveVotingItemsPage(Player p, int page) {
        votePages.put(p.getUniqueId(), page);
        p.getInventory().clear();

        // Build list of votable players
        List<String> names = new ArrayList<>();
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isAlive()) continue;
            if (gp.getUUID().equals(p.getUniqueId())) continue;
            names.add(gp.getName());
        }

        // 7 names per page (slots 0-6), slot 7 = prev (if page>0), slot 8 = next or skip
        int perPage = 7;
        int start = page * perPage;
        int slot = 0;
        for (int i = start; i < Math.min(start + perPage, names.size()); i++) {
            p.getInventory().setItem(slot++, ItemUtils.votePaper(names.get(i)));
        }

        boolean hasPrev = page > 0;
        boolean hasNext = (start + perPage) < names.size();

        if (hasPrev) {
            p.getInventory().setItem(7, ItemUtils.pageButton("§e« Previous", page - 1));
        }
        if (hasNext) {
            p.getInventory().setItem(8, ItemUtils.pageButton("§eNext »", page + 1));
        } else {
            p.getInventory().setItem(8, ItemUtils.skipPaper());
        }

        if (page > 0 || names.size() > perPage) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText("§7Page §f" + (page + 1) + "§7/§f" + ((names.size() + perPage - 1) / perPage)));
        }
    }

    private void stripCombatItems(Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack it = p.getInventory().getItem(i);
            if (it == null) continue;
            if (it.getType() == Material.IRON_AXE || it.getType() == Material.CROSSBOW) {
                p.getInventory().setItem(i, null);
            }
        }
    }

    private void scatterAlivePlayers() {
        if (arenaLocation == null) return;
        List<Player> alive = getAlivePlayers();
        List<Location> spawns = generateSpawnRing(arenaLocation, alive.size(), 24.0);
        int i = 0;
        for (Player p : alive) {
            teleportBlind(p, spawns.get(i++));
            giveRoundItems(p, gamePlayers.get(p.getUniqueId()).getRole());
        }
    }

    private void revealRoleToPlayer(Player p, Role role) {
        p.sendTitle(role.getDisplay(), getRoleSub(role), 10, 80, 20);
        p.sendMessage(PREFIX + "§7You are the " + role.getDisplay());
        if (role == Role.PARASITE) {
            p.sendMessage(PREFIX + "§4Infect crew with right-click. Press §cF §4to swap positions every 2 minutes.");
            p.playSound(p.getLocation(), Sound.AMBIENT_CAVE, 1f, 0.5f);
        } else if (role == Role.DOCTOR) {
            p.sendMessage(PREFIX + "§bRight-click a player during the round to save them from infection.");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        } else {
            p.sendMessage(PREFIX + "§7Find the parasite. Use your §esigns §7and §escanner§7.");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    private String getRoleSub(Role role) {
        return switch (role) {
            case PARASITE -> "§4Infect and destroy the crew!";
            case DOCTOR   -> "§bProtect the crew from infection!";
            case CREWMATE -> "§7Find and eject the parasite!";
        };
    }

    public void teleportBlind(Player player, Location location) {
        addBlindness(player, 3);
        new BukkitRunnable() {
            @Override
            public void run() { player.teleport(location); }
        }.runTaskLater(plugin, 10L);
    }

    public void addBlindness(Player player, int seconds) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, seconds * 20, 1, false, false));
    }

    private List<Location> generateSpawnRing(Location centre, int count, double radius) {
        List<Location> locs = new ArrayList<>();
        if (count == 0) return locs;
        double angleStep = 2 * Math.PI / count;
        for (int i = 0; i < count; i++) {
            double angle = i * angleStep;
            double x = centre.getX() + radius * Math.cos(angle);
            double z = centre.getZ() + radius * Math.sin(angle);
            locs.add(new Location(centre.getWorld(), x, centre.getY(), z, (float) Math.toDegrees(angle + Math.PI), 0));
        }
        return locs;
    }

    private void broadcastAll(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(message);
        Bukkit.getConsoleSender().sendMessage(message);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STAMINA SYSTEM — food bar = stamina bar
    //  Sprinting drains stamina. When empty, player is forced to walk.
    //  Stamina regens when not sprinting. Runs every 2 ticks.
    // ══════════════════════════════════════════════════════════════════════════

    private void startStaminaTask() {
        if (staminaTask != null) { staminaTask.cancel(); staminaTask = null; }
        if (vitalTask != null) { vitalTask.cancel(); vitalTask = null; }
        if (blackoutTask != null) { blackoutTask.cancel(); blackoutTask = null; }
        cancelHaunt();
        staminaTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_ROUND) { cancel(); staminaTask = null; return; }
                for (Player p : getAlivePlayers()) {
                    GamePlayer gp = gamePlayers.get(p.getUniqueId());
                    if (gp == null) continue;

                    int stamina = gp.getStamina();
                    if (p.isSprinting()) {
                        // Drain: -1 every 2 ticks = empty in ~2 seconds (20 units over 40 ticks)
                        stamina = Math.max(0, stamina - 1);
                        if (stamina == 0) {
                            // Force stop sprinting
                            p.setSprinting(false);
                            p.setFoodLevel(0);
                        } else {
                            p.setFoodLevel(stamina);
                        }
                    } else {
                        // Regen: +1 every 2 ticks when not sprinting
                        stamina = Math.min(20, stamina + 1);
                        p.setFoodLevel(stamina);
                    }
                    gp.setStamina(stamina);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void startScoreboardUpdater() {
        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                ScoreboardUtils.updateTabList(GameManager.this);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ── Location management ───────────────────────────────────────────────────

    public void setLobbyLocation(Location loc) {
        lobbyLocation = loc;
        plugin.getConfig().set("locations.lobby", LocationUtils.serialize(loc));
        plugin.saveConfig();
    }

    public void setArenaLocation(Location loc) {
        arenaLocation = loc;
        plugin.getConfig().set("locations.arena", LocationUtils.serialize(loc));
        plugin.saveConfig();
    }

    public void setDiscussionLocation(Location loc) {
        discussionLocation = loc;
        plugin.getConfig().set("locations.discussion", LocationUtils.serialize(loc));
        plugin.saveConfig();
    }

    public void setVotingLocation(Location loc) {
        votingLocation = loc;
        plugin.getConfig().set("locations.voting", LocationUtils.serialize(loc));
        plugin.saveConfig();
    }

    private void loadLocations() {
        lobbyLocation      = LocationUtils.deserialize(plugin.getConfig().getString("locations.lobby"));
        arenaLocation      = LocationUtils.deserialize(plugin.getConfig().getString("locations.arena"));
        discussionLocation = LocationUtils.deserialize(plugin.getConfig().getString("locations.discussion"));
        votingLocation     = LocationUtils.deserialize(plugin.getConfig().getString("locations.voting"));
    }

    // ── Force role ────────────────────────────────────────────────────────────

    public void forceRole(Player target, Role role) {
        forcedRoles.put(target.getUniqueId(), role);
        GamePlayer gp = gamePlayers.get(target.getUniqueId());
        if (gp != null) gp.setRole(role);
    }

    // ── Force stop ────────────────────────────────────────────────────────────

    public void forceStop() {
        if (phaseTask != null) { phaseTask.cancel(); phaseTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        if (staminaTask != null) { staminaTask.cancel(); staminaTask = null; }
        SkinUtils.showAllNames();
        List<Player> forceAll = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int si = 0; si < forceAll.size(); si++) {
            final Player sp = forceAll.get(si);
            final int delay = si * 8;
            plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> SkinUtils.restoreOriginalSkin(sp, plugin), delay);
        }
        SkinUtils.cleanup();
        state = GameState.ENDED;
        broadcastAll(PREFIX + "§cGame forcefully stopped.");
        resetGame();
    }

    private void resetGame() {
        votePages.clear();
        roundLog.clear();
        blackoutUsed = false;
        seanceVotes.clear();
        hauntedPlayer = null;
        sampleRoundCompleted.clear();
        foodStations.clear();
        gamePlayers.clear();
        forcedRoles.clear();
        currentDay = 0;
        timer = 0;
        state = GameState.WAITING;
        for (Player p : Bukkit.getOnlinePlayers()) p.getInventory().clear();
        clearArenaSigns();
        SkinUtils.showAllNames();
        SkinUtils.cleanup();
        ScoreboardUtils.clearAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            SkinUtils.restoreOriginalSkin(p, plugin);
            p.setGameMode(GameMode.SURVIVAL);
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            if (lobbyLocation != null) p.teleport(lobbyLocation);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public GameState getState() { return state; }
    public int getCurrentDay() { return currentDay; }
    public int getTimer() { return timer; }
    public int getPlayerCount() { return gamePlayers.size(); }
    public int getMinPlayers() { return cfgMinPlayers; }
    public int getMaxPlayers() { return cfgMaxPlayers; }
    public boolean isRunning() { return state != GameState.WAITING && state != GameState.ENDED; }

    public GamePlayer getGamePlayer(UUID id) { return gamePlayers.get(id); }
    public Collection<GamePlayer> getAllGamePlayers() { return gamePlayers.values(); }

    public List<Player> getAlivePlayers() {
        return gamePlayers.values().stream()
                .filter(GamePlayer::isAlive)
                .map(gp -> Bukkit.getPlayer(gp.getUUID()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public long getSwapCooldownRemaining(UUID parasiteId) {
        GamePlayer gp = gamePlayers.get(parasiteId);
        if (gp == null || gp.getLastSwapMillis() == 0) return 0;
        long elapsed = (System.currentTimeMillis() - gp.getLastSwapMillis()) / 1000;
        return Math.max(0, cfgSwapCooldown - elapsed);
    }

    public Location getLobbyLocation() { return lobbyLocation; }
    public Location getArenaLocation() { return arenaLocation; }
    public Location getDiscussionLocation() { return discussionLocation; }
    public Location getVotingLocation() { return votingLocation; }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIGN CLEANUP — remove all oak signs within 100 blocks of arena centre
    // ══════════════════════════════════════════════════════════════════════════
    private void clearArenaSigns() {
        if (arenaLocation == null) return;
        org.bukkit.World world = arenaLocation.getWorld();
        if (world == null) return;
        int cx = arenaLocation.getBlockX();
        int cy = arenaLocation.getBlockY();
        int cz = arenaLocation.getBlockZ();
        int radius = 100;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - radius);
                     y <= Math.min(world.getMaxHeight() - 1, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.OAK_SIGN
                            || block.getType() == Material.OAK_WALL_SIGN) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  CONFIG GETTERS / SETTERS (for /parasite config command)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Set a config value at runtime and persist it.
     * Returns an error message or null on success.
     */
    public String setConfigValue(String key, int value) {
        switch (key.toLowerCase()) {
            case "day-duration":
                if (value < 30) return "§cMinimum 30 seconds.";
                cfgDayDuration = value;
                plugin.getConfig().set("game.day-duration", value);
                break;
            case "discussion-duration":
                if (value < 10) return "§cMinimum 10 seconds.";
                cfgDiscussionDuration = value;
                plugin.getConfig().set("game.discussion-duration", value);
                break;
            case "voting-duration":
                if (value < 5) return "§cMinimum 5 seconds.";
                cfgVotingDuration = value;
                plugin.getConfig().set("game.voting-duration", value);
                break;
            case "swap-cooldown":
                if (value < 0) return "§cMust be >= 0.";
                cfgSwapCooldown = value;
                plugin.getConfig().set("game.parasite-swap-cooldown", value);
                break;
            case "min-players":
                if (value < 2) return "§cMinimum 2 players.";
                cfgMinPlayers = value;
                plugin.getConfig().set("game.min-players", value);
                break;
            case "max-players":
                if (value < 2 || value > 100) return "§cMust be 2-100.";
                cfgMaxPlayers = value;
                plugin.getConfig().set("game.max-players", value);
                break;
            case "lobby-countdown":
                if (value < 5) return "§cMinimum 5 seconds.";
                cfgLobbyCountdown = value;
                plugin.getConfig().set("game.lobby-countdown", value);
                break;
            case "parasite-count":
                if (value < 1) return "§cMinimum 1 parasite.";
                cfgParasiteCount = value;
                plugin.getConfig().set("game.parasite-count", value);
                break;
            default:
                return "§cUnknown key. Valid: day-duration, discussion-duration, voting-duration, swap-cooldown, min-players, max-players, lobby-countdown, parasite-count";
        }
        plugin.saveConfig();
        return null;
    }

    public String getConfigSummary() {
        return "§7day-duration§f=" + cfgDayDuration
             + " §7discussion§f=" + cfgDiscussionDuration
             + " §7voting§f=" + cfgVotingDuration
             + "\n§7swap-cooldown§f=" + cfgSwapCooldown
             + " §7min§f=" + cfgMinPlayers
             + " §7max§f=" + cfgMaxPlayers
             + " §7lobby§f=" + cfgLobbyCountdown
             + " §7parasites§f=" + cfgParasiteCount;
    }


    // ══════════════════════════════════════════════════════════════════════════
    //  VITAL MONITOR — shows nutrition status in action bar every 4 seconds
    // ══════════════════════════════════════════════════════════════════════════
    private void startVitalTask() {
        if (vitalTask != null) { vitalTask.cancel(); vitalTask = null; }
        vitalTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_ROUND) { cancel(); vitalTask = null; return; }
                for (Player p : getAlivePlayers()) {
                    GamePlayer gp = gamePlayers.get(p.getUniqueId());
                    if (gp == null) continue;
                    int eaten = gp.getNutritionCount();
                    int required = nutritionRequired;
                    String bar = buildNutritionBar(eaten, required);
                    String status = gp.isInfected() ? " §c[INFECTED?]" : "";
                    String msg = "§7Nutrition: " + bar + " §8(" + eaten + "/" + required + ")" + status;
                    p.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                }
            }
        }.runTaskTimer(plugin, 0L, 80L); // every 4 seconds
    }

    private String buildNutritionBar(int eaten, int required) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < required; i++) {
            sb.append(i < eaten ? "§a█" : "§8█");
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  FOOD STATIONS — right-click a specific block to eat
    //  Set stations with /parasite addfoodstation
    //  60-second per-player cooldown per use
    // ══════════════════════════════════════════════════════════════════════════
    public boolean handleFoodStation(Player player, org.bukkit.block.Block block) {
        if (state != GameState.IN_ROUND) return false;
        GamePlayer gp = gamePlayers.get(player.getUniqueId());
        if (gp == null || !gp.isAlive()) return false;

        boolean isStation = foodStations.stream().anyMatch(loc ->
            loc.getBlockX() == block.getX() &&
            loc.getBlockY() == block.getY() &&
            loc.getBlockZ() == block.getZ() &&
            loc.getWorld().equals(block.getWorld()));
        if (!isStation) return false;

        long now = System.currentTimeMillis();
        long elapsed = (now - gp.getLastFoodStationUse()) / 1000;
        if (gp.getLastFoodStationUse() != 0 && elapsed < 60) {
            long remaining = 60 - elapsed;
            player.sendMessage(PREFIX + "§cFood station on cooldown! §e" + remaining + "s remaining.");
            return true;
        }

        gp.setLastFoodStationUse(now);
        gp.incrementNutrition();
        player.setFoodLevel(Math.min(20, player.getFoodLevel() + 6));
        player.setSaturation(Math.min(20, player.getSaturation() + 3));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 0.5f, 1f);
        player.sendMessage(PREFIX + "§aYou ate. Nutrition: §f" + gp.getNutritionCount() + "§7/§f" + nutritionRequired);
        return true;
    }

    public void addFoodStation(Location loc) {
        foodStations.add(loc.getBlock().getLocation());
        plugin.getConfig().set("foodstations." + foodStations.size(), LocationUtils.serialize(loc));
        plugin.saveConfig();
    }

    public List<Location> getFoodStations() { return foodStations; }

    // ══════════════════════════════════════════════════════════════════════════
    //  WEATHER SHIFT — random each round
    // ══════════════════════════════════════════════════════════════════════════
    private void applyWeatherShift() {
        if (arenaLocation == null) return;
        org.bukkit.World world = arenaLocation.getWorld();
        if (world == null) return;

        double roll = Math.random();
        if (roll < 0.35) {
            // Rain — mild visibility reduction via particles, no actual effect
            currentWeather = "rain";
            world.setStorm(true);
            world.setThundering(false);
            broadcastAll(PREFIX + "§9⛈ Rain settles over the ship...");
        } else if (roll < 0.55) {
            // Thunder — random sound cues, masking audio
            currentWeather = "thunder";
            world.setStorm(true);
            world.setThundering(true);
            broadcastAll(PREFIX + "§8⚡ A thunderstorm rolls in. Stay sharp.");
        } else if (roll < 0.70) {
            // Clear — slight speed boost to all crew
            currentWeather = "clear";
            world.setStorm(false);
            world.setThundering(false);
            for (Player p : getAlivePlayers()) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, cfgDayDuration * 20, 0, false, false));
            }
            broadcastAll(PREFIX + "§e☀ Clear skies. The crew moves faster.");
        } else {
            // No special weather
            currentWeather = "normal";
            world.setStorm(false);
            world.setThundering(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  BLACKOUT — fires once per game at a random time in day 2
    // ══════════════════════════════════════════════════════════════════════════
    private void scheduleBlackout() {
        blackoutUsed = true;
        // Random time between 30s and (dayDuration - 40s) into the round
        int delay = 30 + new Random().nextInt(Math.max(1, cfgDayDuration - 70));
        blackoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_ROUND) return;
                // Parasite gets 5s warning
                for (GamePlayer gp : gamePlayers.values()) {
                    if (gp.getRole() == Role.PARASITE && gp.isAlive()) {
                        Player p = Bukkit.getPlayer(gp.getUUID());
                        if (p != null) p.sendMessage(PREFIX + "§4⚠ Blackout in 5 seconds. Move.");
                    }
                }
                // 5 ticks later — blackout for everyone
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (state != GameState.IN_ROUND) return;
                    broadcastAll(PREFIX + "§8§l⬛ BLACKOUT! Power failure...");
                    roundLog.add("§8Day " + currentDay + " — Blackout triggered.");
                    for (Player p : getAlivePlayers()) {
                        addBlindness(p, 20);
                        p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, 1f, 0.5f);
                    }
                    // Restore after 20s
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        broadcastAll(PREFIX + "§e§lPower restored.");
                        for (Player p : getAlivePlayers())
                            p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    }, 400L);
                }, 100L);
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SAMPLE COLLECTION — medbay mechanic
    //  Right-click sample block -> collect. Bring 3 to lab chest -> reveal next round
    // ══════════════════════════════════════════════════════════════════════════
    public boolean handleSampleCollect(Player player) {
        if (state != GameState.IN_ROUND) return false;
        GamePlayer gp = gamePlayers.get(player.getUniqueId());
        if (gp == null || !gp.isAlive()) return false;
        if (gp.getSamplesCollected() >= 3) {
            player.sendMessage(PREFIX + "§7You already have 3 samples. Bring them to the lab chest.");
            return true;
        }
        gp.setSamplesCollected(gp.getSamplesCollected() + 1);
        player.sendMessage(PREFIX + "§aSample collected! §8(" + gp.getSamplesCollected() + "/3)");
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOTTLE_FILL, 1f, 1.5f);
        return true;
    }

    public boolean handleLabChest(Player player) {
        if (state != GameState.IN_ROUND) return false;
        GamePlayer gp = gamePlayers.get(player.getUniqueId());
        if (gp == null || !gp.isAlive()) return false;

        // Check if already used sample this round or last 2 rounds
        int lastUsed = sampleRoundCompleted.getOrDefault(player.getUniqueId(), -99);
        if (currentDay - lastUsed < 2) {
            player.sendMessage(PREFIX + "§cSample analysis on cooldown for " + (2 - (currentDay - lastUsed)) + " more round(s).");
            return true;
        }
        if (gp.getSamplesCollected() < 3) {
            player.sendMessage(PREFIX + "§cNeed 3 samples first. You have §e" + gp.getSamplesCollected() + "§c.");
            return true;
        }

        sampleRoundCompleted.put(player.getUniqueId(), currentDay);
        gp.setSampleResultReady(true);
        gp.setSamplesCollected(0);
        player.sendMessage(PREFIX + "§aSamples submitted! Results will be ready at the start of next discussion.");
        roundLog.add("§8Day " + currentDay + " — " + player.getName() + " submitted samples.");
        return true;
    }

    // Called at start of discussion — deliver sample results
    private void deliverSampleResults() {
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isSampleResultReady()) continue;
            Player p = Bukkit.getPlayer(gp.getUUID());
            if (p == null) continue;
            // Pick a random alive player to reveal
            List<Player> targets = getAlivePlayers().stream()
                .filter(t -> !t.getUniqueId().equals(p.getUniqueId()))
                .collect(Collectors.toList());
            if (targets.isEmpty()) continue;
            Player target = targets.get(new Random().nextInt(targets.size()));
            GamePlayer tgp = gamePlayers.get(target.getUniqueId());
            if (tgp == null) continue;
            boolean isParasite = tgp.getRole() == Role.PARASITE;
            String result = isParasite ? "§4☣ PARASITE" : "§a✔ HUMAN";
            p.sendMessage(PREFIX + "§aSample Result: §f" + target.getName() + " §7is " + result);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            gp.setSampleResultReady(false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SÉANCE — dead players vote to haunt a living player
    // ══════════════════════════════════════════════════════════════════════════
    public void handleSeanceVote(Player deadPlayer, Player target) {
        GamePlayer gp = gamePlayers.get(deadPlayer.getUniqueId());
        if (gp == null || gp.isAlive()) {
            deadPlayer.sendMessage(PREFIX + "§cOnly dead players can use the séance.");
            return;
        }
        if (state != GameState.IN_ROUND && state != GameState.DISCUSSION) return;
        GamePlayer tgp = gamePlayers.get(target.getUniqueId());
        if (tgp == null || !tgp.isAlive()) {
            deadPlayer.sendMessage(PREFIX + "§cThat player is not alive.");
            return;
        }
        seanceVotes.put(deadPlayer.getUniqueId(), target.getUniqueId());
        deadPlayer.sendMessage(PREFIX + "§8You voted to haunt §7" + target.getName() + "§8.");
        resolveSeance();
    }

    private void resolveSeance() {
        java.util.Map<UUID, Integer> tally = new java.util.HashMap<>();
        for (UUID targetId : seanceVotes.values()) {
            tally.merge(targetId, 1, Integer::sum);
        }
        UUID topTarget = null;
        int topVotes = 0;
        for (java.util.Map.Entry<UUID, Integer> e : tally.entrySet()) {
            if (e.getValue() > topVotes) { topVotes = e.getValue(); topTarget = e.getKey(); }
        }
        if (topTarget == null) return;
        cancelHaunt();
        hauntedPlayer = topTarget;
        startHaunt(topTarget);
    }

    private void startHaunt(UUID targetId) {
        hauntTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.IN_ROUND) { cancel(); return; }
                Player p = Bukkit.getPlayer(targetId);
                if (p == null) { cancel(); return; }
                // Occasional flicker: brief blindness + sound
                if (Math.random() < 0.3) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 8, 0, false, false));
                    p.playSound(p.getLocation(), org.bukkit.Sound.AMBIENT_CAVE, 0.3f, 0.5f);
                }
            }
        }.runTaskTimer(plugin, 40L, 60L); // check every 3 seconds
    }

    private void cancelHaunt() {
        if (hauntTask != null) { hauntTask.cancel(); hauntTask = null; }
        hauntedPlayer = null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LAST WILL — player writes in a book, broadcast on death
    // ══════════════════════════════════════════════════════════════════════════
    public void handleLastWillClose(Player player, org.bukkit.inventory.meta.BookMeta book) {
        GamePlayer gp = gamePlayers.get(player.getUniqueId());
        if (gp == null) return;
        if (book.getPageCount() > 0) {
            String text = book.getPage(1);
            // Strip formatting to keep it readable, max 120 chars
            text = text.replaceAll("§[0-9a-fk-or]", "").trim();
            if (text.length() > 120) text = text.substring(0, 120) + "...";
            gp.setLastWill(text);
            player.sendMessage(PREFIX + "§aLast will saved. It will be read if you die.");
        }
    }

    public void giveLastWillBook(Player player) {
        org.bukkit.inventory.ItemStack book = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WRITABLE_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        meta.setDisplayName("§fLast Will");
        meta.addPage("Write your last will here...");
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
        player.sendMessage(PREFIX + "§7Write your §flast will §7and close the book to save it.");
    }

    // Commands to add/list food stations
    public void addFoodStationCmd(Player admin, Location loc) {
        addFoodStation(loc);
        admin.sendMessage(PREFIX + "§aFood station added at §f" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        admin.sendMessage(PREFIX + "§7Place a §fbarrel §7or §fhay bale §7block here as the visual.");
    }

}