package me.koyere.ecoxpert.modules.integrations.discord;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.market.MarketItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Listens to economic events and sends Discord notifications
 * 
 * Monitors transactions, market changes, inflation, and other economic events
 * to send real-time notifications to Discord channels.
 */
public class DiscordEventListener implements Listener {

    private final EcoXpertPlugin plugin;
    private final DiscordSRVIntegration integration;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final InflationManager inflationManager;

    // Track previous prices for change detection
    private final Map<org.bukkit.Material, BigDecimal> previousPrices;

    // Track last inflation rate
    private double lastInflationRate;

    public DiscordEventListener(EcoXpertPlugin plugin, DiscordSRVIntegration integration) {
        this.plugin = plugin;
        this.integration = integration;
        this.economyManager = plugin.getServiceRegistry().getInstance(EconomyManager.class);
        this.marketManager = plugin.getServiceRegistry().getInstance(MarketManager.class);
        this.inflationManager = plugin.getServiceRegistry().getInstance(InflationManager.class);
        this.previousPrices = new ConcurrentHashMap<>();
        this.lastInflationRate = 0.0;

        // Initialize price tracking
        initializePriceTracking();

        // Schedule periodic checks
        schedulePeriodicChecks();
    }

    /**
     * Initialize price tracking for all market items
     */
    private void initializePriceTracking() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var items = marketManager.getAllItems().join();
                for (MarketItem item : items) {
                    previousPrices.put(item.getMaterial(), item.getCurrentBuyPrice());
                }
                plugin.getLogger().info("Discord price tracking initialized for " + items.size() + " items");
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to initialize Discord price tracking", e);
            }
        });
    }

    /**
     * Schedule periodic checks for market changes and inflation
     */
    private void schedulePeriodicChecks() {
        // Check market prices every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkMarketPrices, 6000L, 6000L);

        // Check inflation every 10 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkInflation, 12000L, 12000L);

        // Daily report at configured time (check every hour)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkDailyReport, 72000L, 72000L);
    }

    /**
     * Check for significant market price changes
     */
    private void checkMarketPrices() {
        if (!integration.isAvailable())
            return;

        try {
            var items = marketManager.getAllItems().join();

            for (MarketItem item : items) {
                BigDecimal currentPrice = item.getCurrentBuyPrice();
                BigDecimal previousPrice = previousPrices.get(item.getMaterial());

                if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
                    previousPrices.put(item.getMaterial(), currentPrice);
                    continue;
                }

                // Calculate percentage change
                BigDecimal change = currentPrice.subtract(previousPrice);
                double changePercent = change.divide(previousPrice, 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

                // Send notification if change is significant
                if (Math.abs(changePercent) >= 20.0) { // Threshold will be checked in integration
                    integration.sendMarketChangeNotification(
                            item.getMaterial().name(),
                            previousPrice.doubleValue(),
                            currentPrice.doubleValue(),
                            changePercent);

                    // Update tracked price
                    previousPrices.put(item.getMaterial(), currentPrice);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking market prices for Discord", e);
        }
    }

    /**
     * Check inflation rate
     */
    private void checkInflation() {
        if (!integration.isAvailable())
            return;

        try {
            double currentRate = inflationManager.getInflationRate();

            // Only notify if rate changed significantly or is above threshold
            if (Math.abs(currentRate - lastInflationRate) > 1.0 || currentRate > 5.0) {
                String severity = getSeverity(currentRate);
                integration.sendInflationAlert(currentRate, severity);
                lastInflationRate = currentRate;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking inflation for Discord", e);
        }
    }

    /**
     * Check if it's time for daily report
     */
    private void checkDailyReport() {
        if (!integration.isAvailable())
            return;

        try {
            // Get current time
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime targetTime = java.time.LocalTime.of(20, 0); // Default 8 PM

            // Check if within 1 hour window
            if (Math.abs(java.time.Duration.between(now, targetTime).toMinutes()) <= 60) {
                sendDailyReport();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error checking daily report time for Discord", e);
        }
    }

    /**
     * Send daily economic report
     */
    private void sendDailyReport() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var stats = marketManager.getMarketStatistics().join();
                double inflationRate = inflationManager.getInflationRate();

                // Get top player
                var topBalances = economyManager.getTopBalances(1).join();
                String topPlayer = "None";
                double topBalance = 0.0;

                if (!topBalances.isEmpty()) {
                    var entry = topBalances.get(0);
                    @SuppressWarnings("deprecation")
                    OfflinePlayer player = Bukkit.getOfflinePlayer(entry.playerUuid());
                    topPlayer = player.getName() != null ? player.getName() : "Unknown";
                    topBalance = entry.balance().doubleValue();
                }

                // Calculate average balance (simplified)
                double avgBalance = stats.getTotalVolume().doubleValue() / Math.max(1, stats.getTotalTransactions());

                integration.sendDailyReport(
                        (int) stats.getTotalTransactions(),
                        stats.getTotalVolume().doubleValue(),
                        avgBalance,
                        inflationRate,
                        topPlayer,
                        topBalance);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending daily report to Discord", e);
            }
        });
    }

    /**
     * Monitor large transactions
     * This would be called from EconomyManager when a transaction occurs
     */
    public void onLargeTransaction(UUID playerId, double amount, String type) {
        if (!integration.isAvailable())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                @SuppressWarnings("deprecation")
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
                String playerName = player.getName() != null ? player.getName() : "Unknown";

                integration.sendLargeTransactionNotification(playerName, amount, type);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending transaction notification to Discord", e);
            }
        });
    }

    /**
     * Monitor economic crises
     * This would be called from InflationManager when a crisis is detected
     */
    public void onEconomicCrisis(String crisisType, String description, String recommendation) {
        if (!integration.isAvailable())
            return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                integration.sendCrisisAlert(crisisType, description, recommendation);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error sending crisis alert to Discord", e);
            }
        });
    }

    /**
     * Get inflation severity
     */
    private String getSeverity(double rate) {
        if (rate < 0)
            return "Deflation";
        if (rate < 2)
            return "Low";
        if (rate < 5)
            return "Moderate";
        if (rate < 10)
            return "High";
        return "Critical";
    }
}
