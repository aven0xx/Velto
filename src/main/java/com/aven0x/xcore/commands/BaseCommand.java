package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class BaseCommand implements CommandExecutor {

    public BaseCommand(String name) {
        if (Bukkit.getPluginCommand(name) != null) {
            Bukkit.getPluginCommand(name).setExecutor(this);
        }
    }

    @Override
    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

    protected void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(MessageUtil.get(key));
    }

    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return false;
        }
        return true;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            sendMessage(sender, "no-permission");
            return false;
        }
        return true;
    }
}