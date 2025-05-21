package com.aven0x.xcore.commands;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DayCommand extends BaseCommand {
    public DayCommand() { super("day"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.timeset")) return true;
        World world = args.length > 0 ? sender.getServer().getWorld(args[0]) : sender instanceof Player ? ((Player) sender).getWorld() : null;
        if (world == null) {
            sendMessage(sender, "invalid-world");
            return true;
        }
        world.setTime(1000);
        sendMessage(sender, "time-set-day");
        return true;
    }
}
