package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TpCommand extends BaseCommand {

    public TpCommand() {
        super("tp");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        // /tp <player>
        if (args.length == 1) {
            if (!hasPermission(sender, "velto.tp")) return true;
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { LangUtil.send(player, "invalid-player"); return true; }
            TeleportManager.getInstance().teleport(player, target.getLocation(),
                    () -> LangUtil.send(player, "tp-self-player", Map.of("%target%", target.getName())));
            return true;
        }

        // /tp <x> <y> <z>
        if (args.length == 3 && isCoord(args[0])) {
            if (!hasPermission(sender, "velto.tp")) return true;
            Location dest = resolveCoords(player.getLocation(), args[0], args[1], args[2]);
            if (dest == null) { LangUtil.send(player, "tp-usage"); return true; }
            TeleportManager.getInstance().teleport(player, dest,
                    () -> LangUtil.send(player, "tp-self-coords", Map.of(
                            "%x%", String.valueOf(dest.getBlockX()),
                            "%y%", String.valueOf(dest.getBlockY()),
                            "%z%", String.valueOf(dest.getBlockZ()))));
            return true;
        }

        // /tp <player> <player>
        if (args.length == 2 && !isCoord(args[0])) {
            if (!hasPermission(sender, "velto.tp.others")) return true;
            Player from = Bukkit.getPlayerExact(args[0]);
            Player to   = Bukkit.getPlayerExact(args[1]);
            if (from == null || to == null) { LangUtil.send(player, "invalid-player"); return true; }
            TeleportManager.getInstance().teleportAsync(from, to.getLocation());
            LangUtil.send(player, "tp-other-success",
                    Map.of("%from%", from.getName(), "%to%", to.getName()));
            return true;
        }

        // /tp <player> <x> <y> <z>
        if (args.length == 4 && !isCoord(args[0])) {
            if (!hasPermission(sender, "velto.tp.others")) return true;
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { LangUtil.send(player, "invalid-player"); return true; }
            Location dest = resolveCoords(target.getLocation(), args[1], args[2], args[3]);
            if (dest == null) { LangUtil.send(player, "tp-usage"); return true; }
            TeleportManager.getInstance().teleportAsync(target, dest);
            LangUtil.send(player, "tp-other-success",
                    Map.of("%from%", target.getName(),
                           "%to%", dest.getBlockX() + ", " + dest.getBlockY() + ", " + dest.getBlockZ()));
            return true;
        }

        LangUtil.send(player, "tp-usage");
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) return List.of();

        return switch (args.length) {
            case 1 -> {
                // player names + "~" to hint coordinates are accepted
                List<String> result = onlineNames(args[0]);
                if ("~".startsWith(args[0])) result.add("~");
                yield result;
            }
            case 2 -> {
                if (isCoord(args[0])) {
                    // /tp <x> <y?>  →  suggest current Y and ~
                    yield coordHints(player.getLocation().getBlockY(), args[1]);
                } else {
                    // /tp <player> <player|x?>  →  suggest players and ~
                    List<String> result = onlineNames(args[1]);
                    if ("~".startsWith(args[1])) result.add("~");
                    yield result;
                }
            }
            case 3 -> {
                if (isCoord(args[0]) && isCoord(args[1])) {
                    // /tp <x> <y> <z?>  →  suggest current Z and ~
                    yield coordHints(player.getLocation().getBlockZ(), args[2]);
                } else if (!isCoord(args[0]) && isCoord(args[1])) {
                    // /tp <player> <x> <y?>  →  suggest current Y and ~
                    yield coordHints(player.getLocation().getBlockY(), args[2]);
                }
                yield List.of();
            }
            case 4 -> {
                if (!isCoord(args[0]) && isCoord(args[1]) && isCoord(args[2])) {
                    // /tp <player> <x> <y> <z?>  →  suggest current Z and ~
                    yield coordHints(player.getLocation().getBlockZ(), args[3]);
                }
                yield List.of();
            }
            default -> List.of();
        };
    }

    // Returns online player names that start with the typed prefix (case-insensitive).
    private List<String> onlineNames(String typed) {
        String lower = typed.toLowerCase();
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase().startsWith(lower)) result.add(p.getName());
        }
        return result;
    }

    // Returns the block coordinate as a string and "~" if they match the typed prefix.
    private List<String> coordHints(int current, String typed) {
        List<String> result = new ArrayList<>();
        String cur = String.valueOf(current);
        if (cur.startsWith(typed)) result.add(cur);
        if ("~".startsWith(typed)) result.add("~");
        return result;
    }

    // True if the argument is a relative (~, ~N) or absolute coordinate.
    private boolean isCoord(String arg) {
        if (arg.startsWith("~")) return true;
        try { Double.parseDouble(arg); return true; } catch (NumberFormatException e) { return false; }
    }

    // Resolves three coordinate strings against a base location. Returns null on bad input.
    private Location resolveCoords(Location base, String xs, String ys, String zs) {
        try {
            double x = parseCoord(base.getX(), xs);
            double y = parseCoord(base.getY(), ys);
            double z = parseCoord(base.getZ(), zs);
            return new Location(base.getWorld(), x, y, z, base.getYaw(), base.getPitch());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double parseCoord(double current, String arg) {
        if (arg.startsWith("~")) {
            String rest = arg.substring(1);
            return rest.isEmpty() ? current : current + Double.parseDouble(rest);
        }
        return Double.parseDouble(arg);
    }
}
