package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Banking and interest management service
 */
public interface BankingService {
    /**
     * Get player's bank account
     * @param playerId Player UUID
     * @return Bank account or null if doesn't exist
     */
    CompletableFuture<BankAccountInfo> getAccount(java.util.UUID playerId);

    /**
     * Create bank account for player
     * @param playerId Player UUID
     * @return Operation result
     */
    CompletableFuture<BankOperationStatus> createAccount(java.util.UUID playerId);

    /**
     * Deposit money to bank account
     * @param playerId Player UUID
     * @param amount Amount to deposit
     * @return Operation result
     */
    CompletableFuture<BankOperationStatus> deposit(java.util.UUID playerId, java.math.BigDecimal amount);

    /**
     * Withdraw money from bank account
     * @param playerId Player UUID
     * @param amount Amount to withdraw
     * @return Operation result
     */
    CompletableFuture<BankOperationStatus> withdraw(java.util.UUID playerId, java.math.BigDecimal amount);

    /**
     * Get bank statistics for player
     * @param playerId Player UUID
     * @return Bank statistics
     */
    CompletableFuture<BankStats> getStatistics(java.util.UUID playerId);

    /**
     * Get current interest rate for player's tier
     * @param playerId Player UUID
     * @return Annual interest rate (e.g., 0.05 for 5%)
     */
    CompletableFuture<Double> getInterestRate(java.util.UUID playerId);
}
