package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.LangUtil;
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
            LangUtil.send(player, "invalid-usage");
            return true;
        }

        String key = args[0];
        LangUtil.send(player, key);
        return true;
    }
}

