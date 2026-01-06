package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WeatherCommand extends BaseCommand {
    public WeatherCommand() {
        super("weather");
        new SunCommand();
        new RainCommand();
        new ThunderCommand();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        // hasPermission() already sends "no-permission"
        if (!hasPermission(sender, "velto.weather")) {
            return true;
        }

        // Usage: /weather <clear|sun|rain|thunder> [world]
        if (args.length < 1) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            } else {
                sender.sendMessage("§cUsage: /weather <clear|sun|rain|thunder> <world>");
            }
            return true;
        }

        String mode = args[0].toLowerCase();

        World world = null;
        // If world provided: /weather <mode> <world>
        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
        } else if (sender instanceof Player player) {
            // Player default: current world
            world = player.getWorld();
        }

        // Console must specify a world for /weather
        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else {
                sender.sendMessage("§cWorld not found. Usage: /weather <clear|sun|rain|thunder> <world>");
            }
            return true;
        }

        switch (mode) {
            case "clear", "sun" -> {
                world.setStorm(false);
                world.setThundering(false);
            }
            case "rain" -> {
                world.setStorm(true);
                world.setThundering(false);
            }
            case "thunder" -> {
                world.setStorm(true);
                world.setThundering(true);
            }
            default -> {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-usage");
                } else {
                    sender.sendMessage("§cUsage: /weather <clear|sun|rain|thunder> <world>");
                }
                return true;
            }
        }

        if (sender instanceof Player player) {
            LangUtil.send(player, "weather-updated");
        } else {
            sender.sendMessage("§aWeather updated in world §f" + world.getName() + "§a.");
        }

        return true;
    }

    // ===== Wrapper helper =====
    static boolean dispatchWeather(CommandSender sender, String mode, String[] args) {
        // Player: keep old behavior (/sun affects current world)
        if (sender instanceof Player) {
            return Bukkit.dispatchCommand(sender, "weather " + mode);
        }

        // Console: allow /sun [world] and preserve old behavior (default first world)
        String worldName;
        if (args.length >= 1) {
            worldName = args[0];
        } else {
            if (Bukkit.getWorlds().isEmpty()) {
                sender.sendMessage("§cNo worlds are loaded.");
                return true;
            }
            worldName = Bukkit.getWorlds().get(0).getName();
        }

        return Bukkit.dispatchCommand(sender, "weather " + mode + " " + worldName);
    }
}

class SunCommand extends BaseCommand {
    public SunCommand() {
        super("sun");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return WeatherCommand.dispatchWeather(sender, "sun", args);
    }
}

class RainCommand extends BaseCommand {
    public RainCommand() {
        super("rain");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return WeatherCommand.dispatchWeather(sender, "rain", args);
    }
}

class ThunderCommand extends BaseCommand {
    public ThunderCommand() {
        super("thunder");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return WeatherCommand.dispatchWeather(sender, "thunder", args);
    }
}
