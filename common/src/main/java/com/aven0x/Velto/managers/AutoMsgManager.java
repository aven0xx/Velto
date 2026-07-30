package com.aven0x.Velto.managers;

import com.aven0x.Velto.platform.Schedulers;
import com.aven0x.Velto.platform.VeltoTask;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.LangUtil;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Random;

public class AutoMsgManager {

    private static AutoMsgManager instance;

    private int index = 0;
    private String lastKey = null;
    private final Random rng = new Random();
    private VeltoTask task;

    public AutoMsgManager() {
        instance = this;
    }

    public static AutoMsgManager getInstance() {
        return instance;
    }

    public void restart() {
        stop();
        index = 0;
        lastKey = null;
        start();
    }

    public void start() {
        if (task != null) return;

        // A server-wide broadcast belongs to no region, so it runs on the global scheduler.
        task = Schedulers.get().globalTimer(this::broadcastNext,
                ConfigUtil.getAutoMessagesIntervalTicks(),
                ConfigUtil.getAutoMessagesIntervalTicks());
    }

    private void broadcastNext() {
        if (!ConfigUtil.isAutoMessagesEnabled()) {
            return; // Silently skip if disabled
        }

        List<String> keys = ConfigUtil.getAutoMessageKeys();
        if (keys.isEmpty()) {
            Bukkit.getLogger().warning("[Velto] No auto-messages defined in config.");
            return;
        }

        boolean random = ConfigUtil.isAutoMessagesRandom();
        String key;

        if (random) {
            // Prevent repeating the same message twice in a row
            if (keys.size() == 1) {
                key = keys.get(0);
            } else {
                do {
                    key = keys.get(rng.nextInt(keys.size()));
                } while (key.equals(lastKey));
            }
            lastKey = key;
        } else {
            key = keys.get(index % keys.size());
            index = (index + 1) % keys.size();
        }

        Bukkit.getLogger().info("[Velto] Broadcasting auto-message: " + key);
        LangUtil.sendGlobal(key);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
    }
}
