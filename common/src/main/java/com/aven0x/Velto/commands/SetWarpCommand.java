package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.WarpManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SetWarpCommand extends BaseCommand {

    public SetWarpCommand() {
        super("setwarp");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.setwarp");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.setwarp")) return true;

        Player player = (Player) sender;

        if (args.length == 0) {
            LangUtil.send(player, "setwarp-usage");
            return true;
        }

        String name = args[0];
        WarpManager.setWarp(name, player.getLocation());
        LangUtil.send(player, "warp-set", Map.of("%warp%", name));
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
