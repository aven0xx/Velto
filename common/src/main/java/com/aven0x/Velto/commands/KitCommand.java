package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.KitManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Bukkit;
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
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.kit");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!hasPermission(sender, "velto.kit")) return true;

        if (args.length == 0) {
            sendKitList(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("preview")) {
            return handlePreview(sender, args);
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

        boolean bypassCooldown = sender.hasPermission("velto.kit.cooldown.bypass");

        // One-time kits: once claimed, blocked forever regardless of cooldown timer.
        if (kit.oneTime() && !bypassCooldown && KitManager.hasClaimedOnce(target.getUniqueId(), kit.name())) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "kit-already-claimed", Map.of("%kit%", kit.name()));
            } else {
                sender.sendMessage("Kit '" + kit.name() + "' was already claimed by " + target.getName() + ".");
            }
            return true;
        }

        // Cooldown (checked against the target for /kit <name> <player>, consistent with EssentialsX)
        if (!kit.oneTime() && kit.cooldownSeconds() > 0 && !bypassCooldown) {
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

        // Kit delivery mutates the target's inventory and drops overflow into their world, so it
        // runs on the region that owns the target — inline when that's us (always on Spigot, and
        // for /kit on yourself), hopped otherwise. The cooldown/claim write and all feedback move
        // inside too, since giveKit's result is only known once delivery has run.
        final Player kitTarget = target;
        final boolean isSelf = self;
        PlayerUtil.onOwningRegion(kitTarget, () -> {
            KitManager.GiveResult result = KitManager.giveKit(kitTarget, kit);
            if (result == KitManager.GiveResult.ERROR) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "kit-give-failed", Map.of("%kit%", kit.name()));
                } else {
                    sender.sendMessage("Failed to fully deliver kit '" + kit.name() + "' to " + kitTarget.getName() + ".");
                }
                return;
            }

            if (kit.oneTime()) {
                KitManager.markClaimedOnce(kitTarget.getUniqueId(), kit.name());
            } else if (kit.cooldownSeconds() > 0) {
                KitManager.setCooldown(kitTarget.getUniqueId(), kit.name());
            }

            boolean noOverflow = result != KitManager.GiveResult.OVERFLOW;

            if (isSelf) {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "kit-given-self", Map.of("%kit%", kit.name()));
                    if (!noOverflow) LangUtil.send(player, "kit-inventory-full");
                }
            } else {
                if (sender instanceof Player player) {
                    LangUtil.send(player, "kit-given-other", Map.of("%kit%", kit.name(), "%player%", kitTarget.getName()));
                } else {
                    sender.sendMessage("Gave kit " + kit.name() + " to " + kitTarget.getName() + ".");
                }
                LangUtil.send(kitTarget, "kit-received", Map.of("%kit%", kit.name(), "%sender%", sender.getName()));
                if (!noOverflow) LangUtil.send(kitTarget, "kit-inventory-full");
            }
        });

        return true;
    }

    private boolean handlePreview(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) return true;
        Player player = (Player) sender;

        if (args.length < 2) {
            LangUtil.send(player, "kit-preview-usage");
            return true;
        }

        KitManager.Kit kit = KitManager.getKit(args[1]);
        if (kit == null) {
            LangUtil.send(player, "kit-not-found", Map.of("%kit%", args[1]));
            return true;
        }

        if (!player.hasPermission("velto.kit." + kit.name().toLowerCase())) {
            LangUtil.send(player, "kit-no-permission");
            return true;
        }

        // /kit preview is self-invoked, so this opens on the viewer's own region — safe as-is.
        player.openInventory(KitManager.buildPreviewInventory(kit));
        return true;
    }

    private void sendKitList(CommandSender sender) {
        List<KitManager.Kit> accessible = new ArrayList<>();
        for (KitManager.Kit kit : KitManager.getKits()) {
            if (sender.hasPermission("velto.kit." + kit.name().toLowerCase())) {
                accessible.add(kit);
            }
        }

        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        if (accessible.isEmpty()) {
            if (isPlayer) LangUtil.send(player, "kit-list-empty");
            else sender.sendMessage("No kits available.");
            return;
        }

        if (isPlayer) LangUtil.send(player, "kit-list-header");
        else sender.sendMessage("Available kits:");

        for (KitManager.Kit kit : accessible) {
            // Player-facing cooldown states carry hex colours so they compose into the lang
            // segment; the console gets the same words uncoloured.
            String cooldownStr;
            if (kit.oneTime()) {
                if (isPlayer) {
                    cooldownStr = KitManager.hasClaimedOnce(player.getUniqueId(), kit.name())
                            ? "&#FF7070Claimed"
                            : "&#57E39AOne-time";
                } else {
                    cooldownStr = "One-time";
                }
            } else if (kit.cooldownSeconds() <= 0) {
                cooldownStr = isPlayer ? "&#57E39ANo cooldown" : "No cooldown";
            } else if (isPlayer) {
                long remaining = KitManager.getCooldownRemaining(player.getUniqueId(), kit.name());
                cooldownStr = remaining > 0
                        ? "&#FF7070" + KitManager.formatCooldown(remaining)
                        : "&#57E39AReady";
            } else {
                cooldownStr = KitManager.formatCooldown(kit.cooldownSeconds());
            }

            if (isPlayer) {
                // Rendered via lang.yml so the kit name is a run_command click → /kit <name>.
                LangUtil.send(player, "kit-list-entry", Map.of(
                        "%kit%", kit.name(),
                        "%cooldown%", cooldownStr,
                        "%items%", String.valueOf(kit.items().size())));
            } else {
                sender.sendMessage(" - " + kit.name() + " (" + cooldownStr + ") ["
                        + kit.items().size() + " items]");
            }
        }
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1) {
            String typed = (args.length == 0 ? "" : args[0]).toLowerCase();
            List<String> names = new ArrayList<>();
            if ("preview".startsWith(typed)) names.add("preview");
            for (KitManager.Kit kit : KitManager.getKits()) {
                if (kit.name().toLowerCase().startsWith(typed)
                        && sender.hasPermission("velto.kit." + kit.name().toLowerCase())) {
                    names.add(kit.name());
                }
            }
            return names;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            String typed = args[1].toLowerCase();
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
            for (Player p : PlayerUtil.onlineSnapshot()) {
                if (p.getName().toLowerCase().startsWith(typed)) names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
