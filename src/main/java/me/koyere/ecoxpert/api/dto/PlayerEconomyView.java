package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Player economy view */
public class PlayerEconomyView {
    private final UUID playerId;
    private final BigDecimal balance;
    private final double wealthPercentile;
    private final double riskScore;
    private final double predictedFutureBalance;

    public PlayerEconomyView(UUID playerId, BigDecimal balance, double wealthPercentile, double riskScore, double predictedFutureBalance) {
        this.playerId = playerId;
        this.balance = balance;
        this.wealthPercentile = wealthPercentile;
        this.riskScore = riskScore;
        this.predictedFutureBalance = predictedFutureBalance;
    }

    public UUID getPlayerId() { return playerId; }
    public BigDecimal getBalance() { return balance; }
    public double getWealthPercentile() { return wealthPercentile; }
    public double getRiskScore() { return riskScore; }
    public double getPredictedFutureBalance() { return predictedFutureBalance; }
}
