package me.koyere.ecoxpert.modules.market;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.DatabaseTransaction;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Material;
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
    private final PriceCalculator priceCalculator;
    // Global price adjustment factors (applied to dynamic prices)
    private volatile double buyPriceFactor = 1.0;  // multiplier for buy prices
    private volatile double sellPriceFactor = 1.0; // multiplier for sell prices
    
    // Cache for market items
    private final Map<Material, MarketItem> itemCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService priceUpdateScheduler;
    private boolean marketOpen = true;
    private volatile boolean initialized = false;
    
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
            // Load market items from database into cache
            loadItemsIntoCache();
            
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
                // Get recent transactions for analysis
                List<MarketTransaction> recentTransactions = getRecentTransactionsSync(100);
                
                // Update prices for each item
                for (MarketItem item : itemCache.values()) {
                    updateItemPrice(item, recentTransactions);
                }
                
                plugin.getLogger().info("Price update completed for " + itemCache.size() + " items");
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update market prices", e);
            }
        });
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
                        SUM(total_amount) as total_volume,
                        AVG(unit_price) as avg_price,
                        COUNT(CASE WHEN created_at >= datetime('now', '-1 day') THEN 1 END) as daily_transactions,
                        SUM(CASE WHEN created_at >= datetime('now', '-1 day') THEN total_amount ELSE 0 END) as daily_volume
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
                    
                    // Estimate market capitalization
                    BigDecimal marketCap = totalVolume.multiply(BigDecimal.valueOf(0.1)); // Simple estimation
                    
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
    
    private MarketTransactionResult processTransaction(Player player, MarketItem item, 
                                                     MarketTransaction.TransactionType type,
                                                     int quantity, BigDecimal unitPrice, 
                                                     BigDecimal totalAmount) {
        try {
            // Start database transaction
            DatabaseTransaction dbTransaction = dataManager.beginTransaction().join();
            
            try {
                // Update player balance
                if (type == MarketTransaction.TransactionType.BUY) {
                    economyManager.removeMoney(player.getUniqueId(), totalAmount, "Market purchase").join();
                } else {
                    economyManager.addMoney(player.getUniqueId(), totalAmount, "Market sale").join();
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
                
                // Commit transaction
                dbTransaction.commit();
                
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
                
                return MarketTransactionResult.success(transaction, message);
                
            } catch (Exception e) {
                dbTransaction.rollback();
                throw e;
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to process market transaction", e);
            return MarketTransactionResult.failure("Transaction processing failed");
        }
    }
    
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
