package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Market item statistics
 * 
 * Provides trading statistics and metrics for individual items
 * in the market system for analytics and reporting.
 */
public final class MarketItemStats {
    
    private final Material material;
    private final int totalSold;
    private final int totalBought;
    private final BigDecimal totalSellVolume;
    private final BigDecimal totalBuyVolume;
    private final BigDecimal averageSellPrice;
    private final BigDecimal averageBuyPrice;
    private final int rank;
    
    /**
     * Constructor for market item statistics
     */
    public MarketItemStats(Material material, int totalSold, int totalBought,
                         BigDecimal totalSellVolume, BigDecimal totalBuyVolume,
                         BigDecimal averageSellPrice, BigDecimal averageBuyPrice, int rank) {
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.totalSold = Math.max(0, totalSold);
        this.totalBought = Math.max(0, totalBought);
        this.totalSellVolume = Objects.requireNonNull(totalSellVolume, "Total sell volume cannot be null");
        this.totalBuyVolume = Objects.requireNonNull(totalBuyVolume, "Total buy volume cannot be null");
        this.averageSellPrice = Objects.requireNonNull(averageSellPrice, "Average sell price cannot be null");
        this.averageBuyPrice = Objects.requireNonNull(averageBuyPrice, "Average buy price cannot be null");
        this.rank = Math.max(1, rank);
    }
    
    // === Getters ===
    
    public Material getMaterial() {
        return material;
    }
    
    public int getTotalSold() {
        return totalSold;
    }
    
    public int getTotalBought() {
        return totalBought;
    }
    
    public BigDecimal getTotalSellVolume() {
        return totalSellVolume;
    }
    
    public BigDecimal getTotalBuyVolume() {
        return totalBuyVolume;
    }
    
    public BigDecimal getAverageSellPrice() {
        return averageSellPrice;
    }
    
    public BigDecimal getAverageBuyPrice() {
        return averageBuyPrice;
    }
    
    public int getRank() {
        return rank;
    }
    
    // === Utility Methods ===
    
    /**
     * Get total transaction count
     */
    public int getTotalTransactions() {
        return totalSold + totalBought;
    }
    
    /**
     * Get total volume (buy + sell)
     */
    public BigDecimal getTotalVolume() {
        return totalSellVolume.add(totalBuyVolume);
    }
    
    /**
     * Get net items traded (bought - sold)
     */
    public int getNetItemsTraded() {
        return totalBought - totalSold;
    }
    
    /**
     * Get net volume (buy volume - sell volume)
     */
    public BigDecimal getNetVolume() {
        return totalBuyVolume.subtract(totalSellVolume);
    }
    
    /**
     * Get average transaction size
     */
    public double getAverageTransactionSize() {
        int totalTransactions = getTotalTransactions();
        if (totalTransactions == 0) return 0.0;
        return (double) (totalSold + totalBought) / totalTransactions;
    }
    
    /**
     * Get market activity score (0.0 to 1.0)
     */
    public double getActivityScore() {
        // Simple activity score based on transaction count and volume
        int transactions = getTotalTransactions();
        BigDecimal volume = getTotalVolume();
        
        // Normalize based on typical market activity
        double transactionScore = Math.min(1.0, transactions / 1000.0);
        double volumeScore = Math.min(1.0, volume.doubleValue() / 100000.0);
        
        return (transactionScore + volumeScore) / 2.0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketItemStats that = (MarketItemStats) obj;
        return material == that.material;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(material);
    }
    
    @Override
    public String toString() {
        return String.format("MarketItemStats{material=%s, rank=%d, transactions=%d, volume=%s}",
            material, rank, getTotalTransactions(), getTotalVolume());
    }
}