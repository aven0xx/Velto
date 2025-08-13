package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
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
        if (!hasPermission(sender, "velto.weather")) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            }
            return true;
        }

        if (args.length < 1) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            }
            return true;
        }

        String mode = args[0].toLowerCase();
        World world = (sender instanceof Player player)
                ? player.getWorld()
                : Bukkit.getWorlds().get(0);

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
                }
                return true;
            }
        }

        if (sender instanceof Player player) {
            LangUtil.send(player, "weather-updated");
        }

        return true;
    }
}

class SunCommand extends BaseCommand {
    public SunCommand() {
        super("sun");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather sun");
    }
}

class RainCommand extends BaseCommand {
    public RainCommand() {
        super("rain");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather rain");
    }
}

class ThunderCommand extends BaseCommand {
    public ThunderCommand() {
        super("thunder");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        return Bukkit.dispatchCommand(sender, "weather thunder");
    }
}
