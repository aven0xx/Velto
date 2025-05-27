package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpeedCommand extends BaseCommand {
    public SpeedCommand() {
        super("speed");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendMessage(sender, "invalid-usage");
            return true;
        }

        int speedLevel;
        try {
            speedLevel = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "invalid-speed");
            return true;
        }

        if (speedLevel < 1 || speedLevel > 10) {
            sendMessage(sender, "invalid-speed");
            return true;
        }

        Player target = args.length > 1 ? Bukkit.getPlayer(args[1]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 1;

        String perm = self ? "xcore.speed" : "xcore.speed.others";
        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            sendMessage(sender, "invalid-player");
            return true;
        }

        float speed = speedLevel / 10.0f;
        if (target.isFlying()) {
            target.setFlySpeed(speed);
        } else {
            target.setWalkSpeed(speed);
        }

        sendMessage(sender, "speed-updated");
        return true;
    }
}

