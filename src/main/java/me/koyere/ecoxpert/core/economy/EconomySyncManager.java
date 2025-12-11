package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Economy Sync Manager
 * 
 * Handles synchronization between EcoXpert and other economy providers
 * when operating in compatibility mode. Ensures data consistency without
 * disrupting existing server economies.
 */
public class EconomySyncManager {

    private final EcoXpertPlugin plugin;
    private final EconomyManager ecoXpertEconomy;
    private final Economy fallbackEconomy;

    private final Map<UUID, BigDecimal> lastKnownBalances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastSyncTimes = new ConcurrentHashMap<>();

    private boolean syncEnabled = false;
    private int syncTaskId = -1;
    private final long SYNC_INTERVAL_TICKS = 20L * 60L; // 1 minute
    private final long MIN_SYNC_INTERVAL_MS = 5000L; // 5 seconds minimum between syncs

    public EconomySyncManager(EcoXpertPlugin plugin, EconomyManager ecoXpertEconomy,
            Economy fallbackEconomy) {
        this.plugin = plugin;
        this.ecoXpertEconomy = ecoXpertEconomy;
        this.fallbackEconomy = fallbackEconomy;
    }

    /**
     * Start synchronization between economy providers
     * 
     * @return true if sync started successfully
     */
    public boolean startSync() {
        if (syncEnabled) {
            plugin.getLogger().info("Economy sync already running");
            return true;
        }

        if (fallbackEconomy == null) {
            plugin.getLogger().warning("Cannot start economy sync - no fallback economy provider");
            return false;
        }

        try {
            // Initial sync
            performInitialSync();

            // Schedule periodic sync
            syncTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    this::performPeriodicSync, SYNC_INTERVAL_TICKS, SYNC_INTERVAL_TICKS).getTaskId();

            syncEnabled = true;
            plugin.getLogger().info("Economy synchronization started with " +
                    fallbackEconomy.getClass().getSimpleName());

            return true;

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start economy sync", e);
            return false;
        }
    }

    /**
     * Stop synchronization
     */
    public void stopSync() {
        if (!syncEnabled) {
            return;
        }

        if (syncTaskId != -1) {
            Bukkit.getScheduler().cancelTask(syncTaskId);
            syncTaskId = -1;
        }

        syncEnabled = false;
        lastKnownBalances.clear();
        lastSyncTimes.clear();

        plugin.getLogger().info("Economy synchronization stopped");
    }

    /**
     * Perform initial synchronization - import existing balances
     */
    private void performInitialSync() {
        plugin.getLogger().info("Performing initial economy synchronization...");

        try {
            // Get all online players for initial sync
            for (OfflinePlayer player : Bukkit.getOnlinePlayers()) {
                syncPlayerBalance(player, true);
            }

            plugin.getLogger().info("Initial economy synchronization completed");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during initial economy sync", e);
        }
    }

    /**
     * Perform periodic synchronization
     */
    private void performPeriodicSync() {
        if (!syncEnabled) {
            return;
        }

        try {
            // Sync online players
            for (OfflinePlayer player : Bukkit.getOnlinePlayers()) {
                syncPlayerBalance(player, false);
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during periodic economy sync", e);
        }
    }

    /**
     * Synchronize a specific player's balance
     * 
     * @param player    Player to sync
     * @param forceSync Force sync even if recently synced
     */
    private void syncPlayerBalance(OfflinePlayer player, boolean forceSync) {
        UUID playerId = player.getUniqueId();

        // Check if we synced this player recently
        if (!forceSync) {
            Long lastSync = lastSyncTimes.get(playerId);
            if (lastSync != null &&
                    (System.currentTimeMillis() - lastSync) < MIN_SYNC_INTERVAL_MS) {
                return; // Skip - synced too recently
            }
        }

        try {
            // Get balance from fallback economy
            double fallbackBalance = fallbackEconomy.getBalance(player);
            BigDecimal fallbackBalanceBD = BigDecimal.valueOf(fallbackBalance);

            // Get our last known balance
            BigDecimal lastKnown = lastKnownBalances.get(playerId);

            // Check if balance changed in fallback economy
            if (lastKnown == null || !lastKnown.equals(fallbackBalanceBD)) {

                // Update our economy to match
                CompletableFuture<Void> syncFuture = ecoXpertEconomy.hasAccount(playerId)
                        .thenCompose(hasAccount -> {
                            if (!hasAccount) {
                                return ecoXpertEconomy.createAccount(playerId, fallbackBalanceBD);
                            } else {
                                return ecoXpertEconomy.setBalance(playerId, fallbackBalanceBD,
                                        "Sync from " + fallbackEconomy.getClass().getSimpleName());
                            }
                        });

                syncFuture.thenRun(() -> {
                    // Update our tracking
                    lastKnownBalances.put(playerId, fallbackBalanceBD);
                    lastSyncTimes.put(playerId, System.currentTimeMillis());

                    if (plugin.getLogger().isLoggable(Level.FINE)) {
                        plugin.getLogger().fine("Synced balance for " + player.getName() +
                                ": " + ecoXpertEconomy.formatMoney(fallbackBalanceBD));
                    }
                }).exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to sync balance for " + player.getName() +
                            ": " + throwable.getMessage());
                    return null;
                });
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Error syncing balance for player " + player.getName(), e);
        }
    }

    /**
     * Manually sync a specific player
     * 
     * @param player Player to sync
     * @return CompletableFuture that completes when sync is done
     */
    public CompletableFuture<Void> syncPlayer(OfflinePlayer player) {
        return CompletableFuture.runAsync(() -> {
            syncPlayerBalance(player, true);
        });
    }

    /**
     * Import balances from fallback economy for all known players
     * 
     * @return CompletableFuture that completes when import is done
     */
    public CompletableFuture<Integer> importBalancesFromFallback() {
        return CompletableFuture.supplyAsync(() -> {
            int importedCount = 0;

            try {
                plugin.getLogger().info("Starting balance import from fallback economy...");

                // Import from all players who have played before
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.hasPlayedBefore()) {
                        try {
                            double balance = fallbackEconomy.getBalance(player);
                            if (balance > 0) {
                                BigDecimal balanceBD = BigDecimal.valueOf(balance);

                                // Create or update account in our system
                                ecoXpertEconomy.hasAccount(player.getUniqueId())
                                        .thenCompose(hasAccount -> {
                                            if (!hasAccount) {
                                                return ecoXpertEconomy.createAccount(player.getUniqueId(), balanceBD);
                                            } else {
                                                return ecoXpertEconomy.setBalance(player.getUniqueId(), balanceBD,
                                                        "Imported from fallback economy");
                                            }
                                        }).join();

                                lastKnownBalances.put(player.getUniqueId(), balanceBD);
                                importedCount++;
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to import balance for " +
                                    player.getName() + ": " + e.getMessage());
                        }
                    }
                }

                plugin.getLogger().info("Balance import completed. Imported " + importedCount + " accounts");

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during balance import", e);
            }

            return importedCount;
        });
    }

    /**
     * Get statistics about synchronization
     * 
     * @return SyncStatistics object
     */
    public SyncStatistics getStatistics() {
        return new SyncStatistics(
                syncEnabled,
                lastKnownBalances.size(),
                lastSyncTimes.size(),
                fallbackEconomy != null ? fallbackEconomy.getClass().getSimpleName() : "None");
    }

    // Getters
    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public Economy getFallbackEconomy() {
        return fallbackEconomy;
    }

    /**
     * Synchronization statistics
     */
    public static class SyncStatistics {
        private final boolean syncEnabled;
        private final int trackedBalances;
        private final int syncedPlayers;
        private final String fallbackProviderName;

        public SyncStatistics(boolean syncEnabled, int trackedBalances,
                int syncedPlayers, String fallbackProviderName) {
            this.syncEnabled = syncEnabled;
            this.trackedBalances = trackedBalances;
            this.syncedPlayers = syncedPlayers;
            this.fallbackProviderName = fallbackProviderName;
        }

        // Getters
        public boolean isSyncEnabled() {
            return syncEnabled;
        }

        public int getTrackedBalances() {
            return trackedBalances;
        }

        public int getSyncedPlayers() {
            return syncedPlayers;
        }

        public String getFallbackProviderName() {
            return fallbackProviderName;
        }

        @Override
        public String toString() {
            return String.format("SyncStats{enabled=%s, tracked=%d, synced=%d, provider=%s}",
                    syncEnabled, trackedBalances, syncedPlayers, fallbackProviderName);
        }
    }
}