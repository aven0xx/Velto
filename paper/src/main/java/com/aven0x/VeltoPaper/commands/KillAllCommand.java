package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class KillAllCommand extends BaseCommand {
    public KillAllCommand() {
        super("killall");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        // Permission check (same pattern as AnvilCommand)
        if (!hasPermission(sender, "velto.killall")) {
            if (sender instanceof Player player) {
                NotificationUtil.send(player, "no-permission");
            }
            return true;
        }

        // Player-only (same as AnvilCommand)
        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /" + label + " <entityType|ALL> [world]");
            return true;
        }

        final String typeArg = args[0].toUpperCase(Locale.ROOT);
        final boolean killAll = typeArg.equals("ALL") || typeArg.equals("*");

        // World defaults to player's world
        final World targetWorld;
        if (args.length >= 2) {
            targetWorld = Bukkit.getWorld(args[1]);
            if (targetWorld == null) {
                player.sendMessage("§cWorld not found: §f" + args[1]);
                return true;
            }
        } else {
            targetWorld = player.getWorld();
        }

        // Resolve entity type (unless ALL)
        EntityType targetType = null;
        if (!killAll) {
            try {
                targetType = EntityType.valueOf(typeArg);
            } catch (IllegalArgumentException ex) {
                player.sendMessage("§cInvalid entity type: §f" + args[0]);
                return true;
            }
            if (targetType == EntityType.PLAYER) {
                player.sendMessage("§cRefusing to target players.");
                return true;
            }
        }

        int removed = 0;
        for (Entity e : targetWorld.getEntities()) {
            if (e.getType() == EntityType.PLAYER) continue; // never touch players
            if (killAll || e.getType() == targetType) {
                e.remove();
                removed++;
            }
        }

        // Keep your NotificationUtil flow
        NotificationUtil.send(player, "killall-done");
        player.sendMessage("§aRemoved §f" + removed + " §aentities of type §f" +
                (killAll ? "ALL" : targetType) + " §ain world §f" + targetWorld.getName() + "§a.");

        return true;
    }
}
