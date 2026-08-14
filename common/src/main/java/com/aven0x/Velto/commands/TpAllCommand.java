package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.utils.LangUtil;
import com.aven0x.Velto.utils.PlayerUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class TpAllCommand extends BaseCommand {

    public TpAllCommand() {
        super("tpall");
    }

    @Override
    public boolean canUse(CommandSender sender) {
        return checkPermission(sender, "velto.tpall");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.tpall")) return true;

        Player target = (Player) sender;
        Location destination = target.getLocation().clone();

        // Snapshot the online players so we never iterate the live view off the owning thread;
        // teleportAsync itself hops each player onto their own region. Clone the destination per
        // call since Bukkit may mutate the passed Location.
        for (Player p : new ArrayList<>(PlayerUtil.onlineSnapshot())) {
            if (p.equals(target)) continue;
            TeleportManager.getInstance().teleportAsync(p, destination.clone());
        }

        LangUtil.send(target, "tpall-success");
        return true;
    }
}
