package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PayCommand extends BaseCommand {

    public PayCommand() {
        super("pay");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return EconomyManager.isEnabled() && checkPermission(sender, "velto.pay");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        if (!EconomyManager.isEnabled()) {
            LangUtil.send(player, "economy-disabled");
            return true;
        }
        if (!hasPermission(sender, "velto.pay")) return true;

        if (args.length < 2) {
            LangUtil.send(player, "pay-usage");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            LangUtil.send(player, "invalid-player");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            LangUtil.send(player, "pay-self");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            LangUtil.send(player, "economy-invalid-amount");
            return true;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            LangUtil.send(player, "economy-invalid-amount");
            return true;
        }

        if (!EconomyManager.transfer(player.getUniqueId(), target.getUniqueId(), amount)) {
            LangUtil.send(player, "pay-insufficient-funds");
            return true;
        }

        String formatted = EconomyManager.format(amount);
        LangUtil.send(player, "pay-sent", Map.of("%player%", target.getName(), "%amount%", formatted));
        LangUtil.send(target, "pay-received", Map.of("%player%", player.getName(), "%amount%", formatted));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : PlayerUtil.onlineSnapshot()) {
                if (p.getName().toLowerCase().startsWith(typed) && !(sender instanceof Player s && p.equals(s))) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
