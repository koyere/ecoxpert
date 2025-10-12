package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import me.koyere.ecoxpert.modules.loans.Loan;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of LoanService
 * Delegates to LoanManager with proper mapping
 */
public class LoanServiceImpl implements LoanService {

    private final LoanManager loanManager;

    public LoanServiceImpl(LoanManager loanManager) {
        this.loanManager = loanManager;
    }

    @Override
    public CompletableFuture<LoanOfferInfo> getSmartOffer(UUID playerId) {
        return loanManager.getOffer(playerId, BigDecimal.ZERO) // Get max offer
            .thenApply(offer -> new LoanOfferInfo(
                offer.amount(),
                offer.interestRate().doubleValue(),
                offer.termDays(),
                offer.score(),
                offer.reason()
            ))
            .exceptionally(ex -> new LoanOfferInfo(
                BigDecimal.ZERO,
                0.0,
                0,
                300,
                "UNKNOWN"
            ));
    }

    @Override
    public CompletableFuture<LoanInfo> requestLoan(UUID playerId, BigDecimal amount) {
        return loanManager.requestLoanSmart(playerId, amount)
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.completedFuture((LoanInfo) null);
                }
                return loanManager.getActiveLoan(playerId)
                    .thenApply(optLoan -> {
                        if (optLoan.isEmpty()) return null;
                        Loan loan = optLoan.get();

                        // Convert internal types to API types
                        UUID loanId = UUID.nameUUIDFromBytes(("loan-" + loan.getId()).getBytes());
                        java.time.Instant issuedAt = loan.getCreatedAt()
                            .atZone(java.time.ZoneId.systemDefault()).toInstant();

                        // Calculate due date (assume 30 days from issue for active loans)
                        java.time.Instant dueDate = issuedAt.plus(java.time.Duration.ofDays(30));

                        return new LoanInfo(
                            loanId,
                            loan.getPlayerUuid(),
                            loan.getPrincipal(),
                            loan.getOutstanding(),
                            loan.getInterestRate().doubleValue(),
                            issuedAt,
                            dueDate,
                            loan.getStatus()
                        );
                    });
            })
            .exceptionally(ex -> null);
    }

    @Override
    public CompletableFuture<PaymentStatus> payLoan(UUID playerId, BigDecimal amount) {
        return loanManager.payLoan(playerId, amount)
            .thenCompose(success -> {
                if (!success) {
                    return CompletableFuture.completedFuture(
                        new PaymentStatus(false, "Payment failed", BigDecimal.ZERO, BigDecimal.ZERO)
                    );
                }
                return loanManager.getActiveLoan(playerId)
                    .thenApply(optLoan -> {
                        BigDecimal remaining = optLoan.map(Loan::getOutstanding)
                            .orElse(BigDecimal.ZERO);
                        return new PaymentStatus(
                            true,
                            "Payment successful",
                            remaining,
                            amount
                        );
                    });
            })
            .exceptionally(ex -> new PaymentStatus(
                false,
                "Payment failed: " + ex.getMessage(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
            ));
    }

    @Override
    public CompletableFuture<LoanStatusInfo> getStatus(UUID playerId) {
        return loanManager.getActiveLoan(playerId)
            .thenCompose(optLoan -> {
                if (optLoan.isEmpty()) {
                    return CompletableFuture.completedFuture(
                        new LoanStatusInfo(false, BigDecimal.ZERO, null, BigDecimal.ZERO, 0)
                    );
                }

                Loan loan = optLoan.get();
                return loanManager.getSchedule(playerId)
                    .thenApply(schedule -> {
                        var pendingPayments = schedule.stream()
                            .filter(p -> "PENDING".equals(p.status()))
                            .toList();

                        var nextPayment = pendingPayments.stream().findFirst();

                        // Convert LocalDate to Instant for next payment
                        java.time.Instant nextPaymentInstant = nextPayment
                            .map(p -> p.dueDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                            .orElse(null);

                        return new LoanStatusInfo(
                            true,
                            loan.getOutstanding(),
                            nextPaymentInstant,
                            nextPayment.map(p -> p.amountDue()).orElse(BigDecimal.ZERO),
                            pendingPayments.size()
                        );
                    });
            })
            .exceptionally(ex -> new LoanStatusInfo(
                false,
                BigDecimal.ZERO,
                null,
                BigDecimal.ZERO,
                0
            ));
    }

    @Override
    public CompletableFuture<Integer> getCreditScore(UUID playerId) {
        // LoanManager doesn't expose calculateCreditScore publicly
        // Use offer's credit score as workaround
        return loanManager.getOffer(playerId, BigDecimal.ZERO)
            .thenApply(offer -> offer.score())
            .exceptionally(ex -> 300); // Minimum credit score
    }
}
