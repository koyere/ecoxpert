package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;

import java.time.Duration;

/** Forecast result for server economic cycle. */
public class CycleForecast {
    private final EconomicIntelligenceEngine.EconomicCycle cycle;
    private final double confidence; // 0..1
    private final Duration horizon;

    public CycleForecast(EconomicIntelligenceEngine.EconomicCycle cycle, double confidence, Duration horizon) {
        this.cycle = cycle;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence));
        this.horizon = horizon;
    }

    public EconomicIntelligenceEngine.EconomicCycle getCycle() { return cycle; }
    public double getConfidence() { return confidence; }
    public Duration getHorizon() { return horizon; }
}

