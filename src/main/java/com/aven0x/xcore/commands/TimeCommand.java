package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TimeCommand extends BaseCommand {
    public TimeCommand() { super("time"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.timeset")) return true;
        if (args.length < 2) {
            sendMessage(sender, "invalid-usage");
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
            sendMessage(sender, "invalid-time");
            return true;
        }
        World world = args.length > 2 ? Bukkit.getWorld(args[2]) : sender instanceof Player ? ((Player) sender).getWorld() : null;
        if (world == null) {
            sendMessage(sender, "invalid-world");
            return true;
        }
        world.setTime(time);
        sendMessage(sender, "time-set");
        return true;
    }
}
