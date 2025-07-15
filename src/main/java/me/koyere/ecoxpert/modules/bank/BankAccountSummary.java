package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Bank account summary
 * 
 * Comprehensive account overview with performance metrics,
 * recent activity, and projected earnings.
 */
public class BankAccountSummary {
    
    private final UUID accountId;
    private final String accountNumber;
    private final BankAccountTier currentTier;
    private final BankAccountTier eligibleTier;
    
    // Current status
    private final BigDecimal currentBalance;
    private final boolean frozen;
    private final String frozenReason;
    private final LocalDateTime accountAge;
    
    // Performance metrics
    private final BigDecimal totalInterestEarned;
    private final BigDecimal monthlyInterestEarned;
    private final BigDecimal projectedAnnualInterest;
    private final BigDecimal effectiveAPY;
    
    // Activity metrics
    private final int totalTransactions;
    private final int monthlyTransactions;
    private final BigDecimal totalDeposited;
    private final BigDecimal totalWithdrawn;
    private final LocalDateTime lastActivity;
    
    // Daily limits status
    private final BigDecimal dailyDepositRemaining;
    private final BigDecimal dailyWithdrawRemaining;
    private final BigDecimal dailyTransferRemaining;
    
    // Security status
    private final int failedTransactionCount;
    private final LocalDateTime lastFailedTransaction;
    private final boolean requiresAttention;
    
    private final LocalDateTime generatedAt;
    
    public BankAccountSummary(UUID accountId, String accountNumber, BankAccountTier currentTier,
                             BankAccountTier eligibleTier, BigDecimal currentBalance, boolean frozen,
                             String frozenReason, LocalDateTime accountAge, BigDecimal totalInterestEarned,
                             BigDecimal monthlyInterestEarned, BigDecimal projectedAnnualInterest,
                             BigDecimal effectiveAPY, int totalTransactions, int monthlyTransactions,
                             BigDecimal totalDeposited, BigDecimal totalWithdrawn, LocalDateTime lastActivity,
                             BigDecimal dailyDepositRemaining, BigDecimal dailyWithdrawRemaining,
                             BigDecimal dailyTransferRemaining, int failedTransactionCount,
                             LocalDateTime lastFailedTransaction, boolean requiresAttention) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.currentTier = currentTier;
        this.eligibleTier = eligibleTier;
        this.currentBalance = currentBalance;
        this.frozen = frozen;
        this.frozenReason = frozenReason;
        this.accountAge = accountAge;
        this.totalInterestEarned = totalInterestEarned;
        this.monthlyInterestEarned = monthlyInterestEarned;
        this.projectedAnnualInterest = projectedAnnualInterest;
        this.effectiveAPY = effectiveAPY;
        this.totalTransactions = totalTransactions;
        this.monthlyTransactions = monthlyTransactions;
        this.totalDeposited = totalDeposited;
        this.totalWithdrawn = totalWithdrawn;
        this.lastActivity = lastActivity;
        this.dailyDepositRemaining = dailyDepositRemaining;
        this.dailyWithdrawRemaining = dailyWithdrawRemaining;
        this.dailyTransferRemaining = dailyTransferRemaining;
        this.failedTransactionCount = failedTransactionCount;
        this.lastFailedTransaction = lastFailedTransaction;
        this.requiresAttention = requiresAttention;
        this.generatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if account is eligible for tier upgrade
     */
    public boolean isEligibleForUpgrade() {
        return eligibleTier != null && eligibleTier != currentTier;
    }
    
    /**
     * Get account age in days
     */
    public long getAccountAgeInDays() {
        return ChronoUnit.DAYS.between(accountAge, LocalDateTime.now());
    }
    
    /**
     * Get days since last activity
     */
    public long getDaysSinceLastActivity() {
        if (lastActivity == null) return -1;
        return ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
    }
    
    /**
     * Check if account is considered active
     */
    public boolean isActiveAccount() {
        return getDaysSinceLastActivity() <= 30; // Active if used within 30 days
    }
    
    /**
     * Get account health score (0-100)
     */
    public int getAccountHealthScore() {
        int score = 100;
        
        // Deduct for frozen status
        if (frozen) score -= 50;
        
        // Deduct for failed transactions
        if (failedTransactionCount > 0) {
            score -= Math.min(failedTransactionCount * 5, 25);
        }
        
        // Deduct for inactivity
        long daysSinceActivity = getDaysSinceLastActivity();
        if (daysSinceActivity > 30) {
            score -= Math.min((daysSinceActivity - 30) / 7 * 5, 20); // 5 points per week of inactivity
        }
        
        // Bonus for consistent activity
        if (monthlyTransactions > 10) {
            score += 5;
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    /**
     * Get performance rating
     */
    public PerformanceRating getPerformanceRating() {
        int healthScore = getAccountHealthScore();
        
        if (healthScore >= 90) return PerformanceRating.EXCELLENT;
        if (healthScore >= 75) return PerformanceRating.GOOD;
        if (healthScore >= 60) return PerformanceRating.FAIR;
        if (healthScore >= 40) return PerformanceRating.POOR;
        return PerformanceRating.CRITICAL;
    }
    
    /**
     * Calculate net worth growth rate
     */
    public BigDecimal getNetWorthGrowthRate() {
        if (totalDeposited.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal netGain = totalInterestEarned.add(currentBalance).subtract(totalDeposited);
        return netGain.divide(totalDeposited, 4, BigDecimal.ROUND_HALF_UP)
                     .multiply(new BigDecimal("100"));
    }
    
    /**
     * Get formatted account overview
     */
    public String getAccountOverview() {
        StringBuilder overview = new StringBuilder();
        overview.append("Account: ").append(accountNumber).append("\n");
        overview.append("Tier: ").append(currentTier.getDisplayName()).append("\n");
        overview.append("Balance: $").append(currentBalance).append("\n");
        overview.append("Interest Earned: $").append(totalInterestEarned).append("\n");
        overview.append("Health Score: ").append(getAccountHealthScore()).append("/100\n");
        overview.append("Performance: ").append(getPerformanceRating().getDisplayName());
        
        if (isEligibleForUpgrade()) {
            overview.append("\n★ Eligible for ").append(eligibleTier.getDisplayName()).append(" upgrade!");
        }
        
        return overview.toString();
    }
    
    /**
     * Performance rating enumeration
     */
    public enum PerformanceRating {
        EXCELLENT("Excellent", "🌟"),
        GOOD("Good", "✅"),
        FAIR("Fair", "⚠️"),
        POOR("Poor", "⚠️"),
        CRITICAL("Critical", "🚨");
        
        private final String displayName;
        private final String emoji;
        
        PerformanceRating(String displayName, String emoji) {
            this.displayName = displayName;
            this.emoji = emoji;
        }
        
        public String getDisplayName() { return displayName; }
        public String getEmoji() { return emoji; }
        public String getFormattedName() { return emoji + " " + displayName; }
    }
    
    // === Getters ===
    
    public UUID getAccountId() { return accountId; }
    public String getAccountNumber() { return accountNumber; }
    public BankAccountTier getCurrentTier() { return currentTier; }
    public BankAccountTier getEligibleTier() { return eligibleTier; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public boolean isFrozen() { return frozen; }
    public String getFrozenReason() { return frozenReason; }
    public LocalDateTime getAccountAge() { return accountAge; }
    
    public BigDecimal getTotalInterestEarned() { return totalInterestEarned; }
    public BigDecimal getMonthlyInterestEarned() { return monthlyInterestEarned; }
    public BigDecimal getProjectedAnnualInterest() { return projectedAnnualInterest; }
    public BigDecimal getEffectiveAPY() { return effectiveAPY; }
    
    public int getTotalTransactions() { return totalTransactions; }
    public int getMonthlyTransactions() { return monthlyTransactions; }
    public BigDecimal getTotalDeposited() { return totalDeposited; }
    public BigDecimal getTotalWithdrawn() { return totalWithdrawn; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    
    public BigDecimal getDailyDepositRemaining() { return dailyDepositRemaining; }
    public BigDecimal getDailyWithdrawRemaining() { return dailyWithdrawRemaining; }
    public BigDecimal getDailyTransferRemaining() { return dailyTransferRemaining; }
    
    public int getFailedTransactionCount() { return failedTransactionCount; }
    public LocalDateTime getLastFailedTransaction() { return lastFailedTransaction; }
    public boolean isRequiresAttention() { return requiresAttention; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}