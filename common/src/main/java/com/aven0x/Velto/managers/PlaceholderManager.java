package com.aven0x.Velto.managers;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class PlaceholderManager {

    // identifier -> resolver(player) => string output
    private static final Map<String, Function<Player, String>> PLACEHOLDERS = new ConcurrentHashMap<>();

    // prefix -> resolver(player, argument) => string output; argument is whatever follows the prefix.
    // Used for parameterized placeholders like %velto_kit_cooldown_<name>%.
    private static final Map<String, BiFunction<Player, String, String>> PREFIX_PLACEHOLDERS = new ConcurrentHashMap<>();

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
     * Register/override a parameterized placeholder matched by prefix.
     *
     * When an incoming identifier isn't an exact match, it's tested against every
     * registered prefix; the first match invokes the resolver with the remainder of the
     * identifier (after the prefix) as its argument.
     *
     * Example:
     * PlaceholderManager.registerPrefixPlaceholder("kit_cooldown_",
     *         (p, kit) -> KitManager.formatCooldown(KitManager.getCooldownRemaining(p.getUniqueId(), kit)));
     *
     * Placeholder becomes: %velto_kit_cooldown_daily% -> resolver(player, "daily")
     *
     * Keep prefixes non-overlapping: if one prefix is itself a prefix of another, which one
     * wins is undefined (registry iteration order).
     */
    public static void registerPrefixPlaceholder(@NotNull String prefix,
                                                 @NotNull BiFunction<Player, String, String> resolver) {
        PREFIX_PLACEHOLDERS.put(normalize(prefix), resolver);
    }

    /**
     * Remove a placeholder.
     */
    public static void unregisterPlaceholder(@NotNull String identifier) {
        PLACEHOLDERS.remove(normalize(identifier));
    }

    /**
     * Remove a parameterized (prefix) placeholder.
     */
    public static void unregisterPrefixPlaceholder(@NotNull String prefix) {
        PREFIX_PLACEHOLDERS.remove(normalize(prefix));
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

        // === Homes ===
        // %velto_homes_count% -> number of homes the player has set
        registerPlaceholder("homes_count",
                player -> String.valueOf(HomeManager.getHomeNames(player.getUniqueId()).size()));
        // %velto_homes_list% -> comma-separated list of the player's home names
        registerPlaceholder("homes_list",
                player -> String.join(", ", HomeManager.getHomeNames(player.getUniqueId())));

        // === Economy ===
        // All economy placeholders resolve to "" while the economy module is disabled,
        // so they respond live to /veltoreload toggling economy.yml's `enabled`.
        // %velto_balance% -> raw numeric balance, formatted to the configured decimal places
        registerPlaceholder("balance", player -> {
            if (!EconomyManager.isEnabled()) return "";
            return String.format("%." + EconomyManager.getDecimalPlaces() + "f",
                    EconomyManager.getBalance(player.getUniqueId()));
        });
        // %velto_balance_formatted% -> balance with currency symbol and thousands separators
        registerPlaceholder("balance_formatted", player -> {
            if (!EconomyManager.isEnabled()) return "";
            return EconomyManager.format(EconomyManager.getBalance(player.getUniqueId()));
        });
        // %velto_currency_symbol%
        registerPlaceholder("currency_symbol",
                player -> EconomyManager.isEnabled() ? EconomyManager.getCurrencySymbol() : "");
        // %velto_currency_name% -> plural currency name
        registerPlaceholder("currency_name",
                player -> EconomyManager.isEnabled() ? EconomyManager.getCurrencyNamePlural() : "");
        // %velto_currency_name_singular%
        registerPlaceholder("currency_name_singular",
                player -> EconomyManager.isEnabled() ? EconomyManager.getCurrencyNameSingular() : "");

        // === God mode ===
        // %velto_god% -> whether the player currently has god mode on
        registerPlaceholder("god", player -> GodManager.isGod(player) ? "true" : "false");

        // === AFK stats ===
        // %velto_afk_time% -> whole seconds since the player's last activity
        registerPlaceholder("afk_time",
                player -> String.valueOf(AfkManager.getTimeSinceLastActivity(player) / 1000L));
        // %velto_afk_timeout% -> configured AFK timeout, in seconds
        registerPlaceholder("afk_timeout",
                player -> String.valueOf(AfkManager.getAfkTimeoutSeconds()));
        // %velto_afk_count% -> number of players currently AFK server-wide
        registerPlaceholder("afk_count",
                player -> String.valueOf(AfkManager.getAfkPlayers().size()));

        registerParameterizedPlaceholders();
    }

    /**
     * Parameterized (prefix-matched) placeholders, where the text after the prefix is an
     * argument such as a kit name or a home name.
     */
    private static void registerParameterizedPlaceholders() {
        // %velto_kit_cooldown_<kit>% -> remaining cooldown, formatted (e.g. "1h 30m", or "0s" if ready)
        registerPrefixPlaceholder("kit_cooldown_", (player, kit) ->
                KitManager.formatCooldown(KitManager.getCooldownRemaining(player.getUniqueId(), kit)));

        // %velto_has_home_<name>% -> true/false whether the player has a home by that name
        registerPrefixPlaceholder("has_home_", (player, name) ->
                HomeManager.hasHome(player.getUniqueId(), name) ? "true" : "false");
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

            String key = normalize(identifier);

            // Exact match first.
            Function<Player, String> resolver = PLACEHOLDERS.get(key);
            if (resolver != null) {
                return safeResolve(identifier, () -> resolver.apply(player));
            }

            // Then parameterized prefix matches (e.g. kit_cooldown_<name>).
            for (Map.Entry<String, BiFunction<Player, String, String>> entry : PREFIX_PLACEHOLDERS.entrySet()) {
                String prefix = entry.getKey();
                if (key.startsWith(prefix) && key.length() > prefix.length()) {
                    // Argument preserves the original case (home/kit names may be case-sensitive).
                    String arg = identifier.trim().substring(prefix.length());
                    BiFunction<Player, String, String> pResolver = entry.getValue();
                    return safeResolve(identifier, () -> pResolver.apply(player, arg));
                }
            }

            return null;
        }

        private static String safeResolve(String identifier, java.util.function.Supplier<String> resolver) {
            try {
                String out = resolver.get();
                return (out == null) ? "" : out;
            } catch (Throwable t) {
                Bukkit.getLogger().severe("[Velto] Placeholder error: %velto_" + identifier + "%");
                t.printStackTrace();
                return "";
            }
        }
    }
}
