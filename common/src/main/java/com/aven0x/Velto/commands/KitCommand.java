package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.KitManager;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KitCommand extends BaseCommand {

    public KitCommand() {
        super("kit");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.kit")) return true;

        if (args.length == 0) {
            sendKitList(sender);
            return true;
        }

        String kitName = args[0];
        KitManager.Kit kit = KitManager.getKit(kitName);
        if (kit == null) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-not-found", Map.of("%kit%", kitName));
            } else {
                sender.sendMessage("Kit not found: " + kitName);
            }
            return true;
        }

        // Resolve target
        Player target;
        boolean self;
        if (args.length >= 2) {
            if (!hasPermission(sender, "velto.kit.others")) return true;
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                if (sender instanceof Player player) LangUtil.send(player, "invalid-player");
                else sender.sendMessage("Player not found.");
                return true;
            }
            self = false;
        } else {
            if (!isPlayer(sender)) return true;
            target = (Player) sender;
            self = true;
        }

        // Per-kit permission
        if (!sender.hasPermission("velto.kit." + kit.name().toLowerCase())) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-no-permission");
            } else {
                sender.sendMessage("No permission for kit: " + kit.name());
            }
            return true;
        }

        // Cooldown (checked against the target for /kit <name> <player>, consistent with EssentialsX)
        if (kit.cooldownSeconds() > 0 && !sender.hasPermission("velto.kit.cooldown.bypass")) {
            long remaining = KitManager.getCooldownRemaining(target.getUniqueId(), kit.name());
            if (remaining > 0) {
                String time = KitManager.formatCooldown(remaining);
                if (sender instanceof Player player) {
                    LangUtil.send(player, "kit-on-cooldown", Map.of("%kit%", kit.name(), "%time%", time));
                } else {
                    sender.sendMessage("Kit cooldown for " + target.getName() + ": " + time);
                }
                return true;
            }
        }

        boolean noOverflow = KitManager.giveKit(target, kit);
        if (kit.cooldownSeconds() > 0) {
            KitManager.setCooldown(target.getUniqueId(), kit.name());
        }

        if (self) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-given-self", Map.of("%kit%", kit.name()));
                if (!noOverflow) LangUtil.send(player, "kit-inventory-full");
            }
        } else {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-given-other", Map.of("%kit%", kit.name(), "%player%", target.getName()));
            } else {
                sender.sendMessage("Gave kit " + kit.name() + " to " + target.getName() + ".");
            }
            LangUtil.send(target, "kit-received", Map.of("%kit%", kit.name(), "%sender%", sender.getName()));
            if (!noOverflow) LangUtil.send(target, "kit-inventory-full");
        }

        return true;
    }

    private void sendKitList(CommandSender sender) {
        List<KitManager.Kit> accessible = new ArrayList<>();
        for (KitManager.Kit kit : KitManager.getKits()) {
            if (sender.hasPermission("velto.kit." + kit.name().toLowerCase())) {
                accessible.add(kit);
            }
        }

        if (accessible.isEmpty()) {
            if (sender instanceof Player player) LangUtil.send(player, "kit-list-empty");
            else sender.sendMessage("No kits available.");
            return;
        }

        if (sender instanceof Player player) LangUtil.send(player, "kit-list-header");
        else sender.sendMessage("Available kits:");

        for (KitManager.Kit kit : accessible) {
            String cooldownStr;
            if (kit.cooldownSeconds() <= 0) {
                cooldownStr = ChatColor.GREEN + "No cooldown";
            } else if (sender instanceof Player player) {
                long remaining = KitManager.getCooldownRemaining(player.getUniqueId(), kit.name());
                cooldownStr = remaining > 0
                        ? ChatColor.RED + KitManager.formatCooldown(remaining)
                        : ChatColor.GREEN + "Ready";
            } else {
                cooldownStr = KitManager.formatCooldown(kit.cooldownSeconds());
            }

            sender.sendMessage(ChatColor.GOLD + " - " + ChatColor.YELLOW + kit.name()
                    + ChatColor.GRAY + " (" + cooldownStr + ChatColor.GRAY + ")"
                    + ChatColor.DARK_GRAY + " [" + kit.items().size() + " items]");
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            for (KitManager.Kit kit : KitManager.getKits()) {
                if (kit.name().toLowerCase().startsWith(typed)
                        && sender.hasPermission("velto.kit." + kit.name().toLowerCase())) {
                    names.add(kit.name());
                }
            }
            return names;
        }
        if (args.length == 2 && sender.hasPermission("velto.kit.others")) {
            String typed = args[1].toLowerCase();
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(typed)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
