package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SudoCommand extends BaseCommand {

    public SudoCommand() {
        super("sudo");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.sudo")) return true;

        if (args.length < 2) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "sudo-usage");
            } else {
                sender.sendMessage("Usage: /sudo <player> <command...>");
            }
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-player");
            } else {
                sender.sendMessage("Player not found.");
            }
            return true;
        }

        String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);

        Bukkit.dispatchCommand(target, cmd);

        if (sender instanceof Player player) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%target%", target.getName());
            placeholders.put("%command%", cmd);
            LangUtil.send(player, "sudo-success", placeholders);
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("velto.sudo")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
