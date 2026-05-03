package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.MsgManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReplyCommand extends BaseCommand {

    public ReplyCommand() {
        super("reply");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.msg")) return true;

        if (args.length < 1) {
            LangUtil.send((Player) sender, "reply-usage");
            return true;
        }

        Player from = (Player) sender;
        UUID targetId = MsgManager.getLastMessenger(from.getUniqueId());

        if (targetId == null) {
            LangUtil.send(from, "reply-no-target");
            return true;
        }

        Player to = Bukkit.getPlayer(targetId);
        if (to == null || !to.isOnline()) {
            LangUtil.send(from, "reply-offline");
            return true;
        }

        String message = String.join(" ", args);

        Map<String, String> toPlaceholders = new HashMap<>();
        toPlaceholders.put("%sender%", from.getName());
        toPlaceholders.put("%message%", message);

        Map<String, String> fromPlaceholders = new HashMap<>();
        fromPlaceholders.put("%recipient%", to.getName());
        fromPlaceholders.put("%message%", message);

        LangUtil.send(to, "msg-received", toPlaceholders);
        LangUtil.send(from, "msg-sent", fromPlaceholders);

        MsgManager.setLastMessenger(to.getUniqueId(), from.getUniqueId());

        return true;
    }
}
