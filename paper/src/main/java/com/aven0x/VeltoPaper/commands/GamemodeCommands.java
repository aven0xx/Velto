package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GamemodeCommands {

    // /gamemode <mode> [player]
    public static class GamemodeCommand extends BaseCommand {
        public GamemodeCommand() {
            super("gamemode");
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
            if (args.length < 1) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-usage-gamemode");
                } else {
                    sender.sendMessage("§cUsage: /gamemode <mode> [player]");
                }
                return true;
            }

            GameMode mode = switch (args[0].toLowerCase()) {
                case "0", "s", "survival" -> GameMode.SURVIVAL;
                case "1", "c", "creative" -> GameMode.CREATIVE;
                case "2", "a", "adventure" -> GameMode.ADVENTURE;
                case "3", "sp", "spectator" -> GameMode.SPECTATOR;
                default -> null;
            };

            if (mode == null) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-gamemode");
                } else {
                    sender.sendMessage("§cInvalid gamemode.");
                }
                return true;
            }

            Player target = args.length >= 2
                    ? Bukkit.getPlayer(args[1])
                    : (sender instanceof Player ? (Player) sender : null);
            boolean self = args.length < 2;

            String perm = self
                    ? "velto.gamemode." + mode.name().toLowerCase()
                    : "velto.gamemode." + mode.name().toLowerCase() + ".others";
            if (!hasPermission(sender, perm)) return true;

            if (target == null || !target.isOnline()) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-player");
                } else {
                    sender.sendMessage("§cInvalid player.");
                }
                return true;
            }

            target.setGameMode(mode);

            Map<String, String> placeholders = Map.of("%mode%", mode.name());
            LangUtil.send(target, "gamemode-set-self", placeholders);

            if (!self) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "gamemode-set-other", Map.of(
                            "%target%", target.getName(),
                            "%mode%", mode.name()
                    ));
                } else {
                    sender.sendMessage("§aSet " + target.getName() + "'s gamemode to " + mode.name());
                }
            }

            return true;
        }
    }

    // /gmc
    public static class GmcCommand extends GamemodeShortcutCommand {
        public GmcCommand() {
            super("gmc", GameMode.CREATIVE);
        }
    }

    // /gms
    public static class GmsCommand extends GamemodeShortcutCommand {
        public GmsCommand() {
            super("gms", GameMode.SURVIVAL);
        }
    }

    // /gma
    public static class GmaCommand extends GamemodeShortcutCommand {
        public GmaCommand() {
            super("gma", GameMode.ADVENTURE);
        }
    }

    // /gmsp
    public static class GmspCommand extends GamemodeShortcutCommand {
        public GmspCommand() {
            super("gmsp", GameMode.SPECTATOR);
        }
    }

    // Shared shortcut logic
    private static abstract class GamemodeShortcutCommand extends BaseCommand {
        private final GameMode mode;

        public GamemodeShortcutCommand(String name, GameMode mode) {
            super(name);
            this.mode = mode;
        }

        @Override
        public boolean onCommand(@NotNull CommandSender sender, Command command, String label, String[] args) {
            Player target = args.length > 0
                    ? Bukkit.getPlayer(args[0])
                    : (sender instanceof Player ? (Player) sender : null);
            boolean self = args.length == 0;

            String perm = self
                    ? "xcore.gamemode." + mode.name().toLowerCase()
                    : "xcore.gamemode." + mode.name().toLowerCase() + ".others";
            if (!hasPermission(sender, perm)) return true;

            if (target == null || !target.isOnline()) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-player");
                } else {
                    sender.sendMessage("§cInvalid player.");
                }
                return true;
            }

            target.setGameMode(mode);

            Map<String, String> placeholders = Map.of("%mode%", mode.name());
            LangUtil.send(target, "gamemode-set-self", placeholders);

            if (!self) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "gamemode-set-other", Map.of(
                            "%target%", target.getName(),
                            "%mode%", mode.name()
                    ));
                } else {
                    sender.sendMessage("§aSet " + target.getName() + "'s gamemode to " + mode.name());
                }
            }

            return true;
        }
    }
}
