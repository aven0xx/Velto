package com.aven0x.Velto.commands;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TimeCommand extends BaseCommand {

    private static final List<String> PRESETS = List.of("day", "night", "noon", "midnight");

    public TimeCommand() {
        super("time");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.timeset");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.timeset")) {
            return true;
        }

        // Accept both Velto's "/time <value> [world]" and vanilla's
        // "/time set <value> [world]" by stripping an optional leading "set".
        String[] a = (args.length >= 1 && args[0].equalsIgnoreCase("set"))
                ? Arrays.copyOfRange(args, 1, args.length)
                : args;

        // Merge a separate am/pm token onto the number: "/time set 6 pm" -> "6pm".
        if (a.length >= 2 && a[0].matches("\\d+")
                && (a[1].equalsIgnoreCase("am") || a[1].equalsIgnoreCase("pm"))) {
            String[] rebuilt = new String[a.length - 1];
            rebuilt[0] = a[0] + a[1];
            System.arraycopy(a, 2, rebuilt, 1, a.length - 2);
            a = rebuilt;
        }

        if (a.length < 1) {
            sendUsage(sender);
            return true;
        }

        Long time = parseTicks(a[0]);
        if (time == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-time");
            } else {
                sender.sendMessage("§cInvalid time specified.");
            }
            return true;
        }

        World world = null;
        if (a.length >= 2) {
            world = Bukkit.getWorld(a[1]);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        }

        if (world == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "invalid-world");
            } else {
                sendUsage(sender);
            }
            return true;
        }

        final World targetWorld = world;
        // World time belongs to the global region on Folia; the message stays on this region.
        Schedulers.get().global(() -> targetWorld.setTime(time));

        if (sender instanceof Player player) {
            LangUtil.send(player, "time-set");
        } else {
            sender.sendMessage("§aTime set in world §f" + world.getName() + " §ato §f" + time + "§a.");
        }

        return true;
    }

    // Resolves a time token to a tick value, or null if it can't be parsed.
    // Accepts: presets (day/night/noon/midnight); a 24-hour hour as "<0-24>h"
    // (e.g. 8h, 18h); a 12-hour hour with am/pm (e.g. 6am, 12pm); or a raw tick count.
    private static Long parseTicks(String token) {
        String t = token.toLowerCase();

        switch (t) {
            case "day":      return 1000L;
            case "noon":     return 6000L;
            case "night":    return 13000L;
            case "midnight": return 18000L;
            default:         break;
        }

        // 24-hour clock: "<0-24>h" (e.g. 8h, 18h; 0h and 24h both mean midnight)
        if (t.endsWith("h")) {
            Integer h = parseIntOrNull(t.substring(0, t.length() - 1));
            if (h == null || h < 0 || h > 24) return null;
            return hourToTicks(h % 24);
        }

        // 12-hour clock: "<1-12>am" or "<1-12>pm"
        if (t.endsWith("am") || t.endsWith("pm")) {
            boolean pm = t.endsWith("pm");
            Integer h = parseIntOrNull(t.substring(0, t.length() - 2));
            if (h == null || h < 1 || h > 12) return null;
            int hour24 = pm ? (h == 12 ? 12 : h + 12) : (h == 12 ? 0 : h);
            return hourToTicks(hour24);
        }

        // Raw tick count (bare numbers are always ticks now — no hour ambiguity).
        Long n = parseLongOrNull(t);
        if (n == null || n < 0) return null;
        return n;
    }

    // Minecraft tick 0 is 06:00; each real hour is 1000 ticks.
    private static long hourToTicks(int hour24) {
        return (((hour24 - 6) * 1000) + 24000) % 24000;
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLongOrNull(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void sendUsage(CommandSender sender) {
        if (sender instanceof Player player) {
            LangUtil.send(player, "invalid-usage");
        } else {
            sender.sendMessage("§cUsage: /time [set] <day|night|noon|midnight|0-24h|1-12am/pm|ticks> [world]");
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        boolean hasSet = args.length >= 1 && args[0].equalsIgnoreCase("set");

        // 1st arg: the "set" keyword plus the time presets.
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("set");
            options.addAll(PRESETS);
            return filter(options, args[0]);
        }

        // 2nd arg: presets after "set", otherwise the world for "/time <value> <world>".
        if (args.length == 2) {
            return hasSet ? filter(PRESETS, args[1]) : filter(worldNames(), args[1]);
        }

        // 3rd arg: only the world, reached via "/time set <value> <world>".
        if (args.length == 3 && hasSet) {
            return filter(worldNames(), args[2]);
        }

        return List.of();
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            names.add(w.getName());
        }
        return names;
    }

    private List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) out.add(option);
        }
        return out;
    }
}
