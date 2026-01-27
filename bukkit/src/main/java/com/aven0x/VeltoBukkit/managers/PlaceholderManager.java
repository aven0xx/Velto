package com.aven0x.VeltoBukkit.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PlaceholderManager {

    // identifier -> resolver(player) => string output
    private static final Map<String, Function<Player, String>> PLACEHOLDERS = new ConcurrentHashMap<>();

    private static boolean expansionRegistered = false;

    private PlaceholderManager() {}

    /**
     * Call once in onEnable().
     * - registers PlaceholderAPI expansion (if available)
     * - registers default Velto placeholders
     */
    public static void init() {
        registerDefaultPlaceholders();
        registerExpansion();
    }

    /**
     * Register/override a placeholder.
     *
     * Example:
     * PlaceholderManager.registerPlaceholder("level", p -> String.valueOf(LevelManager.getLevel(p)));
     *
     * Placeholder becomes: %velto_level%
     */
    public static void registerPlaceholder(@NotNull String identifier,
                                           @NotNull Function<Player, String> resolver) {
        PLACEHOLDERS.put(normalize(identifier), resolver);
    }

    /**
     * Remove a placeholder.
     */
    public static void unregisterPlaceholder(@NotNull String identifier) {
        PLACEHOLDERS.remove(normalize(identifier));
    }

    /**
     * True if this identifier exists (useful for debugging/admin commands).
     */
    public static boolean hasPlaceholder(@NotNull String identifier) {
        return PLACEHOLDERS.containsKey(normalize(identifier));
    }

    /**
     * Registers the placeholders shipped with Velto.
     * Add new defaults here later.
     */
    private static void registerDefaultPlaceholders() {
        // %velto_afk%
        registerPlaceholder("afk", player -> AfkManager.isAfk(player) ? "true" : "false");
    }

    /**
     * Registers the PlaceholderAPI expansion exactly once.
     */
    private static void registerExpansion() {
        if (expansionRegistered) return;

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Bukkit.getLogger().warning("[Velto] PlaceholderAPI not found. Placeholders disabled.");
            return;
        }

        new VeltoExpansion().register();
        expansionRegistered = true;

        Bukkit.getLogger().info("[Velto] Placeholders registered (identifier: %velto_*%).");
    }

    private static String normalize(String identifier) {
        return identifier.toLowerCase().trim();
    }

    private static final class VeltoExpansion extends PlaceholderExpansion {

        @Override
        public @NotNull String getIdentifier() {
            return "velto";
        }

        @Override
        public @NotNull String getAuthor() {
            return "aven0x";
        }

        @Override
        public @NotNull String getVersion() {
            return "1.0.0";
        }

        @Override
        public boolean persist() {
            return true; // survives /papi reload
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
            if (player == null) return "";

            Function<Player, String> resolver = PLACEHOLDERS.get(normalize(identifier));
            if (resolver == null) return null;

            try {
                String out = resolver.apply(player);
                return (out == null) ? "" : out;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[Velto] Placeholder error: %velto_" + identifier + "%");
                t.printStackTrace();
                return "";
            }
        }
    }
}
