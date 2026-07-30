package com.aven0x.Velto.commands;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WeatherCommand extends BaseCommand {
    public WeatherCommand() {
        super("weather");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.weather");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.weather")) {
            return true;
        }

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
        if (args.length >= 2) {
            world = Bukkit.getWorld(args[1]);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        }

        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else {
                sender.sendMessage("§cWorld not found. Usage: /weather <clear|sun|rain|thunder> <world>");
            }
            return true;
        }

        if (!isValidMode(mode)) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            } else {
                sender.sendMessage("§cUsage: /weather <clear|sun|rain|thunder> <world>");
            }
            return true;
        }

        // Weather is global-region state on Folia; only the mutation hops, the message stays here.
        final World targetWorld = world;
        Schedulers.get().global(() -> applyWeather(targetWorld, mode));

        if (sender instanceof Player player) {
            LangUtil.send(player, "weather-updated");
        } else {
            sender.sendMessage("§aWeather updated in world §f" + world.getName() + "§a.");
        }

        return true;
    }

    // Backs /sun /rain /thunder. Applies the weather directly instead of re-dispatching the
    // vanilla /weather command — that re-entered command handling (which must run on the global
    // region on Folia) and depended on the sender holding minecraft:weather; velto.weather now
    // suffices, and feedback is Velto's own message rather than the doubled vanilla one.
    static boolean dispatchWeather(CommandSender sender, String mode, String[] args) {
        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else if (args.length >= 1) {
            world = Bukkit.getWorld(args[0]);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        } else {
            sender.sendMessage("§cNo worlds are loaded.");
            return true;
        }

        if (world == null) {
            sender.sendMessage("§cWorld not found.");
            return true;
        }

        final World targetWorld = world;
        Schedulers.get().global(() -> applyWeather(targetWorld, mode));

        if (sender instanceof Player player) {
            LangUtil.send(player, "weather-updated");
        } else {
            sender.sendMessage("§aWeather updated in world §f" + targetWorld.getName() + "§a.");
        }
        return true;
    }

    private static boolean isValidMode(String mode) {
        return switch (mode) {
            case "clear", "sun", "rain", "thunder" -> true;
            default -> false;
        };
    }

    private static void applyWeather(World world, String mode) {
        switch (mode) {
            case "clear", "sun" -> { world.setStorm(false); world.setThundering(false); }
            case "rain"         -> { world.setStorm(true);  world.setThundering(false); }
            case "thunder"      -> { world.setStorm(true);  world.setThundering(true);  }
        }
    }

    public static class SunCommand extends BaseCommand {
        public SunCommand() {
            super("sun");
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return checkPermission(sender, "velto.weather");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return WeatherCommand.dispatchWeather(sender, "sun", args);
        }
    }

    public static class RainCommand extends BaseCommand {
        public RainCommand() {
            super("rain");
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return checkPermission(sender, "velto.weather");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return WeatherCommand.dispatchWeather(sender, "rain", args);
        }
    }

    public static class ThunderCommand extends BaseCommand {
        public ThunderCommand() {
            super("thunder");
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return checkPermission(sender, "velto.weather");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return WeatherCommand.dispatchWeather(sender, "thunder", args);
        }
    }
}
