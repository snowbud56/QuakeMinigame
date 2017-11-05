package com.snowbud56.constructors;

import com.snowbud56.RollbackHandler;
import com.snowbud56.QuakeMinigame;
import com.snowbud56.data.DataHandler;
import com.snowbud56.utils.ChatUtils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.*;

public class Game {

    private String displayName;
    private int maxPlayers;
    private int minPlayers;
    private World world;
    private List<Location> spawnPoints;
    private GameState gameState = GameState.LOBBY;
    private Location lobbyPoint;
    private Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
    private Objective obj = board.getObjective("points");
    private boolean explosiveMode = false;

    private Set<Player> players;
    private Set<Player> spectators;
    private HashMap<Player, Integer> points;
    private boolean isMovementFrozen = false;

    public Game(String gameName) {
        FileConfiguration fileConfiguration = DataHandler.getInstance().getGameInfo();
        this.spectators = new HashSet<>();
        this.players = new HashSet<>();
        this.spawnPoints = new ArrayList<>();
        this.points = new HashMap<>();
        if (obj == null) obj = board.registerNewObjective("dummy", "points");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.displayName = fileConfiguration.getString("games." + gameName + ".displayName");
        obj.setDisplayName(ChatUtils.format("&6&l" + displayName));
        this.maxPlayers = fileConfiguration.getInt("games." + gameName + ".maxPlayers");
        this.minPlayers = fileConfiguration.getInt("games." + gameName + ".minPlayers");
        File file = new File(fileConfiguration.getString("games." + gameName + ".world") + "_active");
        if (file.exists()) RollbackHandler.getRollbackHandler().delete(file);
        RollbackHandler.getRollbackHandler().rollback(fileConfiguration.getString("games." + gameName + ".world"));
        this.world = Bukkit.createWorld(new WorldCreator(fileConfiguration.getString("games." + gameName + ".world") + "_active"));
        try {
            String[] values = fileConfiguration.getString("games." + gameName + ".lobbyPoint").split(",");
            double x = Double.parseDouble(values[0].split(":")[1]);
            double y = Double.parseDouble(values[1].split(":")[1]);
            double z = Double.parseDouble(values[2].split(":")[1]);
            lobbyPoint = new Location(world, x, y, z);
        } catch (Exception e) {
            QuakeMinigame.getInstance().getLogger().severe("Failed to load lobbyPoint for game + '" + gameName + "'. ExceptionType: " + e);
        }
        for (String location : DataHandler.getInstance().getGameInfo().getStringList("games." + gameName + ".spawnPoints")) {
            try {
                String[] values = location.split(",");
                double x = Double.parseDouble(values[0].split(":")[1]);
                double y = Double.parseDouble(values[1].split(":")[1]);
                double z = Double.parseDouble(values[2].split(":")[1]);
                Location loc = new Location(world, x, y, z);
                spawnPoints.add(loc);
            } catch (Exception e) {
                QuakeMinigame.getInstance().getLogger().severe("Failed to load spawnPoint with metadata " + location + " for game + '" + gameName + "'. ExceptionType: " + e);
            }
        }
    }

    public void quitGame(Player player) {
        sendMessage("&c[-] &7" + player.getName() + " " + (isState(GameState.LOBBY) || isState(GameState.STARTING) ? "&7(&a" + (getPlayers().size() - 1) + "&7/&a" + getMaxPlayers() + "&7)." : "."));
        player.teleport(Bukkit.getWorld("world").getSpawnLocation());
        player.setFoodLevel(25);
        player.setMaxHealth(20);
        player.setHealth(player.getMaxHealth());
        player.sendMessage(ChatUtils.format("&a[!] Successfully left the game."));
        if (players.contains(player)) players.remove(player);
        else if (spectators.contains(player)) spectators.remove(player);
        board.resetScores(player.getName());
        if (isState(GameState.ACTIVE) && players.size() <= 1)for (Player p : players) endGame(p);
    }

    public void joinGame(Player player) {
        if (players.contains(player)) {
            player.sendMessage(ChatUtils.format("&c[!] You're already in the game!"));
            return;
        }
        if (isState(GameState.LOBBY) || isState(GameState.STARTING)) {
            if (getPlayers().size() == getMaxPlayers()) player.sendMessage(ChatUtils.format("&c[!] That game is full!"));
            else {
                player.getInventory().clear();
                getPlayers().add(player);
                player.setScoreboard(board);
                player.teleport((isState(GameState.LOBBY) || isState(GameState.STARTING)) ? lobbyPoint : spawnPoints.get(0));
                player.setMaxHealth(20);
                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(25);
                sendMessage("&a[+] &7" + player.getName() + " &7(&a" + getPlayers().size() + "&7/&a" + getMaxPlayers() + "&7).");
            }
            if (getPlayers().size() == getMinPlayers() && !isState(GameState.STARTING)) {
                startCountDown(false);
                sendMessage("&a[+] &7" + player.getName() + "&7.");
            }
        } else {
            getSpectators().add(player);
        }
    }

    public void startCountDown(Boolean forceStart) {
        setState(GameState.STARTING);
        sendMessage("&a[!] You will be teleported in 20 seconds!");
        new BukkitRunnable() {
            private int time = 20;
            @Override
            public void run() {
                if (isState(GameState.STARTING)) {
                    if (players.size() < minPlayers && !forceStart) {
                        sendMessage("&c[!] Stop cancelled. (Not enough players)");
                        setState(GameState.LOBBY);
                        cancel();
                        return;
                    }
                    time--;
                    if (time <= 0) {
                        startGame();
                        setMovementFrozen(true);
                        int id = 0;
                        ItemStack item = new ItemStack(Material.WOOD_HOE, 1);
                        ItemMeta im = item.getItemMeta();
                        im.spigot().setUnbreakable(true);
                        im.setDisplayName(ChatUtils.format("&7Basic Railgun"));
                        im.setLore(Collections.singletonList(ChatUtils.format("&7Pew! Pew!")));
                        item.setItemMeta(im);
                        for (Player player : players) {
                            player.getInventory().clear();
                            player.getInventory().addItem(item);
                            obj.getScore(player.getName()).setScore(0);
                            try {
                                player.teleport(getSpawnPoints().get(id));
                                id += 1;
                            } catch (IndexOutOfBoundsException ex) {
                                sendMessage("&c&l[!] Something went wrong! Please contact an Administrator!");
                                endGame(null);
                                QuakeMinigame.getInstance().getLogger().severe("Not enough spawn points to satisfy game needs (Game is " + getDisplayName() + ")");
                            }
                        }
                        cancel();
                    } else if (time % 5 == 0) sendMessage("&a[!] You will be teleported in " + time + " seconds!");
                } else cancel();
            }
        }.runTaskTimer(QuakeMinigame.getInstance(), 0, 20);
    }

    private void startGame() {
        new BukkitRunnable() {
            private int startIn = 11;
            @Override
            public void run() {
                if (startIn <= 1) {
                    this.cancel();
                    setState(Game.GameState.ACTIVE);
                    sendMessage("&a[!] The game has started.");
                    setMovementFrozen(false);
                    for (Player p : players) p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                } else {
                    startIn -= 1;
                    sendMessage("&c[*] The game will begin in " + startIn + " second" + (startIn == 1 ? "" : "s") + ".");
                }
            }
        }.runTaskTimer(QuakeMinigame.getInstance(), 0, 20);
    }

    public void endGame(Player winner) {
        setState(GameState.ENDING);
        if (winner == null) Bukkit.broadcastMessage(ChatUtils.format("&a[!] No one won the game in " + getDisplayName() + "!"));
        else Bukkit.broadcastMessage(ChatUtils.format("&a[!] " + winner.getName() + " won the game in " + getDisplayName() + "!"));
        for (Player player : spectators) {
            player.removePotionEffect(PotionEffectType.SPEED);
            player.teleport(Bukkit.getWorld("world").getSpawnLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.setMaxHealth(20);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(25);
            player.getInventory().clear();
        }
        for (Player player : players) {
            player.removePotionEffect(PotionEffectType.SPEED);
            board.resetScores(player.getName());
            player.teleport(Bukkit.getWorld("world").getSpawnLocation());
            player.setGameMode(GameMode.SURVIVAL);
            player.setMaxHealth(20);
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(25);
            player.getInventory().clear();
        }
        RollbackHandler.getRollbackHandler().rollback(world);

        spectators.clear();
        players.clear();
        points.clear();
        explosiveMode = false;
        setState(GameState.LOBBY);
    }

    /*public void switchToSpectator(Player p) {
        players.remove(p);
        spectators.add(p);
    }*/

    public void addPoint(Player p) {
        Integer value = points.get(p);
        if (value == null) value = 0;
        points.put(p, value + 1);
        obj.getScore(p.getName()).setScore(value + 1);
    }

    public Integer getPoints(Player p) {
        points.putIfAbsent(p, 0);
        return points.get(p);
    }

    public boolean isExplosiveMode() {
        return explosiveMode;
    }

    public void setExplosiveMode(boolean explosiveMode1) {
        explosiveMode = explosiveMode1;
    }

    public boolean isMovementFrozen() {
        return isMovementFrozen;
    }

    public void setMovementFrozen(boolean movementFrozen) {
        isMovementFrozen = movementFrozen;
    }

    public boolean isState(GameState state) {
        return gameState.equals(state);
    }

    public void setState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public String getDisplayName() {
        return displayName;
    }

    public World getWorld() {
        return world;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public Location getLobbyPoint() {
        return lobbyPoint;
    }

    public void sendMessage(String message) {
        for (Player player : getPlayers()) {
            player.sendMessage(ChatUtils.format(message));
        }
    }

    public Set<Player> getSpectators() {
        return spectators;
    }

    public enum GameState {
        LOBBY, STARTING, ACTIVE, ENDING
    }
}
