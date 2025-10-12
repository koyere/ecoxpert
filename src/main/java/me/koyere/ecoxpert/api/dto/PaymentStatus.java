package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;

/** Payment status */
public class PaymentStatus {
    private final boolean success;
    private final String message;
    private final BigDecimal remainingDebt;
    private final BigDecimal paidAmount;

    public PaymentStatus(boolean success, String message, BigDecimal remainingDebt, BigDecimal paidAmount) {
        this.success = success;
        this.message = message;
        this.remainingDebt = remainingDebt;
        this.paidAmount = paidAmount;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public BigDecimal getRemainingDebt() { return remainingDebt; }
    public BigDecimal getPaidAmount() { return paidAmount; }
}
