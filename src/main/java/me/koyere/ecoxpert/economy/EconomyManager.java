package me.koyere.ecoxpert.economy;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core economy management interface
 * 
 * Provides comprehensive economy operations with database
 * persistence and transaction logging.
 */
public interface EconomyManager {
    
    /**
     * Initialize the economy system
     */
    void initialize();
    
    /**
     * Shutdown the economy system
     */
    void shutdown();
    
    /**
     * Check if a player has an economy account
     * 
     * @param playerUuid Player UUID
     * @return true if account exists
     */
    CompletableFuture<Boolean> hasAccount(UUID playerUuid);
    
    /**
     * Create a new economy account for a player
     * 
     * @param playerUuid Player UUID
     * @param startingBalance Initial balance
     * @return CompletableFuture that completes when account is created
     */
    CompletableFuture<Void> createAccount(UUID playerUuid, BigDecimal startingBalance);
    
    /**
     * Get a player's current balance
     * 
     * @param playerUuid Player UUID
     * @return CompletableFuture with player's balance
     */
    CompletableFuture<BigDecimal> getBalance(UUID playerUuid);
    
    /**
     * Set a player's balance
     * 
     * @param playerUuid Player UUID
     * @param balance New balance
     * @param reason Transaction reason
     * @return CompletableFuture that completes when balance is set
     */
    CompletableFuture<Void> setBalance(UUID playerUuid, BigDecimal balance, String reason);
    
    /**
     * Add money to a player's account
     * 
     * @param playerUuid Player UUID
     * @param amount Amount to add
     * @param reason Transaction reason
     * @return CompletableFuture that completes when money is added
     */
    CompletableFuture<Void> addMoney(UUID playerUuid, BigDecimal amount, String reason);
    
    /**
     * Remove money from a player's account
     * 
     * @param playerUuid Player UUID
     * @param amount Amount to remove
     * @param reason Transaction reason
     * @return CompletableFuture that completes when money is removed
     */
    CompletableFuture<Boolean> removeMoney(UUID playerUuid, BigDecimal amount, String reason);
    
    /**
     * Transfer money between two players
     * 
     * @param fromUuid Source player UUID
     * @param toUuid Target player UUID
     * @param amount Amount to transfer
     * @param reason Transaction reason
     * @return CompletableFuture that completes when transfer is done
     */
    CompletableFuture<Boolean> transferMoney(UUID fromUuid, UUID toUuid, BigDecimal amount, String reason);
    
    /**
     * Check if a player has sufficient funds
     * 
     * @param playerUuid Player UUID
     * @param amount Amount to check
     * @return CompletableFuture with true if player has sufficient funds
     */
    CompletableFuture<Boolean> hasSufficientFunds(UUID playerUuid, BigDecimal amount);
    
    /**
     * Get the default starting balance for new accounts
     * 
     * @return Starting balance
     */
    BigDecimal getStartingBalance();
    
    /**
     * Get the maximum allowed balance
     * 
     * @return Maximum balance (null for unlimited)
     */
    BigDecimal getMaximumBalance();
    
    /**
     * Format a monetary amount for display
     * 
     * @param amount Amount to format
     * @return Formatted string
     */
    String formatMoney(BigDecimal amount);
    
    /**
     * Get currency name (singular)
     * 
     * @return Currency name
     */
    String getCurrencyNameSingular();
    
    /**
     * Get currency name (plural)
     * 
     * @return Currency name
     */
    String getCurrencyNamePlural();
    
    /**
     * Get currency symbol
     * 
     * @return Currency symbol
     */
    String getCurrencySymbol();

    /**
     * Apply a wealth tax across accounts above a threshold.
     * Reduces balances by (balance * rate) where balance > threshold.
     * Returns the number of affected accounts.
     */
    CompletableFuture<Integer> applyWealthTax(BigDecimal rate, BigDecimal threshold, String reason);

    /**
     * Get the top balances ordered descending.
     *
     * @param limit maximum entries to return (<=0 returns empty list)
     * @return list of top balance entries (ordered)
     */
    CompletableFuture<java.util.List<TopBalanceEntry>> getTopBalances(int limit);

    /**
     * Get the 1-based rank of a player's balance (0 if no account).
     */
    CompletableFuture<Integer> getBalanceRank(UUID playerUuid);

    /**
     * Leaderboard entry (balance with owner UUID).
     */
    record TopBalanceEntry(UUID playerUuid, BigDecimal balance) { }
}
