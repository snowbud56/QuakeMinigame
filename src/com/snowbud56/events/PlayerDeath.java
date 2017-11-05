package com.snowbud56.events;

import com.snowbud56.QuakeMinigame;
import com.snowbud56.constructors.Game;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PlayerDeath implements Listener {

    @EventHandler
    public void onDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            Game game = QuakeMinigame.getGame(p);
            if (game != null) {
                if (p.getHealth() - e.getDamage() <= 0) {
                    e.setCancelled(true);
                    game.sendMessage("&a[!] " + p.getName() + " died!");
                    List<Location> spawns = game.getSpawnPoints();
                    Integer value = 0;
                    for (Location loc : spawns) {
                        Collection<Entity> entities = loc.getWorld().getNearbyEntities(loc, 10, 10, 10);
                        if (entities.size() > 0) {
                            if (value == spawns.size()){
                                handle(p, spawns.get(0));
                                return;
                            } else value++;
                        } else {
                            handle(p, loc);
                            return;
                        }
                    }
                }
            }
        }
    }

    private void handle(Player p, Location loc) {
        p.setMaxHealth(20);
        p.setFoodLevel(20);
        p.setHealth(p.getMaxHealth());
        p.teleport(loc);
        p.setHealth(p.getMaxHealth());
    }
}
