package com.aven0x.VeltoPaper.commands;

import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemLoreCommand extends BaseCommand implements TabExecutor {
    public ItemLoreCommand() {
        super("itemlore");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        // Permission
        if (!hasPermission(sender, "velto.lore")) {
            if (sender instanceof Player p) {
                LangUtil.send(p, "no-permission");
            }
            return true;
        }

        // Player-only
        if (!(sender instanceof Player player)) {
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            LangUtil.send(player, "lore-no-item");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        // Get or create lore list (legacy String API; suppress deprecation warnings)
        @SuppressWarnings("deprecation")
        ItemMeta meta = item.getItemMeta();
        @SuppressWarnings("deprecation")
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        switch (sub) {
            case "show": {
                if (lore.isEmpty()) {
                    LangUtil.send(player, "lore-empty");
                    return true;
                }
                player.sendMessage(ChatColor.YELLOW + "Lore (" + lore.size() + " lines):");
                for (int i = 0; i < lore.size(); i++) {
                    String line = lore.get(i) == null ? "" : lore.get(i);
                    player.sendMessage(ChatColor.GRAY + String.valueOf(i + 1) + ". " + ChatColor.RESET + line);
                }
                return true;
            }
            case "clear": {
                lore.clear();
                @SuppressWarnings("deprecation")
                List<String> finalLore = null; // set to null to fully clear
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                m2.setLore(finalLore);
                item.setItemMeta(m2);
                LangUtil.send(player, "lore-cleared");
                return true;
            }
            case "add": {
                if (args.length < 2) {
                    sendUsage(player, label);
                    return true;
                }
                String text = colorize(joinFrom(args, 1));
                lore.add(text);
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                m2.setLore(lore);
                item.setItemMeta(m2);
                LangUtil.send(player, "lore-add-success");
                player.sendMessage(ChatColor.GREEN + "Added line " + lore.size() + ".");
                return true;
            }
            case "insert": {
                if (args.length < 3) {
                    sendUsage(player, label);
                    return true;
                }
                Integer idx1 = parsePositiveInt(args[1]);
                if (idx1 == null) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                int index0 = idx1 - 1; // 1-based -> 0-based
                // Insert allows index = size + 1 (append)
                if (index0 < 0 || index0 > lore.size()) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                String text = colorize(joinFrom(args, 2));
                lore.add(index0, text);
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                m2.setLore(lore);
                item.setItemMeta(m2);
                LangUtil.send(player, "lore-insert-success");
                player.sendMessage(ChatColor.GREEN + "Inserted at line " + idx1 + ".");
                return true;
            }
            case "set": {
                if (args.length < 3) {
                    sendUsage(player, label);
                    return true;
                }
                if (lore.isEmpty()) {
                    LangUtil.send(player, "lore-empty");
                    return true;
                }
                Integer idx1 = parsePositiveInt(args[1]);
                if (idx1 == null) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                int index0 = idx1 - 1;
                if (index0 < 0 || index0 >= lore.size()) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                String text = colorize(joinFrom(args, 2));
                lore.set(index0, text);
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                m2.setLore(lore);
                item.setItemMeta(m2);
                LangUtil.send(player, "lore-set-success");
                player.sendMessage(ChatColor.GREEN + "Updated line " + idx1 + ".");
                return true;
            }
            case "remove": {
                if (args.length < 2) {
                    sendUsage(player, label);
                    return true;
                }
                if (lore.isEmpty()) {
                    LangUtil.send(player, "lore-empty");
                    return true;
                }
                Integer idx1 = parsePositiveInt(args[1]);
                if (idx1 == null) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                int index0 = idx1 - 1;
                if (index0 < 0 || index0 >= lore.size()) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                lore.remove(index0);
                // If empty after removal, set null to fully clear
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                @SuppressWarnings("deprecation")
                List<String> finalLore = lore.isEmpty() ? null : lore;
                m2.setLore(finalLore);
                item.setItemMeta(m2);
                LangUtil.send(player, "lore-remove-success");
                player.sendMessage(ChatColor.GREEN + "Removed line " + idx1 + ".");
                return true;
            }
            default:
                sendUsage(player, label);
                return true;
        }
    }

    private void sendUsage(@NotNull Player player, @NotNull String label) {
        LangUtil.send(player, "lore-usage");
        player.sendMessage(ChatColor.YELLOW + "Usage:");
        player.sendMessage(ChatColor.GRAY + "/" + label + " show");
        player.sendMessage(ChatColor.GRAY + "/" + label + " clear");
        player.sendMessage(ChatColor.GRAY + "/" + label + " add <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " insert <line> <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " set <line> <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " remove <line>");
    }

    private @NotNull String joinFrom(@NotNull String[] arr, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private @NotNull String colorize(@NotNull String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private Integer parsePositiveInt(String s) {
        try {
            int v = Integer.parseInt(s);
            return v > 0 ? v : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    // Tab completion
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender,
                                               @NotNull Command command,
                                               @NotNull String alias,
                                               @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player)) return out;

        if (args.length == 1) {
            String p = args[0].toLowerCase(Locale.ROOT);
            for (String opt : new String[]{"show", "clear", "add", "insert", "set", "remove"}) {
                if (opt.startsWith(p)) out.add(opt);
            }
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("set") || sub.equals("remove") || sub.equals("insert")) {
                // Suggest valid line numbers based on current lore length
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    @SuppressWarnings("deprecation")
                    List<String> lore = item.getItemMeta().getLore();
                    int size = lore == null ? 0 : lore.size();
                    int max = sub.equals("insert") ? size + 1 : size;
                    for (int i = 1; i <= Math.max(1, max); i++) {
                        out.add(String.valueOf(i));
                    }
                } else {
                    out.add("1");
                }
            }
            return out;
        }

        // No suggestions for free-text args
        return out;
    }
}
