package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    public record TpaRequest(UUID requester, String requesterName, UUID target, long expiresAt) {}

    // One outgoing request per requester at a time.
    private static final Map<UUID, PendingTpa> outgoing = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> incomingByTarget = new ConcurrentHashMap<>();

    private record PendingTpa(TpaRequest request, BukkitTask expiryTask) {}

    // Sends a request from requester to target, cancelling any previous outgoing request.
    public static void sendRequest(Player requester, Player target) {
        cancelRequest(requester.getUniqueId());

        long expiresAt = System.currentTimeMillis() + ConfigUtil.getTpaExpireSeconds() * 1000L;
        TpaRequest request = new TpaRequest(requester.getUniqueId(), requester.getName(), target.getUniqueId(), expiresAt);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(VeltoPlugin.get(), () -> {
            PendingTpa pending = outgoing.get(requester.getUniqueId());
            if (pending != null && pending.request().target().equals(target.getUniqueId())) {
                removeRequest(requester.getUniqueId(), false);
                if (requester.isOnline()) {
                    LangUtil.send(requester, "tpa-expired", Map.of("%target%", target.getName()));
                }
            }
        }, ConfigUtil.getTpaExpireSeconds() * 20L);

        outgoing.put(requester.getUniqueId(), new PendingTpa(request, task));
        incomingByTarget.computeIfAbsent(target.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet())
                .add(requester.getUniqueId());
    }

    // Returns the pending outgoing request for this player if it has not expired.
    public static TpaRequest getOutgoing(UUID requesterId) {
        PendingTpa pending = outgoing.get(requesterId);
        if (pending == null) return null;

        TpaRequest request = pending.request();
        if (System.currentTimeMillis() > request.expiresAt()) {
            removeRequest(requesterId, true);
            return null;
        }
        return request;
    }

    // Returns all non-expired incoming requests for this target, newest first.
    public static List<TpaRequest> getIncoming(UUID targetId) {
        Set<UUID> requesterIds = incomingByTarget.get(targetId);
        if (requesterIds == null || requesterIds.isEmpty()) return List.of();

        List<TpaRequest> requests = new ArrayList<>();
        for (UUID requesterId : new HashSet<>(requesterIds)) {
            TpaRequest request = getOutgoing(requesterId);
            if (request != null && request.target().equals(targetId)) {
                requests.add(request);
            }
        }

        requests.sort(Comparator.comparingLong(TpaRequest::expiresAt).reversed());
        return requests;
    }

    // Returns a specific incoming request from requesterId to targetId, or null.
    public static TpaRequest getIncomingFrom(UUID requesterId, UUID targetId) {
        TpaRequest req = getOutgoing(requesterId);
        if (req != null && req.target().equals(targetId)) return req;
        return null;
    }

    public static void cancelRequest(UUID requesterId) {
        removeRequest(requesterId, true);
    }

    public static void cleanup(UUID playerId) {
        cancelRequest(playerId);
        Set<UUID> incoming = incomingByTarget.remove(playerId);
        if (incoming == null) return;

        for (UUID requesterId : incoming) {
            cancelRequest(requesterId);
        }
    }

    private static void removeRequest(UUID requesterId, boolean cancelTask) {
        PendingTpa removed = outgoing.remove(requesterId);
        if (removed == null) return;

        if (cancelTask) {
            removed.expiryTask().cancel();
        }

        Set<UUID> incoming = incomingByTarget.get(removed.request().target());
        if (incoming == null) return;

        incoming.remove(requesterId);
        if (incoming.isEmpty()) {
            incomingByTarget.remove(removed.request().target(), incoming);
        }
    }
}
