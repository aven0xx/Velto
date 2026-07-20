package com.aven0x.Velto.commands;

import com.aven0x.Velto.utils.CommandUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public abstract class BaseCommand {

    protected final String name;

    public BaseCommand(String name) {
        this.name = name;
    }

    /**
     * Execute the command. Return true if handled, false to show usage.
     */
    public abstract boolean execute(CommandSender sender, String label, String[] args);

    /**
     * Provide tab completion suggestions.
     */
    public List<String> complete(CommandSender sender, String label, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Whether this command should be visible to the sender at all (gates tab-completion/listing
     * on both Paper and Bukkit). Override with the same permission(s) checked in execute().
     */
    public boolean canUse(CommandSender sender) {
        return true;
    }

    protected boolean isPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return false;
        }
        return true;
    }

    protected boolean checkPermission(CommandSender sender, String perm) {
        return sender.hasPermission(CommandUtil.getPermission(this.name, perm));
    }

    /**
     * Resolves a player name to an online player, or a previously-seen offline player —
     * so console/admin commands can target players who aren't currently online.
     *
     * Returns {@code null} if nobody online matches and no cached offline player matches.
     * Deliberately avoids {@code Bukkit.getOfflinePlayer(String)}: that can block on a
     * Mojang web lookup and fabricate an entry for a name that has never joined. Matching
     * against {@link Bukkit#getOfflinePlayers()} stays local and only ever returns players
     * the server has actually seen.
     */
    protected static OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offline.getName())) return offline;
        }
        return null;
    }

    protected boolean hasPermission(CommandSender sender, String perm) {
        if (!checkPermission(sender, perm)) {
            if (sender instanceof Player player) {
                LangUtil.send(player, "no-permission");
            } else {
                sender.sendMessage("§cYou do not have permission.");
            }
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }
}
