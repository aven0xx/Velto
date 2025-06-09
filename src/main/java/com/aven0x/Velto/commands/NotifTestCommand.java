package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.NotificationUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class NotifTestCommand extends BaseCommand {

    public NotifTestCommand() {
        super("notiftest");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!hasPermission(sender, "velto.notiftest")) {
            return true;
        }

        if (args.length != 1) {
            NotificationUtil.send(player, "invalid-usage");
            return true;
        }

        String key = args[0];
        NotificationUtil.send(player, key);
        return true;
    }
}

