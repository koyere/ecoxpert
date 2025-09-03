package me.koyere.ecoxpert.modules.loans;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Loan domain model representing a player's loan.
 *
 * This minimal model supports a single active loan per player
 * with fixed interest rate and simple outstanding tracking.
 */
public class Loan {
    private final long id;
    private final UUID playerUuid;
    private final BigDecimal principal;
    private final BigDecimal outstanding;
    private final BigDecimal interestRate;
    private final LocalDateTime createdAt;
    private final String status;

    public Loan(long id, UUID playerUuid, BigDecimal principal, BigDecimal outstanding,
                BigDecimal interestRate, LocalDateTime createdAt, String status) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.principal = principal;
        this.outstanding = outstanding;
        this.interestRate = interestRate;
        this.createdAt = createdAt;
        this.status = status;
    }

    public long getId() { return id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public BigDecimal getPrincipal() { return principal; }
    public BigDecimal getOutstanding() { return outstanding; }
    public BigDecimal getInterestRate() { return interestRate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
}

