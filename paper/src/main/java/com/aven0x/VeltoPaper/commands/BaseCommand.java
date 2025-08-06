package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.CommandUtil;
import com.aven0x.VeltoPaper.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public abstract boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args);

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList(); // Default: no suggestions
    }

    // Remove if unused; otherwise keep for future use
    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }
        return true;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            } else {
                sender.sendMessage("§cYou do not have permission.");
            }
            return false;
        }
        return true;
    }
}
