package me.koyere.ecoxpert.core.safety;

/**
 * Safe Mode manager monitors backend health (DB latency, error rate)
 * and exposes a simple active flag to degrade functionality gracefully
 * without disrupting server operations.
 */
public interface SafeModeManager {
    void initialize();
    void shutdown();

    boolean isActive();

    /** Record a critical error to evaluate error-rate threshold. */
    void recordCriticalError();
}

