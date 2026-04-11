package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.AfkManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AfkCommand extends BaseCommand {

    public AfkCommand() {
        super("afk");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            if (!hasPermission(sender, "velto.afk.list")) return true;

            var afkPlayers = AfkManager.getAfkPlayers();
            if (afkPlayers.isEmpty()) {
                LangUtil.send((Player) sender, "afk-list-empty");
            } else {
                String playerNames = afkPlayers.stream()
                        .map(Player::getName)
                        .collect(Collectors.joining(", "));

                Map<String, String> placeholders = Map.of(
                        "%count%", String.valueOf(afkPlayers.size()),
                        "%players%", playerNames
                );
                LangUtil.send((Player) sender, "afk-list", placeholders);
            }
            return true;
        }

        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            AfkManager.toggleAfk(player);
            return true;

        } else if (args.length == 1) {
            if (!hasPermission(sender, "velto.afk.others")) return true;

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                LangUtil.send(player, "invalid-player");
                return true;
            }

            boolean newState = AfkManager.toggleAfk(target);

            Map<String, String> placeholders = Map.of(
                    "%target%", target.getName(),
                    "%status%", newState ? "AFK" : "active"
            );
            LangUtil.send(player, "afk-set-other", placeholders);

            return true;
        }

        LangUtil.send(player, "afk-usage");
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String arg = args[0].toLowerCase();

            if (sender.hasPermission("velto.afk.list") && "list".startsWith(arg)) {
                completions.add("list");
            }

            if (sender.hasPermission("velto.afk.others")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(arg))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}
