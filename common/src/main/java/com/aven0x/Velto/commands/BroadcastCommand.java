package com.aven0x.Velto.commands;

import com.aven0x.Velto.Velto;
import com.aven0x.Velto.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand extends BaseCommand {
    public BroadcastCommand() {
        super("broadcast");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.broadcast")) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "no-permission");
            }
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "broadcast-usage");
            }
            return true;
        }

        // Join args and broadcast using NotificationUtil
        String rawMessage = String.join(" ", args);

        // Send as chat-type global message
        NotificationUtil.sendGlobalRaw(rawMessage, "chat", 80);

        // Log to console with &-formatted color codes
        Bukkit.getLogger().info(rawMessage);

        return true;
    }
}
