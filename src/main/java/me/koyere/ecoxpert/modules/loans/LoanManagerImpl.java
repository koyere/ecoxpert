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
                    .thenApply(rows -> rows > 0);
            });
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
