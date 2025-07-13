package me.koyere.ecoxpert.core.failsafe;

import java.time.Instant;
import java.util.Map;

/**
 * Health status of the failsafe system
 */
public class FailsafeHealth {
    private final boolean healthy;
    private final Instant lastCheck;
    private final Map<String, String> healthMetrics;
    private final String status;
    
    public FailsafeHealth(boolean healthy, Instant lastCheck, 
                         Map<String, String> healthMetrics, String status) {
        this.healthy = healthy;
        this.lastCheck = lastCheck;
        this.healthMetrics = Map.copyOf(healthMetrics);
        this.status = status;
    }
    
    public boolean isHealthy() {
        return healthy;
    }
    
    public Instant getLastCheck() {
        return lastCheck;
    }
    
    public Map<String, String> getHealthMetrics() {
        return healthMetrics;
    }
    
    public String getStatus() {
        return status;
    }
}