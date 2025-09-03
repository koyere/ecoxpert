package me.koyere.ecoxpert.modules.loans;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Loan manager interface for requesting, paying and querying player loans.
 */
public interface LoanManager {

    /**
     * Request a new loan for the player.
     * Adds the loan amount to the player's balance if successful.
     *
     * @param playerUuid Player UUID
     * @param amount Principal amount requested
     * @param interestRate Simple interest rate (0.0-1.0)
     * @return true if loan created and deposited
     */
    CompletableFuture<Boolean> requestLoan(UUID playerUuid, BigDecimal amount, BigDecimal interestRate);

    /**
     * Pay an amount towards the player's active loan.
     * Deducts money from the player's balance if enough funds.
     *
     * @param playerUuid Player UUID
     * @param amount Payment amount
     * @return true if payment applied
     */
    CompletableFuture<Boolean> payLoan(UUID playerUuid, BigDecimal amount);

    /**
     * Get the player's active loan, if any.
     */
    CompletableFuture<Optional<Loan>> getActiveLoan(UUID playerUuid);
}

