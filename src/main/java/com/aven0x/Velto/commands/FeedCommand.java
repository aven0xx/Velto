package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.NotificationUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class FeedCommand extends BaseCommand {

    public FeedCommand() {
        super("feed");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player) sender : null);
        boolean self = args.length == 0;
        String perm = self ? "velto.feed" : "velto.feed.others";

        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "invalid-player");
            } else {
                sender.sendMessage("§cInvalid player.");
            }
            return true;
        }

        target.setFoodLevel(20);
        target.setSaturation(20f);

        if (self) {
            NotificationUtil.send(target, "fed-self");
        } else {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "fed-other", Map.of("%target%", target.getName()));
            } else {
                sender.sendMessage("§aFed: " + target.getName());
            }
            NotificationUtil.send(target, "fed-self");
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("velto.feed.others")) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(player -> !PlayerUtil.isVanished(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
