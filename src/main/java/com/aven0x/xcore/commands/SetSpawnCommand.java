package com.aven0x.xcore.commands;

import com.aven0x.xcore.Xcore;
import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSpawnCommand extends BaseCommand {
    public SetSpawnCommand() {
        super("setspawn");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.setspawn")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true; // tu peux aussi ajouter NotificationUtil pour only-player ici si tu veux
        }

        Xcore.getInstance().getConfig().set("spawn", player.getLocation());
        Xcore.getInstance().saveConfig();

        NotificationUtil.send(player, "spawn-set");
        return true;
    }
}