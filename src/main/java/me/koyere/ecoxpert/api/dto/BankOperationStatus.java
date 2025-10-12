package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;

/** Bank operation status */
public class BankOperationStatus {
    private final boolean success;
    private final String message;
    private final BigDecimal newBalance;

    public BankOperationStatus(boolean success, String message, BigDecimal newBalance) {
        this.success = success;
        this.message = message;
        this.newBalance = newBalance;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public BigDecimal getNewBalance() { return newBalance; }
}
