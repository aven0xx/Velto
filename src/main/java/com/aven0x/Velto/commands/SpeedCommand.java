package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.NotificationUtil;
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
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-usage");
            }
            return true;
        }

        int speedLevel;
        try {
            speedLevel = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-speed");
            }
            return true;
        }

        if (speedLevel < 1 || speedLevel > 10) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-speed");
            }
            return true;
        }

        Player target = args.length > 1
                ? Bukkit.getPlayer(args[1])
                : sender instanceof Player ? (Player) sender : null;

        boolean self = args.length == 1;
        String perm = self ? "velto.speed" : "velto.speed.others";

        if (!hasPermission(sender, perm)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            }
            return true;
        }

        float speed = speedLevel / 10.0f;
        if (target.isFlying()) {
            target.setFlySpeed(speed);
        } else {
            target.setWalkSpeed(speed);
        }

        if (sender instanceof Player player) {
            NotificationUtil.send(player, "speed-updated");
        }

        return true;
    }
}
