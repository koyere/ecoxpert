package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Bank account information */
public class BankAccountInfo {
    private final UUID playerId;
    private final BigDecimal balance;
    private final String tier;
    private final double interestRate;
    private final Instant lastInterest;

    public BankAccountInfo(UUID playerId, BigDecimal balance, String tier, double interestRate, Instant lastInterest) {
        this.playerId = playerId;
        this.balance = balance;
        this.tier = tier;
        this.interestRate = interestRate;
        this.lastInterest = lastInterest;
    }

    public UUID getPlayerId() { return playerId; }
    public BigDecimal getBalance() { return balance; }
    public String getTier() { return tier; }
    public double getInterestRate() { return interestRate; }
    public Instant getLastInterest() { return lastInterest; }
}
