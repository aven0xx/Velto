package com.aven0x.Velto.commands;

import com.aven0x.Velto.managers.TpaManager;
import com.aven0x.Velto.managers.TeleportManager;
import com.aven0x.Velto.managers.TpaManager.TpaRequest;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class TpaAcceptCommand extends BaseCommand {

    public TpaAcceptCommand() {
        super("tpaaccept");
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!isPlayer(sender)) return true;
        if (!hasPermission(sender, "velto.tpa")) return true;

        Player target = (Player) sender;
        TpaRequest request;

        if (args.length >= 1) {
            // Accept from a specific requester.
            Player requester = Bukkit.getPlayerExact(args[0]);
            if (requester == null) {
                LangUtil.send(target, "invalid-player");
                return true;
            }
            request = TpaManager.getIncomingFrom(requester.getUniqueId(), target.getUniqueId());
        } else {
            // Accept the most recent incoming request.
            List<TpaRequest> incoming = TpaManager.getIncoming(target.getUniqueId());
            request = incoming.isEmpty() ? null : incoming.get(0);
        }

        if (request == null) {
            LangUtil.send(target, "tpa-no-request");
            return true;
        }

        Player requester = Bukkit.getPlayer(request.requester());
        TpaManager.cancelRequest(request.requester());

        if (requester == null || !requester.isOnline()) {
            LangUtil.send(target, "tpa-no-request");
            return true;
        }

        LangUtil.send(target, "tpa-accepted-target", Map.of("%requester%", requester.getName()));
        LangUtil.send(requester, "tpa-accepted-requester", Map.of("%target%", target.getName()));
        final String targetName = target.getName();
        TeleportManager.getInstance().teleport(requester, target.getLocation(),
                () -> LangUtil.send(requester, "tpa-teleported", Map.of("%target%", targetName)));
        return true;
    }

    @Override
    public List<String> complete(CommandSender sender, String label, String[] args) {
        if (args.length <= 1 && sender instanceof Player target) {
            String typed = args.length == 0 ? "" : args[0].toLowerCase();
            return TpaManager.getIncoming(target.getUniqueId()).stream()
                    .map(r -> Bukkit.getPlayer(r.requester()))
                    .filter(p -> p != null && p.getName().toLowerCase().startsWith(typed))
                    .map(Player::getName)
                    .toList();
        }
        return List.of();
    }
}
