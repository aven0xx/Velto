package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NightCommand extends BaseCommand {
    public NightCommand() {
        super("night");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.timeset")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        World world = args.length > 0
                ? sender.getServer().getWorld(args[0])
                : sender instanceof Player ? ((Player) sender).getWorld() : null;

        if (world == null) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-world");
            }
            return true;
        }

        world.setTime(13000);

        if (sender instanceof Player player) {
            NotificationUtil.send(player, "time-set-night");
        }

        return true;
    }
}