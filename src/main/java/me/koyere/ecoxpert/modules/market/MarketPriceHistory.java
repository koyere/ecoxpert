package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Market price history record
 * 
 * Immutable data class for tracking historical price data
 * to enable trend analysis and price forecasting.
 */
public final class MarketPriceHistory {
    
    private final Material material;
    private final BigDecimal buyPrice;
    private final BigDecimal sellPrice;
    private final LocalDateTime timestamp;
    private final int transactionCount;
    private final BigDecimal volume;
    
    /**
     * Constructor for price history record
     */
    public MarketPriceHistory(Material material, BigDecimal buyPrice, BigDecimal sellPrice,
                            LocalDateTime timestamp, int transactionCount, BigDecimal volume) {
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.buyPrice = Objects.requireNonNull(buyPrice, "Buy price cannot be null");
        this.sellPrice = Objects.requireNonNull(sellPrice, "Sell price cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.transactionCount = Math.max(0, transactionCount);
        this.volume = Objects.requireNonNull(volume, "Volume cannot be null");
    }
    
    // === Getters ===
    
    public Material getMaterial() {
        return material;
    }
    
    public BigDecimal getBuyPrice() {
        return buyPrice;
    }
    
    public BigDecimal getSellPrice() {
        return sellPrice;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public int getTransactionCount() {
        return transactionCount;
    }
    
    public BigDecimal getVolume() {
        return volume;
    }
    
    // === Utility Methods ===
    
    /**
     * Get average price (buy + sell / 2)
     */
    public BigDecimal getAveragePrice() {
        return buyPrice.add(sellPrice).divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get price spread (buy - sell)
     */
    public BigDecimal getPriceSpread() {
        return buyPrice.subtract(sellPrice);
    }
    
    /**
     * Get price spread percentage
     */
    public BigDecimal getPriceSpreadPercentage() {
        if (buyPrice.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return getPriceSpread().divide(buyPrice, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketPriceHistory that = (MarketPriceHistory) obj;
        return material == that.material &&
               Objects.equals(timestamp, that.timestamp);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(material, timestamp);
    }
    
    @Override
    public String toString() {
        return String.format("MarketPriceHistory{material=%s, buyPrice=%s, sellPrice=%s, timestamp=%s}",
            material, buyPrice, sellPrice, timestamp);
    }
}