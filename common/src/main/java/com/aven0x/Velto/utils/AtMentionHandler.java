package com.aven0x.Velto.utils;

import com.aven0x.Velto.managers.MsgManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class AtMentionHandler {

    private AtMentionHandler() {}

    /**
     * Attempts to handle a chat message that starts with @player.
     * Returns true if the message was consumed (chat should be cancelled), false otherwise.
     */
    public static boolean handle(Player sender, String message) {
        if (!message.startsWith("@")) return false;

        int space = message.indexOf(' ');
        if (space == -1) return false;

        String targetName = message.substring(1, space);
        String body = message.substring(space + 1).trim();
        if (body.isEmpty()) return false;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            LangUtil.send(sender, "invalid-player");
            return true;
        }

        if (target.equals(sender)) {
            LangUtil.send(sender, "msg-self");
            return true;
        }

        Map<String, String> toPlaceholders = new HashMap<>();
        toPlaceholders.put("%sender%", sender.getName());
        toPlaceholders.put("%message%", body);

        Map<String, String> fromPlaceholders = new HashMap<>();
        fromPlaceholders.put("%recipient%", target.getName());
        fromPlaceholders.put("%message%", body);

        LangUtil.send(target, "msg-received", toPlaceholders);
        LangUtil.send(sender, "msg-sent", fromPlaceholders);

        MsgManager.setLastMessenger(target.getUniqueId(), sender.getUniqueId());

        return true;
    }
}
