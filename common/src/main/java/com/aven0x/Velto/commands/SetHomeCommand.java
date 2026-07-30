package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.HomeManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public class SetHomeCommand extends BaseCommand {

    public SetHomeCommand() {
        super("sethome");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.sethome");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.sethome")) return true;

        Player player = (Player) sender;
        String name = args.length > 0 ? args[0] : HomeManager.DEFAULT_HOME;

        // Enforce the per-player home cap, but only when creating a new home —
        // overwriting an existing one doesn't increase the count.
        if (!HomeManager.hasHome(player.getUniqueId(), name)) {
            int max = HomeManager.getMaxHomes(player);
            int used = HomeManager.getHomeNames(player.getUniqueId()).size();
            if (used >= max) {
                LangUtil.send(player, "home-limit-reached", Map.of("%max%", String.valueOf(max)));
                return true;
            }
        }

        HomeManager.setHome(player.getUniqueId(), name, player.getLocation());
        LangUtil.send(player, "home-set", Map.of("%home%", name));
        return true;
    }
}
