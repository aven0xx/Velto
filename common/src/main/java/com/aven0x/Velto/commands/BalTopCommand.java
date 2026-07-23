package com.aven0x.Velto.commands;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.managers.EconomyManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BalTopCommand extends BaseCommand {

    private static final int PAGE_SIZE = 10;

    public BalTopCommand() {
        super("baltop");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return EconomyManager.isEnabled() && checkPermission(sender, "velto.baltop");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!EconomyManager.isEnabled()) {
            if (sender instanceof Player player) LangUtil.send(player, "economy-disabled");
            else sender.sendMessage("The economy module is currently disabled.");
            return true;
        }

        if (!hasPermission(sender, "velto.baltop")) return true;

        // A bad or missing page just falls back to page 1 (clamped once we know the total).
        int requestedPage = 1;
        if (args.length > 0) {
            try {
                requestedPage = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
                requestedPage = 1;
            }
        }
        final int page = requestedPage;

        // Scanning every player's balance can hit disk for offline players, so build the ranking
        // off the main thread and only come back to it for name resolution + messaging.
        Bukkit.getScheduler().runTaskAsynchronously(VeltoPlugin.get(), () -> {
            List<Map.Entry<UUID, Double>> sorted = EconomyManager.getSortedBalances();
            Bukkit.getScheduler().runTask(VeltoPlugin.get(), () -> render(sender, sorted, page));
        });
        return true;
    }

    private void render(CommandSender sender, List<Map.Entry<UUID, Double>> sorted, int requestedPage) {
        if (sender instanceof Player player && !player.isOnline()) return;

        if (sorted.isEmpty()) {
            send(sender, "baltop-empty", Map.of(), "No balances to show yet.");
            return;
        }

        int totalPages = (sorted.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.min(requestedPage, totalPages);
        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        Map<String, String> pageInfo = Map.of(
                "%page%", String.valueOf(page),
                "%pages%", String.valueOf(totalPages));

        send(sender, "baltop-header", pageInfo,
                "=== Balance Top (page " + page + "/" + totalPages + ") ===");

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Double> entry = sorted.get(i);
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = "Unknown";
            String formatted = EconomyManager.format(entry.getValue());

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%rank%", String.valueOf(i + 1));
            placeholders.put("%player%", name);
            placeholders.put("%balance%", formatted);

            send(sender, "baltop-entry", placeholders,
                    "#" + (i + 1) + " " + name + " - " + formatted);
        }

        send(sender, "baltop-footer", pageInfo, "Page " + page + "/" + totalPages);
    }

    private void send(CommandSender sender, String key, Map<String, String> placeholders, String consoleFallback) {
        if (sender instanceof Player player) {
            LangUtil.send(player, key, placeholders);
        } else {
            sender.sendMessage(consoleFallback);
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        return List.of();
    }
}
