package me.koyere.ecoxpert.modules.bank;

import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional banking system manager
 * 
 * Provides secure banking operations with interest calculations,
 * daily limits, audit trails, and anti-exploitation measures.
 * All operations are async and transactionally safe.
 */
public interface BankManager {
    
    /**
     * Initialize banking system
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Shutdown banking system gracefully
     */
    CompletableFuture<Void> shutdown();
    
    // === Account Management ===
    
    /**
     * Get or create bank account for player
     */
    CompletableFuture<BankAccount> getOrCreateAccount(UUID playerId);
    
    /**
     * Get bank account by ID
     */
    CompletableFuture<Optional<BankAccount>> getAccount(UUID playerId);
    
    /**
     * Check if player has bank account
     */
    CompletableFuture<Boolean> hasAccount(UUID playerId);
    
    /**
     * Get account tier for player
     */
    CompletableFuture<BankAccountTier> getAccountTier(UUID playerId);
    
    /**
     * Upgrade account tier (requires conditions)
     */
    CompletableFuture<BankOperationResult> upgradeAccountTier(UUID playerId, BankAccountTier newTier);
    
    // === Core Banking Operations ===
    
    /**
     * Deposit money to bank account
     * Validates daily limits and transfers from economy balance
     */
    CompletableFuture<BankOperationResult> deposit(Player player, BigDecimal amount);
    
    /**
     * Withdraw money from bank account
     * Validates daily limits and transfers to economy balance
     */
    CompletableFuture<BankOperationResult> withdraw(Player player, BigDecimal amount);
    
    /**
     * Transfer money between bank accounts
     * Validates both accounts and daily limits
     */
    CompletableFuture<BankOperationResult> transfer(Player fromPlayer, UUID toPlayerId, BigDecimal amount);
    
    /**
     * Get current bank balance
     */
    CompletableFuture<BigDecimal> getBalance(UUID playerId);
    
    /**
     * Get available daily transaction limit remaining
     */
    CompletableFuture<BigDecimal> getDailyLimitRemaining(UUID playerId, BankTransactionType type);
    
    // === Interest System ===
    
    /**
     * Calculate current interest for account
     */
    CompletableFuture<BigDecimal> calculateInterest(UUID playerId);
    
    /**
     * Apply daily interest to all accounts
     * Called by scheduler
     */
    CompletableFuture<Void> processDailyInterest();
    
    /**
     * Get interest rate for account tier
     */
    BigDecimal getInterestRate(BankAccountTier tier);
    
    /**
     * Get projected interest for time period
     */
    CompletableFuture<BigDecimal> getProjectedInterest(UUID playerId, int days);
    
    // === Transaction History ===
    
    /**
     * Get transaction history for account
     */
    CompletableFuture<List<BankTransaction>> getTransactionHistory(UUID playerId, int limit);
    
    /**
     * Get transactions for date range
     */
    CompletableFuture<List<BankTransaction>> getTransactionHistory(UUID playerId, 
        LocalDate startDate, LocalDate endDate);
    
    /**
     * Get monthly statement
     */
    CompletableFuture<BankStatement> getMonthlyStatement(UUID playerId, int year, int month);
    
    // === Analytics & Statistics ===
    
    /**
     * Get banking statistics
     */
    CompletableFuture<BankStatistics> getBankStatistics();
    
    /**
     * Get account summary
     */
    CompletableFuture<BankAccountSummary> getAccountSummary(UUID playerId);
    
    /**
     * Check if banking system is operational
     */
    boolean isBankingAvailable();
    
    // === Admin Operations ===
    
    /**
     * Force interest calculation for account (admin)
     */
    CompletableFuture<BankOperationResult> forceInterestCalculation(UUID playerId);
    
    /**
     * Reset daily limits for account (admin)
     */
    CompletableFuture<BankOperationResult> resetDailyLimits(UUID playerId);
    
    /**
     * Freeze/unfreeze account (admin)
     */
    CompletableFuture<BankOperationResult> setAccountFrozen(UUID playerId, boolean frozen, String reason);
}