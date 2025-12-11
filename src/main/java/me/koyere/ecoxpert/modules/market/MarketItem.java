package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a market item with pricing data
 * 
 * Immutable data class for market items with comprehensive
 * pricing information and trading availability.
 */
public final class MarketItem {

    private final Material material;
    private final BigDecimal basePrice;
    private final BigDecimal currentBuyPrice;
    private final BigDecimal currentSellPrice;
    private final boolean buyable;
    private final boolean sellable;
    private final int totalSold;
    private final int totalBought;
    private final LocalDateTime lastPriceUpdate;
    private final BigDecimal priceVolatility;

    /**
     * Constructor for market item
     */
    public MarketItem(Material material, BigDecimal basePrice, BigDecimal currentBuyPrice,
            BigDecimal currentSellPrice, boolean buyable, boolean sellable,
            int totalSold, int totalBought, LocalDateTime lastPriceUpdate,
            BigDecimal priceVolatility) {
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.basePrice = Objects.requireNonNull(basePrice, "Base price cannot be null");
        this.currentBuyPrice = Objects.requireNonNull(currentBuyPrice, "Current buy price cannot be null");
        this.currentSellPrice = Objects.requireNonNull(currentSellPrice, "Current sell price cannot be null");
        this.buyable = buyable;
        this.sellable = sellable;
        this.totalSold = Math.max(0, totalSold);
        this.totalBought = Math.max(0, totalBought);
        this.lastPriceUpdate = lastPriceUpdate;
        this.priceVolatility = Objects.requireNonNull(priceVolatility, "Price volatility cannot be null");
    }

    /**
     * Builder for creating market items
     */
    public static Builder builder(Material material, BigDecimal basePrice) {
        return new Builder(material, basePrice);
    }

    // === Getters ===

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public BigDecimal getCurrentBuyPrice() {
        return currentBuyPrice;
    }

    public BigDecimal getCurrentSellPrice() {
        return currentSellPrice;
    }

    public boolean isBuyable() {
        return buyable;
    }

    public boolean isSellable() {
        return sellable;
    }

    public int getTotalSold() {
        return totalSold;
    }

    public int getTotalBought() {
        return totalBought;
    }

    public LocalDateTime getLastPriceUpdate() {
        return lastPriceUpdate;
    }

    public BigDecimal getPriceVolatility() {
        return priceVolatility;
    }

    // === Utility Methods ===

    /**
     * Check if this item is actively traded
     */
    public boolean isActivelyTraded() {
        return buyable || sellable;
    }

    /**
     * Get price difference from base price
     */
    public BigDecimal getPriceDifferenceFromBase() {
        return currentBuyPrice.subtract(basePrice);
    }

    /**
     * Get price change percentage from base price
     */
    public BigDecimal getPriceChangePercentage() {
        if (basePrice.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        return getPriceDifferenceFromBase()
                .divide(basePrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Get total trading volume
     */
    public int getTotalVolume() {
        return totalSold + totalBought;
    }

    /**
     * Create a copy with updated prices
     */
    public MarketItem withPrices(BigDecimal newBuyPrice, BigDecimal newSellPrice) {
        return new MarketItem(
                material, basePrice, newBuyPrice, newSellPrice,
                buyable, sellable, totalSold, totalBought,
                LocalDateTime.now(), priceVolatility);
    }

    /**
     * Create a copy with updated trading statistics
     */
    public MarketItem withUpdatedStats(int additionalSold, int additionalBought) {
        return new MarketItem(
                material, basePrice, currentBuyPrice, currentSellPrice,
                buyable, sellable, totalSold + additionalSold, totalBought + additionalBought,
                lastPriceUpdate, priceVolatility);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        MarketItem that = (MarketItem) obj;
        return material == that.material;
    }

    @Override
    public int hashCode() {
        return Objects.hash(material);
    }

    @Override
    public String toString() {
        return String.format(
                "MarketItem{material=%s, basePrice=%s, buyPrice=%s, sellPrice=%s, buyable=%s, sellable=%s}",
                material, basePrice, currentBuyPrice, currentSellPrice, buyable, sellable);
    }

    /**
     * Builder pattern for creating MarketItem instances
     */
    public static class Builder {
        private final Material material;
        private final BigDecimal basePrice;
        private BigDecimal currentBuyPrice;
        private BigDecimal currentSellPrice;
        private boolean buyable = true;
        private boolean sellable = true;
        private int totalSold = 0;
        private int totalBought = 0;
        private LocalDateTime lastPriceUpdate = LocalDateTime.now();
        private BigDecimal priceVolatility = BigDecimal.valueOf(0.1); // 10% default volatility

        private Builder(Material material, BigDecimal basePrice) {
            this.material = material;
            this.basePrice = basePrice;
            this.currentBuyPrice = basePrice;
            this.currentSellPrice = basePrice.multiply(BigDecimal.valueOf(0.8)); // 80% sell price default
        }

        public Builder currentBuyPrice(BigDecimal price) {
            this.currentBuyPrice = price;
            return this;
        }

        public Builder currentSellPrice(BigDecimal price) {
            this.currentSellPrice = price;
            return this;
        }

        public Builder buyable(boolean buyable) {
            this.buyable = buyable;
            return this;
        }

        public Builder sellable(boolean sellable) {
            this.sellable = sellable;
            return this;
        }

        public Builder totalSold(int totalSold) {
            this.totalSold = totalSold;
            return this;
        }

        public Builder totalBought(int totalBought) {
            this.totalBought = totalBought;
            return this;
        }

        public Builder lastPriceUpdate(LocalDateTime lastPriceUpdate) {
            this.lastPriceUpdate = lastPriceUpdate;
            return this;
        }

        public Builder priceVolatility(BigDecimal volatility) {
            this.priceVolatility = volatility;
            return this;
        }

        public MarketItem build() {
            return new MarketItem(
                    material, basePrice, currentBuyPrice, currentSellPrice,
                    buyable, sellable, totalSold, totalBought,
                    lastPriceUpdate, priceVolatility);
        }
    }
}