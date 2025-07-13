package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional market management interface
 * 
 * Provides comprehensive market operations with dynamic pricing,
 * transaction management, and inventory integration.
 */
public interface MarketManager {
    
    /**
     * Initialize the market system
     */
    void initialize();
    
    /**
     * Shutdown the market system gracefully
     */
    void shutdown();
    
    /**
     * Check if the market is currently open for trading
     */
    boolean isMarketOpen();
    
    /**
     * Set market open/closed status
     */
    void setMarketOpen(boolean open);
    
    // === Item Management ===
    
    /**
     * Get all available market items
     */
    CompletableFuture<List<MarketItem>> getAllItems();
    
    /**
     * Get market item by material
     */
    CompletableFuture<Optional<MarketItem>> getItem(Material material);
    
    /**
     * Add or update an item in the market
     */
    CompletableFuture<Void> addItem(Material material, BigDecimal basePrice, boolean buyable, boolean sellable);
    
    /**
     * Remove an item from the market
     */
    CompletableFuture<Void> removeItem(Material material);
    
    // === Price Management ===
    
    /**
     * Get current buy price for an item
     */
    CompletableFuture<BigDecimal> getBuyPrice(Material material);
    
    /**
     * Get current sell price for an item
     */
    CompletableFuture<BigDecimal> getSellPrice(Material material);
    
    /**
     * Get price history for an item
     */
    CompletableFuture<List<MarketPriceHistory>> getPriceHistory(Material material, int days);
    
    /**
     * Update item prices based on market activity
     */
    CompletableFuture<Void> updatePrices();
    
    // === Transaction Operations ===
    
    /**
     * Buy items from the market
     */
    CompletableFuture<MarketTransactionResult> buyItem(Player player, Material material, int quantity);
    
    /**
     * Sell items to the market
     */
    CompletableFuture<MarketTransactionResult> sellItem(Player player, Material material, int quantity);
    
    /**
     * Get transaction history for a player
     */
    CompletableFuture<List<MarketTransaction>> getPlayerTransactions(UUID playerUuid, int limit);
    
    /**
     * Get recent market transactions
     */
    CompletableFuture<List<MarketTransaction>> getRecentTransactions(int limit);
    
    // === Market Analytics ===
    
    /**
     * Get market statistics
     */
    CompletableFuture<MarketStatistics> getMarketStatistics();
    
    /**
     * Get top traded items
     */
    CompletableFuture<List<MarketItemStats>> getTopTradedItems(int limit);
    
    /**
     * Get market trends for specific item
     */
    CompletableFuture<MarketTrend> getItemTrend(Material material);
    
    // === Utility Methods ===
    
    /**
     * Check if player has enough money to buy items
     */
    boolean canAfford(Player player, BigDecimal amount);
    
    /**
     * Check if player has enough items to sell
     */
    boolean hasItems(Player player, Material material, int quantity);
    
    /**
     * Count items in player inventory
     */
    int countItems(Player player, Material material);
    
    /**
     * Add items to player inventory
     */
    boolean addItemsToInventory(Player player, ItemStack itemStack);
    
    /**
     * Remove items from player inventory
     */
    boolean removeItemsFromInventory(Player player, Material material, int quantity);
}