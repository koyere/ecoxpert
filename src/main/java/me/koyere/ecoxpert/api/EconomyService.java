package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Basic economy operations service
 */
public interface EconomyService {
    /**
     * Get player balance
     * @param playerId Player UUID
     * @return Balance amount
     */
    CompletableFuture<java.math.BigDecimal> getBalance(java.util.UUID playerId);

    /**
     * Check if player has an account
     * @param playerId Player UUID
     * @return true if account exists
     */
    CompletableFuture<Boolean> hasAccount(java.util.UUID playerId);

    /**
     * Deposit money to player account
     * @param playerId Player UUID
     * @param amount Amount to deposit
     * @param reason Transaction reason
     * @return Operation result
     */
    CompletableFuture<TransactionResult> deposit(java.util.UUID playerId, java.math.BigDecimal amount, String reason);

    /**
     * Withdraw money from player account
     * @param playerId Player UUID
     * @param amount Amount to withdraw
     * @param reason Transaction reason
     * @return Operation result
     */
    CompletableFuture<TransactionResult> withdraw(java.util.UUID playerId, java.math.BigDecimal amount, String reason);

    /**
     * Transfer money between players
     * @param from Source player UUID
     * @param to Target player UUID
     * @param amount Amount to transfer
     * @param reason Transaction reason
     * @return Operation result
     */
    CompletableFuture<TransactionResult> transfer(java.util.UUID from, java.util.UUID to, java.math.BigDecimal amount, String reason);

    /**
     * Format amount as currency string
     * @param amount Amount to format
     * @return Formatted currency string
     */
    String formatCurrency(java.math.BigDecimal amount);
}
