package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Inflation control and monitoring service
 */
public interface InflationService {
    /**
     * Get current inflation rate
     * @return Inflation rate (e.g., 0.02 for 2%)
     */
    double getCurrentInflation();

    /**
     * Get economic health (0.0 - 1.0)
     * @return Economic health score
     */
    double getEconomicHealth();

    /**
     * Get current economic cycle
     * @return Economic cycle
     */
    EconomicCycleInfo getCurrentCycle();

    /**
     * Get player's economic profile
     * @param playerId Player UUID
     * @return Player economic profile
     */
    java.util.Optional<PlayerEconomyProfile> getPlayerProfile(java.util.UUID playerId);

    /**
     * Get total money in circulation
     * @return Total server money
     */
    CompletableFuture<java.math.BigDecimal> getTotalMoney();

    /**
     * Get average player balance
     * @return Average balance
     */
    CompletableFuture<java.math.BigDecimal> getAverageBalance();

    /**
     * Get Gini coefficient (wealth inequality, 0-1)
     * @return Gini coefficient
     */
    CompletableFuture<Double> getGiniCoefficient();
}
