package com.snowbud56.events;

import com.snowbud56.QuakeMinigame;
import com.snowbud56.constructors.Game;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockInteract implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Game game = QuakeMinigame.getGame(p);
        if (game != null) {
            if (p.getGameMode().equals(GameMode.SURVIVAL)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Game game = QuakeMinigame.getGame(p);
        if (game != null) {
            if (p.getGameMode().equals(GameMode.SURVIVAL)) e.setCancelled(true);
        }
    }
}
