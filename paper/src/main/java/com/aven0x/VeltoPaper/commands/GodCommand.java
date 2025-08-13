package com.aven0x.VeltoPaper.commands;

import com.aven0x.Velto.manager.GodManager;
import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class GodCommand extends BaseCommand {
    public GodCommand() {
        super("god");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
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
}
