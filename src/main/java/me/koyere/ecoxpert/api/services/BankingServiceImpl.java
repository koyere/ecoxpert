package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.bank.BankManager;
import me.koyere.ecoxpert.modules.bank.BankAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of BankingService
 * Delegates to BankManager with UUID to Player conversion
 */
public class BankingServiceImpl implements BankingService {

    private final BankManager bankManager;

    public BankingServiceImpl(BankManager bankManager) {
        this.bankManager = bankManager;
    }

    @Override
    public CompletableFuture<BankAccountInfo> getAccount(UUID playerId) {
        return bankManager.getAccount(playerId)
                .thenApply(optAccount -> {
                    if (optAccount.isEmpty())
                        return null;
                    BankAccount account = optAccount.get();

                    // Convert LocalDateTime to Instant
                    java.time.Instant lastInterest = account.getLastInterestCalculation()
                            .atZone(java.time.ZoneId.systemDefault()).toInstant();

                    return new BankAccountInfo(
                            playerId,
                            account.getBalance(),
                            account.getTier().name(),
                            account.getTier().getAnnualInterestRate().doubleValue(),
                            lastInterest);
                })
                .exceptionally(ex -> null);
    }

    @Override
    public CompletableFuture<BankOperationStatus> createAccount(UUID playerId) {
        return bankManager.getOrCreateAccount(playerId)
                .thenApply(account -> new BankOperationStatus(
                        true,
                        "Account created successfully",
                        account.getBalance()))
                .exceptionally(ex -> new BankOperationStatus(
                        false,
                        "Failed to create account: " + ex.getMessage(),
                        BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<BankOperationStatus> deposit(UUID playerId, BigDecimal amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return CompletableFuture.completedFuture(
                    new BankOperationStatus(false, "Player not online", BigDecimal.ZERO));
        }

        return bankManager.deposit(player, amount)
                .thenApply(result -> new BankOperationStatus(
                        result.isSuccess(),
                        result.getMessage(),
                        result.getNewBalance()))
                .exceptionally(ex -> new BankOperationStatus(
                        false,
                        "Deposit failed: " + ex.getMessage(),
                        BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<BankOperationStatus> withdraw(UUID playerId, BigDecimal amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return CompletableFuture.completedFuture(
                    new BankOperationStatus(false, "Player not online", BigDecimal.ZERO));
        }

        return bankManager.withdraw(player, amount)
                .thenApply(result -> new BankOperationStatus(
                        result.isSuccess(),
                        result.getMessage(),
                        result.getNewBalance()))
                .exceptionally(ex -> new BankOperationStatus(
                        false,
                        "Withdrawal failed: " + ex.getMessage(),
                        BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<BankStats> getStatistics(UUID playerId) {
        // BankManager doesn't have getStatistics(UUID)
        // We'd need to calculate from transaction history
        return bankManager.getTransactionHistory(playerId, 1000)
                .thenApply(transactions -> {
                    BigDecimal totalDeposited = BigDecimal.ZERO;
                    BigDecimal totalWithdrawn = BigDecimal.ZERO;
                    BigDecimal totalInterest = BigDecimal.ZERO;

                    for (var tx : transactions) {
                        switch (tx.getType()) {
                            case DEPOSIT -> totalDeposited = totalDeposited.add(tx.getAmount());
                            case WITHDRAW -> totalWithdrawn = totalWithdrawn.add(tx.getAmount());
                            case INTEREST -> totalInterest = totalInterest.add(tx.getAmount());
                            // Other transaction types (TRANSFER_IN, TRANSFER_OUT, FEE, ADMIN_ADJUSTMENT,
                            // FREEZE, UNFREEZE, TIER_UPGRADE) don't affect these basic statistics
                            default -> {
                            } // Explicitly ignore other types
                        }
                    }

                    return new BankStats(
                            totalDeposited,
                            totalWithdrawn,
                            totalInterest,
                            transactions.size());
                })
                .exceptionally(ex -> new BankStats(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0));
    }

    @Override
    public CompletableFuture<Double> getInterestRate(UUID playerId) {
        return bankManager.getAccountTier(playerId)
                .thenApply(tier -> tier.getAnnualInterestRate().doubleValue())
                .exceptionally(ex -> 0.0);
    }
}
