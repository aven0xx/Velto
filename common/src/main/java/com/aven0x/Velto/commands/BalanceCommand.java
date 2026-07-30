package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BalanceCommand extends BaseCommand {

    public BalanceCommand() {
        super("balance");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return EconomyManager.isEnabled()
                && (checkPermission(sender, "velto.balance") || checkPermission(sender, "velto.balance.others"));
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!EconomyManager.isEnabled()) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-disabled");
            else sender.sendMessage("The economy module is currently disabled.");
            return true;
        }

        if (args.length == 0) {
            if (!isPlayer(sender)) return true;
            if (!hasPermission(sender, "velto.balance")) return true;

            Player player = (Player) sender;
            String formatted = EconomyManager.format(EconomyManager.getBalance(player.getUniqueId()));
            LangUtil.send(player, "balance-self", Map.of("%balance%", formatted));
            return true;
        }

        if (!hasPermission(sender, "velto.balance.others")) return true;

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            if (sender instanceof Player player) LangUtil.send(player, "invalid-player");
            else sender.sendMessage("Player not found.");
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];
        String formatted = EconomyManager.format(EconomyManager.getBalance(target.getUniqueId()));
        if (sender instanceof Player player) {
            LangUtil.send(player, "balance-other", Map.of("%player%", targetName, "%balance%", formatted));
        } else {
            sender.sendMessage(targetName + "'s balance: " + formatted);
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender.hasPermission("velto.balance.others")) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(typed)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
