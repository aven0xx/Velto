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
    private final Random rng = new Random();

    public void start() {
        List<String> keys = ConfigUtil.getAutoMessageKeys();

        if (keys.isEmpty()) {
            Bukkit.getLogger().warning("[Velto] No auto-messages defined in config.");
            return;
        }

        int interval = ConfigUtil.getAutoMessagesIntervalTicks();
        boolean random = ConfigUtil.isAutoMessagesRandom();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ConfigUtil.isAutoMessagesEnabled()) {
                    Bukkit.getLogger().info("[Velto] Auto-messages disabled in config.");
                    return;
                }

                String key = random
                        ? keys.get(rng.nextInt(keys.size()))
                        : keys.get(index++ % keys.size());

                Bukkit.getLogger().info("[Velto] Broadcasting auto-message: " + key);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    NotificationUtil.send(player, key);
                }
            }
        }.runTaskTimer(Velto.getInstance(), interval, interval);
    }
}
