package com.snowbud56;

import com.snowbud56.constructors.Game;
import com.snowbud56.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QuakeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(ChatUtils.format("&c[!] Invalid usage! Please provide a valid subcommand."));
            sender.sendMessage(ChatUtils.format("&c[!] Subcommands: join, quit" + (p.isOp() ? ", start, stop" : "")));
            return false;
        }
        String arg1 = args[0].toLowerCase();
        if (args.length >= 2) {
            Game game = QuakeMinigame.getGame(args[1]);
            if (game == null) {
                p.sendMessage(ChatUtils.format("&c[!] Something went wrong! (That game doesn't exist!)"));
                return false;
            }
            switch (arg1) {
                case ("join"):
                    game.joinGame(p);
                    break;
                case ("info"):
                    p.sendMessage(ChatUtils.format("&aGame Information for " + game.getDisplayName()));
                    p.sendMessage(ChatUtils.format("&aMin players: " + game.getMinPlayers()));
                    p.sendMessage(ChatUtils.format("&aMax players: " + game.getMaxPlayers()));
                    p.sendMessage(ChatUtils.format("&aState: " + game.getGameState().toString()));
                    p.sendMessage(ChatUtils.format("&aPlayers: "));
                    for (Player player : game.getPlayers()) p.sendMessage(ChatUtils.format("&a- " + player.getName()));
                    p.sendMessage(ChatUtils.format("&aSpectators: "));
                    for (Player player : game.getSpectators()) p.sendMessage(ChatUtils.format("&a- " + player.getName()));
                    p.sendMessage(ChatUtils.format("&aWorld: " + game.getWorld().getName().split("_")[0]));
                    break;
                case ("start"):
                    if (game.isState(Game.GameState.LOBBY)) {
                        game.sendMessage("&a[!] " + p.getName() + " has started the game.");
                        game.startCountDown(true);
                    } else {
                        p.sendMessage(ChatUtils.format("&c[!] Unable to start the game (The game has already been started)"));
                    }
                    break;
                case ("stop"):
                    if (game.isState(Game.GameState.STARTING)) {
                        game.setState(Game.GameState.LOBBY);
                        game.sendMessage("&a[!] " + p.getName() + " has stopped the game.");
                    } else if (game.isState(Game.GameState.ACTIVE)) game.endGame(null);
                    else p.sendMessage(ChatUtils.format("&c[!] Unable to stop the game (The game hasn't been started yet)"));
                    break;
                case ("explosive"):
                    if (game.isExplosiveMode()) {
                        game.sendMessage("&c[!] " + p.getName() + " has disabled explosive mode!");
                        p.sendMessage(ChatUtils.format("&c[✔] Successfully disabled explosive mode"));
                        game.setExplosiveMode(false);
                    } else {
                        game.sendMessage("&a[!] " + p.getName() + " has enabled explosive mode!");
                        p.sendMessage(ChatUtils.format("&a[✔] Successfully enabled explosive mode"));
                        game.setExplosiveMode(true);
                    }
                    break;
            }
        } else if (arg1.equals("quit")) {
            Game game = QuakeMinigame.getGame(p);
            if (game != null) game.quitGame(p);
            else p.sendMessage(ChatUtils.format("&c[!] Something went wrong! (You're not in a game!)"));
        } else {
            p.sendMessage(ChatUtils.format("&c[!] Please provide a game name!"));
        }
        return false;
    }
}
