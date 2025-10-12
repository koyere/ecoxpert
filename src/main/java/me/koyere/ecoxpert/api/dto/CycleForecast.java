package me.koyere.ecoxpert.api.dto;

import java.time.Duration;
import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;

/** Cycle forecast */
public class CycleForecast {
    private final EconomicIntelligenceEngine.EconomicCycle predictedCycle;
    private final double confidence;
    private final Duration horizon;

    public CycleForecast(EconomicIntelligenceEngine.EconomicCycle predictedCycle, double confidence, Duration horizon) {
        this.predictedCycle = predictedCycle;
        this.confidence = confidence;
        this.horizon = horizon;
    }

    public EconomicIntelligenceEngine.EconomicCycle getPredictedCycle() { return predictedCycle; }
    public double getConfidence() { return confidence; }
    public Duration getHorizon() { return horizon; }
}
