package me.koyere.ecoxpert.modules.market;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.DatabaseTransaction;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Professional implementation of MarketManager
 * 
 * Provides comprehensive market operations with database persistence,
 * dynamic pricing, and asynchronous transaction processing.
 */
public class MarketManagerImpl implements MarketManager {
    
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final TranslationManager translationManager;
    private final ConfigManager configManager;
    private me.koyere.ecoxpert.modules.professions.ProfessionsManager professionsManager; // lazy
    private final PriceCalculator priceCalculator;
    // Global price adjustment factors (applied to dynamic prices)
    private volatile double buyPriceFactor = 1.0;  // multiplier for buy prices
    private volatile double sellPriceFactor = 1.0; // multiplier for sell prices
    // Per-item temporary factors with expiry in millis
    private final Map<Material, ItemFactor> itemFactors = new ConcurrentHashMap<>();

    private static class ItemFactor {
        final double buy;
        final double sell;
        final long expiresAt;
        ItemFactor(double buy, double sell, long expiresAt) {
            this.buy = buy; this.sell = sell; this.expiresAt = expiresAt;
        }
        boolean expired() { return System.currentTimeMillis() > expiresAt; }
    }
    
    // Cache for market items
    private final Map<Material, MarketItem> itemCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService priceUpdateScheduler;
    private boolean marketOpen = true;
    private volatile boolean initialized = false;
    // Slimefun auto-flagging (abundance) trackers
    private final java.util.concurrent.ConcurrentHashMap<Material, java.util.concurrent.atomic.AtomicInteger> sellWindowCounts = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<Material, Long> slimeFlaggedUntil = new java.util.concurrent.ConcurrentHashMap<>();
    
    // Configuration
    private static final int PRICE_UPDATE_INTERVAL_MINUTES = 5;
    private static final int CACHE_REFRESH_INTERVAL_MINUTES = 10;
    private static final int MAX_TRANSACTION_HISTORY = 1000;
    
    public MarketManagerImpl(EcoXpertPlugin plugin, DataManager dataManager, 
                           EconomyManager economyManager, TranslationManager translationManager,
                           ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.translationManager = translationManager;
        this.configManager = configManager;
        this.priceCalculator = new PriceCalculator();
        this.priceUpdateScheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "EcoXpert-Market-" + Thread.currentThread().getId());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing Market System...");
        
        try {
            // Load pricing configuration
            loadPricingConfig();
            // Load market items from database into cache
            loadItemsIntoCache();

            // Optional seeding on empty markets (first run)
            try {
                boolean seedOnEmpty = configManager.getConfig().getBoolean("modules.market.seed-on-empty", true);
                if (seedOnEmpty && itemCache.isEmpty()) {
                    seedDefaultMarketItems();
                    loadItemsIntoCache();
                }
            } catch (Exception ignored) {}
            
            // Schedule price updates
            schedulePriceUpdates();
            
            // Schedule cache refresh
            scheduleCacheRefresh();
            
            this.initialized = true;
            plugin.getLogger().info("Market System initialized successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize Market System", e);
            throw new RuntimeException("Market System initialization failed", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (initialized) {
            plugin.getLogger().info("Shutting down Market System...");
            
            try {
                // Stop scheduled tasks
                priceUpdateScheduler.shutdown();
                if (!priceUpdateScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    priceUpdateScheduler.shutdownNow();
                }
                
                // Clear cache
                itemCache.clear();
                
                this.initialized = false;
                plugin.getLogger().info("Market System shutdown complete");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during Market System shutdown", e);
            }
        }
    }
    
    @Override
    public boolean isMarketOpen() {
        return marketOpen && initialized;
    }
    
    @Override
    public void setMarketOpen(boolean open) {
        this.marketOpen = open;
        plugin.getLogger().info("Market is now " + (open ? "open" : "closed"));
    }

    @Override
    public void setGlobalPriceFactors(double buyFactor, double sellFactor) {
        // Clamp to sane bounds to avoid extremes
        this.buyPriceFactor = Math.max(0.5, Math.min(1.5, buyFactor));
        this.sellPriceFactor = Math.max(0.5, Math.min(1.5, sellFactor));
        plugin.getLogger().info("Market global price factors set: buy=" + this.buyPriceFactor + 
            ", sell=" + this.sellPriceFactor);
    }

    @Override
    public double[] getGlobalPriceFactors() {
        return new double[]{buyPriceFactor, sellPriceFactor};
    }

    @Override
    public void applyTemporaryItemFactors(Map<Material, double[]> factors, int minutes) {
        long expires = System.currentTimeMillis() + minutes * 60_000L;
        for (Map.Entry<Material, double[]> e : factors.entrySet()) {
            double[] f = e.getValue();
            double buy = Math.max(0.5, Math.min(1.5, f[0]));
            double sell = Math.max(0.5, Math.min(1.5, f[1]));
            itemFactors.put(e.getKey(), new ItemFactor(buy, sell, expires));
        }
        // Schedule cleanup
        Bukkit.getScheduler().runTaskLater(plugin, () ->
            itemFactors.entrySet().removeIf(en -> en.getValue().expired()), 20L * 60 * Math.max(1, minutes));
    }
    
    // === Item Management ===
    
    @Override
    public CompletableFuture<List<MarketItem>> getAllItems() {
        return CompletableFuture.supplyAsync(() -> {
            if (!itemCache.isEmpty()) {
                return new ArrayList<>(itemCache.values());
            }
            
            // Fallback to database query if cache is empty
            return loadItemsFromDatabase();
        });
    }
    
    @Override
    public CompletableFuture<Optional<MarketItem>> getItem(Material material) {
        return CompletableFuture.supplyAsync(() -> {
            MarketItem cachedItem = itemCache.get(material);
            if (cachedItem != null) {
                return Optional.of(cachedItem);
            }
            
            // Load from database if not cached
            return loadItemFromDatabase(material);
        });
    }
    
    @Override
    public CompletableFuture<Void> addItem(Material material, BigDecimal basePrice, boolean buyable, boolean sellable) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = """
                    INSERT OR REPLACE INTO ecoxpert_market_items 
                    (material, base_price, current_buy_price, current_sell_price, buyable, sellable, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
                
                BigDecimal sellPrice = basePrice.multiply(BigDecimal.valueOf(0.8));
                
                dataManager.executeUpdate(sql, 
                    material.name(), basePrice, basePrice, sellPrice, 
                    buyable, sellable, Timestamp.valueOf(LocalDateTime.now())
                ).join();
                
                // Update cache
                MarketItem newItem = MarketItem.builder(material, basePrice)
                    .currentBuyPrice(basePrice)
                    .currentSellPrice(sellPrice)
                    .buyable(buyable)
                    .sellable(sellable)
                    .build();
                
                itemCache.put(material, newItem);
                
                plugin.getLogger().info("Added market item: " + material.name());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add market item: " + material.name(), e);
                throw new RuntimeException("Failed to add market item", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> removeItem(Material material) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql = "DELETE FROM ecoxpert_market_items WHERE material = ?";
                dataManager.executeUpdate(sql, material.name()).join();
                
                // Remove from cache
                itemCache.remove(material);
                
                plugin.getLogger().info("Removed market item: " + material.name());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove market item: " + material.name(), e);
                throw new RuntimeException("Failed to remove market item", e);
            }
        });
    }
    
    // === Price Management ===
    
    @Override
    public CompletableFuture<BigDecimal> getBuyPrice(Material material) {
        return getItem(material)
            .thenApply(item -> item.map(MarketItem::getCurrentBuyPrice).orElse(BigDecimal.ZERO));
    }
    
    @Override
    public CompletableFuture<BigDecimal> getSellPrice(Material material) {
        return getItem(material)
            .thenApply(item -> item.map(MarketItem::getCurrentSellPrice).orElse(BigDecimal.ZERO));
    }
    
    @Override
    public CompletableFuture<List<MarketPriceHistory>> getPriceHistory(Material material, int days) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = """
                    SELECT * FROM ecoxpert_market_price_history 
                    WHERE material = ? AND snapshot_time >= datetime('now', '-' || ? || ' days')
                    ORDER BY snapshot_time DESC
                    """;
                
                try (QueryResult result = dataManager.executeQuery(sql, material.name(), days).join()) {
                    List<MarketPriceHistory> history = new ArrayList<>();
                    while (result.next()) {
                        history.add(new MarketPriceHistory(
                            material,
                            result.getBigDecimal("buy_price"),
                            result.getBigDecimal("sell_price"),
                            result.getTimestamp("snapshot_time").toLocalDateTime(),
                            result.getInt("transaction_count"),
                            result.getBigDecimal("volume")
                        ));
                    }
                    return history;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get price history for: " + material.name(), e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> updatePrices() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Safe Mode: skip market updates
                var safe = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
                if (safe != null && safe.isActive()) {
                    plugin.getLogger().info("Safe Mode active: skipping market price updates");
                    return;
                }
                // Refresh pricing config to allow dynamic tuning
                loadPricingConfig();
                // Get recent transactions for analysis
                List<MarketTransaction> recentTransactions = getRecentTransactionsSync(100);
                
                // Update prices for each item
                for (MarketItem item : itemCache.values()) {
                    updateItemPrice(item, recentTransactions);
                }
                
                plugin.getLogger().info("Price update completed for " + itemCache.size() + " items");
                
            } catch (Exception e) {
                var safe = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
                if (safe != null) safe.recordCriticalError();
                plugin.getLogger().log(Level.SEVERE, "Failed to update market prices", e);
            }
        });
    }

    private void loadPricingConfig() {
        try {
            double maxChange;
            double damping;
            int trendHours;
            if (configManager.isSimpleMode()) {
                var root = configManager.getConfig();
                maxChange = root.getDouble("simple.market.max_price_change", 0.15);
                damping = root.getDouble("simple.market.volatility_damping", 0.90);
                trendHours = root.getInt("simple.market.trend_analysis_hours", 24);
            } else {
                var marketCfg = configManager.getModuleConfig("market");
                maxChange = marketCfg.getDouble("pricing.max_price_change", 0.20);
                damping = marketCfg.getDouble("pricing.volatility_damping", 0.85);
                trendHours = marketCfg.getInt("pricing.trend_analysis_hours", 24);
            }
            priceCalculator.configure(maxChange, damping, trendHours);
        } catch (Exception e) {
            // Keep defaults on error
        }
    }
    
    // === Transaction Operations ===
    
    @Override
    public CompletableFuture<MarketTransactionResult> buyItem(Player player, Material material, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isMarketOpen()) {
                return MarketTransactionResult.failure(
                    MarketTransactionResult.TransactionError.MARKET_CLOSED,
                    translationManager.getMessage("market.market-closed")
                );
            }
            
            try {
                Optional<MarketItem> itemOpt = getItem(material).join();
                if (itemOpt.isEmpty() || !itemOpt.get().isBuyable()) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.ITEM_NOT_BUYABLE,
                        translationManager.getMessage("market.item-not-buyable")
                    );
                }
                
                MarketItem item = itemOpt.get();
                BigDecimal unitPrice = item.getCurrentBuyPrice();
                BigDecimal totalCost = unitPrice.multiply(BigDecimal.valueOf(quantity));
                // Apply profession buy factor (discounts) including context
                double profF = getProfessionFactor(player.getUniqueId(), material, true);
                double integF = getIntegrationsFactor(material, true);
                double terrF = getTerritoryFactor(player, material, true);
                double slimeF = getInflationaryMaterialFactor(material, true);
                totalCost = totalCost
                    .multiply(BigDecimal.valueOf(profF))
                    .multiply(BigDecimal.valueOf(integF))
                    .multiply(BigDecimal.valueOf(terrF))
                    .multiply(BigDecimal.valueOf(slimeF))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
                
                // Check if player can afford
                if (!canAfford(player, totalCost)) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.INSUFFICIENT_FUNDS,
                        translationManager.getMessage("market.insufficient-funds", 
                            economyManager.formatMoney(totalCost),
                            economyManager.formatMoney(economyManager.getBalance(player.getUniqueId()).join())
                        )
                    );
                }
                
                // Check inventory space
                ItemStack itemStack = new ItemStack(material, quantity);
                if (!addItemsToInventory(player, itemStack)) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.INVENTORY_FULL,
                        translationManager.getMessage("market.inventory-full")
                    );
                }
                
                // Process transaction
                return processTransaction(player, item, MarketTransaction.TransactionType.BUY, 
                                       quantity, unitPrice, totalCost);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to process buy transaction", e);
                return MarketTransactionResult.failure("System error occurred");
            }
        });
    }
    
    @Override
    public CompletableFuture<MarketTransactionResult> sellItem(Player player, Material material, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isMarketOpen()) {
                return MarketTransactionResult.failure(
                    MarketTransactionResult.TransactionError.MARKET_CLOSED,
                    translationManager.getMessage("market.market-closed")
                );
            }
            
            try {
                Optional<MarketItem> itemOpt = getItem(material).join();
                if (itemOpt.isEmpty() || !itemOpt.get().isSellable()) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.ITEM_NOT_SELLABLE,
                        translationManager.getMessage("market.item-not-sellable")
                    );
                }
                
                MarketItem item = itemOpt.get();
                
                // Check if player has items
                if (!hasItems(player, material, quantity)) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.INSUFFICIENT_ITEMS,
                        translationManager.getMessage("market.not-enough-items", material.name())
                    );
                }
                
                BigDecimal unitPrice = item.getCurrentSellPrice();
                BigDecimal totalEarning = unitPrice.multiply(BigDecimal.valueOf(quantity));
                // Apply profession/integrations/territory/slimefun factors for SELL
                double profF = getProfessionFactor(player.getUniqueId(), material, false);
                double integF = getIntegrationsFactor(material, false);
                double terrF = getTerritoryFactor(player, material, false);
                double slimeF = getInflationaryMaterialFactor(material, false);
                totalEarning = totalEarning
                    .multiply(BigDecimal.valueOf(profF))
                    .multiply(BigDecimal.valueOf(integF))
                    .multiply(BigDecimal.valueOf(terrF))
                    .multiply(BigDecimal.valueOf(slimeF))
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
                
                // Remove items from inventory
                if (!removeItemsFromInventory(player, material, quantity)) {
                    return MarketTransactionResult.failure(
                        MarketTransactionResult.TransactionError.INSUFFICIENT_ITEMS,
                        translationManager.getMessage("market.not-enough-items", material.name())
                    );
                }
                
                // Process transaction
                return processTransaction(player, item, MarketTransaction.TransactionType.SELL, 
                                       quantity, unitPrice, totalEarning);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to process sell transaction", e);
                return MarketTransactionResult.failure("System error occurred");
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MarketTransaction>> getPlayerTransactions(UUID playerUuid, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = """
                    SELECT * FROM ecoxpert_market_transactions 
                    WHERE player_uuid = ? 
                    ORDER BY created_at DESC 
                    LIMIT ?
                    """;
                
                return loadTransactionsFromQuery(sql, playerUuid.toString(), limit);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player transactions", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MarketTransaction>> getRecentTransactions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = """
                    SELECT * FROM ecoxpert_market_transactions 
                    ORDER BY created_at DESC 
                    LIMIT ?
                    """;
                
                return loadTransactionsFromQuery(sql, limit);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get recent transactions", e);
                return Collections.emptyList();
            }
        });
    }
    
    // === Market Analytics ===
    
    @Override
    public CompletableFuture<MarketStatistics> getMarketStatistics() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Count total and active items
                int totalItems = itemCache.size();
                int activeItems = (int) itemCache.values().stream()
                    .filter(MarketItem::isActivelyTraded)
                    .count();
                
                // Get transaction statistics
                String transactionSql = """
                    SELECT 
                        COUNT(*) as total_transactions,
                        COALESCE(SUM(total_amount), 0) as total_volume,
                        COALESCE(AVG(unit_price), 0) as avg_price,
                        COUNT(CASE WHEN created_at >= datetime('now', '-1 day') THEN 1 END) as daily_transactions,
                        COALESCE(SUM(CASE WHEN created_at >= datetime('now', '-1 day') THEN total_amount ELSE 0 END), 0) as daily_volume
                    FROM ecoxpert_market_transactions
                    """;
                
                try (QueryResult result = dataManager.executeQuery(transactionSql).join()) {
                if (result.next()) {
                    long totalTransactions = result.getLong("total_transactions");
                    BigDecimal totalVolume = result.getBigDecimal("total_volume");
                    BigDecimal avgPrice = result.getBigDecimal("avg_price");
                    int dailyTransactions = result.getInt("daily_transactions");
                    BigDecimal dailyVolume = result.getBigDecimal("daily_volume");
                    
                    // Calculate market activity (0.0 to 1.0)
                    double activity = Math.min(1.0, dailyTransactions / 100.0);
                    
                    // Estimate market capitalization (guard against nulls)
                    BigDecimal marketCap = (totalVolume != null ? totalVolume : BigDecimal.ZERO)
                        .multiply(BigDecimal.valueOf(0.1)); // Simple estimation
                    
                    return new MarketStatistics(
                        totalItems, activeItems, totalTransactions, totalVolume,
                        avgPrice, marketCap, LocalDateTime.now(),
                        dailyVolume, dailyTransactions, activity
                    );
                }
                }
                
                // Return empty statistics if no data
                return new MarketStatistics(
                    totalItems, activeItems, 0, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now(),
                    BigDecimal.ZERO, 0, 0.0
                );
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get market statistics", e);
                return new MarketStatistics(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 
                                          BigDecimal.ZERO, LocalDateTime.now(), BigDecimal.ZERO, 0, 0.0);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<MarketItemStats>> getTopTradedItems(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = """
                    SELECT 
                        m.material,
                        m.total_sold,
                        m.total_bought,
                        COALESCE(SUM(CASE WHEN mt.transaction_type = 'sell' THEN mt.total_amount ELSE 0 END), 0) as sell_volume,
                        COALESCE(SUM(CASE WHEN mt.transaction_type = 'buy' THEN mt.total_amount ELSE 0 END), 0) as buy_volume,
                        COALESCE(AVG(CASE WHEN mt.transaction_type = 'sell' THEN mt.unit_price END), 0) as avg_sell_price,
                        COALESCE(AVG(CASE WHEN mt.transaction_type = 'buy' THEN mt.unit_price END), 0) as avg_buy_price
                    FROM ecoxpert_market_items m
                    LEFT JOIN ecoxpert_market_transactions mt ON m.material = mt.material
                    GROUP BY m.material
                    ORDER BY (m.total_sold + m.total_bought) DESC
                    LIMIT ?
                    """;
                
                try (QueryResult result = dataManager.executeQuery(sql, limit).join()) {
                List<MarketItemStats> stats = new ArrayList<>();
                int rank = 1;
                
                while (result.next()) {
                    Material material = Material.valueOf(result.getString("material"));
                    stats.add(new MarketItemStats(
                        material,
                        result.getInt("total_sold"),
                        result.getInt("total_bought"),
                        result.getBigDecimal("sell_volume"),
                        result.getBigDecimal("buy_volume"),
                        result.getBigDecimal("avg_sell_price"),
                        result.getBigDecimal("avg_buy_price"),
                        rank++
                    ));
                }
                
                return stats;
                }
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get top traded items", e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public CompletableFuture<MarketTrend> getItemTrend(Material material) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get recent price history
                List<MarketPriceHistory> history = getPriceHistory(material, 7).join();
                
                if (history.size() < 2) {
                    // Not enough data for trend analysis
                    Optional<MarketItem> itemOpt = getItem(material).join();
                    if (itemOpt.isPresent()) {
                        MarketItem item = itemOpt.get();
                        return new MarketTrend(
                            material, MarketTrend.TrendDirection.STABLE,
                            item.getCurrentBuyPrice(), BigDecimal.ZERO, BigDecimal.ZERO,
                            0.1, 0.0, LocalDateTime.now(),
                            "Insufficient data for trend analysis"
                        );
                    }
                }
                
                // Analyze trend from price history
                MarketPriceHistory latest = history.get(0);
                MarketPriceHistory previous = history.get(Math.min(1, history.size() - 1));
                
                BigDecimal currentPrice = latest.getBuyPrice();
                BigDecimal previousPrice = previous.getBuyPrice();
                BigDecimal priceChange = currentPrice.subtract(previousPrice);
                BigDecimal changePercentage = BigDecimal.ZERO;
                
                if (!previousPrice.equals(BigDecimal.ZERO)) {
                    changePercentage = priceChange.divide(previousPrice, 4, BigDecimal.ROUND_HALF_UP)
                                                 .multiply(BigDecimal.valueOf(100));
                }
                
                // Calculate volatility from price history
                double volatility = calculateVolatilityFromHistory(history);
                
                // Determine trend direction
                MarketTrend.TrendDirection direction;
                double changePercent = changePercentage.doubleValue();
                
                if (volatility > 0.3) {
                    direction = MarketTrend.TrendDirection.VOLATILE;
                } else if (changePercent > 5.0) {
                    direction = MarketTrend.TrendDirection.STRONG_UPWARD;
                } else if (changePercent > 1.0) {
                    direction = MarketTrend.TrendDirection.UPWARD;
                } else if (changePercent < -5.0) {
                    direction = MarketTrend.TrendDirection.STRONG_DOWNWARD;
                } else if (changePercent < -1.0) {
                    direction = MarketTrend.TrendDirection.DOWNWARD;
                } else {
                    direction = MarketTrend.TrendDirection.STABLE;
                }
                
                // Calculate momentum (simplified)
                double momentum = Math.min(1.0, Math.max(-1.0, changePercent / 10.0));
                
                return new MarketTrend(
                    material, direction, currentPrice, priceChange, changePercentage,
                    volatility, momentum, LocalDateTime.now(), null
                );
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get item trend for: " + material.name(), e);
                return new MarketTrend(
                    material, MarketTrend.TrendDirection.STABLE, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0.0, 0.0, 
                    LocalDateTime.now(), "Error calculating trend"
                );
            }
        });
    }
    
    // === Utility Methods ===
    
    @Override
    public boolean canAfford(Player player, BigDecimal amount) {
        try {
            BigDecimal balance = economyManager.getBalance(player.getUniqueId()).join();
            return balance.compareTo(amount) >= 0;
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to check player balance", e);
            return false;
        }
    }
    
    @Override
    public boolean hasItems(Player player, Material material, int quantity) {
        return countItems(player, material) >= quantity;
    }
    
    @Override
    public int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    @Override
    public boolean addItemsToInventory(Player player, ItemStack itemStack) {
        // Check if inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            // Check if existing stacks can accommodate
            int remainingAmount = itemStack.getAmount();
            for (ItemStack existing : player.getInventory().getContents()) {
                if (existing != null && existing.isSimilar(itemStack)) {
                    int maxStack = existing.getMaxStackSize();
                    int canAdd = maxStack - existing.getAmount();
                    if (canAdd > 0) {
                        remainingAmount -= Math.min(canAdd, remainingAmount);
                        if (remainingAmount <= 0) break;
                    }
                }
            }
            if (remainingAmount > 0) return false;
        }
        
        // Add items to inventory
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
        return leftover.isEmpty();
    }
    
    @Override
    public boolean removeItemsFromInventory(Player player, Material material, int quantity) {
        int remaining = quantity;
        ItemStack[] contents = player.getInventory().getContents();
        
        // First pass: count available items
        int available = countItems(player, material);
        if (available < quantity) return false;
        
        // Second pass: remove items
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int removeAmount = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - removeAmount);
                if (item.getAmount() <= 0) {
                    contents[i] = null;
                }
                remaining -= removeAmount;
            }
        }
        
        player.getInventory().setContents(contents);
        return remaining == 0;
    }
    
    // === Private Helper Methods ===
    
    private void loadItemsIntoCache() {
        List<MarketItem> items = loadItemsFromDatabase();
        itemCache.clear();
        for (MarketItem item : items) {
            itemCache.put(item.getMaterial(), item);
        }
        plugin.getLogger().info("Loaded " + items.size() + " market items into cache");
    }

    private double getProfessionFactor(java.util.UUID uuid, boolean isBuy) {
        try {
            if (professionsManager == null) {
                professionsManager = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            }
            var roleOpt = professionsManager.getRole(uuid).join();
            if (roleOpt.isEmpty()) return 1.0;
            String key = "roles." + roleOpt.get().name().toLowerCase() + "." + (isBuy ? "buy_factor" : "sell_factor");
            var profCfg = configManager.getModuleConfig("professions");
            double v = profCfg.getDouble(key, 1.0);
            // level bonuses
            int level = professionsManager.getLevel(uuid).join();
            int maxLevel = profCfg.getInt("max_level", 5);
            level = Math.max(1, Math.min(level, maxLevel));
            double perLevel = profCfg.getDouble("roles." + roleOpt.get().name().toLowerCase() + "." + (isBuy ? "buy_bonus_per_level" : "sell_bonus_per_level"), 0.0);
            if (isBuy) {
                v = v * (1.0 - (perLevel * (level - 1))); // more discount with level
            } else {
                v = v * (1.0 + (perLevel * (level - 1))); // more bonus with level
            }
            // clamp reasonable range
            if (v < 0.5) v = 0.5; if (v > 1.5) v = 1.5;
            return v;
        } catch (Exception ignored) { return 1.0; }
    }

    // Overload including contextual bonuses by category and active events
    private double getProfessionFactor(java.util.UUID uuid, org.bukkit.Material material, boolean isBuy) {
        double v = getProfessionFactor(uuid, isBuy);
        try {
            if (professionsManager == null) {
                professionsManager = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            }
            var roleOpt = professionsManager.getRole(uuid).join();
            if (roleOpt.isEmpty()) return v;
            String roleKey = "roles." + roleOpt.get().name().toLowerCase();
            var profCfg = configManager.getModuleConfig("professions");

            // Category bonuses (multiply if material belongs to multiple categories)
            for (String cat : resolveCategories(material)) {
                String ckey = roleKey + ".category_bonuses." + cat.toLowerCase() + "." + (isBuy ? "buy_factor" : "sell_factor");
                double cf = profCfg.getDouble(ckey, 1.0);
                v *= cf;
            }

            // Event bonuses for active events
            var events = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.events.EconomicEventEngine.class);
            if (events != null) {
                for (var ev : events.getActiveEvents().values()) {
                    String ekey = roleKey + ".event_bonuses." + ev.getType().name() + "." + (isBuy ? "buy_factor" : "sell_factor");
                    double ef = profCfg.getDouble(ekey, 1.0);
                    v *= ef;
                }
            }

            // Clamp
            if (v < 0.5) v = 0.5; if (v > 1.5) v = 1.5;
            return v;
        } catch (Exception ignored) {
            return v;
        }
    }

    private double getIntegrationsFactor(org.bukkit.Material material, boolean isBuy) {
        try {
            var sr = plugin.getServiceRegistry();
            var integ = sr.getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class);
            var cfg = configManager.getModuleConfig("integrations");
            if (cfg == null || !cfg.getBoolean("adjustments.enabled", true)) return 1.0;
            double factor = 1.0;
            if (integ != null && integ.hasJobs()) {
                factor *= cfg.getDouble("adjustments.jobs." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            }
            if (integ != null && integ.hasTowny()) {
                factor *= cfg.getDouble("adjustments.towny." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            }
            if (integ != null && integ.hasLands()) {
                factor *= cfg.getDouble("adjustments.lands." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            }
            if (integ != null && integ.hasSlimefun()) {
                factor *= cfg.getDouble("adjustments.slimefun." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            }
            if (integ != null && integ.hasMcMMO()) {
                factor *= cfg.getDouble("adjustments.mcmmo." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            }
            if (factor < 0.9) factor = 0.9; if (factor > 1.1) factor = 1.1; // gentle clamp
            return factor;
        } catch (Exception ignored) { return 1.0; }
    }

    private double getInflationaryMaterialFactor(org.bukkit.Material material, boolean isBuy) {
        try {
            var cfg = configManager.getModuleConfig("integrations");
            var list = cfg.getStringList("slimefun.inflationary.materials");
            if (list == null || list.isEmpty()) return 1.0;
            for (String m : list) {
                if (material.name().equalsIgnoreCase(m.trim())) {
                    double bf = cfg.getDouble("slimefun.inflationary.buy_factor", 1.02);
                    double sf = cfg.getDouble("slimefun.inflationary.sell_factor", 0.98);
                    double f = isBuy ? bf : sf;
                    if (f < 0.8) f = 0.8; if (f > 1.2) f = 1.2;
                    return f;
                }
            }
            return 1.0;
        } catch (Exception e) { return 1.0; }
    }

    private double getTerritoryFactor(org.bukkit.entity.Player player, org.bukkit.Material material, boolean isBuy) {
        try {
            var integ = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class);
            var cfg = configManager.getModuleConfig("integrations");
            if (cfg == null || !cfg.getBoolean("territory.enabled", true)) return 1.0;
            String region = "";
            String land = "";
            String town = "";
            if (integ != null) {
                if (integ.hasWorldGuard()) region = integ.getWorldGuardRegions(player);
                if (integ.hasLands()) land = integ.getLandsLand(player);
                if (integ.hasTowny()) town = integ.getTownyTown(player);
            }
            double factor = 1.0;
            // WorldGuard rules
            var rules = cfg.getConfigurationSection("territory.worldguard.rules");
            if (rules != null && region != null && !region.isEmpty()) {
                java.util.List<String> ids = java.util.Arrays.asList(region.split(","));
                for (String key : rules.getKeys(false)) {
                    String pattern = key; // use key as pattern
                    for (String id : ids) {
                        if (globMatches(pattern, id)) {
                            double bf = rules.getDouble(key + ".buy_factor", 1.0);
                            double sf = rules.getDouble(key + ".sell_factor", 1.0);
                            factor *= isBuy ? bf : sf;
                        }
                    }
                }
            }
            // Lands rule (single)
            var lands = cfg.getConfigurationSection("territory.lands");
            if (lands != null && land != null && !land.isEmpty()) {
                for (String key : lands.getKeys(false)) {
                    if (globMatches(key, land)) {
                        double bf = lands.getDouble(key + ".buy_factor", 1.0);
                        double sf = lands.getDouble(key + ".sell_factor", 1.0);
                        factor *= isBuy ? bf : sf;
                    }
                }
                // default
                if (lands.isConfigurationSection("default") && (factor == 1.0)) {
                    double bf = lands.getDouble("default.buy_factor", 1.0);
                    double sf = lands.getDouble("default.sell_factor", 1.0);
                    factor *= isBuy ? bf : sf;
                }
            }
            // Towny rules (single town name) + population scaling (phase 2)
            var towny = cfg.getConfigurationSection("territory.towny");
            if (towny != null) {
                var townRules = towny.getConfigurationSection("rules");
                if (townRules != null && town != null && !town.isEmpty()) {
                    for (String key : townRules.getKeys(false)) {
                        if (globMatches(key, town)) {
                            double bf = townRules.getDouble(key + ".buy_factor", 1.0);
                            double sf = townRules.getDouble(key + ".sell_factor", 1.0);
                            factor *= isBuy ? bf : sf;
                        }
                    }
                }
                if (towny.isConfigurationSection("default")) {
                    double bf = towny.getDouble("default.buy_factor", 1.0);
                    double sf = towny.getDouble("default.sell_factor", 1.0);
                    factor *= isBuy ? bf : sf;
                }
                // Population scaling
                var scaling = towny.getConfigurationSection("scaling");
                if (scaling != null && scaling.getBoolean("enabled", false)) {
                    int residents = getTownyResidentsCountSafe(player, town);
                    java.util.List<?> thresholds = scaling.getList("thresholds");
                    if (thresholds != null && !thresholds.isEmpty()) {
                        double chosen = 1.0;
                        for (Object o : thresholds) {
                            if (o instanceof java.util.Map<?,?> m) {
                                Object r = m.get("residents");
                                int req = r instanceof Number ? ((Number) r).intValue() : Integer.parseInt(String.valueOf(r));
                                if (residents >= req) {
                                    Object bf = m.get("buy_factor");
                                    Object sf = m.get("sell_factor");
                                    double fac = isBuy
                                        ? (bf instanceof Number ? ((Number) bf).doubleValue() : Double.parseDouble(String.valueOf(bf)))
                                        : (sf instanceof Number ? ((Number) sf).doubleValue() : Double.parseDouble(String.valueOf(sf)));
                                    chosen = fac; // keep last matching (highest)
                                }
                            }
                        }
                        factor *= chosen;
                    }
                }
            }
            if (factor < 0.8) factor = 0.8; if (factor > 1.2) factor = 1.2;
            return factor;
        } catch (Exception e) { return 1.0; }
    }

    // Best-effort Towny residents count via reflection, using player or town name
    private int getTownyResidentsCountSafe(org.bukkit.entity.Player player, String townName) {
        try {
            Class<?> apiClass = Class.forName("com.palmergames.bukkit.towny.TownyAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object town = null;
            // First try by player
            try { town = apiClass.getMethod("getTown", org.bukkit.entity.Player.class).invoke(api, player); }
            catch (NoSuchMethodException ignored) { /* try by name next */ }
            if (town == null && townName != null && !townName.isEmpty()) {
                try { town = apiClass.getMethod("getTown", String.class).invoke(api, townName); }
                catch (NoSuchMethodException ignored) { /* older API? */ }
            }
            if (town == null) return 0;
            // Try multiple ways to get residents count
            try { return (int) town.getClass().getMethod("getNumResidents").invoke(town); }
            catch (NoSuchMethodException ignored) {}
            try { return (int) town.getClass().getMethod("getResidentsCount").invoke(town); }
            catch (NoSuchMethodException ignored) {}
            try {
                Object coll = town.getClass().getMethod("getResidents").invoke(town);
                if (coll instanceof java.util.Collection<?>) return ((java.util.Collection<?>) coll).size();
            } catch (NoSuchMethodException ignored) {}
            return 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private boolean globMatches(String glob, String text) {
        try {
            String regex = glob.replace("*", ".*").replace("?", ".");
            return text.matches("(?i)" + regex);
        } catch (Exception e) { return false; }
    }

    private java.util.List<String> resolveCategories(org.bukkit.Material material) {
        java.util.List<String> list = new java.util.ArrayList<>();
        try {
            var marketCfg = configManager.getModuleConfig("market");
            var section = marketCfg.getConfigurationSection("categories");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    java.util.List<String> mats = marketCfg.getStringList("categories." + key + ".materials");
                    for (String m : mats) {
                        if (material.name().equalsIgnoreCase(m.trim())) {
                            list.add(key.toUpperCase());
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void seedDefaultMarketItems() {
        plugin.getLogger().info("Seeding default market items (first run)…");
        Map<Material, BigDecimal> defaults = new LinkedHashMap<>();
        defaults.put(Material.WHEAT, new BigDecimal("2.00"));
        defaults.put(Material.BREAD, new BigDecimal("5.00"));
        defaults.put(Material.APPLE, new BigDecimal("3.00"));
        defaults.put(Material.COAL, new BigDecimal("8.00"));
        defaults.put(Material.IRON_INGOT, new BigDecimal("20.00"));
        defaults.put(Material.COPPER_INGOT, new BigDecimal("10.00"));
        defaults.put(Material.GOLD_INGOT, new BigDecimal("30.00"));
        defaults.put(Material.REDSTONE, new BigDecimal("6.00"));
        defaults.put(Material.LAPIS_LAZULI, new BigDecimal("12.00"));
        defaults.put(Material.DIAMOND, new BigDecimal("150.00"));
        defaults.put(Material.EMERALD, new BigDecimal("120.00"));
        defaults.put(Material.NETHERITE_INGOT, new BigDecimal("500.00"));

        int seeded = 0;
        for (Map.Entry<Material, BigDecimal> e : defaults.entrySet()) {
            try {
                addItem(e.getKey(), e.getValue(), true, true).join();
                seeded++;
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to seed item " + e.getKey() + ": " + ex.getMessage());
            }
        }
        plugin.getLogger().info("Seeded " + seeded + " default market items");
    }
    
    private List<MarketItem> loadItemsFromDatabase() {
        try {
            String sql = "SELECT * FROM ecoxpert_market_items ORDER BY material";
            try (QueryResult result = dataManager.executeQuery(sql).join()) {
            List<MarketItem> items = new ArrayList<>();
            
            while (result.next()) {
                Material material = Material.valueOf(result.getString("material"));
                items.add(MarketItem.builder(material, result.getBigDecimal("base_price"))
                    .currentBuyPrice(result.getBigDecimal("current_buy_price"))
                    .currentSellPrice(result.getBigDecimal("current_sell_price"))
                    .buyable(result.getBoolean("buyable"))
                    .sellable(result.getBoolean("sellable"))
                    .totalSold(result.getInt("total_sold"))
                    .totalBought(result.getInt("total_bought"))
                    .priceVolatility(result.getBigDecimal("price_volatility"))
                    .lastPriceUpdate(result.getTimestamp("last_price_update").toLocalDateTime())
                    .build());
            }
            
            return items;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load market items from database", e);
            return Collections.emptyList();
        }
    }
    
    private Optional<MarketItem> loadItemFromDatabase(Material material) {
        try {
            String sql = "SELECT * FROM ecoxpert_market_items WHERE material = ?";
            try (QueryResult result = dataManager.executeQuery(sql, material.name()).join()) {
            
            if (result.next()) {
                MarketItem item = MarketItem.builder(material, result.getBigDecimal("base_price"))
                    .currentBuyPrice(result.getBigDecimal("current_buy_price"))
                    .currentSellPrice(result.getBigDecimal("current_sell_price"))
                    .buyable(result.getBoolean("buyable"))
                    .sellable(result.getBoolean("sellable"))
                    .totalSold(result.getInt("total_sold"))
                    .totalBought(result.getInt("total_bought"))
                    .priceVolatility(result.getBigDecimal("price_volatility"))
                    .lastPriceUpdate(result.getTimestamp("last_price_update").toLocalDateTime())
                    .build();
                
                // Cache the item
                itemCache.put(material, item);
                return Optional.of(item);
            }
            
            return Optional.empty();
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load market item: " + material.name(), e);
            return Optional.empty();
        }
    }
    
    private void schedulePriceUpdates() {
        priceUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                updatePrices().join();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during scheduled price update", e);
            }
        }, PRICE_UPDATE_INTERVAL_MINUTES, PRICE_UPDATE_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        plugin.getLogger().info("Scheduled price updates every " + PRICE_UPDATE_INTERVAL_MINUTES + " minutes");
    }
    
    private void scheduleCacheRefresh() {
        priceUpdateScheduler.scheduleAtFixedRate(() -> {
            try {
                loadItemsIntoCache();
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error during scheduled cache refresh", e);
            }
        }, CACHE_REFRESH_INTERVAL_MINUTES, CACHE_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
        
        plugin.getLogger().info("Scheduled cache refresh every " + CACHE_REFRESH_INTERVAL_MINUTES + " minutes");
    }
    
    private void updateItemPrice(MarketItem item, List<MarketTransaction> recentTransactions) {
        try {
            // Calculate new prices using PriceCalculator
            PriceCalculator.MarketPriceUpdate priceUpdate = 
                priceCalculator.calculatePriceUpdate(item, recentTransactions);
            
            // Apply global market factors for intelligent interventions
            BigDecimal adjustedBuy = priceUpdate.getNewBuyPrice()
                .multiply(BigDecimal.valueOf(buyPriceFactor));
            BigDecimal adjustedSell = priceUpdate.getNewSellPrice()
                .multiply(BigDecimal.valueOf(sellPriceFactor));

            // Apply per-item factor if present and not expired
            ItemFactor f = itemFactors.get(item.getMaterial());
            if (f != null && !f.expired()) {
                adjustedBuy = adjustedBuy.multiply(BigDecimal.valueOf(f.buy));
                adjustedSell = adjustedSell.multiply(BigDecimal.valueOf(f.sell));
            } else if (f != null && f.expired()) {
                itemFactors.remove(item.getMaterial());
            }

            // Compute change percentages for API event
            java.math.BigDecimal oldBuy = item.getCurrentBuyPrice();
            java.math.BigDecimal oldSell = item.getCurrentSellPrice();
            double buyDelta = safeRelativeChange(oldBuy, adjustedBuy);
            double sellDelta = safeRelativeChange(oldSell, adjustedSell);

            // Update database
            String sql = """
                UPDATE ecoxpert_market_items 
                SET current_buy_price = ?, current_sell_price = ?, 
                    price_volatility = ?, last_price_update = ?, updated_at = ?
                WHERE material = ?
                """;
            
            dataManager.executeUpdate(sql,
                adjustedBuy,
                adjustedSell,
                BigDecimal.valueOf(priceUpdate.getVolatility()),
                Timestamp.valueOf(priceUpdate.getUpdateTime()),
                Timestamp.valueOf(LocalDateTime.now()),
                item.getMaterial().name()
            ).join();
            
            // Update cache
            MarketItem updatedItem = item.withPrices(
                adjustedBuy, 
                adjustedSell
            );
            itemCache.put(item.getMaterial(), updatedItem);
            
            // Record price history
            recordPriceHistory(priceUpdate);

            // Fire market price change event if above threshold
            try {
                double threshold = 0.15; // 15% default
                try {
                    var cfg = configManager.getModuleConfig("market");
                    threshold = cfg.getDouble("api.events.price_change_threshold_percent", 0.15);
                } catch (Exception ignored) {}
                if (Math.max(Math.abs(buyDelta), Math.abs(sellDelta)) >= threshold) {
                    java.math.BigDecimal nb = adjustedBuy;
                    java.math.BigDecimal ns = adjustedSell;
                    var mat = item.getMaterial();
                    var ts = java.time.Instant.now();
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () ->
                        org.bukkit.Bukkit.getPluginManager().callEvent(
                            new me.koyere.ecoxpert.api.events.MarketPriceChangeEvent(mat, oldBuy, nb, oldSell, ns, priceUpdate.getVolatility(), ts)
                        )
                    );
                }
            } catch (Exception ignored) {}
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to update price for " + item.getMaterial().name(), e);
        }
    }
    
    private void recordPriceHistory(PriceCalculator.MarketPriceUpdate priceUpdate) {
        try {
            String sql = """
                INSERT INTO ecoxpert_market_price_history 
                (material, buy_price, sell_price, snapshot_time)
                VALUES (?, ?, ?, ?)
                """;
            
            dataManager.executeUpdate(sql,
                priceUpdate.getMaterial().name(),
                priceUpdate.getNewBuyPrice(),
                priceUpdate.getNewSellPrice(),
                Timestamp.valueOf(priceUpdate.getUpdateTime())
            );
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, 
                "Failed to record price history for " + priceUpdate.getMaterial().name(), e);
        }
    }

    private double safeRelativeChange(java.math.BigDecimal oldVal, java.math.BigDecimal newVal) {
        try {
            if (oldVal == null || newVal == null) return 0.0;
            if (oldVal.compareTo(java.math.BigDecimal.ZERO) == 0) {
                // Relative to small epsilon to avoid div by zero
                return newVal.subtract(oldVal).doubleValue() / 0.01;
            }
            return newVal.subtract(oldVal)
                .divide(oldVal.abs(), 6, java.math.RoundingMode.HALF_UP)
                .doubleValue();
        } catch (Exception e) { return 0.0; }
    }

    /**
     * Apply a small immediate price adjustment after a trade to reflect demand/supply.
     * Buys nudge buy price up and sells nudge sell price down, capped by configured max change.
     */
    private void immediateAdjustAfterTrade(MarketItem item, MarketTransaction.TransactionType type, int quantity) {
        try {
            var cfg = configManager.getModuleConfig("market");
            double maxChange = cfg.getDouble("pricing.max_price_change", 0.20);
            // Compute a tiny per-tx delta: 0.1% per 10 units traded, capped to half of max change
            double baseDelta = Math.min(maxChange / 2.0, Math.max(0.0, (quantity / 10.0) * 0.001));
            if (baseDelta <= 0.0) return;

            java.math.BigDecimal newBuy = item.getCurrentBuyPrice();
            java.math.BigDecimal newSell = item.getCurrentSellPrice();

            if (type == MarketTransaction.TransactionType.BUY) {
                newBuy = newBuy.multiply(java.math.BigDecimal.valueOf(1.0 + baseDelta))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            } else {
                newSell = newSell.multiply(java.math.BigDecimal.valueOf(1.0 - baseDelta))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            }

            // Persist and update cache
            String sql = "UPDATE ecoxpert_market_items SET current_buy_price = ?, current_sell_price = ?, updated_at = CURRENT_TIMESTAMP WHERE material = ?";
            dataManager.executeUpdate(sql, newBuy, newSell, item.getMaterial().name()).join();
            itemCache.put(item.getMaterial(), item.withPrices(newBuy, newSell));
        } catch (Exception ignored) {}
    }
    
    private MarketTransactionResult processTransaction(Player player, MarketItem item, 
                                                     MarketTransaction.TransactionType type,
                                                     int quantity, BigDecimal unitPrice, 
                                                     BigDecimal totalAmount) {
        try {
            // Start database transaction (auto-close to avoid leaks)
            try (DatabaseTransaction dbTransaction = dataManager.beginTransaction().join()) {
                // Ensure account exists and adjust balance WITHIN the same transaction for atomicity
                String playerId = player.getUniqueId().toString();
                BigDecimal starting = economyManager.getStartingBalance();

                // Load current balance (if any)
                BigDecimal currentBalance = BigDecimal.ZERO;
                try (var result = dbTransaction.executeQuery(
                        "SELECT balance FROM ecoxpert_accounts WHERE player_uuid = ?",
                        playerId).join()) {
                    if (result.next()) {
                        BigDecimal b = result.getBigDecimal("balance");
                        currentBalance = (b != null ? b : BigDecimal.ZERO);
                    } else {
                        // Create account if missing
                        dbTransaction.executeUpdate(
                            "INSERT OR IGNORE INTO ecoxpert_accounts (player_uuid, balance) VALUES (?, ?)",
                            playerId, starting).join();
                        currentBalance = starting;
                    }
                }

                if (type == MarketTransaction.TransactionType.BUY) {
                    // Check sufficient funds atomically
                    if (currentBalance.compareTo(totalAmount) < 0) {
                        return MarketTransactionResult.failure(
                            MarketTransactionResult.TransactionError.INSUFFICIENT_FUNDS,
                            translationManager.getMessage("market.insufficient-funds",
                                economyManager.formatMoney(totalAmount),
                                economyManager.formatMoney(currentBalance))
                        );
                    }
                    // Debit balance
                    dbTransaction.executeUpdate(
                        "UPDATE ecoxpert_accounts SET balance = balance - ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                        totalAmount, playerId).join();
                    // Log economy transaction
                    dbTransaction.executeUpdate(
                        "INSERT INTO ecoxpert_transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)",
                        playerId, null, totalAmount, "WITHDRAWAL", "Market purchase").join();
                } else { // SELL
                    // Credit balance
                    dbTransaction.executeUpdate(
                        "UPDATE ecoxpert_accounts SET balance = balance + ?, updated_at = CURRENT_TIMESTAMP WHERE player_uuid = ?",
                        totalAmount, playerId).join();
                    // Log economy transaction
                    dbTransaction.executeUpdate(
                        "INSERT INTO ecoxpert_transactions (from_uuid, to_uuid, amount, type, description) VALUES (?, ?, ?, ?, ?)",
                        null, playerId, totalAmount, "DEPOSIT", "Market sale").join();
                }
                
                // Create transaction record
                MarketTransaction transaction = MarketTransaction.builder()
                    .player(player.getUniqueId(), player.getName())
                    .material(item.getMaterial())
                    .type(type)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .totalAmount(totalAmount)
                    .build();
                
                // Insert transaction into database
                String transactionSql = """
                    INSERT INTO ecoxpert_market_transactions 
                    (player_uuid, player_name, material, transaction_type, quantity, 
                     unit_price, total_amount, description, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                dbTransaction.executeUpdate(transactionSql,
                    player.getUniqueId().toString(),
                    player.getName(),
                    item.getMaterial().name(),
                    type.getDisplayName(),
                    quantity,
                    unitPrice,
                    totalAmount,
                    transaction.getDescription(),
                    Timestamp.valueOf(transaction.getTimestamp())
                );
                
                // Update item statistics
                String updateItemSql = """
                    UPDATE ecoxpert_market_items 
                    SET total_sold = total_sold + ?, total_bought = total_bought + ?, updated_at = ?
                    WHERE material = ?
                    """;
                
                int soldIncrement = type == MarketTransaction.TransactionType.SELL ? quantity : 0;
                int boughtIncrement = type == MarketTransaction.TransactionType.BUY ? quantity : 0;
                
                dbTransaction.executeUpdate(updateItemSql,
                    soldIncrement,
                    boughtIncrement,
                    Timestamp.valueOf(LocalDateTime.now()),
                    item.getMaterial().name()
                );
                
                // Commit transaction and release connection
                dbTransaction.commit().join();
                
                // Update cache
                MarketItem updatedItem = item.withUpdatedStats(soldIncrement, boughtIncrement);
                itemCache.put(item.getMaterial(), updatedItem);
                
                // Send success message
                String messageKey = type == MarketTransaction.TransactionType.BUY ? 
                    "market.item-bought" : "market.item-sold";
                String message = translationManager.getMessage(messageKey,
                    quantity, item.getMaterial().name().toLowerCase().replace('_', ' '),
                    economyManager.formatMoney(totalAmount)
                );
                
                // Professions XP (async best-effort)
                try {
                    if (professionsManager == null) {
                        professionsManager = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
                    }
                    var profCfg = configManager.getModuleConfig("professions");
                    int perTx = type == MarketTransaction.TransactionType.BUY ?
                        profCfg.getInt("xp.per_buy", 1) : profCfg.getInt("xp.per_sell", 2);
                    int per100 = type == MarketTransaction.TransactionType.BUY ?
                        profCfg.getInt("xp.per_100_money_buy", 0) : profCfg.getInt("xp.per_100_money_sell", 1);
                    int blocks = BigDecimal.ZERO.compareTo(totalAmount) < 0 ?
                        totalAmount.divide(new java.math.BigDecimal("100"), 0, java.math.RoundingMode.DOWN).intValue() : 0;
                    int xpDelta = Math.max(0, perTx + (blocks * per100));

                    if (xpDelta > 0) {
                        int prevLevel = professionsManager.getLevel(player.getUniqueId()).join();
                        professionsManager.addXp(player.getUniqueId(), xpDelta).thenAccept(newLevel -> {
                            try {
                                // Notify XP gain
                                String xpKey = type == MarketTransaction.TransactionType.BUY ?
                                    "professions.xp.gained.buy" : "professions.xp.gained.sell";
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    player.sendMessage(translationManager.getMessage("prefix") +
                                        translationManager.getMessage(xpKey, xpDelta));
                                });
                                // Notify level up
                                if (newLevel > prevLevel) {
                                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                                        player.sendMessage(translationManager.getMessage("prefix") +
                                            translationManager.getMessage("professions.levelup", newLevel));
                                    });
                                }
                            } catch (Exception ignored) {}
                        });
                    }
                } catch (Exception ignored) {}

                // Immediate micro-adjustment of price based on this trade
                try { immediateAdjustAfterTrade(updatedItem, type, quantity); } catch (Exception ignored) {}
                // Slimefun auto-flagging for abundance (SELL-heavy)
                try { maybeFlagSlimefunAbundance(type, item.getMaterial(), quantity); } catch (Exception ignored) {}

                return MarketTransactionResult.success(transaction, message);

            } catch (Exception e) {
                // Automatic rollback will occur on close if not committed
                throw e;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process market transaction", e);
            return MarketTransactionResult.failure("Transaction processing failed");
        }
    }

    private void maybeFlagSlimefunAbundance(MarketTransaction.TransactionType type, Material material, int quantity) {
        try {
            // Only consider SELLs for abundance
            if (type != MarketTransaction.TransactionType.SELL) return;
            var integ = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class);
            if (integ == null || !integ.hasSlimefun()) return;
            var cfg = configManager.getModuleConfig("integrations");
            var sec = cfg.getConfigurationSection("slimefun.auto_flagging");
            if (sec == null || !sec.getBoolean("enabled", false)) return;

            int windowMin = Math.max(1, sec.getInt("window_minutes", 10));
            int threshold = Math.max(1, sec.getInt("sell_threshold", 256));
            int flagMinutes = Math.max(1, sec.getInt("flag_minutes", 30));
            double flagBuy = sec.getDouble("flag_buy_factor", 1.02);
            double flagSell = sec.getDouble("flag_sell_factor", 0.98);

            long now = System.currentTimeMillis();
            // Windowed counter
            java.util.concurrent.atomic.AtomicInteger counter = sellWindowCounts.computeIfAbsent(material, m -> new java.util.concurrent.atomic.AtomicInteger(0));
            long windowStart = slimeFlaggedUntil.getOrDefault(Material.AIR, 0L); // dummy read to avoid new map; we'll track window per material via a simple scheme
            // Simplify: reset every windowMin using modulus of current time
            // On each call, if last reset > windowMin, reset counter. We'll store last reset in slimeFlaggedUntil map with negated key is hacky; avoid complexity: reset roughly by using a time bucket.
            long bucket = now / (windowMin * 60_000L);
            // Use a secondary map keyed by material name + bucket via compute logic
            String key = material.name() + "#" + bucket;
            // If first time in this bucket, reset counter
            if (sellWindowCounts.putIfAbsent(material, counter) == null) {
                // nothing; initial
            }
            // For simplicity, whenever bucket changes (detected by absence in slimeFlaggedUntil for this key), reset counter
            if (!bucketKeySeen(key)) {
                counter.set(0);
                markBucketSeen(key);
            }
            int newCount = counter.addAndGet(quantity);

            Long flaggedUntil = slimeFlaggedUntil.get(material);
            boolean alreadyFlagged = flaggedUntil != null && flaggedUntil > now;
            if (!alreadyFlagged && newCount >= threshold) {
                // Flag material for flagMinutes using itemFactors and internal map for transactional factor
                java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
                factors.put(material, new double[]{flagBuy, flagSell});
                applyTemporaryItemFactors(factors, flagMinutes);
                slimeFlaggedUntil.put(material, now + flagMinutes * 60_000L);
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("SLIMEFUN FLAG - Material " + material.name() + " flagged for " + flagMinutes + " min (sell threshold=" + threshold + ")");
                }
            }
        } catch (Exception ignored) {}
    }

    // Simple seen-bucket tracking (best-effort)
    private final java.util.Set<String> seenBuckets = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private boolean bucketKeySeen(String k) { return seenBuckets.contains(k); }
    private void markBucketSeen(String k) { seenBuckets.add(k); }
    
    private List<MarketTransaction> getRecentTransactionsSync(int limit) {
        try {
            String sql = """
                SELECT * FROM ecoxpert_market_transactions 
                ORDER BY created_at DESC 
                LIMIT ?
                """;
            
            return loadTransactionsFromQuery(sql, limit);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get recent transactions", e);
            return Collections.emptyList();
        }
    }
    
    private List<MarketTransaction> loadTransactionsFromQuery(String sql, Object... params) {
        try {
            try (QueryResult result = dataManager.executeQuery(sql, params).join()) {
            List<MarketTransaction> transactions = new ArrayList<>();
            
            while (result.next()) {
                transactions.add(MarketTransaction.builder()
                    .transactionId(result.getLong("id"))
                    .player(
                        UUID.fromString(result.getString("player_uuid")),
                        result.getString("player_name")
                    )
                    .material(Material.valueOf(result.getString("material")))
                    .type(MarketTransaction.TransactionType.fromString(result.getString("transaction_type")))
                    .quantity(result.getInt("quantity"))
                    .unitPrice(result.getBigDecimal("unit_price"))
                    .totalAmount(result.getBigDecimal("total_amount"))
                    .timestamp(result.getTimestamp("created_at").toLocalDateTime())
                    .description(result.getString("description"))
                    .build());
            }
            
            return transactions;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load transactions from query", e);
            return Collections.emptyList();
        }
    }
    
    private double calculateVolatilityFromHistory(List<MarketPriceHistory> history) {
        if (history.size() < 2) return 0.1;
        
        List<BigDecimal> prices = history.stream()
            .map(MarketPriceHistory::getAveragePrice)
            .toList();
        
        // Calculate average
        BigDecimal average = prices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, BigDecimal.ROUND_HALF_UP);
        
        // Calculate variance
        BigDecimal variance = prices.stream()
            .map(price -> price.subtract(average).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, BigDecimal.ROUND_HALF_UP);
        
        // Return normalized volatility
        double standardDeviation = Math.sqrt(variance.doubleValue());
        double volatility = standardDeviation / average.doubleValue();
        
        return Math.min(0.5, Math.max(0.01, volatility));
    }
}
