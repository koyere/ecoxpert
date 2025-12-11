package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Banking system statistics
 * 
 * Immutable statistics object providing comprehensive
 * banking system analytics and performance metrics.
 */
public class BankStatistics {

    private final int totalAccounts;
    private final int activeAccounts;
    private final int frozenAccounts;
    private final BigDecimal totalDeposits;
    private final BigDecimal totalBalance;
    private final BigDecimal totalInterestPaid;
    private final int dailyTransactions;
    private final BigDecimal dailyVolume;
    private final LocalDateTime generatedAt;

    // Account tier distribution
    private final int basicAccounts;
    private final int silverAccounts;
    private final int goldAccounts;
    private final int platinumAccounts;

    // Performance metrics
    private final BigDecimal averageBalance;
    private final BigDecimal largestBalance;
    private final BigDecimal smallestBalance;
    private final double averageInterestRate;

    public BankStatistics(int totalAccounts, int activeAccounts, int frozenAccounts,
            BigDecimal totalDeposits, BigDecimal totalBalance, BigDecimal totalInterestPaid,
            int dailyTransactions, BigDecimal dailyVolume,
            int basicAccounts, int silverAccounts, int goldAccounts, int platinumAccounts,
            BigDecimal averageBalance, BigDecimal largestBalance, BigDecimal smallestBalance,
            double averageInterestRate) {
        this.totalAccounts = totalAccounts;
        this.activeAccounts = activeAccounts;
        this.frozenAccounts = frozenAccounts;
        this.totalDeposits = totalDeposits;
        this.totalBalance = totalBalance;
        this.totalInterestPaid = totalInterestPaid;
        this.dailyTransactions = dailyTransactions;
        this.dailyVolume = dailyVolume;
        this.basicAccounts = basicAccounts;
        this.silverAccounts = silverAccounts;
        this.goldAccounts = goldAccounts;
        this.platinumAccounts = platinumAccounts;
        this.averageBalance = averageBalance;
        this.largestBalance = largestBalance;
        this.smallestBalance = smallestBalance;
        this.averageInterestRate = averageInterestRate;
        this.generatedAt = LocalDateTime.now();
    }

    /**
     * Calculate account activity percentage
     */
    public double getActivityPercentage() {
        if (totalAccounts == 0)
            return 0.0;
        return (double) activeAccounts / totalAccounts * 100.0;
    }

    /**
     * Calculate frozen account percentage
     */
    public double getFrozenPercentage() {
        if (totalAccounts == 0)
            return 0.0;
        return (double) frozenAccounts / totalAccounts * 100.0;
    }

    /**
     * Get tier distribution as percentages
     */
    public TierDistribution getTierDistribution() {
        if (totalAccounts == 0) {
            return new TierDistribution(0, 0, 0, 0);
        }

        double basicPct = (double) basicAccounts / totalAccounts * 100.0;
        double silverPct = (double) silverAccounts / totalAccounts * 100.0;
        double goldPct = (double) goldAccounts / totalAccounts * 100.0;
        double platinumPct = (double) platinumAccounts / totalAccounts * 100.0;

        return new TierDistribution(basicPct, silverPct, goldPct, platinumPct);
    }

    /**
     * Calculate system health score (0-100)
     */
    public int getSystemHealthScore() {
        int score = 100;

        // Deduct for frozen accounts
        if (getFrozenPercentage() > 5) {
            score -= (int) (getFrozenPercentage() - 5) * 2;
        }

        // Deduct for low activity
        if (getActivityPercentage() < 50) {
            score -= (int) (50 - getActivityPercentage());
        }

        // Deduct for extreme balance concentration
        if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal concentration = largestBalance.divide(totalBalance, 4, RoundingMode.HALF_UP);
            if (concentration.doubleValue() > 0.5) { // Single account holds >50%
                score -= 20;
            }
        }

        return Math.max(0, Math.min(100, score));
    }

    // === Getters ===

    public int getTotalAccounts() {
        return totalAccounts;
    }

    public int getActiveAccounts() {
        return activeAccounts;
    }

    public int getFrozenAccounts() {
        return frozenAccounts;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public BigDecimal getTotalInterestPaid() {
        return totalInterestPaid;
    }

    public int getDailyTransactions() {
        return dailyTransactions;
    }

    public BigDecimal getDailyVolume() {
        return dailyVolume;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public int getBasicAccounts() {
        return basicAccounts;
    }

    public int getSilverAccounts() {
        return silverAccounts;
    }

    public int getGoldAccounts() {
        return goldAccounts;
    }

    public int getPlatinumAccounts() {
        return platinumAccounts;
    }

    public BigDecimal getAverageBalance() {
        return averageBalance;
    }

    public BigDecimal getLargestBalance() {
        return largestBalance;
    }

    public BigDecimal getSmallestBalance() {
        return smallestBalance;
    }

    public double getAverageInterestRate() {
        return averageInterestRate;
    }

    /**
     * Tier distribution data class
     */
    public static class TierDistribution {
        private final double basicPercentage;
        private final double silverPercentage;
        private final double goldPercentage;
        private final double platinumPercentage;

        public TierDistribution(double basicPercentage, double silverPercentage,
                double goldPercentage, double platinumPercentage) {
            this.basicPercentage = basicPercentage;
            this.silverPercentage = silverPercentage;
            this.goldPercentage = goldPercentage;
            this.platinumPercentage = platinumPercentage;
        }

        public double getBasicPercentage() {
            return basicPercentage;
        }

        public double getSilverPercentage() {
            return silverPercentage;
        }

        public double getGoldPercentage() {
            return goldPercentage;
        }

        public double getPlatinumPercentage() {
            return platinumPercentage;
        }
    }
}