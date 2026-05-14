package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    public record TpaRequest(UUID requester, String requesterName, UUID target, long expiresAt) {}

    // One outgoing request per requester at a time.
    private static final Map<UUID, TpaRequest> pending = new ConcurrentHashMap<>();

    // Sends a request from requester to target, cancelling any previous outgoing request.
    public static void sendRequest(Player requester, Player target) {
        cancelRequest(requester.getUniqueId());

        long expiresAt = System.currentTimeMillis() + ConfigUtil.getTpaExpireSeconds() * 1000L;
        pending.put(requester.getUniqueId(), new TpaRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId(), expiresAt));

        Bukkit.getScheduler().runTaskLater(VeltoPlugin.get(), () -> {
            TpaRequest req = pending.get(requester.getUniqueId());
            if (req != null && req.target().equals(target.getUniqueId())) {
                pending.remove(requester.getUniqueId());
                if (requester.isOnline()) {
                    LangUtil.send(requester, "tpa-expired", Map.of("%target%", target.getName()));
                }
            }
        }, ConfigUtil.getTpaExpireSeconds() * 20L);
    }

    // Returns the pending outgoing request for this player if it has not expired.
    public static TpaRequest getOutgoing(UUID requesterId) {
        TpaRequest req = pending.get(requesterId);
        if (req == null) return null;
        if (System.currentTimeMillis() > req.expiresAt()) {
            pending.remove(requesterId);
            return null;
        }
        return req;
    }

    // Returns all non-expired incoming requests for this target, newest first.
    public static List<TpaRequest> getIncoming(UUID targetId) {
        long now = System.currentTimeMillis();
        return pending.values().stream()
                .filter(r -> r.target().equals(targetId) && r.expiresAt() > now)
                .sorted((a, b) -> Long.compare(b.expiresAt(), a.expiresAt()))
                .toList();
    }

    // Returns a specific incoming request from requesterId to targetId, or null.
    public static TpaRequest getIncomingFrom(UUID requesterId, UUID targetId) {
        TpaRequest req = getOutgoing(requesterId);
        if (req != null && req.target().equals(targetId)) return req;
        return null;
    }

    public static void cancelRequest(UUID requesterId) {
        pending.remove(requesterId);
    }
}
