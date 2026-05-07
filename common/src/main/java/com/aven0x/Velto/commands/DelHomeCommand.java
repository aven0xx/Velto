package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.HomeManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class DelHomeCommand extends BaseCommand {

    public DelHomeCommand() {
        super("delhome");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.delhome")) return true;

        Player player = (Player) sender;
        String name = args.length > 0 ? args[0] : HomeManager.DEFAULT_HOME;

        if (!HomeManager.hasHome(player.getUniqueId(), name)) {
            LangUtil.send(player, "home-not-found", Map.of("%home%", name));
            return true;
        }

        HomeManager.deleteHome(player.getUniqueId(), name);
        LangUtil.send(player, "home-deleted", Map.of("%home%", name));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            String typed = args[0].toLowerCase();
            return HomeManager.getHomeNames(player.getUniqueId()).stream()
                    .filter(h -> h.toLowerCase().startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
