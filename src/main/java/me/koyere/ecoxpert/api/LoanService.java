package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Loan and credit management service
 */
public interface LoanService {
    /**
     * Get smart loan offer for player based on credit score
     * @param playerId Player UUID
     * @return Loan offer with terms
     */
    CompletableFuture<LoanOfferInfo> getSmartOffer(java.util.UUID playerId);

    /**
     * Request a loan
     * @param playerId Player UUID
     * @param amount Loan amount
     * @return Loan information
     */
    CompletableFuture<LoanInfo> requestLoan(java.util.UUID playerId, java.math.BigDecimal amount);

    /**
     * Pay loan installment or full amount
     * @param playerId Player UUID
     * @param amount Amount to pay
     * @return Payment result
     */
    CompletableFuture<PaymentStatus> payLoan(java.util.UUID playerId, java.math.BigDecimal amount);

    /**
     * Get player's active loan status
     * @param playerId Player UUID
     * @return Loan status or null if no active loan
     */
    CompletableFuture<LoanStatusInfo> getStatus(java.util.UUID playerId);

    /**
     * Get player's credit score (300-1000)
     * @param playerId Player UUID
     * @return Credit score
     */
    CompletableFuture<Integer> getCreditScore(java.util.UUID playerId);
}
