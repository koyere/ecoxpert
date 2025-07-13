package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Professional price calculation engine
 * 
 * Implements sophisticated dynamic pricing algorithms based on
 * supply/demand, market trends, and economic modeling.
 */
public class PriceCalculator {
    
    // Algorithm configuration constants
    private static final BigDecimal MAX_PRICE_CHANGE = BigDecimal.valueOf(0.20); // 20% max change per update
    private static final BigDecimal MIN_PRICE_RATIO = BigDecimal.valueOf(0.10); // 10% of base price minimum
    private static final BigDecimal MAX_PRICE_RATIO = BigDecimal.valueOf(10.0); // 1000% of base price maximum
    private static final BigDecimal VOLATILITY_DAMPING = BigDecimal.valueOf(0.85); // Volatility decay factor
    private static final BigDecimal MOMENTUM_FACTOR = BigDecimal.valueOf(0.3); // Momentum influence
    private static final int TREND_ANALYSIS_HOURS = 24; // Hours for trend analysis
    
    /**
     * Calculate new prices based on market activity
     */
    public MarketPriceUpdate calculatePriceUpdate(MarketItem item, List<MarketTransaction> recentTransactions) {
        BigDecimal basePrice = item.getBasePrice();
        BigDecimal currentBuyPrice = item.getCurrentBuyPrice();
        BigDecimal currentSellPrice = item.getCurrentSellPrice();
        
        // Analyze supply and demand
        SupplyDemandAnalysis analysis = analyzeSupplyDemand(recentTransactions, item.getMaterial());
        
        // Calculate price adjustments
        BigDecimal demandMultiplier = calculateDemandMultiplier(analysis);
        BigDecimal volatilityAdjustment = calculateVolatilityAdjustment(item, analysis);
        BigDecimal momentumAdjustment = calculateMomentumAdjustment(recentTransactions);
        
        // Apply algorithms to base price
        BigDecimal newBuyPrice = calculateNewBuyPrice(basePrice, currentBuyPrice, 
            demandMultiplier, volatilityAdjustment, momentumAdjustment);
        BigDecimal newSellPrice = calculateNewSellPrice(newBuyPrice, analysis);
        
        // Apply safety constraints
        newBuyPrice = applySafetyConstraints(basePrice, newBuyPrice);
        newSellPrice = applySafetyConstraints(basePrice.multiply(BigDecimal.valueOf(0.8)), newSellPrice);
        
        // Calculate trend information
        MarketTrend.TrendDirection direction = determineTrendDirection(currentBuyPrice, newBuyPrice, analysis);
        BigDecimal priceChange = newBuyPrice.subtract(currentBuyPrice);
        BigDecimal priceChangePercentage = calculatePercentageChange(currentBuyPrice, newBuyPrice);
        
        return new MarketPriceUpdate(
            item.getMaterial(),
            newBuyPrice,
            newSellPrice,
            priceChange,
            priceChangePercentage,
            direction,
            analysis.getVolatility(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Analyze supply and demand from recent transactions
     */
    private SupplyDemandAnalysis analyzeSupplyDemand(List<MarketTransaction> transactions, Material material) {
        int buyTransactions = 0;
        int sellTransactions = 0;
        BigDecimal buyVolume = BigDecimal.ZERO;
        BigDecimal sellVolume = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;
        
        // Filter transactions for this material within analysis period
        LocalDateTime cutoff = LocalDateTime.now().minusHours(TREND_ANALYSIS_HOURS);
        
        for (MarketTransaction transaction : transactions) {
            if (!transaction.getMaterial().equals(material) || 
                transaction.getTimestamp().isBefore(cutoff)) {
                continue;
            }
            
            BigDecimal transactionVolume = transaction.getTotalAmount();
            totalVolume = totalVolume.add(transactionVolume);
            
            if (transaction.isBuyTransaction()) {
                buyTransactions++;
                buyVolume = buyVolume.add(transactionVolume);
            } else {
                sellTransactions++;
                sellVolume = sellVolume.add(transactionVolume);
            }
        }
        
        // Calculate metrics
        double demandRatio = calculateDemandRatio(buyTransactions, sellTransactions);
        double volumeRatio = calculateVolumeRatio(buyVolume, sellVolume);
        double volatility = calculateVolatility(transactions, material);
        double activityLevel = calculateActivityLevel(buyTransactions + sellTransactions);
        
        return new SupplyDemandAnalysis(
            buyTransactions, sellTransactions, buyVolume, sellVolume,
            demandRatio, volumeRatio, volatility, activityLevel
        );
    }
    
    /**
     * Calculate demand multiplier based on supply/demand ratio
     */
    private BigDecimal calculateDemandMultiplier(SupplyDemandAnalysis analysis) {
        double demandRatio = analysis.getDemandRatio();
        double volumeRatio = analysis.getVolumeRatio();
        double activityLevel = analysis.getActivityLevel();
        
        // Weighted combination of ratios
        double combinedRatio = (demandRatio * 0.6) + (volumeRatio * 0.4);
        
        // Apply activity level influence
        double activityMultiplier = 0.5 + (activityLevel * 0.5);
        combinedRatio = combinedRatio * activityMultiplier;
        
        // Convert to price multiplier (1.0 = no change)
        double multiplier = 1.0 + ((combinedRatio - 1.0) * 0.1); // 10% max influence per update
        
        return BigDecimal.valueOf(Math.max(0.9, Math.min(1.1, multiplier)));
    }
    
    /**
     * Calculate volatility adjustment
     */
    private BigDecimal calculateVolatilityAdjustment(MarketItem item, SupplyDemandAnalysis analysis) {
        double currentVolatility = item.getPriceVolatility().doubleValue();
        double marketVolatility = analysis.getVolatility();
        
        // Smooth volatility changes
        double newVolatility = (currentVolatility * 0.7) + (marketVolatility * 0.3);
        
        // Apply damping to reduce extreme volatility
        newVolatility = newVolatility * VOLATILITY_DAMPING.doubleValue();
        
        return BigDecimal.valueOf(Math.max(0.01, Math.min(0.5, newVolatility)));
    }
    
    /**
     * Calculate momentum adjustment from transaction velocity
     */
    private BigDecimal calculateMomentumAdjustment(List<MarketTransaction> transactions) {
        if (transactions.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        // Calculate transaction velocity (transactions per hour)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        long recentTransactions = transactions.stream()
            .filter(t -> t.getTimestamp().isAfter(oneHourAgo))
            .count();
        
        // Convert to momentum factor
        double momentum = Math.min(1.0, recentTransactions / 10.0); // Normalize to 0-1
        
        return BigDecimal.valueOf(momentum * MOMENTUM_FACTOR.doubleValue());
    }
    
    /**
     * Calculate new buy price with all adjustments
     */
    private BigDecimal calculateNewBuyPrice(BigDecimal basePrice, BigDecimal currentPrice,
                                          BigDecimal demandMultiplier, BigDecimal volatilityAdjustment,
                                          BigDecimal momentumAdjustment) {
        
        // Start with current price
        BigDecimal newPrice = currentPrice;
        
        // Apply demand-based adjustment
        newPrice = newPrice.multiply(demandMultiplier);
        
        // Apply volatility (random walk component)
        BigDecimal volatilityChange = BigDecimal.valueOf(Math.random() - 0.5)
            .multiply(volatilityAdjustment)
            .multiply(basePrice);
        newPrice = newPrice.add(volatilityChange);
        
        // Apply momentum
        BigDecimal momentumChange = momentumAdjustment.multiply(basePrice);
        newPrice = newPrice.add(momentumChange);
        
        // Limit maximum change per update
        BigDecimal maxChange = currentPrice.multiply(MAX_PRICE_CHANGE);
        BigDecimal change = newPrice.subtract(currentPrice);
        
        if (change.abs().compareTo(maxChange) > 0) {
            BigDecimal limitedChange = change.signum() == 1 ? maxChange : maxChange.negate();
            newPrice = currentPrice.add(limitedChange);
        }
        
        return newPrice.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate new sell price based on buy price and market spread
     */
    private BigDecimal calculateNewSellPrice(BigDecimal newBuyPrice, SupplyDemandAnalysis analysis) {
        // Base sell ratio (typically 80% of buy price)
        BigDecimal baseSellRatio = BigDecimal.valueOf(0.80);
        
        // Adjust spread based on market conditions
        double activityLevel = analysis.getActivityLevel();
        double volatility = analysis.getVolatility();
        
        // Higher activity = smaller spread, higher volatility = larger spread
        double spreadAdjustment = (1.0 - activityLevel * 0.1) + (volatility * 0.1);
        BigDecimal adjustedRatio = baseSellRatio.multiply(BigDecimal.valueOf(spreadAdjustment));
        
        // Ensure ratio stays within reasonable bounds
        adjustedRatio = adjustedRatio.max(BigDecimal.valueOf(0.60));
        adjustedRatio = adjustedRatio.min(BigDecimal.valueOf(0.95));
        
        return newBuyPrice.multiply(adjustedRatio).setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Apply safety constraints to prevent extreme prices
     */
    private BigDecimal applySafetyConstraints(BigDecimal basePrice, BigDecimal newPrice) {
        BigDecimal minPrice = basePrice.multiply(MIN_PRICE_RATIO);
        BigDecimal maxPrice = basePrice.multiply(MAX_PRICE_RATIO);
        
        if (newPrice.compareTo(minPrice) < 0) {
            return minPrice;
        }
        if (newPrice.compareTo(maxPrice) > 0) {
            return maxPrice;
        }
        
        return newPrice;
    }
    
    // === Utility calculation methods ===
    
    private double calculateDemandRatio(int buyTransactions, int sellTransactions) {
        if (sellTransactions == 0) {
            return buyTransactions > 0 ? 2.0 : 1.0;
        }
        return (double) buyTransactions / sellTransactions;
    }
    
    private double calculateVolumeRatio(BigDecimal buyVolume, BigDecimal sellVolume) {
        if (sellVolume.equals(BigDecimal.ZERO)) {
            return buyVolume.compareTo(BigDecimal.ZERO) > 0 ? 2.0 : 1.0;
        }
        return buyVolume.divide(sellVolume, 4, RoundingMode.HALF_UP).doubleValue();
    }
    
    private double calculateVolatility(List<MarketTransaction> transactions, Material material) {
        if (transactions.size() < 2) {
            return 0.1; // Default low volatility
        }
        
        // Calculate price variance from recent transactions
        List<BigDecimal> prices = transactions.stream()
            .filter(t -> t.getMaterial().equals(material))
            .map(MarketTransaction::getUnitPrice)
            .toList();
        
        if (prices.size() < 2) {
            return 0.1;
        }
        
        // Calculate standard deviation of prices
        BigDecimal average = prices.stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);
        
        BigDecimal variance = prices.stream()
            .map(price -> price.subtract(average).pow(2))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(prices.size()), 4, RoundingMode.HALF_UP);
        
        double standardDeviation = Math.sqrt(variance.doubleValue());
        double volatility = standardDeviation / average.doubleValue();
        
        return Math.min(0.5, Math.max(0.01, volatility));
    }
    
    private double calculateActivityLevel(int totalTransactions) {
        // Normalize transaction count to activity level (0.0 to 1.0)
        return Math.min(1.0, totalTransactions / 50.0); // 50+ transactions = max activity
    }
    
    private MarketTrend.TrendDirection determineTrendDirection(BigDecimal oldPrice, BigDecimal newPrice, 
                                                             SupplyDemandAnalysis analysis) {
        BigDecimal changePercentage = calculatePercentageChange(oldPrice, newPrice);
        double volatility = analysis.getVolatility();
        
        if (volatility > 0.3) {
            return MarketTrend.TrendDirection.VOLATILE;
        }
        
        double changePercent = changePercentage.doubleValue();
        
        if (changePercent > 5.0) {
            return MarketTrend.TrendDirection.STRONG_UPWARD;
        } else if (changePercent > 1.0) {
            return MarketTrend.TrendDirection.UPWARD;
        } else if (changePercent < -5.0) {
            return MarketTrend.TrendDirection.STRONG_DOWNWARD;
        } else if (changePercent < -1.0) {
            return MarketTrend.TrendDirection.DOWNWARD;
        } else {
            return MarketTrend.TrendDirection.STABLE;
        }
    }
    
    private BigDecimal calculatePercentageChange(BigDecimal oldValue, BigDecimal newValue) {
        if (oldValue.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        
        return newValue.subtract(oldValue)
            .divide(oldValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }
    
    // === Inner classes ===
    
    /**
     * Supply and demand analysis data
     */
    private static class SupplyDemandAnalysis {
        private final int buyTransactions;
        private final int sellTransactions;
        private final BigDecimal buyVolume;
        private final BigDecimal sellVolume;
        private final double demandRatio;
        private final double volumeRatio;
        private final double volatility;
        private final double activityLevel;
        
        public SupplyDemandAnalysis(int buyTransactions, int sellTransactions,
                                  BigDecimal buyVolume, BigDecimal sellVolume,
                                  double demandRatio, double volumeRatio,
                                  double volatility, double activityLevel) {
            this.buyTransactions = buyTransactions;
            this.sellTransactions = sellTransactions;
            this.buyVolume = buyVolume;
            this.sellVolume = sellVolume;
            this.demandRatio = demandRatio;
            this.volumeRatio = volumeRatio;
            this.volatility = volatility;
            this.activityLevel = activityLevel;
        }
        
        // Getters
        public double getDemandRatio() { return demandRatio; }
        public double getVolumeRatio() { return volumeRatio; }
        public double getVolatility() { return volatility; }
        public double getActivityLevel() { return activityLevel; }
    }
    
    /**
     * Price update result
     */
    public static class MarketPriceUpdate {
        private final Material material;
        private final BigDecimal newBuyPrice;
        private final BigDecimal newSellPrice;
        private final BigDecimal priceChange;
        private final BigDecimal priceChangePercentage;
        private final MarketTrend.TrendDirection trendDirection;
        private final double volatility;
        private final LocalDateTime updateTime;
        
        public MarketPriceUpdate(Material material, BigDecimal newBuyPrice, BigDecimal newSellPrice,
                               BigDecimal priceChange, BigDecimal priceChangePercentage,
                               MarketTrend.TrendDirection trendDirection, double volatility,
                               LocalDateTime updateTime) {
            this.material = material;
            this.newBuyPrice = newBuyPrice;
            this.newSellPrice = newSellPrice;
            this.priceChange = priceChange;
            this.priceChangePercentage = priceChangePercentage;
            this.trendDirection = trendDirection;
            this.volatility = volatility;
            this.updateTime = updateTime;
        }
        
        // Getters
        public Material getMaterial() { return material; }
        public BigDecimal getNewBuyPrice() { return newBuyPrice; }
        public BigDecimal getNewSellPrice() { return newSellPrice; }
        public BigDecimal getPriceChange() { return priceChange; }
        public BigDecimal getPriceChangePercentage() { return priceChangePercentage; }
        public MarketTrend.TrendDirection getTrendDirection() { return trendDirection; }
        public double getVolatility() { return volatility; }
        public LocalDateTime getUpdateTime() { return updateTime; }
    }
}