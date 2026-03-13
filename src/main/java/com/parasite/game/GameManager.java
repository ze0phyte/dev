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

    public static final String PREFIX = "§8[§5☣§8] §r";

    public GameManager(ParasitePlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        loadLocations();
        startScoreboardUpdater();
    }

    private void reloadConfig() {
        cfgDayDuration        = plugin.getConfig().getInt("game.day-duration", 300);
        cfgDiscussionDuration = plugin.getConfig().getInt("game.discussion-duration", 120);
        cfgVotingDuration     = plugin.getConfig().getInt("game.voting-duration", 15);
        cfgSwapCooldown       = plugin.getConfig().getInt("game.parasite-swap-cooldown", 120);
        cfgMinPlayers         = plugin.getConfig().getInt("game.min-players", 4);
        cfgMaxPlayers         = plugin.getConfig().getInt("game.max-players", 16);
        cfgLobbyCountdown     = plugin.getConfig().getInt("game.lobby-countdown", 30);
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

        List<Player> active = getAlivePlayers();
        for (Player p : active) {
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setFoodLevel(20);
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            SkinUtils.setCrewSkin(p);
        }

        List<Location> spawnPoints = generateSpawnRing(arenaLocation, active.size(), 8.0);
        int i = 0;
        for (Player p : active) {
            teleportBlind(p, spawnPoints.get(i++));
        }

        SkinUtils.hideAllNames();

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

        boolean hasParasite = gamePlayers.values().stream().anyMatch(gp -> gp.getRole() == Role.PARASITE);
        boolean hasDoctor   = gamePlayers.values().stream().anyMatch(gp -> gp.getRole() == Role.DOCTOR);

        List<UUID> unassigned = ids.stream().filter(id -> !assigned.contains(id)).collect(Collectors.toList());
        int idx = 0;
        if (!hasParasite && !unassigned.isEmpty()) {
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

        // Show names during discussion
        SkinUtils.showAllNames();

        // Remove combat items, keep signs
        for (Player p : getAlivePlayers()) {
            stripCombatItems(p);
        }

        broadcastAll(PREFIX + "§e§l☎ DISCUSSION TIME! §7" + cfgDiscussionDuration + "s to discuss.");
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
                            SkinUtils.hideAllNames();
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
        if (tgp.isInfected()) { parasite.sendMessage(PREFIX + "§cThat crewmate is already infected."); return; }

        tgp.setInfected(true);
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

        // Blind everyone during swap
        for (Player p : getAlivePlayers()) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 255, false, false));
        }

        parasite.teleport(targetLoc);
        target.teleport(parasiteLoc);

        pgp.setLastSwapMillis(now);
        parasite.sendMessage(PREFIX + "§5You swapped positions with someone on the ship!");
        parasite.playSound(parasite.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.2f, 2f);
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

        StringBuilder reveal = new StringBuilder("\n§8§m══════════════════════════§r\n§e§l         GAME OVER\n\n");
        for (GamePlayer gp : gamePlayers.values()) {
            String status = gp.isAlive() ? "§a(Alive)" : "§c(Dead)";
            reveal.append("  §f").append(gp.getName()).append(" §8— ")
                    .append(gp.getRole().getDisplay()).append(" ").append(status).append("\n");
        }
        reveal.append("§8§m══════════════════════════§r");
        broadcastAll(reveal.toString());

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(title, sub, 10, 100, 30);
            addBlindness(p, 5);
            if (parasiteWon) p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.5f, 0.5f);
            else p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            SkinUtils.restoreOriginalSkin(p);
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
        p.getInventory().setItem(3, ItemUtils.scanCrossbow());
        if (role == Role.PARASITE) {
            p.getInventory().setItem(8, ItemUtils.parasiteIndicator());
        } else if (role == Role.DOCTOR) {
            p.getInventory().setItem(8, ItemUtils.doctorIndicator());
        }
    }

    private void giveVotingItems(Player p) {
        p.getInventory().clear();
        int slot = 0;
        for (GamePlayer gp : gamePlayers.values()) {
            if (!gp.isAlive()) continue;
            if (gp.getUUID().equals(p.getUniqueId())) continue;
            if (slot < 8) p.getInventory().setItem(slot++, ItemUtils.votePaper(gp.getName()));
        }
        p.getInventory().setItem(8, ItemUtils.skipPaper());
        // Doctor save papers REMOVED — doctor saves during round only
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
        List<Location> spawns = generateSpawnRing(arenaLocation, alive.size(), 8.0);
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
        SkinUtils.showAllNames();
        for (Player p : Bukkit.getOnlinePlayers()) SkinUtils.restoreOriginalSkin(p);
        SkinUtils.cleanup();
        state = GameState.ENDED;
        broadcastAll(PREFIX + "§cGame forcefully stopped.");
        resetGame();
    }

    private void resetGame() {
        gamePlayers.clear();
        forcedRoles.clear();
        currentDay = 0;
        timer = 0;
        state = GameState.WAITING;
        SkinUtils.showAllNames();
        SkinUtils.cleanup();
        ScoreboardUtils.clearAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            SkinUtils.restoreOriginalSkin(p);
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
}