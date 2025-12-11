package me.koyere.ecoxpert.modules.market;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Market statistics data container
 * 
 * Provides comprehensive market analytics and metrics
 * for monitoring economic health and trends.
 */
public final class MarketStatistics {

    private final int totalItems;
    private final int activeItems;
    private final long totalTransactions;
    private final BigDecimal totalVolume;
    private final BigDecimal averagePrice;
    private final BigDecimal marketCapitalization;
    private final LocalDateTime lastUpdate;
    private final BigDecimal dailyVolume;
    private final int dailyTransactions;
    private final double marketActivity; // 0.0 to 1.0

    /**
     * Constructor for market statistics
     */
    public MarketStatistics(int totalItems, int activeItems, long totalTransactions,
            BigDecimal totalVolume, BigDecimal averagePrice,
            BigDecimal marketCapitalization, LocalDateTime lastUpdate,
            BigDecimal dailyVolume, int dailyTransactions, double marketActivity) {
        this.totalItems = Math.max(0, totalItems);
        this.activeItems = Math.max(0, activeItems);
        this.totalTransactions = Math.max(0, totalTransactions);
        this.totalVolume = Objects.requireNonNull(totalVolume, "Total volume cannot be null");
        this.averagePrice = Objects.requireNonNull(averagePrice, "Average price cannot be null");
        this.marketCapitalization = Objects.requireNonNull(marketCapitalization, "Market cap cannot be null");
        this.lastUpdate = Objects.requireNonNull(lastUpdate, "Last update cannot be null");
        this.dailyVolume = Objects.requireNonNull(dailyVolume, "Daily volume cannot be null");
        this.dailyTransactions = Math.max(0, dailyTransactions);
        this.marketActivity = Math.max(0.0, Math.min(1.0, marketActivity));
    }

    // === Getters ===

    public int getTotalItems() {
        return totalItems;
    }

    public int getActiveItems() {
        return activeItems;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public BigDecimal getMarketCapitalization() {
        return marketCapitalization;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public BigDecimal getDailyVolume() {
        return dailyVolume;
    }

    public int getDailyTransactions() {
        return dailyTransactions;
    }

    public double getMarketActivity() {
        return marketActivity;
    }

    // === Utility Methods ===

    /**
     * Get average transaction value
     */
    public BigDecimal getAverageTransactionValue() {
        if (totalTransactions == 0) {
            return BigDecimal.ZERO;
        }
        return totalVolume.divide(BigDecimal.valueOf(totalTransactions), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Get market activity level description
     */
    public String getActivityLevel() {
        if (marketActivity >= 0.8)
            return "Very High";
        if (marketActivity >= 0.6)
            return "High";
        if (marketActivity >= 0.4)
            return "Medium";
        if (marketActivity >= 0.2)
            return "Low";
        return "Very Low";
    }

    /**
     * Get percentage of active items
     */
    public double getActiveItemsPercentage() {
        if (totalItems == 0)
            return 0.0;
        return (double) activeItems / totalItems * 100.0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MarketStatistics that = (MarketStatistics) obj;
        return totalItems == that.totalItems &&
                activeItems == that.activeItems &&
                totalTransactions == that.totalTransactions &&
                Objects.equals(totalVolume, that.totalVolume) &&
                Objects.equals(lastUpdate, that.lastUpdate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalItems, activeItems, totalTransactions, totalVolume, lastUpdate);
    }

    @Override
    public String toString() {
        return String.format("MarketStatistics{items=%d/%d, transactions=%d, volume=%s, activity=%s}",
                activeItems, totalItems, totalTransactions, totalVolume, getActivityLevel());
    }
}