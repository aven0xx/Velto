package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EconomyCommand extends BaseCommand {

    private static final List<String> SUBCOMMANDS = List.of("give", "take", "set", "reset");

    public EconomyCommand() {
        super("economy");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return EconomyManager.isEnabled() && (
                checkPermission(sender, "velto.economy.give")
                        || checkPermission(sender, "velto.economy.take")
                        || checkPermission(sender, "velto.economy.set")
                        || checkPermission(sender, "velto.economy.reset"));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!EconomyManager.isEnabled()) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-disabled");
            else sender.sendMessage("The economy module is currently disabled.");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "";
        if (args.length < 2 || !SUBCOMMANDS.contains(sub)) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-usage");
            else sender.sendMessage("Usage: /economy <give|take|set|reset> <player> [amount]");
            return true;
        }

        if (!hasPermission(sender, "velto.economy." + sub)) return true;

        OfflinePlayer target = resolveTarget(args[1]);
        if (target == null) {
            if (sender instanceof Player player) LangUtil.send(player, "invalid-player");
            else sender.sendMessage("Player not found.");
            return true;
        }

        // Display name (may be null for a cached offline player) and the online handle, if any —
        // target-facing messages only go out when the target is actually online to see them.
        String targetName = target.getName() != null ? target.getName() : args[1];
        Player targetOnline = target.getPlayer();

        if (sub.equals("reset")) {
            EconomyManager.resetBalance(target.getUniqueId());
            String formatted = EconomyManager.format(EconomyManager.getStartingBalance());
            if (sender instanceof Player player) {
                LangUtil.send(player, "economy-reset", Map.of("%player%", targetName, "%amount%", formatted));
            } else {
                sender.sendMessage("Reset " + targetName + "'s balance to " + formatted + ".");
            }
            if (targetOnline != null) LangUtil.send(targetOnline, "economy-reset-target", Map.of("%amount%", formatted));
            return true;
        }

        if (args.length < 3) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-usage");
            else sender.sendMessage("Usage: /economy <give|take|set|reset> <player> [amount]");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-invalid-amount");
            else sender.sendMessage("Invalid amount.");
            return true;
        }

        if (!Double.isFinite(amount) || amount < 0) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-invalid-amount");
            else sender.sendMessage("Invalid amount.");
            return true;
        }

        String formatted = EconomyManager.format(amount);
        Map<String, String> senderPlaceholders = Map.of("%player%", targetName, "%amount%", formatted);
        Map<String, String> targetPlaceholders = Map.of("%amount%", formatted);

        switch (sub) {
            case "give" -> {
                EconomyManager.add(target.getUniqueId(), amount);
                if (sender instanceof Player player) LangUtil.send(player, "economy-give", senderPlaceholders);
                else sender.sendMessage("Gave " + formatted + " to " + targetName + ".");
                if (targetOnline != null) LangUtil.send(targetOnline, "economy-give-target", targetPlaceholders);
            }
            case "take" -> {
                EconomyManager.subtract(target.getUniqueId(), amount);
                if (sender instanceof Player player) LangUtil.send(player, "economy-take", senderPlaceholders);
                else sender.sendMessage("Took " + formatted + " from " + targetName + ".");
                if (targetOnline != null) LangUtil.send(targetOnline, "economy-take-target", targetPlaceholders);
            }
            case "set" -> {
                EconomyManager.setBalance(target.getUniqueId(), amount);
                if (sender instanceof Player player) LangUtil.send(player, "economy-set", senderPlaceholders);
                else sender.sendMessage("Set " + targetName + "'s balance to " + formatted + ".");
                if (targetOnline != null) LangUtil.send(targetOnline, "economy-set-target", targetPlaceholders);
            }
        }

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> subs = new ArrayList<>();
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(typed) && sender.hasPermission("velto.economy." + s)) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2) {
            String typed = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : PlayerUtil.onlineSnapshot()) {
                if (p.getName().toLowerCase().startsWith(typed)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
