package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GamemodeCommands {

    /**
     * Retourne la clé LangUtil correspondant au mode de jeu.
     */
    private static String getGamemodeLangKey(GameMode mode) {
        return switch (mode) {
            case CREATIVE -> "gamemode-creative";
            case SURVIVAL -> "gamemode-survival";
            case ADVENTURE -> "gamemode-adventure";
            case SPECTATOR -> "gamemode-spectator";
        };
    }

    // /gamemode <mode> [player]
    public static class GamemodeCommand extends BaseCommand {

        public GamemodeCommand() {
            super("gamemode");
        }

        @Override
        public boolean canUse(CommandSender sender) {
            for (GameMode mode : GameMode.values()) {
                String basePermission =
                        "velto.gamemode." + mode.name().toLowerCase();

                if (checkPermission(sender, basePermission)
                        || checkPermission(sender, basePermission + ".others")) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean execute(
                CommandSender sender,
                String label,
                String[] args
        ) {
            if (args.length < 1) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-usage-gamemode");
                } else {
                    sender.sendMessage(
                            "§cUsage: /gamemode <mode> [player]"
                    );
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

            boolean self = args.length < 2;

            Player target = self
                    ? sender instanceof Player player ? player : null
                    : Bukkit.getPlayer(args[1]);

            String permission = self
                    ? "velto.gamemode."
                        + mode.name().toLowerCase()
                    : "velto.gamemode."
                        + mode.name().toLowerCase()
                        + ".others";

            if (!hasPermission(sender, permission)) {
                return true;
            }

            if (target == null || !target.isOnline()) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-player");
                } else {
                    sender.sendMessage("§cInvalid player.");
                }

                return true;
            }

            // Game mode is per-entity state; set it on the region that owns the target and send
            // that player their message from there (packet sends are region-safe).
            PlayerUtil.onOwningRegion(target, () -> {
                target.setGameMode(mode);
                LangUtil.send(target, getGamemodeLangKey(mode));
            });

            /*
             * Envoie une confirmation à la personne ayant modifié
             * le mode d'un autre joueur.
             */
            if (!self) {
                String otherMessageKey =
                        getGamemodeLangKey(mode) + "-other";

                if (sender instanceof Player player) {
                    LangUtil.send(
                            player,
                            otherMessageKey,
                            Map.of("%target%", target.getName())
                    );
                } else {
                    sender.sendMessage(
                            "§aSet "
                                    + target.getName()
                                    + "'s gamemode to "
                                    + mode.name().toLowerCase()
                    );
                }
            }

            return true;
        }

        @Override
        public List<String> complete(
                CommandSender sender,
                String label,
                String[] args
        ) {
            if (args.length <= 1) {
                String typed = args.length == 0
                        ? ""
                        : args[0].toLowerCase();

                return List.of(
                                "survival",
                                "creative",
                                "adventure",
                                "spectator"
                        )
                        .stream()
                        .filter(mode -> mode.startsWith(typed))
                        .toList();
            }

            if (args.length == 2) {
                String typed = args[1].toLowerCase();
                List<String> names = new ArrayList<>();

                for (Player player : PlayerUtil.onlineSnapshot()) {
                    if (player.getName()
                            .toLowerCase()
                            .startsWith(typed)) {
                        names.add(player.getName());
                    }
                }

                return names;
            }

            return List.of();
        }
    }

    // /gmc
    public static class GmcCommand
            extends GamemodeShortcutCommand {

        public GmcCommand() {
            super("gmc", GameMode.CREATIVE);
        }
    }

    // /gms
    public static class GmsCommand
            extends GamemodeShortcutCommand {

        public GmsCommand() {
            super("gms", GameMode.SURVIVAL);
        }
    }

    // /gma
    public static class GmaCommand
            extends GamemodeShortcutCommand {

        public GmaCommand() {
            super("gma", GameMode.ADVENTURE);
        }
    }

    // /gmsp
    public static class GmspCommand
            extends GamemodeShortcutCommand {

        public GmspCommand() {
            super("gmsp", GameMode.SPECTATOR);
        }
    }

    /**
     * Logique partagée par /gmc, /gms, /gma et /gmsp.
     */
    private static abstract class GamemodeShortcutCommand
            extends BaseCommand {

        private final GameMode mode;

        public GamemodeShortcutCommand(
                String name,
                GameMode mode
        ) {
            super(name);
            this.mode = mode;
        }

        @Override
        public boolean canUse(CommandSender sender) {
            String basePermission =
                    "velto.gamemode."
                            + mode.name().toLowerCase();

            return checkPermission(sender, basePermission)
                    || checkPermission(
                            sender,
                            basePermission + ".others"
                    );
        }

        @Override
        public boolean execute(
                CommandSender sender,
                String label,
                String[] args
        ) {
            boolean self = args.length == 0;

            Player target = self
                    ? sender instanceof Player player ? player : null
                    : Bukkit.getPlayer(args[0]);

            String permission = self
                    ? "velto.gamemode."
                        + mode.name().toLowerCase()
                    : "velto.gamemode."
                        + mode.name().toLowerCase()
                        + ".others";

            if (!hasPermission(sender, permission)) {
                return true;
            }

            if (target == null || !target.isOnline()) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "invalid-player");
                } else {
                    sender.sendMessage("§cInvalid player.");
                }

                return true;
            }

            // Game mode is per-entity state; set it on the region that owns the target and send
            // that player their message from there (packet sends are region-safe).
            PlayerUtil.onOwningRegion(target, () -> {
                target.setGameMode(mode);
                LangUtil.send(target, getGamemodeLangKey(mode));
            });

            /*
             * Envoie une confirmation lorsque le mode d'un autre
             * joueur est modifié.
             */
            if (!self) {
                String otherMessageKey =
                        getGamemodeLangKey(mode) + "-other";

                if (sender instanceof Player player) {
                    LangUtil.send(
                            player,
                            otherMessageKey,
                            Map.of("%target%", target.getName())
                    );
                } else {
                    sender.sendMessage(
                            "§aSet "
                                    + target.getName()
                                    + "'s gamemode to "
                                    + mode.name().toLowerCase()
                    );
                }
            }

            return true;
        }

        @Override
        public List<String> complete(
                CommandSender sender,
                String label,
                String[] args
        ) {
            if (args.length <= 1) {
                String typed = args.length == 0
                        ? ""
                        : args[0].toLowerCase();

                List<String> names = new ArrayList<>();

                for (Player player : PlayerUtil.onlineSnapshot()) {
                    if (player.getName()
                            .toLowerCase()
                            .startsWith(typed)) {
                        names.add(player.getName());
                    }
                }

                return names;
            }

            return List.of();
        }
    }
}
