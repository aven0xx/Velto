package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TimeCommand extends BaseCommand {
    public TimeCommand() {
        super("time");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.timeset")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (args.length < 2) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-usage");
            }
            return true;
        }

        String timeArg = args[1].toLowerCase();
        long time;

        try {
            time = switch (timeArg) {
                case "day" -> 1000L;
                case "night" -> 13000L;
                default -> Long.parseLong(timeArg);
            };
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-time");
            }
            return true;
        }

        World world = args.length > 2
                ? Bukkit.getWorld(args[2])
                : sender instanceof Player ? ((Player) sender).getWorld() : null;

        if (world == null) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-world");
            }
            return true;
        }

        world.setTime(time);

        if (sender instanceof Player player) {
            NotificationUtil.send(player, "time-set");
        }

        return true;
    }
}