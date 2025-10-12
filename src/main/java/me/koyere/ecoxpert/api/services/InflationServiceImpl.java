package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.inflation.PlayerEconomicProfile;
import me.koyere.ecoxpert.core.data.DataManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of InflationService
 * Delegates to InflationManager with database queries for statistics
 */
public class InflationServiceImpl implements InflationService {

    private final InflationManager inflationManager;
    private final DataManager dataManager;

    public InflationServiceImpl(InflationManager inflationManager, DataManager dataManager) {
        this.inflationManager = inflationManager;
        this.dataManager = dataManager;
    }

    @Override
    public double getCurrentInflation() {
        return inflationManager.getInflationRate();
    }

    @Override
    public double getEconomicHealth() {
        return inflationManager.getEconomicHealth();
    }

    @Override
    public EconomicCycleInfo getCurrentCycle() {
        var cycle = inflationManager.getCurrentCycle();
        if (cycle == null) {
            return new EconomicCycleInfo("UNKNOWN", 0.0, 0.0, "Economic system initializing");
        }

        return new EconomicCycleInfo(
            cycle.name(),
            getEconomicHealth(),
            getCurrentInflation(),
            getCycleDescription(cycle)
        );
    }

    @Override
    public Optional<me.koyere.ecoxpert.api.dto.PlayerEconomyProfile> getPlayerProfile(UUID playerId) {
        PlayerEconomicProfile profile = inflationManager.getPlayerProfile(playerId);
        if (profile == null) {
            return Optional.empty();
        }

        return Optional.of(new me.koyere.ecoxpert.api.dto.PlayerEconomyProfile(
            playerId,
            profile.getRiskScore(),
            0.0, // Wealth percentile calculated separately
            BigDecimal.valueOf(profile.getPredictedFutureBalance()),
            classifyEconomicClass(profile.getPredictedFutureBalance())
        ));
    }

    @Override
    public CompletableFuture<BigDecimal> getTotalMoney() {
        return CompletableFuture.supplyAsync(() -> {
            try (var result = dataManager.executeQuery(
                "SELECT COALESCE(SUM(balance), 0) AS total FROM ecoxpert_accounts"
            ).join()) {
                if (result.next()) {
                    BigDecimal total = result.getBigDecimal("total");
                    return total != null ? total : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        });
    }

    @Override
    public CompletableFuture<BigDecimal> getAverageBalance() {
        return CompletableFuture.supplyAsync(() -> {
            try (var result = dataManager.executeQuery(
                "SELECT COALESCE(AVG(balance), 0) AS avg FROM ecoxpert_accounts"
            ).join()) {
                if (result.next()) {
                    BigDecimal avg = result.getBigDecimal("avg");
                    return avg != null ? avg : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            } catch (Exception e) {
                return BigDecimal.ZERO;
            }
        });
    }

    @Override
    public CompletableFuture<Double> getGiniCoefficient() {
        // Gini coefficient calculation from sorted balances
        return CompletableFuture.supplyAsync(() -> {
            try (var result = dataManager.executeQuery(
                "SELECT balance FROM ecoxpert_accounts ORDER BY balance ASC"
            ).join()) {
                java.util.List<Double> balances = new java.util.ArrayList<>();
                while (result.next()) {
                    BigDecimal balance = result.getBigDecimal("balance");
                    if (balance != null) {
                        balances.add(balance.doubleValue());
                    }
                }

                if (balances.isEmpty()) return 0.0;

                // Calculate Gini coefficient
                int n = balances.size();
                double sumOfDifferences = 0.0;
                double sumOfBalances = 0.0;

                for (int i = 0; i < n; i++) {
                    sumOfBalances += balances.get(i);
                    for (int j = 0; j < n; j++) {
                        sumOfDifferences += Math.abs(balances.get(i) - balances.get(j));
                    }
                }

                if (sumOfBalances == 0.0) return 0.0;

                return sumOfDifferences / (2.0 * n * sumOfBalances);
            } catch (Exception e) {
                return 0.0;
            }
        });
    }

    private String getCycleDescription(me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle cycle) {
        return switch (cycle) {
            case DEPRESSION -> "Severe economic downturn with deflation";
            case RECESSION -> "Economic slowdown with reduced activity";
            case STABLE -> "Balanced economic conditions";
            case GROWTH -> "Economic expansion with increasing activity";
            case BOOM -> "High economic activity and rapid growth";
            case BUBBLE -> "Overheated economy with potential correction";
        };
    }

    private String classifyEconomicClass(double balance) {
        if (balance < 1000) return "POOR";
        if (balance < 10000) return "LOWER_MIDDLE";
        if (balance < 100000) return "MIDDLE";
        if (balance < 1000000) return "UPPER_MIDDLE";
        return "WEALTHY";
    }
}
