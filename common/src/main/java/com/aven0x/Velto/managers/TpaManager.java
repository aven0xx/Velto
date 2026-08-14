package com.aven0x.Velto.managers;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.platform.VeltoTask;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

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

    private record PendingTpa(TpaRequest request, VeltoTask expiryTask) {}

    // Sends a request from requester to target, cancelling any previous outgoing request.
    public static void sendRequest(Player requester, Player target) {
        UUID requesterId = requester.getUniqueId();
        UUID targetId = target.getUniqueId();
        String targetName = target.getName();

        cancelRequest(requesterId);

        long expiresAt = System.currentTimeMillis() + ConfigUtil.getTpaExpireSeconds() * 1000L;
        TpaRequest request = new TpaRequest(requesterId, requester.getName(), targetId, expiresAt);

        // The expiry only messages the requester, which is safe from the global region.
        // Capture ids/name rather than the Player objects: holding a Player across the whole
        // expiry window leaks the reference (and would be a cross-region hold on Folia).
        VeltoTask task = Schedulers.get().globalDelayed(() -> {
            PendingTpa pending = outgoing.get(requesterId);
            if (pending != null && pending.request().target().equals(targetId)) {
                removeRequest(requesterId, false);
                Player requesterPlayer = Bukkit.getPlayer(requesterId);
                if (requesterPlayer != null && requesterPlayer.isOnline()) {
                    LangUtil.send(requesterPlayer, "tpa-expired", Map.of("%target%", targetName));
                }
            }
        }, ConfigUtil.getTpaExpireSeconds() * 20L);

        outgoing.put(requesterId, new PendingTpa(request, task));
        incomingByTarget.computeIfAbsent(targetId, ignored -> ConcurrentHashMap.newKeySet())
                .add(requesterId);
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
