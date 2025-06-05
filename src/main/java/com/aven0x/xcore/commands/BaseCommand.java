package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.CommandUtil;
import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

    public BaseCommand(String name) {
        PluginCommand cmd = Bukkit.getPluginCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setAliases(CommandUtil.getAliases(name)); // ✅ Set aliases from commands.yml
        }
    }

    @Override
    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "only-player");
            }
            return false;
        }
        return true;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return false;
        }
        return true;
    }
}
