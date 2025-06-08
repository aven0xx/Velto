package com.aven0x.Velto.commands;

import com.aven0x.Velto.Velto;
import com.aven0x.Velto.utils.NotificationUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BroadcastCommand extends BaseCommand {
    public BroadcastCommand() {
        super("broadcast");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "velto.broadcast")) {
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

        // Send Adventure message to all players using BukkitAudiences
        for (Player player : Bukkit.getOnlinePlayers()) {
            Velto.getInstance().adventure().player(player).sendMessage(messageComponent);
        }

        // Log plain text to console (with & codes)
        Bukkit.getLogger().info(rawMessage.replace('§', '&'));

        return true;
    }
}
