package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.economy.EconomyManager;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of EconomyService
 * Delegates to EconomyManager with proper error handling
 */
public class EconomyServiceImpl implements EconomyService {

    private final EconomyManager economyManager;

    public EconomyServiceImpl(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @Override
    public CompletableFuture<BigDecimal> getBalance(UUID playerId) {
        return economyManager.getBalance(playerId);
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(UUID playerId) {
        return economyManager.hasAccount(playerId);
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(UUID playerId, BigDecimal amount, String reason) {
        return economyManager.addMoney(playerId, amount, reason)
            .thenCompose(v -> economyManager.getBalance(playerId))
            .thenApply(newBalance -> new TransactionResult(true, "Deposit successful", newBalance))
            .exceptionally(ex -> new TransactionResult(false, "Deposit failed: " + ex.getMessage(), BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(UUID playerId, BigDecimal amount, String reason) {
        return economyManager.removeMoney(playerId, amount, reason)
            .thenCompose(success -> {
                if (success) {
                    return economyManager.getBalance(playerId)
                        .thenApply(newBalance -> new TransactionResult(true, "Withdrawal successful", newBalance));
                } else {
                    return CompletableFuture.completedFuture(
                        new TransactionResult(false, "Insufficient funds", BigDecimal.ZERO)
                    );
                }
            })
            .exceptionally(ex -> new TransactionResult(false, "Withdrawal failed: " + ex.getMessage(), BigDecimal.ZERO));
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(UUID from, UUID to, BigDecimal amount, String reason) {
        return economyManager.transferMoney(from, to, amount, reason)
            .thenCompose(success -> {
                if (success) {
                    return economyManager.getBalance(from)
                        .thenApply(newBalance -> new TransactionResult(true, "Transfer successful", newBalance));
                } else {
                    return CompletableFuture.completedFuture(
                        new TransactionResult(false, "Transfer failed", BigDecimal.ZERO)
                    );
                }
            })
            .exceptionally(ex -> new TransactionResult(false, "Transfer failed: " + ex.getMessage(), BigDecimal.ZERO));
    }

    @Override
    public String formatCurrency(BigDecimal amount) {
        // EconomyManager has formatMoney, not formatCurrency
        return economyManager.formatMoney(amount);
    }
}
