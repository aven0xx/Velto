package com.aven0x.VeltoPaper.managers;

import com.aven0x.Velto.utils.PlayerUtil;
import com.aven0x.VeltoPaper.VeltoPaper;
import com.aven0x.VeltoPaper.utils.AfkPositionStorage;
import com.aven0x.VeltoPaper.utils.ConfigUtil;
import com.aven0x.VeltoPaper.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class AfkManager implements Listener {

    // Thread-safe collections (recommended)
    private static final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();
    private static final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();

    // store the player's location right before teleporting to AFK zone
    private static final Map<UUID, Location> preAfkLocations = new ConcurrentHashMap<>();

    private static final long AFK_TIMEOUT = 300000; // 5 minutes
    private static BukkitRunnable afkChecker;

    // Simple debug toggle (optional): plug into your config if you want
    private static boolean DEBUG = false;

    private static Logger log() {
        return VeltoPaper.getInstance().getLogger();
    }

    private static void debug(String msg) {
        if (DEBUG) log().info("[AFK-DEBUG] " + msg);
    }

    /**
     * Start AFK system
     */
    public static void start() {
        if (afkChecker != null) {
            afkChecker.cancel();
        }

        afkChecker = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    long lastActivityTime = lastActivity.getOrDefault(uuid, currentTime);

                    boolean wasAfk = afkPlayers.contains(uuid);
                    boolean shouldBeAfk = (currentTime - lastActivityTime) >= AFK_TIMEOUT;

                    if (!wasAfk && shouldBeAfk) {
                        setAfk(player, true);
                    }
                }
            }
        };

        // Check every 30 seconds
        afkChecker.runTaskTimer(VeltoPaper.getInstance(), 600L, 600L);
    }

    /**
     * Stop AFK system
     */
    public static void stop() {
        if (afkChecker != null) {
            afkChecker.cancel();
            afkChecker = null;
        }
        lastActivity.clear();
        afkPlayers.clear();
        preAfkLocations.clear();
    }

    /**
     * Update player activity status
     */
    public static void updateActivity(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        // If the player was AFK, mark active (main-thread safe thanks to setAfk airbag)
        if (afkPlayers.contains(uuid)) {
            setAfk(player, false);
        }
    }

    public static boolean isAfk(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    /**
     * Toggle AFK
     */
    public static void setAfk(Player player, boolean afk) {
        // Airbag: ensure main thread
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(VeltoPaper.getInstance(), () -> setAfk(player, afk));
            return;
        }

        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        boolean wasAfk = afkPlayers.contains(uuid);

        if (afk && !wasAfk) {
            // Becoming AFK
            afkPlayers.add(uuid);

            boolean afkzoneEnabled = ConfigUtil.isAfkzoneOn();
            debug("Player " + player.getName() + " becoming AFK. afkzoneEnabled=" + afkzoneEnabled);

            if (afkzoneEnabled) {
                // Save current location BEFORE teleporting
                try {
                    preAfkLocations.put(uuid, player.getLocation().clone());
                    debug("Saved pre-AFK location: " + preAfkLocations.get(uuid));
                } catch (Exception e) {
                    log().warning("Failed to save pre-AFK location for " + player.getName() + ": " + e.getMessage());
                }

                Location afkZone = ConfigUtil.getAfkzone();
                if (afkZone != null && afkZone.getWorld() != null) {
                    tryTeleportWithRetry(player, afkZone, "AFK teleport");
                } else {
                    log().warning("AFK zone is null or world is null (check config).");
                }
            }

            // Notify
            Map<String, String> placeholders = Map.of("%player%", player.getName());
            LangUtil.send(player, "afk-enabled", placeholders);

            if (!PlayerUtil.isVanished(player)) {
                LangUtil.sendGlobal("afk-player", placeholders);
            }

        } else if (!afk && wasAfk) {
            // Leaving AFK
            afkPlayers.remove(uuid);
            lastActivity.put(uuid, System.currentTimeMillis());

            if (ConfigUtil.isAfkzoneOn()) {
                Location back = preAfkLocations.remove(uuid);

                if (back != null) {
                    // Validate return location
                    if (back.getWorld() == null) {
                        log().warning("Return location world is null for " + player.getName() + ". Not teleporting.");
                    } else {
                        // optional: ensure chunk is loaded
                        try {
                            back.getChunk().load();
                        } catch (Exception ignored) {}

                        tryTeleportWithRetry(player, back, "Return from AFK");
                    }
                } else {
                    debug("No pre-AFK location stored for " + player.getName() + ".");
                }
            } else {
                preAfkLocations.remove(uuid);
            }

            // Notify
            Map<String, String> placeholders = Map.of("%player%", player.getName());
            LangUtil.send(player, "afk-disabled", placeholders);

            if (!PlayerUtil.isVanished(player)) {
                LangUtil.sendGlobal("afk-player-back", placeholders);
            }
        }
    }

    private static void tryTeleportWithRetry(Player player, Location target, String label) {
        try {
            boolean success = player.teleport(target);
            debug(label + " result: " + success);

            if (!success) {
                Bukkit.getScheduler().runTaskLater(VeltoPaper.getInstance(), () -> {
                    try {
                        boolean delayed = player.teleport(target);
                        debug(label + " delayed result: " + delayed);
                    } catch (Exception e) {
                        log().warning(label + " delayed exception for " + player.getName() + ": " + e.getMessage());
                    }
                }, 1L);
            }
        } catch (Exception e) {
            log().warning(label + " exception for " + player.getName() + ": " + e.getMessage());
        }
    }

    public static boolean toggleAfk(Player player) {
        boolean newState = !isAfk(player);
        setAfk(player, newState);
        return newState;
    }

    public static Set<Player> getAfkPlayers() {
        Set<Player> players = ConcurrentHashMap.newKeySet();
        for (UUID uuid : afkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    public static long getTimeSinceLastActivity(Player player) {
        UUID uuid = player.getUniqueId();
        long lastTime = lastActivity.getOrDefault(uuid, System.currentTimeMillis());
        return System.currentTimeMillis() - lastTime;
    }

    public static void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.remove(uuid);
        afkPlayers.remove(uuid);
        preAfkLocations.remove(uuid);
    }

    public static int getAfkTimeoutSeconds() {
        return (int) (AFK_TIMEOUT / 1000);
    }

    // ===== EVENT LISTENERS =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        Location from = event.getFrom();

        // Ignore rotation-only changes
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            updateActivity(event.getPlayer());
        }
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        // Async event -> schedule on main thread
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTask(VeltoPaper.getInstance(), () -> updateActivity(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String command = event.getMessage().toLowerCase();
        if (!command.startsWith("/afk")) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.isCancelled() && event.getWhoClicked() instanceof Player player) {
            updateActivity(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        updateActivity(player);

        // Pending return from persistent storage (quit while AFK)
        if (AfkPositionStorage.isInitialized() && AfkPositionStorage.has(uuid)) {
            Location back = AfkPositionStorage.get(uuid);

            Bukkit.getScheduler().runTaskLater(VeltoPaper.getInstance(), () -> {
                if (player.isOnline() && back != null && back.getWorld() != null) {
                    try {
                        back.getChunk().load();
                    } catch (Exception ignored) {}

                    tryTeleportWithRetry(player, back, "Pending return teleport");
                } else {
                    log().warning("Pending return invalid for " + player.getName() + " (location/world missing).");
                }

                AfkPositionStorage.remove(uuid);
                AfkPositionStorage.save();
            }, 1L);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // If player quits while AFK, persist pre-AFK location for next join
        if (AfkPositionStorage.isInitialized() && isAfk(player)) {
            Location back = preAfkLocations.get(uuid);
            if (back != null) {
                AfkPositionStorage.put(uuid, back);
                AfkPositionStorage.save();
                debug("Saved pending return for " + player.getName());
            }
        }

        cleanup(player);
    }
}
