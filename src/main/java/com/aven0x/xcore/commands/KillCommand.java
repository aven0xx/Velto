package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import com.aven0x.xcore.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class KillCommand extends BaseCommand {

    public KillCommand() {
        super("kill");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.kill")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            } else {
                sender.sendMessage("§cYou don't have permission.");
            }
            return true;
        }

        Player target = args.length > 0
                ? Bukkit.getPlayer(args[0])
                : sender instanceof Player ? (Player) sender : null;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "invalid-player");
            } else {
                sender.sendMessage("§cInvalid player.");
            }
            return true;
        }

        target.setHealth(0);

        if (sender instanceof Player playerSender) {
            NotificationUtil.send(playerSender, "player-killed");
        } else {
            sender.sendMessage("§aKilled: " + target.getName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("xcore.kill")) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !PlayerUtil.isVanished(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}