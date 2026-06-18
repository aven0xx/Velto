package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.KitManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitResetCommand extends BaseCommand {

    public KitResetCommand() {
        super("kitreset");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.kit.reset")) return true;

        if (args.length < 2) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kitreset-usage");
            } else {
                sender.sendMessage("Usage: /kitreset <player> <kit>");
            }
            return true;
        }

        KitManager.Kit kit = KitManager.getKit(args[1]);
        if (kit == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-not-found", Map.of("%kit%", args[1]));
            } else {
                sender.sendMessage("Kit not found: " + args[1]);
            }
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-player");
            } else {
                sender.sendMessage("Player not found.");
            }
            return true;
        }

        KitManager.resetCooldown(target.getUniqueId(), kit.name());

        if (sender instanceof Player player) {
            LangUtil.send(player, "kit-reset-success", Map.of("%kit%", kit.name(), "%player%", target.getName()));
        } else {
            sender.sendMessage("Reset kit '" + kit.name() + "' for " + target.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            String typed = args[0].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(typed)) names.add(p.getName());
            }
            return names;
        }
        if (args.length == 2) {
            String typed = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (KitManager.Kit kit : KitManager.getKits()) {
                if (kit.name().toLowerCase().startsWith(typed)) names.add(kit.name());
            }
            return names;
        }
        return List.of();
    }
}
