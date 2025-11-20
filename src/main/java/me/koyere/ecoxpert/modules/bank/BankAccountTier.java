package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;

/**
 * Bank account tier system
 * 
 * Defines different account levels with varying benefits,
 * interest rates, and daily transaction limits.
 */
public enum BankAccountTier {
    
    BASIC("Basic", 
          new BigDecimal("0.01"),    // 1% annual interest
          new BigDecimal("1000000"), // default high daily deposit limit
          new BigDecimal("500000"),  // default high daily withdraw limit
          new BigDecimal("250000"),  // default high daily transfer limit
          BigDecimal.ZERO),          // No minimum balance
          
    SILVER("Silver", 
           new BigDecimal("0.015"),   // 1.5% annual interest
           new BigDecimal("5000000"),   // higher defaults to avoid early blocking
           new BigDecimal("2500000"),
           new BigDecimal("1000000"),
           new BigDecimal("10000")),  // $10000 minimum balance
           
    GOLD("Gold", 
         new BigDecimal("0.02"),     // 2% annual interest
         new BigDecimal("25000000"),
         new BigDecimal("10000000"),
         new BigDecimal("5000000"),
         new BigDecimal("50000")),   // $50000 minimum balance
         
    PLATINUM("Platinum", 
             new BigDecimal("0.025"),  // 2.5% annual interest
             new BigDecimal("100000000"),
             new BigDecimal("50000000"),
             new BigDecimal("25000000"),
             new BigDecimal("250000")); // $250000 minimum balance
    
    private final String displayName;
    private final BigDecimal annualInterestRate;
    private final BigDecimal dailyDepositLimit;
    private final BigDecimal dailyWithdrawLimit;
    private final BigDecimal dailyTransferLimit;
    private final BigDecimal minimumBalance;
    private static java.util.Map<BankAccountTier, TierLimits> overrides = java.util.Map.of();
    
    BankAccountTier(String displayName, BigDecimal annualInterestRate, 
                   BigDecimal dailyDepositLimit, BigDecimal dailyWithdrawLimit,
                   BigDecimal dailyTransferLimit, BigDecimal minimumBalance) {
        this.displayName = displayName;
        this.annualInterestRate = annualInterestRate;
        this.dailyDepositLimit = dailyDepositLimit;
        this.dailyWithdrawLimit = dailyWithdrawLimit;
        this.dailyTransferLimit = dailyTransferLimit;
        this.minimumBalance = minimumBalance;
    }
    
    /**
     * Get daily interest rate (annual rate / 365)
     */
    public BigDecimal getDailyInterestRate() {
        return annualInterestRate.divide(new BigDecimal("365"), 10, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Get daily limit for transaction type
     */
    public BigDecimal getDailyLimit(BankTransactionType type) {
        TierLimits override = overrides.get(this);
        return switch (type) {
            case DEPOSIT -> override != null && override.depositLimit() != null ? override.depositLimit() : dailyDepositLimit;
            case WITHDRAW -> override != null && override.withdrawLimit() != null ? override.withdrawLimit() : dailyWithdrawLimit;
            case TRANSFER_OUT -> override != null && override.transferLimit() != null ? override.transferLimit() : dailyTransferLimit;
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Check if balance qualifies for this tier
     */
    public boolean qualifiesForTier(BigDecimal balance) {
        return balance.compareTo(minimumBalance) >= 0;
    }
    
    /**
     * Get next tier up
     */
    public BankAccountTier getNextTier() {
        return switch (this) {
            case BASIC -> SILVER;
            case SILVER -> GOLD;
            case GOLD -> PLATINUM;
            case PLATINUM -> PLATINUM; // Already at highest
        };
    }
    
    /**
     * Check if upgrade is available
     */
    public boolean canUpgrade() {
        return this != PLATINUM;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public BigDecimal getDailyDepositLimit() { return dailyDepositLimit; }
    public BigDecimal getDailyWithdrawLimit() { return dailyWithdrawLimit; }
    public BigDecimal getDailyTransferLimit() { return dailyTransferLimit; }
    public BigDecimal getMinimumBalance() { return minimumBalance; }

    public static void applyOverrides(java.util.Map<BankAccountTier, TierLimits> tierOverrides) {
        overrides = tierOverrides != null ? java.util.Map.copyOf(tierOverrides) : java.util.Map.of();
    }

    public record TierLimits(BigDecimal depositLimit, BigDecimal withdrawLimit, BigDecimal transferLimit) { }
}
