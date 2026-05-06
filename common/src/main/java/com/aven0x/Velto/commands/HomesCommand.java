package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.HomeManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HomesCommand extends BaseCommand {

    public HomesCommand() {
        super("homes");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.homes")) return true;

        Player player = (Player) sender;
        List<String> names = HomeManager.getHomeNames(player.getUniqueId());

        if (names.isEmpty()) {
            LangUtil.send(player, "home-no-homes");
            return true;
        }

        LangUtil.send(player, "home-list", Map.of("%homes%", String.join(", ", names)));
        return true;
    }
}
