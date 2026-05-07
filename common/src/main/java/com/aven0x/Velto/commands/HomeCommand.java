package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.HomeManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HomeCommand extends BaseCommand {

    public HomeCommand() {
        super("home");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.home")) return true;

        Player player = (Player) sender;
        String name = args.length > 0 ? args[0] : HomeManager.DEFAULT_HOME;

        Location home = HomeManager.getHome(player.getUniqueId(), name);
        if (home == null) {
            LangUtil.send(player, "home-not-found", Map.of("%home%", name));
            return true;
        }

        TeleportManager.getInstance().teleportAsync(player, home);
        LangUtil.send(player, "home-teleported", Map.of("%home%", name));
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
