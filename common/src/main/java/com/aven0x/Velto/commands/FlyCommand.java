package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlyCommand extends BaseCommand {

    public FlyCommand() {
        super("fly");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.fly") || checkPermission(sender, "velto.fly.others");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
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

        // Read the current flight state and flip it on the region that owns the target; the
        // resulting messages depend on that state, and packet sends are safe from there.
        PlayerUtil.onOwningRegion(target, () -> {
            boolean flying = !target.getAllowFlight();
            target.setAllowFlight(flying);
            if (!flying) target.setFlying(false);

            LangUtil.send(target, flying ? "fly-enabled" : "fly-disabled");
            if (!self && sender instanceof Player playerSender) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("%target%", target.getName());
                LangUtil.send(playerSender, flying ? "fly-enabled-other" : "fly-disabled-other", placeholders);
            }
        });

        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender.hasPermission("velto.fly.others")) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(typed)) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
