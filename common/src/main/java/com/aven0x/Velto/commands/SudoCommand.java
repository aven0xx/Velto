package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.sudo");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
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

        String rawCmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        final String command = rawCmd.startsWith("/") ? rawCmd.substring(1) : rawCmd;

        // A command executed as a player must run on the region owning that player.
        PlayerUtil.onOwningRegion(target, () -> Bukkit.dispatchCommand(target, command));

        if (sender instanceof Player player) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%target%", target.getName());
            placeholders.put("%command%", command);
            LangUtil.send(player, "sudo-success", placeholders);
        }

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender.hasPermission("velto.sudo")) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : PlayerUtil.onlineSnapshot()) {
                if (p.getName().toLowerCase().startsWith(typed)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
