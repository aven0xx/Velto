package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BroadcastCommand extends BaseCommand {
    public BroadcastCommand() { super("broadcast"); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.broadcast")) return true;
        if (args.length == 0) {
            sendMessage(sender, "broadcast-usage");
            return true;
        }
        String message = String.join(" ", args);
        String formatted = MessageUtil.color(message);
        Bukkit.broadcastMessage(formatted);
        Bukkit.getLogger().info("[Broadcast] " + message);
        return true;
    }
}
