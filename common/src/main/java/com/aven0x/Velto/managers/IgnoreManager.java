package com.aven0x.Velto.managers;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Per-player ignore lists, persisted in each player's userdata under the {@code ignored} key
 * (a list of ignored-player UUID strings). Backed entirely by {@link UserdataManager}, so there's
 * no transient in-memory state to clean up on quit.
 */
public final class IgnoreManager {

    private IgnoreManager() {}

    private static final String KEY = "ignored";

    /**
     * Players holding this permission can't be ignored — their chat and private messages always
     * get through. Op-only by default, so ignoring works normally for regular players.
     */
    public static final String EXEMPT_PERMISSION = "velto.ignore.exempt";

    public static List<UUID> getIgnored(UUID owner) {
        List<String> raw = UserdataManager.getStringList(owner, KEY);
        List<UUID> result = new ArrayList<>(raw.size());
        for (String s : raw) {
            try {
                result.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                // Malformed entry — skip it.
            }
        }
        return result;
    }

    /** Whether {@code owner} is currently ignoring {@code target}. */
    public static boolean ignores(UUID owner, UUID target) {
        if (owner == null || target == null) return false;
        return UserdataManager.getStringList(owner, KEY).contains(target.toString());
    }

    /** Adds {@code target} to {@code owner}'s ignore list. Returns false if already ignored. */
    public static boolean addIgnore(UUID owner, UUID target) {
        List<String> list = new ArrayList<>(UserdataManager.getStringList(owner, KEY));
        String id = target.toString();
        if (list.contains(id)) return false;
        list.add(id);
        UserdataManager.set(owner, KEY, list);
        UserdataManager.save(owner);
        return true;
    }

    /** Removes {@code target} from {@code owner}'s ignore list. Returns false if not ignored. */
    public static boolean removeIgnore(UUID owner, UUID target) {
        List<String> list = new ArrayList<>(UserdataManager.getStringList(owner, KEY));
        if (!list.remove(target.toString())) return false;
        UserdataManager.set(owner, KEY, list);
        UserdataManager.save(owner);
        return true;
    }

    /**
     * Whether a message or chat line from {@code from} should be hidden from {@code to} because
     * {@code to} is ignoring them. Senders with {@link #EXEMPT_PERMISSION} are never blocked.
     */
    public static boolean isBlocked(Player from, Player to) {
        if (from == null || to == null) return false;
        if (from.hasPermission(EXEMPT_PERMISSION)) return false;
        return ignores(to.getUniqueId(), from.getUniqueId());
    }
}
