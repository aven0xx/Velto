package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.GodManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GodCommand extends BaseCommand {
    public GodCommand() {
        super("god");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.god") || checkPermission(sender, "velto.god.others");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        Player target = args.length > 0 ? Bukkit.getPlayer(args[0]) : sender instanceof Player ? (Player) sender : null;
        boolean self = args.length == 0;

        String perm = self ? "velto.god" : "velto.god.others";
        if (!hasPermission(sender, perm)) return true;

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player playerSender) {
                LangUtil.send(playerSender, "invalid-player");
            }
            return true;
        }

        boolean enabled = GodManager.toggleGod(target);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%target%", target.getName());

        if (self) {
            LangUtil.send(target, enabled ? "god-enabled" : "god-disabled");
        } else {
            LangUtil.send(target, enabled ? "god-enabled" : "god-disabled");
            if (sender instanceof Player playerSender) {
                LangUtil.send(playerSender, enabled ? "god-enabled-other" : "god-disabled-other", placeholders);
            }
        }

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender.hasPermission("velto.god.others")) {
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
