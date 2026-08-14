package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.WarpManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class DelWarpCommand extends BaseCommand {

    public DelWarpCommand() {
        super("delwarp");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.delwarp");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.delwarp")) return true;

        // Deleting by name needs no player context, so the console can run it too.
        if (args.length == 0) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "delwarp-usage");
            } else {
                sender.sendMessage("Usage: /delwarp <name>");
            }
            return true;
        }

        String name = args[0];
        boolean removed = WarpManager.deleteWarp(name);

        if (!removed) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "warp-not-found", Map.of("%warp%", name));
            } else {
                sender.sendMessage("Warp not found: " + name);
            }
            return true;
        }

        if (sender instanceof Player player) {
            LangUtil.send(player, "warp-deleted", Map.of("%warp%", name));
        } else {
            sender.sendMessage("Deleted warp: " + name);
        }
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            return WarpManager.getWarpNames().stream()
                    .filter(w -> w.toLowerCase().startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
