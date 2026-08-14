package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.IgnoreManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class IgnoreCommand extends BaseCommand {

    public IgnoreCommand() {
        super("ignore");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.ignore");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.ignore")) return true;

        Player player = (Player) sender;

        if (args.length < 1) {
            LangUtil.send(player, "ignore-usage");
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            LangUtil.send(player, "invalid-player");
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            LangUtil.send(player, "ignore-self");
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];

        if (!IgnoreManager.addIgnore(player.getUniqueId(), target.getUniqueId())) {
            LangUtil.send(player, "ignore-already", Map.of("%player%", targetName));
            return true;
        }

        LangUtil.send(player, "ignore-success", Map.of("%player%", targetName));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            return PlayerUtil.onlineSnapshot().stream()
                    .filter(player -> !PlayerUtil.isVanished(player))
                    .filter(player -> !(sender instanceof Player p && p.getUniqueId().equals(player.getUniqueId())))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
