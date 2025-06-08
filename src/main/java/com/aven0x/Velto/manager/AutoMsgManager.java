package com.aven0x.Velto.manager;

import com.aven0x.Velto.Velto;
import com.aven0x.Velto.utils.ConfigUtil;
import com.aven0x.Velto.utils.NotificationUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class AutoMsgManager {

    private int index = 0;
    private String lastKey = null;
    private final Random rng = new Random();

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
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
                    key = keys.get(index++ % keys.size());
                }

                Bukkit.getLogger().info("[Velto] Broadcasting auto-message: " + key);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    NotificationUtil.send(player, key);
                }
            }
        }.runTaskTimer(Velto.getInstance(),
                ConfigUtil.getAutoMessagesIntervalTicks(),
                ConfigUtil.getAutoMessagesIntervalTicks());
    }
}
