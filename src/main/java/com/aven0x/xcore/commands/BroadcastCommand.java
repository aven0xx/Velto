package com.aven0x.xcore.commands;

import com.aven0x.xcore.utils.NotificationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BroadcastCommand extends BaseCommand {
    public BroadcastCommand() {
        super("broadcast");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "xcore.broadcast")) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "no-permission");
            }
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player playerSender) {
                NotificationUtil.send(playerSender, "broadcast-usage");
            }
            return true;
        }

        String rawMessage = String.join(" ", args).replace('&', '§');
        Component messageComponent = LegacyComponentSerializer.legacySection().deserialize(rawMessage);

        // Broadcast to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(messageComponent);
        }

        // Log it for the console
        Bukkit.getLogger().info("[Broadcast] " + rawMessage.replace('§', '&'));

        return true;
    }
}
