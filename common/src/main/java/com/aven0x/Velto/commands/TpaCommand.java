package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TpaManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class TpaCommand extends BaseCommand {

    public TpaCommand() {
        super("tpa");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.tpa")) return true;

        Player player = (Player) sender;

        if (args.length != 1) {
            LangUtil.send(player, "tpa-usage");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            LangUtil.send(player, "invalid-player");
            return true;
        }

        if (target.equals(player)) {
            LangUtil.send(player, "tpa-self");
            return true;
        }

        TpaManager.sendRequest(player, target);
        LangUtil.send(player, "tpa-sent", Map.of("%target%", target.getName()));
        LangUtil.send(target, "tpa-received", Map.of("%requester%", player.getName()));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender instanceof Player) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(typed))
                    .toList();
        }
        return List.of();
    }
}
