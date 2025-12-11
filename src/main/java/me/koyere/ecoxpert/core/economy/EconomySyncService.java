package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.economy.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Periodic balance synchronization against a fallback economy (e.g.,
 * EssentialsX).
 *
 * - Only syncs balances (bank/market/loans are left untouched).
 * - Modes: off, pull (fallback → EcoXpert), push (EcoXpert → fallback),
 * bidirectional.
 * - Simple change detection using last known synced balance to avoid bouncing
 * values.
 */
public class EconomySyncService {

    public enum SyncMode {
        OFF, PULL, PUSH, BIDIRECTIONAL;

        public static SyncMode fromString(String raw) {
            try {
                return SyncMode.valueOf(raw.trim().toUpperCase());
            } catch (Exception ex) {
                return OFF;
            }
        }
    }

    public enum SyncSource {
        LOCAL, FALLBACK
    }

    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    private final Map<UUID, SyncSnapshot> snapshots = new ConcurrentHashMap<>();
    private int taskId = -1;

    public EconomySyncService(EcoXpertPlugin plugin, EconomyManager economyManager,
            ConfigManager configManager, DataManager dataManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.configManager = configManager;
        this.dataManager = dataManager;
    }

    public void start() {
        stop();
        SyncMode mode = getMode();
        if (mode == SyncMode.OFF) {
            plugin.getLogger().info("Economy sync is disabled (economy.sync.mode=off)");
            return;
        }

        int intervalSeconds = Math.max(30, configManager.getConfig().getInt("economy.sync.interval-seconds", 120));
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                () -> safeSyncOnline(mode),
                intervalSeconds * 20L,
                intervalSeconds * 20L).getTaskId();

        plugin.getLogger().info(String.format(
                "Economy sync started (mode=%s, interval=%ss, min-delta=%.2f)",
                mode,
                intervalSeconds,
                getMinDelta()));
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public SyncMode getMode() {
        return SyncMode.fromString(configManager.getConfig().getString("economy.sync.mode", "off"));
    }

    public double getMinDelta() {
        return Math.max(0.0, configManager.getConfig().getDouble("economy.sync.min-delta", 0.01));
    }

    public SyncStatus getStatus() {
        ProviderHandle prov = resolveFallbackProvider().orElse(null);
        String name = prov != null && prov.pluginName != null ? prov.pluginName : "None";
        return new SyncStatus(getMode(), taskId != -1, name, snapshots.size());
    }

    public CompletableFuture<SyncResult> syncOnlineNow() {
        SyncMode mode = getMode();
        if (mode == SyncMode.OFF) {
            return CompletableFuture.completedFuture(SyncResult.disabled());
        }
        return syncPlayers(new java.util.ArrayList<>(Bukkit.getOnlinePlayers()), mode);
    }

    public CompletableFuture<SyncResult> syncPlayerNow(OfflinePlayer player) {
        SyncMode mode = getMode();
        if (mode == SyncMode.OFF) {
            return CompletableFuture.completedFuture(SyncResult.disabled());
        }
        return syncPlayers(java.util.Collections.singletonList(player), mode);
    }

    public CompletableFuture<ImportResult> importAllFromFallback() {
        Optional<ProviderHandle> fallback = resolveFallbackProvider();
        if (fallback.isEmpty()) {
            return CompletableFuture.completedFuture(ImportResult.noneFound());
        }
        Economy fb = fallback.get().provider;
        String providerName = fallback.get().pluginName != null ? fallback.get().pluginName : "Unknown";
        return CompletableFuture.supplyAsync(() -> {
            int imported = 0;
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (!player.hasPlayedBefore())
                    continue;
                try {
                    double fbBal = fb.getBalance(player);
                    if (fbBal < getMinDelta()) {
                        continue;
                    }
                    BigDecimal fbAmount = BigDecimal.valueOf(fbBal).setScale(2, RoundingMode.HALF_UP);
                    economyManager.hasAccount(player.getUniqueId())
                            .thenCompose(has -> has
                                    ? economyManager.setBalance(player.getUniqueId(), fbAmount,
                                            "Imported from " + providerName)
                                    : economyManager.createAccount(player.getUniqueId(), fbAmount)
                                            .thenCompose(v -> economyManager.setBalance(player.getUniqueId(), fbAmount,
                                                    "Imported from " + providerName)))
                            .join();
                    snapshots.put(player.getUniqueId(),
                            new SyncSnapshot(fbAmount, SyncSource.FALLBACK, System.currentTimeMillis()));
                    imported++;
                } catch (Exception ex) {
                    plugin.getLogger()
                            .warning("Failed to import balance for " + player.getName() + ": " + ex.getMessage());
                }
            }
            return new ImportResult(providerName, imported);
        });
    }

    private void safeSyncOnline(SyncMode mode) {
        try {
            syncPlayers(new java.util.ArrayList<>(Bukkit.getOnlinePlayers()), mode).join();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Economy sync failed", ex);
        }
    }

    private CompletableFuture<SyncResult> syncPlayers(Iterable<? extends OfflinePlayer> players, SyncMode mode) {
        Optional<ProviderHandle> fallbackOpt = resolveFallbackProvider();
        if (fallbackOpt.isEmpty()) {
            return CompletableFuture.completedFuture(SyncResult.noProvider());
        }
        ProviderHandle handle = fallbackOpt.get();
        Economy fallback = handle.provider;
        String providerName = handle.pluginName != null ? handle.pluginName : "Unknown";
        double minDelta = getMinDelta();
        SyncCounters counters = new SyncCounters();

        return CompletableFuture.runAsync(() -> {
            for (OfflinePlayer player : players) {
                try {
                    syncPlayerBalance(player, fallback, providerName, mode, minDelta, counters);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Sync error for " + player.getName() + ": " + ex.getMessage());
                }
            }
        }).thenApply(v -> new SyncResult(providerName, counters.pulled, counters.pushed, counters.skipped));
    }

    private void syncPlayerBalance(OfflinePlayer player, Economy fallback, String providerName, SyncMode mode,
            double minDelta, SyncCounters counters) {
        UUID uuid = player.getUniqueId();
        double fbBalRaw = fallback.getBalance(player);
        BigDecimal fbBal = BigDecimal.valueOf(fbBalRaw).setScale(2, RoundingMode.HALF_UP);

        AccountSnapshot localSnapshot = loadLocalSnapshot(uuid).join();
        if (localSnapshot == null) {
            return;
        }

        if (approximatelyEqual(localSnapshot.balance, fbBal, minDelta)) {
            counters.skipped++;
            snapshots.put(uuid, new SyncSnapshot(localSnapshot.balance, SyncSource.LOCAL, System.currentTimeMillis()));
            return;
        }

        SyncSnapshot last = snapshots.get(uuid);
        boolean localChanged = last != null && !approximatelyEqual(localSnapshot.balance, last.lastBalance, minDelta);
        boolean fallbackChanged = last != null && !approximatelyEqual(fbBal, last.lastBalance, minDelta);

        switch (mode) {
            case PULL -> {
                pull(uuid, fbBal, localSnapshot.balance, providerName);
                counters.pulled++;
            }
            case PUSH -> {
                push(player, fbBal, localSnapshot.balance, fallback);
                counters.pushed++;
            }
            case BIDIRECTIONAL -> {
                // Determine direction based on last synced value
                if (localChanged && !fallbackChanged) {
                    push(player, fbBal, localSnapshot.balance, fallback);
                    counters.pushed++;
                } else {
                    pull(uuid, fbBal, localSnapshot.balance, providerName);
                    counters.pulled++;
                }
            }
            default -> counters.skipped++;
        }
    }

    private void pull(UUID uuid, BigDecimal fallbackBalance, BigDecimal currentLocal, String providerName) {
        economyManager.setBalance(uuid, fallbackBalance, "Sync pull from " + providerName).join();
        snapshots.put(uuid, new SyncSnapshot(fallbackBalance, SyncSource.FALLBACK, System.currentTimeMillis()));
        if (plugin.getLogger().isLoggable(Level.FINEST)) {
            plugin.getLogger().finest("Pulled balance for " + uuid + ": " + currentLocal + " -> " + fallbackBalance);
        }
    }

    private void push(OfflinePlayer player, BigDecimal fallbackBalance, BigDecimal currentLocal, Economy fallback) {
        double delta = currentLocal.subtract(fallbackBalance).doubleValue();
        if (Math.abs(delta) < getMinDelta()) {
            return;
        }
        if (delta > 0) {
            fallback.depositPlayer(player, delta);
        } else {
            fallback.withdrawPlayer(player, Math.abs(delta));
        }
        snapshots.put(player.getUniqueId(),
                new SyncSnapshot(currentLocal, SyncSource.LOCAL, System.currentTimeMillis()));
        if (plugin.getLogger().isLoggable(Level.FINEST)) {
            plugin.getLogger()
                    .finest("Pushed balance for " + player.getName() + ": " + fallbackBalance + " -> " + currentLocal);
        }
    }

    private Optional<ProviderHandle> resolveFallbackProvider() {
        try {
            var regs = plugin.getServer().getServicesManager().getRegistrations(Economy.class);
            return regs.stream()
                    .filter(r -> r.getPlugin() != null && !r.getPlugin().getName().toLowerCase().contains("ecoxpert"))
                    .sorted(Comparator.comparing(r -> priorityScore(r.getPlugin().getName())))
                    .map(r -> new ProviderHandle(r.getProvider(), r.getPlugin().getName()))
                    .findFirst();
        } catch (Exception ex) {
            plugin.getLogger().fine("No fallback economy provider found: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private int priorityScore(String pluginName) {
        String lower = pluginName.toLowerCase();
        if (lower.contains("essentials"))
            return 0;
        if (lower.contains("cmi"))
            return 1;
        return 2;
    }

    private CompletableFuture<AccountSnapshot> loadLocalSnapshot(UUID uuid) {
        return economyManager.hasAccount(uuid).thenCompose(has -> {
            CompletableFuture<Void> ensure = has ? CompletableFuture.completedFuture(null)
                    : economyManager.createAccount(uuid, economyManager.getStartingBalance());
            return ensure.thenCompose(v -> dataManager.executeQuery(
                    "SELECT balance, updated_at FROM ecoxpert_accounts WHERE player_uuid = ?",
                    uuid.toString()).thenApply(this::mapAccountSnapshot));
        });
    }

    private AccountSnapshot mapAccountSnapshot(QueryResult qr) {
        try (qr) {
            if (qr.next()) {
                return new AccountSnapshot(
                        qr.getBigDecimal("balance"),
                        qr.getTimestamp("updated_at") != null ? qr.getTimestamp("updated_at").getTime()
                                : System.currentTimeMillis());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to load account snapshot: " + ex.getMessage());
        }
        return null;
    }

    private boolean approximatelyEqual(BigDecimal a, BigDecimal b, double minDelta) {
        return a.subtract(b).abs().doubleValue() < minDelta;
    }

    private record AccountSnapshot(BigDecimal balance, long updatedAt) {
    }

    private record ProviderHandle(Economy provider, String pluginName) {
    }

    private static class SyncSnapshot {
        private final BigDecimal lastBalance;

        SyncSnapshot(BigDecimal lastBalance, SyncSource source, long syncedAt) {
            this.lastBalance = lastBalance;
        }
    }

    public record SyncStatus(SyncMode mode, boolean running, String providerName, int trackedPlayers) {
    }

    public record SyncResult(String providerName, int pulled, int pushed, int skipped) {
        public static SyncResult noProvider() {
            return new SyncResult("None", 0, 0, 0);
        }

        public static SyncResult disabled() {
            return new SyncResult("Disabled", 0, 0, 0);
        }
    }

    public record ImportResult(String providerName, int imported) {
        public static ImportResult noneFound() {
            return new ImportResult("None", 0);
        }
    }

    private static class SyncCounters {
        int pulled = 0;
        int pushed = 0;
        int skipped = 0;
    }
}
