package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Market trend analysis for an item
 * 
 * Provides trend analysis and price movement indicators
 * for market intelligence and decision making.
 */
public final class MarketTrend {
    
    private final Material material;
    private final TrendDirection direction;
    private final BigDecimal currentPrice;
    private final BigDecimal priceChange;
    private final BigDecimal priceChangePercentage;
    private final double volatility;
    private final double momentum;
    private final LocalDateTime analysisTime;
    private final String trendDescription;
    
    /**
     * Trend direction enumeration
     */
    public enum TrendDirection {
        STRONG_UPWARD("Strong Upward", 1.0),
        UPWARD("Upward", 0.5),
        STABLE("Stable", 0.0),
        DOWNWARD("Downward", -0.5),
        STRONG_DOWNWARD("Strong Downward", -1.0),
        VOLATILE("Volatile", 0.0);
        
        private final String displayName;
        private final double strength;
        
        TrendDirection(String displayName, double strength) {
            this.displayName = displayName;
            this.strength = strength;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public double getStrength() {
            return strength;
        }
        
        public boolean isUpward() {
            return strength > 0;
        }
        
        public boolean isDownward() {
            return strength < 0;
        }
        
        public boolean isStable() {
            return strength == 0 && this == STABLE;
        }
    }
    
    /**
     * Constructor for market trend
     */
    public MarketTrend(Material material, TrendDirection direction, BigDecimal currentPrice,
                      BigDecimal priceChange, BigDecimal priceChangePercentage,
                      double volatility, double momentum, LocalDateTime analysisTime,
                      String trendDescription) {
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.direction = Objects.requireNonNull(direction, "Direction cannot be null");
        this.currentPrice = Objects.requireNonNull(currentPrice, "Current price cannot be null");
        this.priceChange = Objects.requireNonNull(priceChange, "Price change cannot be null");
        this.priceChangePercentage = Objects.requireNonNull(priceChangePercentage, "Price change percentage cannot be null");
        this.volatility = Math.max(0.0, volatility);
        this.momentum = momentum;
        this.analysisTime = Objects.requireNonNull(analysisTime, "Analysis time cannot be null");
        this.trendDescription = trendDescription != null ? trendDescription : generateDescription();
    }
    
    // === Getters ===
    
    public Material getMaterial() {
        return material;
    }
    
    public TrendDirection getDirection() {
        return direction;
    }
    
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
    
    public BigDecimal getPriceChange() {
        return priceChange;
    }
    
    public BigDecimal getPriceChangePercentage() {
        return priceChangePercentage;
    }
    
    public double getVolatility() {
        return volatility;
    }
    
    public double getMomentum() {
        return momentum;
    }
    
    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }
    
    public String getTrendDescription() {
        return trendDescription;
    }
    
    // === Utility Methods ===
    
    /**
     * Get volatility level description
     */
    public String getVolatilityLevel() {
        if (volatility >= 0.3) return "Very High";
        if (volatility >= 0.2) return "High";
        if (volatility >= 0.1) return "Medium";
        if (volatility >= 0.05) return "Low";
        return "Very Low";
    }
    
    /**
     * Get momentum level description
     */
    public String getMomentumLevel() {
        if (momentum >= 0.5) return "Strong Bullish";
        if (momentum >= 0.2) return "Bullish";
        if (momentum >= -0.2) return "Neutral";
        if (momentum >= -0.5) return "Bearish";
        return "Strong Bearish";
    }
    
    /**
     * Check if trend is bullish
     */
    public boolean isBullish() {
        return direction.isUpward() && momentum > 0;
    }
    
    /**
     * Check if trend is bearish
     */
    public boolean isBearish() {
        return direction.isDownward() && momentum < 0;
    }
    
    /**
     * Get trend strength (0.0 to 1.0)
     */
    public double getTrendStrength() {
        return Math.abs(direction.getStrength());
    }
    
    /**
     * Generate trend description
     */
    private String generateDescription() {
        StringBuilder description = new StringBuilder();
        description.append(material.name().toLowerCase().replace('_', ' '))
                  .append(" is showing a ")
                  .append(direction.getDisplayName().toLowerCase())
                  .append(" trend with ")
                  .append(getVolatilityLevel().toLowerCase())
                  .append(" volatility");
        
        if (Math.abs(priceChangePercentage.doubleValue()) > 5.0) {
            description.append(" and significant price movement (");
            if (priceChangePercentage.doubleValue() > 0) {
                description.append("+");
            }
            description.append(priceChangePercentage).append("%)");
        }
        
        return description.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketTrend that = (MarketTrend) obj;
        return material == that.material &&
               Objects.equals(analysisTime, that.analysisTime);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(material, analysisTime);
    }
    
    @Override
    public String toString() {
        return String.format("MarketTrend{material=%s, direction=%s, change=%s%%, volatility=%s}",
            material, direction.getDisplayName(), priceChangePercentage, getVolatilityLevel());
    }
}