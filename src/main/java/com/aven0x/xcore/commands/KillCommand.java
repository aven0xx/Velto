package com.aven0x.xcore.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KillCommand extends BaseCommand {
    public KillCommand() {
        super("kill");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.kill")) return true;

        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : sender instanceof Player ? (Player) sender : null;

        if (target == null || !target.isOnline()) {
            sendMessage(sender, "invalid-player");
            return true;
        }

        target.setHealth(0);
        sendMessage(sender, "player-killed");
        return true;
    }
}

