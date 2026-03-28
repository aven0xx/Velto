package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.managers.MsgManager;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MsgCommand extends BaseCommand {

    public MsgCommand() {
        super("msg");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.msg")) return true;

        if (args.length < 2) {
            LangUtil.send((Player) sender, "msg-usage");
            return true;
        }

        Player from = (Player) sender;
        Player to = Bukkit.getPlayer(args[0]);

        if (to == null || !to.isOnline()) {
            LangUtil.send(from, "invalid-player");
            return true;
        }

        if (to.equals(from)) {
            LangUtil.send(from, "msg-self");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        Map<String, String> toPlaceholders = new HashMap<>();
        toPlaceholders.put("%sender%", from.getName());
        toPlaceholders.put("%message%", message);

        Map<String, String> fromPlaceholders = new HashMap<>();
        fromPlaceholders.put("%recipient%", to.getName());
        fromPlaceholders.put("%message%", message);

        LangUtil.send(to, "msg-received", toPlaceholders);
        LangUtil.send(from, "msg-sent", fromPlaceholders);

        // Track last messenger for /reply
        MsgManager.setLastMessenger(to.getUniqueId(), from.getUniqueId());

        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(sender) && p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    names.add(p.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
