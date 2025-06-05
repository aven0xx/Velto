package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.CommandUtil;
import com.aven0x.xcore.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    public BaseCommand(String name) {
        PluginCommand cmd = Bukkit.getPluginCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setAliases(CommandUtil.getAliases(name));
            cmd.setTabCompleter(this); // ✅ Tab completer set centrally
        }
    }

    @Override
    public abstract boolean onCommand(CommandSender sender, Command command, String label, String[] args);

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList(); // Default: no suggestions
    }

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
