package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.IgnoreManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnignoreCommand extends BaseCommand {

    public UnignoreCommand() {
        super("unignore");
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
            LangUtil.send(player, "unignore-usage");
            return true;
        }

        OfflinePlayer target = resolveTarget(args[0]);
        if (target == null) {
            LangUtil.send(player, "invalid-player");
            return true;
        }

        String targetName = target.getName() != null ? target.getName() : args[0];

        if (!IgnoreManager.removeIgnore(player.getUniqueId(), target.getUniqueId())) {
            LangUtil.send(player, "unignore-not", Map.of("%player%", targetName));
            return true;
        }

        LangUtil.send(player, "unignore-success", Map.of("%player%", targetName));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender instanceof Player player) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<UUID> ignored = IgnoreManager.getIgnored(player.getUniqueId());
            return ignored.stream()
                    .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                    .filter(name -> name != null && name.toLowerCase().startsWith(typed))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
