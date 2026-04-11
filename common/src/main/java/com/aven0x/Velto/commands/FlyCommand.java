package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlyCommand extends BaseCommand {

    public FlyCommand() {
        super("fly");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 0;

        String perm = self ? "velto.fly" : "velto.fly.others";
        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-player");
            }
            return true;
        }

        boolean flying = !target.getAllowFlight();
        target.setAllowFlight(flying);
        if (!flying) target.setFlying(false);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());

        if (self) {
            LangUtil.send(target, flying ? "fly-enabled" : "fly-disabled");
        } else {
            LangUtil.send(target, flying ? "fly-enabled" : "fly-disabled");
            if (sender instanceof Player playerSender) {
                LangUtil.send(playerSender, flying ? "fly-enabled-other" : "fly-disabled-other", placeholders);
            }
        }

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("velto.fly.others")) {
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
