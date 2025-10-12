package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Loan information */
public class LoanInfo {
    private final UUID loanId;
    private final UUID playerId;
    private final BigDecimal principal;
    private final BigDecimal outstanding;
    private final double interestRate;
    private final Instant issuedAt;
    private final Instant dueDate;
    private final String status;

    public LoanInfo(UUID loanId, UUID playerId, BigDecimal principal, BigDecimal outstanding, double interestRate, Instant issuedAt, Instant dueDate, String status) {
        this.loanId = loanId;
        this.playerId = playerId;
        this.principal = principal;
        this.outstanding = outstanding;
        this.interestRate = interestRate;
        this.issuedAt = issuedAt;
        this.dueDate = dueDate;
        this.status = status;
    }

    public UUID getLoanId() { return loanId; }
    public UUID getPlayerId() { return playerId; }
    public BigDecimal getPrincipal() { return principal; }
    public BigDecimal getOutstanding() { return outstanding; }
    public double getInterestRate() { return interestRate; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getDueDate() { return dueDate; }
    public String getStatus() { return status; }
}
