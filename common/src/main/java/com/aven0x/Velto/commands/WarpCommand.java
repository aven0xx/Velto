package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.managers.WarpManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class WarpCommand extends BaseCommand {

    public WarpCommand() {
        super("warp");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.warp");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.warp")) return true;

        Player player = (Player) sender;

        if (args.length == 0) {
            List<String> names = WarpManager.getWarpNames();
            if (names.isEmpty()) {
                LangUtil.send(player, "warp-no-warps");
            } else {
                LangUtil.send(player, "warp-list", Map.of("%warps%", String.join(", ", names)));
            }
            return true;
        }

        String name = args[0];
        Location warp = WarpManager.getWarp(name);
        if (warp == null) {
            LangUtil.send(player, "warp-not-found", Map.of("%warp%", name));
            return true;
        }

        TeleportManager.getInstance().teleport(player, warp, () -> LangUtil.send(player, "warp-teleported", Map.of("%warp%", name)));
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
