package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.managers.AfkManager;
import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AfkCommand extends BaseCommand {

    public AfkCommand() {
        super("afk");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        // /afk list - Affiche les joueurs AFK (permission requise)
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

        // /afk [joueur] - Toggle AFK pour soi ou un autre joueur
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        if (args.length == 0) {
            // Toggle AFK pour soi-même
            boolean newState = AfkManager.toggleAfk(player);
            return true;

        } else if (args.length == 1) {
            // Toggle AFK pour un autre joueur (permission requise)
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

        // Usage invalide
        LangUtil.send(player, "afk-usage");
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String arg = args[0].toLowerCase();

            // Ajouter "list" si on a la permission
            if (sender.hasPermission("velto.afk.list") && "list".startsWith(arg)) {
                completions.add("list");
            }

            // Ajouter les noms de joueurs si on a la permission
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