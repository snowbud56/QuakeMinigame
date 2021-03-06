package com.snowbud56;

import com.snowbud56.constructors.Game;
import com.snowbud56.data.DataHandler;
import com.snowbud56.events.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class QuakeMinigame extends JavaPlugin {
    private static QuakeMinigame instance;
    private static Set<Game> games;
    private int gamesLimit;

    @Override
    public void onEnable() {
        instance = this;
        getConfig().options().copyDefaults(true);
        getConfig().options().copyHeader(true);
        saveDefaultConfig();
        getCommand("quake").setExecutor(new QuakeCommand());
        games = new HashSet<>();
        gamesLimit = getConfig().getInt("max-games");
        if (DataHandler.getInstance().getGameInfo().getConfigurationSection("games") != null) {
            for (String gameName : DataHandler.getInstance().getGameInfo().getConfigurationSection("games").getKeys(false)) {
                Game game = new Game(gameName);
                this.registerGame(game);
            }
        } else {
            getLogger().info("There are no games set up! Please create one using the creation command.");
        }
        registerListeners(new PlayerJoin(), new FoodLevelChange(), new PlayerDamage(), new PlayerDeath(), new PlayerQuit(), new QuakeGuns(), new BlockInteract(), new PlayerJoin());
        getLogger().info("Plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        instance = null;
        for (Game game : games) {
            if (game.isState(Game.GameState.ACTIVE)) {
                for (Player player : game.getPlayers()) {
                    player.setGameMode(GameMode.SURVIVAL);
                    for (PotionEffectType type : PotionEffectType.values()) player.removePotionEffect(type);
                    player.teleport(Bukkit.getWorld("world").getSpawnLocation());
                    player.getInventory().clear();
                }
            }
        }
    }

    public static QuakeMinigame getInstance() {
        return instance;
    }

    public void registerGame(Game game) {
        if (gamesLimit != -1 && games.size() == gamesLimit) return;
        games.add(game);
    }

    public static Game getGame(String gameName) {
        for (Game game : games) if (gameName.equalsIgnoreCase(game.getDisplayName())) return game;
        return null;
    }
    public static Game getGame(Player player) {
        for (Game game : games) if (game.getPlayers().contains(player)) return game;
        return null;
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) getServer().getPluginManager().registerEvents(listener, this);
    }

    public static Set<Game> getGames() {
        return games;
    }
}
