package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;

/** Transaction result */
public class TransactionResult {
    private final boolean success;
    private final String message;
    private final BigDecimal newBalance;

    public TransactionResult(boolean success, String message, BigDecimal newBalance) {
        this.success = success;
        this.message = message;
        this.newBalance = newBalance;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public BigDecimal getNewBalance() { return newBalance; }
}
