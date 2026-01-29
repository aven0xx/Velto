package com.aven0x.VeltoBukkit.commands;

import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedCommand extends BaseCommand {
    public SpeedCommand() {
        super("speed");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-usage");
            }
            return true;
        }

        int speedLevel;
        try {
            speedLevel = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-speed");
            }
            return true;
        }

        if (speedLevel < 0 || speedLevel > 10) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-speed");
            }
            return true;
        }

        Player target = args.length > 1
                ? Bukkit.getPlayer(args[1])
                : (sender instanceof Player ? (Player) sender : null);

        if (target == null || !target.isOnline()) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-player");
            }
            return true;
        }

        boolean self = (target == sender);
        String perm = self ? "velto.speed" : "velto.speed.others";

        if (!hasPermission(sender, perm)) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            }
            return true;
        }

        // Logique de vitesse corrigée pour correspondre à Vanilla au niveau 1
        float finalWalkSpeed;
        float finalFlySpeed;

        if (speedLevel <= 1) {
            // Niveau 0 ou 1 = Vitesse Vanilla exacte
            finalWalkSpeed = 0.2f;
            finalFlySpeed = 0.1f;
        } else {
            // Progression : Niveau 2 = 2x plus vite, etc.
            finalWalkSpeed = 0.2f * speedLevel;
            finalFlySpeed = 0.1f * speedLevel;
        }

        // On applique aux deux pour éviter les déséquilibres marche/vol
        target.setWalkSpeed(Math.min(finalWalkSpeed, 1.0f));
        target.setFlySpeed(Math.min(finalFlySpeed, 1.0f));

        // Message de confirmation
        if (sender instanceof Player player) {
            String messageKey = (speedLevel <= 1) ? "speed-reset" : "speed-updated";
            LangUtil.send(player, messageKey);
        }

        return true;
    }
}