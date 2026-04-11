package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ItemLoreCommand extends BaseCommand {
    public ItemLoreCommand() {
        super("itemlore");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.lore")) {
            return true;
        }

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

        @SuppressWarnings("deprecation")
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
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
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                if (m2 == null) return true;
                m2.setLore(null);
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
                if (m2 == null) return true;
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
                int index0 = idx1 - 1;
                if (index0 < 0 || index0 > lore.size()) {
                    LangUtil.send(player, "lore-out-of-range");
                    return true;
                }
                String text = colorize(joinFrom(args, 2));
                lore.add(index0, text);
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                if (m2 == null) return true;
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
                if (m2 == null) return true;
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
                @SuppressWarnings("deprecation")
                ItemMeta m2 = item.getItemMeta();
                if (m2 == null) return true;
                m2.setLore(lore.isEmpty() ? null : lore);
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

    private void sendUsage(Player player, String label) {
        LangUtil.send(player, "lore-usage");
        player.sendMessage(ChatColor.YELLOW + "Usage:");
        player.sendMessage(ChatColor.GRAY + "/" + label + " show");
        player.sendMessage(ChatColor.GRAY + "/" + label + " clear");
        player.sendMessage(ChatColor.GRAY + "/" + label + " add <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " insert <line> <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " set <line> <text...>");
        player.sendMessage(ChatColor.GRAY + "/" + label + " remove <line>");
    }

    private String joinFrom(String[] arr, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private String colorize(String s) {
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

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
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
            String s = args[0].toLowerCase(Locale.ROOT);
            if (s.equals("set") || s.equals("remove") || s.equals("insert")) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item != null && !item.getType().isAir()) {
                    @SuppressWarnings("deprecation")
                    ItemMeta completeMeta = item.getItemMeta();
                    List<String> lore = completeMeta != null ? completeMeta.getLore() : null;
                    int size = lore == null ? 0 : lore.size();
                    int max = s.equals("insert") ? size + 1 : size;
                    for (int i = 1; i <= Math.max(1, max); i++) {
                        out.add(String.valueOf(i));
                    }
                } else {
                    out.add("1");
                }
            }
            return out;
        }

        return out;
    }
}
