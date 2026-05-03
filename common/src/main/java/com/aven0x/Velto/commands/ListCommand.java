package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ListCommand extends BaseCommand {
    public ListCommand() {
        super("list");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!hasPermission(player, "velto.list")) {
            LangUtil.send(player, "no-permission");
            return true;
        }

        String players = Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.joining(", "));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%players%", players);

        LangUtil.send(player, "list-command", placeholders);
        return true;
    }
}
