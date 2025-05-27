package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WeatherCommand extends BaseCommand {
    public WeatherCommand() {
        super("weather");
        new SunCommand();
        new RainCommand();
        new ThunderCommand();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.weather")) return true;
        if (args.length < 1) {
            sendMessage(sender, "invalid-usage");
            return true;
        }
        String mode = args[0].toLowerCase();
        World world = sender instanceof Player ? ((Player) sender).getWorld() : Bukkit.getWorlds().get(0);

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
                sendMessage(sender, "invalid-usage");
                return true;
            }
        }
        sendMessage(sender, "weather-updated");
        return true;
    }
}

class SunCommand extends BaseCommand {
    public SunCommand() { super("sun"); }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather clear");
    }
}

class RainCommand extends BaseCommand {
    public RainCommand() { super("rain"); }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather rain");
    }
}

class ThunderCommand extends BaseCommand {
    public ThunderCommand() { super("thunder"); }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather thunder");
    }
}
