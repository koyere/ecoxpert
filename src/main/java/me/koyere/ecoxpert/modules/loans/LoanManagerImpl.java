package me.koyere.ecoxpert.modules.loans;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.economy.EconomyManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Minimal implementation of the LoanManager.
 *
 * Notes:
 * - Supports a single active loan per player (status = ACTIVE).
 * - Simple interest rate is stored for future extensions (no accrual here).
 * - Uses EconomyManager to deposit on request and deduct on payment.
 */
public class LoanManagerImpl implements LoanManager {

    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;

    public LoanManagerImpl(EcoXpertPlugin plugin, DataManager dataManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
    }

    @Override
    public CompletableFuture<Boolean> requestLoan(UUID playerUuid, BigDecimal amount, BigDecimal interestRate) {
        if (amount == null || amount.signum() <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        // Prevent multiple active loans
        return getActiveLoan(playerUuid).thenCompose(active -> {
            if (active.isPresent()) {
                return CompletableFuture.completedFuture(false);
            }

            // Create loan row first
            String sql = "INSERT INTO ecoxpert_loans (player_uuid, principal, outstanding, interest_rate, status, created_at) " +
                         "VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)";

            return dataManager.executeUpdate(sql, playerUuid.toString(), amount, amount, interestRate)
                .thenCompose(updated -> {
                    if (updated <= 0) return CompletableFuture.completedFuture(false);
                    // Deposit amount to player's balance
                    return economyManager.addMoney(playerUuid, amount, "Loan disbursement")
                        .thenApply(v -> true);
                });
        });
    }

    @Override
    public CompletableFuture<LoanOffer> getOffer(UUID playerUuid, BigDecimal amount) {
        return CompletableFuture.supplyAsync(() -> new LoanScoringPolicy(plugin, dataManager)
            .computeOffer(playerUuid, amount));
    }

    @Override
    public CompletableFuture<Boolean> requestLoanSmart(UUID playerUuid, BigDecimal amount) {
        return getOffer(playerUuid, amount).thenCompose(offer -> {
            if (!offer.approved()) return CompletableFuture.completedFuture(false);
            BigDecimal rate = offer.interestRate();

            // Prevent multiple active loans
            return getActiveLoan(playerUuid).thenCompose(active -> {
                if (active.isPresent()) return CompletableFuture.completedFuture(false);

                String sql = "INSERT INTO ecoxpert_loans (player_uuid, principal, outstanding, interest_rate, status, created_at) " +
                             "VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)";
                return dataManager.executeUpdate(sql, playerUuid.toString(), amount, amount, rate)
                    .thenCompose(updated -> {
                        if (updated <= 0) return CompletableFuture.completedFuture(false);
                        // Deposit funds
                        return economyManager.addMoney(playerUuid, amount, "Loan disbursement").thenCompose(v -> {
                            // Create schedule
                            return new LoanScoringPolicy(plugin, dataManager).createScheduleFor(playerUuid, amount, rate, offer.termDays())
                                .thenApply(ok -> true);
                        });
                    });
            });
        });
    }

    @Override
    public CompletableFuture<java.util.List<LoanPayment>> getSchedule(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> new LoanScoringPolicy(plugin, dataManager)
            .getSchedule(playerUuid));
    }

    @Override
    public CompletableFuture<Boolean> payLoan(UUID playerUuid, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return CompletableFuture.completedFuture(false);
        }

        return getActiveLoan(playerUuid).thenCompose(active -> {
            if (active.isEmpty()) return CompletableFuture.completedFuture(false);
            Loan loan = active.get();

            BigDecimal computedOutstanding = loan.getOutstanding().subtract(amount);
            // Ensure we do not go below zero (no overpayment)
            final BigDecimal newOutstanding = (computedOutstanding.signum() < 0)
                ? BigDecimal.ZERO
                : computedOutstanding;

            // Attempt to deduct funds first
            return economyManager.removeMoney(playerUuid, amount, "Loan payment").thenCompose(success -> {
                if (!success) return CompletableFuture.completedFuture(false);

                String sql = "UPDATE ecoxpert_loans SET outstanding = ?, last_payment_at = CURRENT_TIMESTAMP, " +
                             "status = CASE WHEN ? <= 0 THEN 'PAID' ELSE 'ACTIVE' END WHERE id = ?";

                return dataManager.executeUpdate(sql, newOutstanding, newOutstanding, loan.getId())
                    .thenCompose(rows -> {
                        // Apply payment to next due installment(s)
                        return applyPaymentToSchedule(loan.getId(), amount)
                            .thenApply(v -> rows > 0);
                    });
            });
        });
    }

    private CompletableFuture<Void> applyPaymentToSchedule(long loanId, BigDecimal amount) {
        return CompletableFuture.runAsync(() -> {
            BigDecimal remaining = amount;
            try {
                while (remaining.signum() > 0) {
                    try (QueryResult qr = dataManager.executeQuery(
                        "SELECT id, amount_due, paid_amount FROM ecoxpert_loan_schedules WHERE loan_id = ? AND status != 'PAID' ORDER BY installment_no LIMIT 1",
                        loanId).join()) {
                        if (!qr.next()) break;
                        long schedId = qr.getLong("id");
                        BigDecimal due = qr.getBigDecimal("amount_due");
                        BigDecimal paid = qr.getBigDecimal("paid_amount");
                        BigDecimal toPay = due.subtract(paid);
                        BigDecimal payNow = remaining.min(toPay);
                        BigDecimal newPaid = paid.add(payNow);
                        String newStatus = newPaid.compareTo(due) >= 0 ? "PAID" : "PENDING";
                        dataManager.executeUpdate(
                            "UPDATE ecoxpert_loan_schedules SET paid_amount = ?, status = ?, paid_at = CASE WHEN ? >= amount_due THEN CURRENT_TIMESTAMP ELSE paid_at END WHERE id = ?",
                            newPaid, newStatus, newPaid, schedId
                        ).join();
                        remaining = remaining.subtract(payNow);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public CompletableFuture<Optional<Loan>> getActiveLoan(UUID playerUuid) {
        String sql = "SELECT id, player_uuid, principal, outstanding, interest_rate, created_at, status " +
                     "FROM ecoxpert_loans WHERE player_uuid = ? AND status = 'ACTIVE' ORDER BY id DESC LIMIT 1";

        return dataManager.executeQuery(sql, playerUuid.toString()).thenApply(this::mapSingleLoan);
    }

    private Optional<Loan> mapSingleLoan(QueryResult result) {
        try (result) {
            if (!result.next()) return Optional.empty();

            long id = result.getLong("id");
            UUID uuid = UUID.fromString(result.getString("player_uuid"));
            BigDecimal principal = result.getBigDecimal("principal");
            BigDecimal outstanding = result.getBigDecimal("outstanding");
            BigDecimal rate = result.getBigDecimal("interest_rate");
            LocalDateTime created = result.getTimestamp("created_at").toLocalDateTime();
            String status = result.getString("status");

            return Optional.of(new Loan(id, uuid, principal, outstanding, rate, created, status));
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to map loan row: " + e.getMessage());
            return Optional.empty();
        }
    }
}
