package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.market.MarketItemStats;
import org.bukkit.Material;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of MarketService
 * Delegates to MarketManager with proper mapping
 */
public class MarketServiceImpl implements MarketService {

    private final MarketManager marketManager;

    public MarketServiceImpl(MarketManager marketManager) {
        this.marketManager = marketManager;
    }

    @Override
    public CompletableFuture<Double> getCurrentPrice(Material material, PriceType type) {
        if (type == PriceType.BUY) {
            return marketManager.getBuyPrice(material)
                .thenApply(price -> price.doubleValue())
                .exceptionally(ex -> 0.0);
        } else {
            return marketManager.getSellPrice(material)
                .thenApply(price -> price.doubleValue())
                .exceptionally(ex -> 0.0);
        }
    }

    @Override
    public CompletableFuture<MarketTrend> getItemTrend(Material material, Duration period) {
        return marketManager.getItemTrend(material)
            .thenApply(trend -> {
                // MarketTrend from manager has: getDirection() (TrendDirection enum), getPriceChangePercentage() (BigDecimal)
                String trendDirection = trend.getDirection().name();
                double change = trend.getPriceChangePercentage().doubleValue();
                long vol = 0L; // MarketTrend doesn't have volume, use 0 as placeholder
                return new MarketTrend(material, trendDirection, change, vol);
            })
            .exceptionally(ex -> new MarketTrend(material, "UNKNOWN", 0.0, 0L));
    }

    @Override
    public CompletableFuture<List<TrendingItem>> getTrendingItems(int limit) {
        return marketManager.getTopTradedItems(limit)
            .thenApply(itemStatsList -> {
                List<TrendingItem> trending = new ArrayList<>();
                for (MarketItemStats stats : itemStatsList) {
                    // MarketItemStats has: getMaterial(), getTotalBought(), getTotalSold()
                    // We need buy/sell prices - fetch them
                    Material mat = stats.getMaterial();
                    // Simplified: use current prices, volume as total
                    double buyPrice = 0;
                    double sellPrice = 0;
                    try {
                        buyPrice = marketManager.getBuyPrice(mat).join().doubleValue();
                        sellPrice = marketManager.getSellPrice(mat).join().doubleValue();
                    } catch (Exception ignored) {}

                    trending.add(new TrendingItem(
                        mat,
                        buyPrice,
                        sellPrice,
                        stats.getTotalBought() + stats.getTotalSold(),
                        0.0 // Price change not available in stats
                    ));
                }
                return trending;
            })
            .exceptionally(ex -> new ArrayList<>());
    }

    @Override
    public CompletableFuture<MarketStats> getMarketStatistics() {
        return marketManager.getMarketStatistics()
            .thenApply(stats -> new MarketStats(
                stats.getTotalVolume().doubleValue(),
                stats.getMarketActivity(),
                stats.getActiveItems(),
                (int) stats.getTotalTransactions()
            ))
            .exceptionally(ex -> new MarketStats(0.0, 0.0, 0, 0));
    }

    @Override
    public CompletableFuture<Long> getTradeVolume(Material material, Duration period) {
        // MarketManager doesn't have material-specific volume by duration
        // MarketTrend doesn't have volume, use 0 as placeholder
        return marketManager.getItemTrend(material)
            .thenApply(trend -> 0L)
            .exceptionally(ex -> 0L);
    }
}
