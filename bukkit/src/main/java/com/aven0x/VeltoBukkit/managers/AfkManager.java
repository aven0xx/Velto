package com.aven0x.VeltoBukkit.managers;

import com.aven0x.Velto.utils.PlayerUtil;
import com.aven0x.VeltoBukkit.VeltoBukkit;
import com.aven0x.VeltoBukkit.utils.ConfigUtil;
import com.aven0x.VeltoBukkit.utils.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AfkManager implements Listener {

    private static final Map<UUID, Long> lastActivity = new HashMap<>();
    private static final Set<UUID> afkPlayers = new HashSet<>();
    private static final long AFK_TIMEOUT = 300000; // 5 minutes en millisecondes
    private static BukkitRunnable afkChecker;

    // NEW: store the player's location right before teleporting to AFK zone
    private static final Map<UUID, Location> preAfkLocations = new HashMap<>();

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
                        // Joueur devient AFK
                        setAfk(player, true);
                    }
                }
            }
        };

        // Check every 30 seconds
        afkChecker.runTaskTimer(VeltoBukkit.getInstance(), 600L, 600L);
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
        preAfkLocations.clear(); // NEW: also clear stored pre-AFK locations
    }

    /**
     * Update player activity status
     */
    public static void updateActivity(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID uuid = player.getUniqueId();
        lastActivity.put(uuid, System.currentTimeMillis());

        // Si le joueur était AFK, le marquer comme actif
        if (afkPlayers.contains(uuid)) {
            setAfk(player, false);
        }
    }

    /**
     * Check AFK status
     */
    public static boolean isAfk(Player player) {
        return afkPlayers.contains(player.getUniqueId());
    }

    /**
     * Toggle AFK
     */
    public static void setAfk(Player player, boolean afk) {
        UUID uuid = player.getUniqueId();
        boolean wasAfk = afkPlayers.contains(uuid);

        if (afk && !wasAfk) {
            // Devient AFK
            afkPlayers.add(uuid);

            System.out.println("=== DEBUG: Player " + player.getName() + " becoming AFK ===");

            // Teleport player to AFK zone if enabled in config
            boolean afkzoneEnabled = ConfigUtil.isAfkzoneOn();
            System.out.println("DEBUG: AFK Zone enabled: " + afkzoneEnabled);

            if (afkzoneEnabled) {
                // Save current location BEFORE teleporting (clone to avoid mutation)
                try {
                    preAfkLocations.put(uuid, player.getLocation().clone());
                    System.out.println("DEBUG: Saved pre-AFK location for " + player.getName() + ": " + preAfkLocations.get(uuid));
                } catch (Exception e) {
                    System.out.println("DEBUG: Failed to save pre-AFK location for " + player.getName() + ": " + e.getMessage());
                }

                Location afkZone = ConfigUtil.getAfkzone();
                System.out.println("DEBUG: AFK Zone location from config: " + afkZone);

                if (afkZone != null) {
                    System.out.println("DEBUG: AFK Zone world: " + afkZone.getWorld());
                    System.out.println("DEBUG: AFK Zone coordinates: x=" + afkZone.getX() + ", y=" + afkZone.getY() + ", z=" + afkZone.getZ());

                    if (afkZone.getWorld() != null) {
                        System.out.println("DEBUG: Player current location: " + player.getLocation());
                        System.out.println("DEBUG: Attempting teleportation...");

                        try {
                            boolean success = player.teleport(afkZone);
                            System.out.println("DEBUG: Teleport result: " + success);

                            if (!success) {
                                System.out.println("DEBUG: Teleport failed - trying with delay...");

                                // Try with a small delay in case of timing issues
                                Bukkit.getScheduler().runTaskLater(VeltoBukkit.getInstance(), () -> {
                                    boolean delayedSuccess = player.teleport(afkZone);
                                    System.out.println("DEBUG: Delayed teleport result: " + delayedSuccess);
                                }, 1L);
                            }
                        } catch (Exception e) {
                            System.out.println("DEBUG: Exception during teleport: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("DEBUG: ERROR - AFK Zone world is null!");
                    }
                } else {
                    System.out.println("DEBUG: ERROR - AFK Zone location is null!");
                }
            } else {
                System.out.println("DEBUG: AFK Zone is disabled in config");
            }

            System.out.println("=== DEBUG END ===");

            //Keep indent

            // Notification to the player
            Map<String, String> placeholders = Map.of("%player%", player.getName());
            LangUtil.send(player, "afk-enabled", placeholders);

            // Global Notification (if not vanished)
            if (!PlayerUtil.isVanished(player)) {
                LangUtil.sendGlobal("afk-player", placeholders);
            }

        } else if (!afk && wasAfk) {
            // N'est plus AFK
            afkPlayers.remove(uuid);
            lastActivity.put(uuid, System.currentTimeMillis());

            // If AFK zone is on and we have a saved location, try to send them back
            if (ConfigUtil.isAfkzoneOn()) {
                Location back = preAfkLocations.remove(uuid);
                if (back != null) {
                    System.out.println("DEBUG: Restoring " + player.getName() + " to pre-AFK location: " + back);
                    try {
                        boolean success = player.teleport(back);
                        System.out.println("DEBUG: Return teleport result: " + success);
                        if (!success) {
                            Bukkit.getScheduler().runTaskLater(VeltoBukkit.getInstance(), () -> {
                                boolean delayed = player.teleport(back);
                                System.out.println("DEBUG: Delayed return teleport result: " + delayed);
                            }, 1L);
                        }
                    } catch (Exception e) {
                        System.out.println("DEBUG: Exception while returning from AFK: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("DEBUG: No pre-AFK location stored for " + player.getName() + " (maybe AFK zone was disabled or save failed).");
                }
            } else {
                // If AFK zone is OFF now, we don't force a return teleport.
                // Clean any leftover entry
                preAfkLocations.remove(uuid);
            }

            // Notification to the player
            Map<String, String> placeholders = Map.of("%player%", player.getName());
            LangUtil.send(player, "afk-disabled", placeholders);

            // Global Notification (if not vanished)
            if (!PlayerUtil.isVanished(player)) {
                LangUtil.sendGlobal("afk-player-back", placeholders);
            }
        }
    }

    /**
     * Toggle AFK status (for AFK command)
     */
    public static boolean toggleAfk(Player player) {
        boolean newState = !isAfk(player);
        setAfk(player, newState);
        return newState;
    }

    /**
     * Get all AFK players
     */
    public static Set<Player> getAfkPlayers() {
        Set<Player> players = new HashSet<>();
        for (UUID uuid : afkPlayers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                players.add(player);
            }
        }
        return players;
    }

    /**
     * Get Time since a player last activity
     */
    public static long getTimeSinceLastActivity(Player player) {
        UUID uuid = player.getUniqueId();
        long lastTime = lastActivity.getOrDefault(uuid, System.currentTimeMillis());
        return System.currentTimeMillis() - lastTime;
    }

    /**
     * Clean up data when a player disconnect
     */
    public static void cleanup(Player player) {
        UUID uuid = player.getUniqueId();
        lastActivity.remove(uuid);
        afkPlayers.remove(uuid);
        preAfkLocations.remove(uuid); // NEW: avoid stale stored locations
    }

    /**
     * Get AFK Timeout
     */
    public static int getAfkTimeoutSeconds() {
        return (int) (AFK_TIMEOUT / 1000);
    }

    // ===== EVENT LISTENERS =====

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Seulement si le joueur a bougé significativement (pas juste rotation)
        if (event.getFrom().distanceSquared(event.getTo()) > 0.01) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!event.isCancelled()) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!event.isCancelled()) {
            String command = event.getMessage().toLowerCase();

            // Ignorer la commande /afk pour éviter les boucles infinies
            if (!command.startsWith("/afk")) {
                updateActivity(event.getPlayer());
            }
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
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean data on player disconnect
        cleanup(event.getPlayer());
    }
}
