package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Loan status information */
public class LoanStatusInfo {
    private final boolean hasActiveLoan;
    private final BigDecimal outstanding;
    private final Instant nextPaymentDue;
    private final BigDecimal nextPaymentAmount;
    private final int paymentsRemaining;

    public LoanStatusInfo(boolean hasActiveLoan, BigDecimal outstanding, Instant nextPaymentDue, BigDecimal nextPaymentAmount, int paymentsRemaining) {
        this.hasActiveLoan = hasActiveLoan;
        this.outstanding = outstanding;
        this.nextPaymentDue = nextPaymentDue;
        this.nextPaymentAmount = nextPaymentAmount;
        this.paymentsRemaining = paymentsRemaining;
    }

    public boolean hasActiveLoan() { return hasActiveLoan; }
    public BigDecimal getOutstanding() { return outstanding; }
    public Instant getNextPaymentDue() { return nextPaymentDue; }
    public BigDecimal getNextPaymentAmount() { return nextPaymentAmount; }
    public int getPaymentsRemaining() { return paymentsRemaining; }
}
