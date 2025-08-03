package me.koyere.ecoxpert.modules.inflation;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Inflation Manager Interface
 * 
 * The main interface for EcoXpert's revolutionary inflation control system.
 * This isn't just traditional inflation management - it's a complete
 * economic intelligence system that creates a living, breathing economy.
 */
public interface InflationManager {
    
    /**
     * Initialize the inflation management system
     * 
     * @return CompletableFuture that completes when system is ready
     */
    CompletableFuture<Void> initialize();
    
    /**
     * Shutdown the inflation management system
     */
    void shutdown();
    
    /**
     * Get current economic cycle
     * 
     * @return Current economic cycle (STABLE, GROWTH, RECESSION, etc.)
     */
    EconomicIntelligenceEngine.EconomicCycle getCurrentCycle();
    
    /**
     * Get current economic health (0.0 = crisis, 1.0 = perfect)
     * 
     * @return Economic health score
     */
    double getEconomicHealth();
    
    /**
     * Get current inflation rate (-1.0 = deflation, +1.0 = hyperinflation)
     * 
     * @return Current inflation rate
     */
    double getInflationRate();
    
    /**
     * Get current velocity of money (how fast money circulates)
     * 
     * @return Velocity of money
     */
    double getVelocityOfMoney();
    
    /**
     * Get economic snapshot with current conditions
     * 
     * @return CompletableFuture with current economic snapshot
     */
    CompletableFuture<EconomicSnapshot> getCurrentSnapshot();
    
    /**
     * Get player economic profile
     * 
     * @param playerId Player UUID
     * @return Player's economic profile
     */
    PlayerEconomicProfile getPlayerProfile(UUID playerId);
    
    /**
     * Get economic forecast for the next period
     * 
     * @return Economic forecast with predictions
     */
    EconomicMemory.EconomicForecast getEconomicForecast();
    
    /**
     * Detect economic anomalies that might indicate problems
     * 
     * @return List of detected anomalies
     */
    List<EconomicMemory.EconomicAnomaly> detectAnomalies();
    
    /**
     * Force an economic intervention (admin command)
     * 
     * @param type Type of intervention to apply
     * @param magnitude Strength of the intervention (0.0 to 1.0)
     * @return CompletableFuture that completes when intervention is applied
     */
    CompletableFuture<Void> forceIntervention(EconomicSnapshot.InterventionType type, double magnitude);
    
    /**
     * Get intervention effectiveness for a specific type
     * 
     * @param type Intervention type
     * @return Effectiveness score (0.0 to 1.0)
     */
    double getInterventionEffectiveness(EconomicSnapshot.InterventionType type);
    
    /**
     * Record a player transaction for behavioral analysis
     * 
     * @param playerId Player UUID
     * @param amount Transaction amount
     * @param type Transaction type (MARKET, BANK, TRANSFER, etc.)
     */
    void recordPlayerTransaction(UUID playerId, double amount, String type);
    
    /**
     * Get economic statistics summary
     * 
     * @return Economic statistics as formatted string
     */
    String getEconomicStatistics();
    
    /**
     * Run economic diagnostics
     * 
     * @return Diagnostic report
     */
    String runDiagnostics();
    
    /**
     * Check if the inflation system is running
     * 
     * @return true if system is active
     */
    boolean isActive();
    
    /**
     * Get system status information
     * 
     * @return Status information
     */
    InflationSystemStatus getSystemStatus();
    
    /**
     * System Status Information
     */
    class InflationSystemStatus {
        private final boolean active;
        private final EconomicIntelligenceEngine.EconomicCycle currentCycle;
        private final double economicHealth;
        private final double inflationRate;
        private final int trackedPlayers;
        private final long dataPoints;
        private final String lastIntervention;
        
        public InflationSystemStatus(boolean active, EconomicIntelligenceEngine.EconomicCycle currentCycle,
                                   double economicHealth, double inflationRate, int trackedPlayers,
                                   long dataPoints, String lastIntervention) {
            this.active = active;
            this.currentCycle = currentCycle;
            this.economicHealth = economicHealth;
            this.inflationRate = inflationRate;
            this.trackedPlayers = trackedPlayers;
            this.dataPoints = dataPoints;
            this.lastIntervention = lastIntervention;
        }
        
        // Getters
        public boolean isActive() { return active; }
        public EconomicIntelligenceEngine.EconomicCycle getCurrentCycle() { return currentCycle; }
        public double getEconomicHealth() { return economicHealth; }
        public double getInflationRate() { return inflationRate; }
        public int getTrackedPlayers() { return trackedPlayers; }
        public long getDataPoints() { return dataPoints; }
        public String getLastIntervention() { return lastIntervention; }
        
        @Override
        public String toString() {
            return String.format(
                "InflationSystem{active=%s, cycle=%s, health=%.1f%%, inflation=%.2f%%, players=%d, data=%d}",
                active, currentCycle, economicHealth * 100, inflationRate * 100, trackedPlayers, dataPoints
            );
        }
    }
}