package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;

public class KillAllCommand extends BaseCommand {
    public KillAllCommand() {
        super("killall");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.killall");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.killall")) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /" + label + " <entityType|ALL> [world]");
            return true;
        }

        final String typeArg = args[0].toUpperCase(Locale.ROOT);
        final boolean killAll = typeArg.equals("ALL") || typeArg.equals("*");

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
        String scope;
        if (ServerUtil.isFolia()) {
            // Folia has no consistent world-wide entity list, so clear the area this region owns
            // around the player (~8 chunks). getNearbyEntities is safe here: /killall runs on the
            // region that owns the player, which owns roughly that radius around them. The [world]
            // argument can't be honoured across regions, so it's ignored on Folia.
            final int radius = 8 * 16;
            for (Entity e : player.getNearbyEntities(radius, 512, radius)) {
                if (e instanceof Player) continue;
                if (killAll || e.getType() == targetType) {
                    e.remove();
                    removed++;
                }
            }
            scope = "near you";
        } else {
            // Spigot / non-Folia Paper: a single main thread, so a whole-world scan is safe.
            for (Entity e : targetWorld.getEntities()) {
                if (e.getType() == EntityType.PLAYER) continue;
                if (killAll || e.getType() == targetType) {
                    e.remove();
                    removed++;
                }
            }
            scope = "in world " + targetWorld.getName();
        }

        LangUtil.send(player, "killall-done");
        player.sendMessage("§aRemoved §f" + removed + " §aentities of type §f" +
                (killAll ? "ALL" : targetType) + " §a" + scope + "§a.");

        return true;
    }
}
