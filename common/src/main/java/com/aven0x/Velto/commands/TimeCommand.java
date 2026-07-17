package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeCommand extends BaseCommand {
    public TimeCommand() {
        super("time");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.timeset");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.timeset")) {
            return true;
        }

        // Accept both Velto's "/time <value> [world]" and vanilla's
        // "/time set <value> [world]" by stripping an optional leading "set".
        String[] a = (args.length >= 1 && args[0].equalsIgnoreCase("set"))
                ? Arrays.copyOfRange(args, 1, args.length)
                : args;

        if (a.length < 1) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            } else {
                sender.sendMessage("§cUsage: /time [set] <day|night|ticks> [world]");
            }
            return true;
        }

        String timeArg = a[0].toLowerCase();
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

        if (a.length >= 2) {
            world = Bukkit.getWorld(a[1]);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        }

        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else {
                sender.sendMessage("§cWorld not found. Usage: /time [set] <day|night|ticks> [world]");
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

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        boolean hasSet = args.length >= 1 && args[0].equalsIgnoreCase("set");

        // 1st arg: the "set" keyword plus the time presets.
        if (args.length == 1) {
            return filter(List.of("set", "day", "night"), args[0]);
        }

        // 2nd arg: presets after "set", otherwise the world for "/time <value> <world>".
        if (args.length == 2) {
            return hasSet ? filter(List.of("day", "night"), args[1]) : filter(worldNames(), args[1]);
        }

        // 3rd arg: only the world, reached via "/time set <value> <world>".
        if (args.length == 3 && hasSet) {
            return filter(worldNames(), args[2]);
        }

        return List.of();
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            names.add(w.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) out.add(option);
        }
        return out;
    }
}
