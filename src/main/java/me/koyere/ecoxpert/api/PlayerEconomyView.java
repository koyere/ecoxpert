package me.koyere.ecoxpert.api;

import java.math.BigDecimal;

/** Extended per-player economy view for integrations. */
public class PlayerEconomyView {
    private final java.util.UUID playerId;
    private final BigDecimal balance;
    private final double wealthPercentile; // 0..1
    private final double riskScore;        // 0..1
    private final double predictedFutureBalance;

    public PlayerEconomyView(java.util.UUID playerId,
                             BigDecimal balance,
                             double wealthPercentile,
                             double riskScore,
                             double predictedFutureBalance) {
        this.playerId = playerId;
        this.balance = balance;
        this.wealthPercentile = Math.max(0.0, Math.min(1.0, wealthPercentile));
        this.riskScore = Math.max(0.0, Math.min(1.0, riskScore));
        this.predictedFutureBalance = Math.max(0.0, predictedFutureBalance);
    }

    public java.util.UUID getPlayerId() { return playerId; }
    public BigDecimal getBalance() { return balance; }
    public double getWealthPercentile() { return wealthPercentile; }
    public double getRiskScore() { return riskScore; }
    public double getPredictedFutureBalance() { return predictedFutureBalance; }
}

