package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GamemodeCommand extends BaseCommand {
    public GamemodeCommand() {
        super("gamemode");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "gamemode-usage");
            return true;
        }

        GameMode gm;
        try {
            gm = switch (args[0].toLowerCase()) {
                case "0", "survival" -> GameMode.SURVIVAL;
                case "1", "creative" -> GameMode.CREATIVE;
                case "2", "adventure" -> GameMode.ADVENTURE;
                case "3", "spectator" -> GameMode.SPECTATOR;
                default -> null;
            };
        } catch (Exception e) {
            gm = null;
        }

        if (gm == null) {
            sendMessage(sender, "invalid-gamemode");
            return true;
        }

        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 1;
        String perm = "xcore.gamemode" + (self ? "" : ".others");

        if (!hasPermission(sender, perm)) return true;
        if (target == null || !target.isOnline()) {
            sendMessage(sender, "invalid-player");
            return true;
        }

        target.setGameMode(gm);
        sendMessage(sender, "gamemode-set");
        return true;
    }
}