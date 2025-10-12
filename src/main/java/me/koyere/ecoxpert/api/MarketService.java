package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Market analytics and pricing service
 */
public interface MarketService {
    /**
     * Get current market price for an item
     * @param material Item material
     * @param type Price type (BUY or SELL)
     * @return Current price
     */
    CompletableFuture<Double> getCurrentPrice(org.bukkit.Material material, PriceType type);

    /**
     * Get item trend over a period
     * @param material Item material
     * @param period Time period to analyze
     * @return Market trend data
     */
    CompletableFuture<MarketTrend> getItemTrend(org.bukkit.Material material, java.time.Duration period);

    /**
     * Get trending items on the market
     * @param limit Maximum number of items to return
     * @return List of trending items
     */
    CompletableFuture<java.util.List<TrendingItem>> getTrendingItems(int limit);

    /**
     * Get overall market statistics
     * @return Market statistics
     */
    CompletableFuture<MarketStats> getMarketStatistics();

    /**
     * Get total trade volume for a material
     * @param material Item material
     * @param period Time period
     * @return Trade volume
     */
    CompletableFuture<Long> getTradeVolume(org.bukkit.Material material, java.time.Duration period);
}
