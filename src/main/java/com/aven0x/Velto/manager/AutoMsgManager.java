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

    private final List<String> messages;
    private final boolean random;
    private final int intervalTicks;
    private final Random rng = new Random();

    public AutoMsgManager() {
        this.messages = ConfigUtil.getAutoMessageKeys();
        this.random = ConfigUtil.isAutoMessagesRandom();
        this.intervalTicks = ConfigUtil.getAutoMessagesIntervalTicks();
    }

    public void start() {
        if (messages.isEmpty()) return;

        new BukkitRunnable() {
            private int currentIndex = 0;

            @Override
            public void run() {
                if (!ConfigUtil.isAutoMessagesEnabled()) return;

                String key;
                if (random) {
                    key = messages.get(rng.nextInt(messages.size()));
                } else {
                    key = messages.get(currentIndex);
                    currentIndex = (currentIndex + 1) % messages.size();
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    NotificationUtil.send(player, key);
                }
            }
        }.runTaskTimerAsynchronously(Velto.getInstance(), intervalTicks, intervalTicks);
    }
}
