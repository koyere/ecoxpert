package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bank account data model
 * 
 * Represents a player's bank account with balance tracking,
 * interest calculations, daily limits, and security features.
 */
public class BankAccount {

    private final UUID playerId;
    private final String accountNumber;
    private BigDecimal balance;
    private BankAccountTier tier;
    private LocalDateTime createdAt;
    private LocalDateTime lastInterestCalculation;
    private BigDecimal totalInterestEarned;
    private boolean frozen;
    private String frozenReason;

    // Daily limits tracking
    private LocalDate lastResetDate;
    private BigDecimal dailyDepositUsed;
    private BigDecimal dailyWithdrawUsed;
    private BigDecimal dailyTransferUsed;

    // Security features
    private int failedTransactionCount;
    private LocalDateTime lastFailedTransaction;

    public BankAccount(UUID playerId, String accountNumber, BankAccountTier tier) {
        this.playerId = playerId;
        this.accountNumber = accountNumber;
        this.balance = BigDecimal.ZERO;
        this.tier = tier;
        this.createdAt = LocalDateTime.now();
        this.lastInterestCalculation = LocalDateTime.now();
        this.totalInterestEarned = BigDecimal.ZERO;
        this.frozen = false;
        this.frozenReason = null;

        // Initialize daily limits
        this.lastResetDate = LocalDate.now();
        this.dailyDepositUsed = BigDecimal.ZERO;
        this.dailyWithdrawUsed = BigDecimal.ZERO;
        this.dailyTransferUsed = BigDecimal.ZERO;

        // Security
        this.failedTransactionCount = 0;
        this.lastFailedTransaction = null;
    }

    // === Balance Operations ===

    /**
     * Add amount to balance
     */
    public void addBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    /**
     * Subtract amount from balance
     */
    public boolean subtractBalance(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0 && this.balance.compareTo(amount) >= 0) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }

    /**
     * Check if account has sufficient funds
     */
    public boolean hasSufficientFunds(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }

    // === Interest Operations ===

    /**
     * Add interest to balance
     */
    public void addInterest(BigDecimal interestAmount) {
        if (interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(interestAmount);
            this.totalInterestEarned = this.totalInterestEarned.add(interestAmount);
            this.lastInterestCalculation = LocalDateTime.now();
        }
    }

    /**
     * Check if interest calculation is due
     */
    public boolean isInterestCalculationDue() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        return this.lastInterestCalculation.isBefore(yesterday);
    }

    // === Daily Limits Management ===

    /**
     * Check and reset daily limits if needed
     */
    public void checkAndResetDailyLimits() {
        LocalDate today = LocalDate.now();
        if (!this.lastResetDate.equals(today)) {
            this.dailyDepositUsed = BigDecimal.ZERO;
            this.dailyWithdrawUsed = BigDecimal.ZERO;
            this.dailyTransferUsed = BigDecimal.ZERO;
            this.lastResetDate = today;
        }
    }

    /**
     * Check if transaction is within daily limit
     */
    public boolean isWithinDailyLimit(BankTransactionType type, BigDecimal amount) {
        checkAndResetDailyLimits();

        BigDecimal currentUsed = switch (type) {
            case DEPOSIT -> this.dailyDepositUsed;
            case WITHDRAW -> this.dailyWithdrawUsed;
            case TRANSFER_OUT -> this.dailyTransferUsed;
            default -> BigDecimal.ZERO;
        };

        BigDecimal dailyLimit = this.tier.getDailyLimit(type);
        return currentUsed.add(amount).compareTo(dailyLimit) <= 0;
    }

    /**
     * Add to daily limit usage
     */
    public void addToDailyUsage(BankTransactionType type, BigDecimal amount) {
        checkAndResetDailyLimits();

        switch (type) {
            case DEPOSIT -> this.dailyDepositUsed = this.dailyDepositUsed.add(amount);
            case WITHDRAW -> this.dailyWithdrawUsed = this.dailyWithdrawUsed.add(amount);
            case TRANSFER_OUT -> this.dailyTransferUsed = this.dailyTransferUsed.add(amount);
            // Other transaction types (TRANSFER_IN, INTEREST, FEE, ADMIN_ADJUSTMENT,
            // FREEZE, UNFREEZE, TIER_UPGRADE)
            // don't affect daily limits, so we explicitly ignore them
            default -> {
            }
        }
    }

    /**
     * Get remaining daily limit
     */
    public BigDecimal getRemainingDailyLimit(BankTransactionType type) {
        checkAndResetDailyLimits();

        BigDecimal currentUsed = switch (type) {
            case DEPOSIT -> this.dailyDepositUsed;
            case WITHDRAW -> this.dailyWithdrawUsed;
            case TRANSFER_OUT -> this.dailyTransferUsed;
            default -> BigDecimal.ZERO;
        };

        BigDecimal dailyLimit = this.tier.getDailyLimit(type);
        return dailyLimit.subtract(currentUsed).max(BigDecimal.ZERO);
    }

    // === Security Features ===

    /**
     * Record failed transaction
     */
    public void recordFailedTransaction() {
        this.failedTransactionCount++;
        this.lastFailedTransaction = LocalDateTime.now();

        // Auto-freeze if too many failed transactions
        if (this.failedTransactionCount >= 10) {
            this.frozen = true;
            this.frozenReason = "Too many failed transactions - auto-frozen for security";
        }
    }

    /**
     * Reset failed transaction count
     */
    public void resetFailedTransactionCount() {
        this.failedTransactionCount = 0;
        this.lastFailedTransaction = null;
    }

    /**
     * Freeze account
     */
    public void freeze(String reason) {
        this.frozen = true;
        this.frozenReason = reason;
    }

    /**
     * Unfreeze account
     */
    public void unfreeze() {
        this.frozen = false;
        this.frozenReason = null;
        resetFailedTransactionCount();
    }

    // === Getters and Setters ===

    public UUID getPlayerId() {
        return playerId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BankAccountTier getTier() {
        return tier;
    }

    public void setTier(BankAccountTier tier) {
        this.tier = tier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastInterestCalculation() {
        return lastInterestCalculation;
    }

    public void setLastInterestCalculation(LocalDateTime lastInterestCalculation) {
        this.lastInterestCalculation = lastInterestCalculation;
    }

    public BigDecimal getTotalInterestEarned() {
        return totalInterestEarned;
    }

    public void setTotalInterestEarned(BigDecimal totalInterestEarned) {
        this.totalInterestEarned = totalInterestEarned;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public String getFrozenReason() {
        return frozenReason;
    }

    public void setFrozenReason(String frozenReason) {
        this.frozenReason = frozenReason;
    }

    public LocalDate getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(LocalDate lastResetDate) {
        this.lastResetDate = lastResetDate;
    }

    public BigDecimal getDailyDepositUsed() {
        return dailyDepositUsed;
    }

    public void setDailyDepositUsed(BigDecimal dailyDepositUsed) {
        this.dailyDepositUsed = dailyDepositUsed;
    }

    public BigDecimal getDailyWithdrawUsed() {
        return dailyWithdrawUsed;
    }

    public void setDailyWithdrawUsed(BigDecimal dailyWithdrawUsed) {
        this.dailyWithdrawUsed = dailyWithdrawUsed;
    }

    public BigDecimal getDailyTransferUsed() {
        return dailyTransferUsed;
    }

    public void setDailyTransferUsed(BigDecimal dailyTransferUsed) {
        this.dailyTransferUsed = dailyTransferUsed;
    }

    public int getFailedTransactionCount() {
        return failedTransactionCount;
    }

    public void setFailedTransactionCount(int failedTransactionCount) {
        this.failedTransactionCount = failedTransactionCount;
    }

    public LocalDateTime getLastFailedTransaction() {
        return lastFailedTransaction;
    }

    public void setLastFailedTransaction(LocalDateTime lastFailedTransaction) {
        this.lastFailedTransaction = lastFailedTransaction;
    }
}