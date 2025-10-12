package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;

/** Bank statistics */
public class BankStats {
    private final BigDecimal totalDeposited;
    private final BigDecimal totalWithdrawn;
    private final BigDecimal totalInterestEarned;
    private final int transactionCount;

    public BankStats(BigDecimal totalDeposited, BigDecimal totalWithdrawn, BigDecimal totalInterestEarned, int transactionCount) {
        this.totalDeposited = totalDeposited;
        this.totalWithdrawn = totalWithdrawn;
        this.totalInterestEarned = totalInterestEarned;
        this.transactionCount = transactionCount;
    }

    public BigDecimal getTotalDeposited() { return totalDeposited; }
    public BigDecimal getTotalWithdrawn() { return totalWithdrawn; }
    public BigDecimal getTotalInterestEarned() { return totalInterestEarned; }
    public int getTransactionCount() { return transactionCount; }
}
