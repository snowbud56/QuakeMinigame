package com.snowbud56.events;

import com.snowbud56.QuakeMinigame;
import com.snowbud56.constructors.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerDamage implements Listener {
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Game game = QuakeMinigame.getGame((Player) e.getEntity());
            if (game == null) return;
            if (game.isState(Game.GameState.ACTIVE)){
                EntityDamageEvent.DamageCause cause = e.getCause();
                if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK) || cause.equals(EntityDamageEvent.DamageCause.FALL) || cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
                    e.setCancelled(true);
                }
            } else if (game.isState(Game.GameState.STARTING) || game.isState(Game.GameState.LOBBY)) e.setCancelled(true);
        }
    }
}
