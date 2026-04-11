package com.aven0x.Velto.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MsgManager {

    // Maps a player's UUID to the UUID of the last player they received a message from (for /reply)
    private static final Map<UUID, UUID> lastMessenger = new HashMap<>();

    public static void setLastMessenger(UUID recipient, UUID sender) {
        lastMessenger.put(recipient, sender);
    }

    public static UUID getLastMessenger(UUID recipient) {
        return lastMessenger.get(recipient);
    }

    public static void remove(UUID uuid) {
        lastMessenger.remove(uuid);
    }
}
