package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
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
            return true;
        }

        // Usage: /time <day|night|ticks> [world]
        if (args.length < 1) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            } else {
                sender.sendMessage("§cUsage: /time <day|night|ticks> <world>");
            }
            return true;
        }

        String timeArg = args[0].toLowerCase();
        long time;

        try {
            time = switch (timeArg) {
                case "day" -> 1000L;
                case "night" -> 13000L;
                default -> Long.parseLong(timeArg);
            };
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-time");
            } else {
                sender.sendMessage("§cInvalid time specified.");
            }
            return true;
        }

        World world = null;

        // If world was provided
        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
        } else if (sender instanceof Player player) {
            // Player default: current world
            world = player.getWorld();
        }

        // Console must specify a world
        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else {
                sender.sendMessage("§cWorld not found. Usage: /time <day|night|ticks> <world>");
            }
            return true;
        }

        world.setTime(time);

        if (sender instanceof Player player) {
            LangUtil.send(player, "time-set");
        } else {
            sender.sendMessage("§aTime set in world §f" + world.getName() + " §ato §f" + time + "§a.");
        }

        return true;
    }
}
