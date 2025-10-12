package me.koyere.ecoxpert.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Player economic profile */
public class PlayerEconomyProfile {
    private final UUID playerId;
    private final double riskScore;
    private final double wealthPercentile;
    private final BigDecimal predictedBalance;
    private final String economicClass;

    public PlayerEconomyProfile(UUID playerId, double riskScore, double wealthPercentile, BigDecimal predictedBalance, String economicClass) {
        this.playerId = playerId;
        this.riskScore = riskScore;
        this.wealthPercentile = wealthPercentile;
        this.predictedBalance = predictedBalance;
        this.economicClass = economicClass;
    }

    public UUID getPlayerId() { return playerId; }
    public double getRiskScore() { return riskScore; }
    public double getWealthPercentile() { return wealthPercentile; }
    public BigDecimal getPredictedBalance() { return predictedBalance; }
    public String getEconomicClass() { return economicClass; }
}
