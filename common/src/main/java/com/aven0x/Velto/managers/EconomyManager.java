package com.aven0x.Velto.managers;

import com.aven0x.Velto.VeltoPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EconomyManager {

    private EconomyManager() {}

    private static volatile boolean enabled = true;
    private static volatile String currencyNameSingular = "Dollar";
    private static volatile String currencyNamePlural = "Dollars";
    private static volatile String currencySymbol = "$";
    private static volatile int decimalPlaces = 2;
    private static volatile double startingBalance = 0.0;
    private static volatile boolean vaultEnabled = true;

    public static void load() {
        File file = new File(VeltoPlugin.get().getDataFolder(), "economy.yml");
        if (!file.exists()) {
            VeltoPlugin.get().saveResource("economy.yml", false);
        }
        FileConfiguration c = YamlConfiguration.loadConfiguration(file);

        enabled = c.getBoolean("enabled", true);
        currencyNameSingular = c.getString("currency.name-singular", "Dollar");
        currencyNamePlural = c.getString("currency.name-plural", "Dollars");
        currencySymbol = c.getString("currency.symbol", "$");
        decimalPlaces = Math.max(0, c.getInt("currency.decimal-places", 2));
        startingBalance = Math.max(0.0, c.getDouble("starting-balance", 0.0));
        vaultEnabled = c.getBoolean("vault.enabled", true);

        if (enabled) {
            VeltoPlugin.get().getLogger().info("Economy module enabled (currency: " + currencyNamePlural
                    + ", Vault integration requested: " + vaultEnabled + ").");
        } else {
            VeltoPlugin.get().getLogger().info("Economy module disabled in economy.yml.");
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isVaultEnabled() {
        return vaultEnabled;
    }

    public static String getCurrencyNameSingular() {
        return currencyNameSingular;
    }

    public static String getCurrencyNamePlural() {
        return currencyNamePlural;
    }

    public static String getCurrencySymbol() {
        return currencySymbol;
    }

    public static int getDecimalPlaces() {
        return decimalPlaces;
    }

    public static double getStartingBalance() {
        return startingBalance;
    }

    public static String format(double amount) {
        return currencySymbol + String.format("%,." + decimalPlaces + "f", amount);
    }

    // === Balances (stored per-player in userdata, persisted immediately on change) ===

    public static double getBalance(UUID uuid) {
        return UserdataManager.getData(uuid).getDouble("economy.balance", startingBalance);
    }

    public static void setBalance(UUID uuid, double amount) {
        UserdataManager.set(uuid, "economy.balance", Math.max(0.0, amount));
        UserdataManager.save(uuid);
    }

    public static void add(UUID uuid, double amount) {
        if (amount <= 0) return;
        setBalance(uuid, getBalance(uuid) + amount);
    }

    // Forced admin subtraction; clamps at zero instead of failing.
    public static void subtract(UUID uuid, double amount) {
        if (amount <= 0) return;
        setBalance(uuid, getBalance(uuid) - amount);
    }

    // Strict withdrawal used for transfers: fails (no change) if funds are insufficient.
    public static boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return true;
        double balance = getBalance(uuid);
        if (balance < amount) return false;
        setBalance(uuid, balance - amount);
        return true;
    }

    public static boolean transfer(UUID from, UUID to, double amount) {
        if (!withdraw(from, amount)) return false;
        add(to, amount);
        return true;
    }

    public static void resetBalance(UUID uuid) {
        setBalance(uuid, startingBalance);
    }

    /**
     * Every known player's balance, highest first — the backing data for {@code /baltop}.
     *
     * Merges the union of currently-loaded players and every on-disk userdata file, preferring a
     * loaded player's live in-memory balance (so unsaved changes count) and falling back to a
     * direct, non-caching disk read for offline players. Because the offline reads never populate
     * {@link UserdataManager}'s cache, scanning the whole player base here doesn't leak cache
     * entries (see DATA_STORAGE.md). All the reads it performs are thread-safe, so callers should
     * run this off the main thread and only touch Bukkit/messaging back on the main thread.
     */
    public static List<Map.Entry<UUID, Double>> getSortedBalances() {
        Set<UUID> all = new HashSet<>(UserdataManager.listStoredUuids());
        all.addAll(UserdataManager.getCachedUuids());

        List<Map.Entry<UUID, Double>> balances = new ArrayList<>(all.size());
        for (UUID uuid : all) {
            Double live = UserdataManager.getLoadedDouble(uuid, "economy.balance");
            double balance = (live != null)
                    ? live
                    : UserdataManager.readDoubleFromDisk(uuid, "economy.balance", startingBalance);
            balances.add(Map.entry(uuid, balance));
        }

        balances.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return balances;
    }
}
