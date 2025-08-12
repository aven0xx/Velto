package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ListCommand extends BaseCommand {
    public ListCommand() {
        super("list");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        // Permission check
        if (!hasPermission(player, "velto.list")) {
            NotificationUtil.send(player, "no-permission");
            return true;
        }

        String players = Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%players%", players);

        NotificationUtil.send(player, "list-command", placeholders);
        return true;
    }
}
