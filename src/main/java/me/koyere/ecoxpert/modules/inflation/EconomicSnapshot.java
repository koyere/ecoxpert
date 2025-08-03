package me.koyere.ecoxpert.modules.inflation;

import java.time.LocalDateTime;

/**
 * Economic Snapshot
 * 
 * Captures the complete economic state of the server at a specific moment.
 * Used by the Economic Intelligence Engine to analyze trends, make predictions,
 * and trigger intelligent interventions.
 */
public class EconomicSnapshot {
    
    private final LocalDateTime timestamp;
    private final double totalMoney;
    private final double averageBalance;
    private final int activeUsers;
    private final double transactionVolume;
    private final double marketActivity;
    private final EconomicIntelligenceEngine.EconomicCycle cycle;
    private final double economicHealth;
    private final double inflationRate;
    private final double velocityOfMoney;
    
    // Additional metrics for deep analysis
    private final double giniCoefficient; // Wealth inequality measure
    private final double economicMomentum; // Rate of economic change
    private final double marketVolatility; // Market stability measure
    
    public EconomicSnapshot(LocalDateTime timestamp, double totalMoney, double averageBalance,
                          int activeUsers, double transactionVolume, double marketActivity,
                          EconomicIntelligenceEngine.EconomicCycle cycle, double economicHealth,
                          double inflationRate, double velocityOfMoney) {
        this.timestamp = timestamp;
        this.totalMoney = totalMoney;
        this.averageBalance = averageBalance;
        this.activeUsers = activeUsers;
        this.transactionVolume = transactionVolume;
        this.marketActivity = marketActivity;
        this.cycle = cycle;
        this.economicHealth = economicHealth;
        this.inflationRate = inflationRate;
        this.velocityOfMoney = velocityOfMoney;
        
        // Calculate additional metrics
        this.giniCoefficient = calculateGiniCoefficient();
        this.economicMomentum = calculateEconomicMomentum();
        this.marketVolatility = calculateMarketVolatility();
    }
    
    /**
     * Calculate wealth inequality using Gini coefficient
     * 0.0 = perfect equality, 1.0 = maximum inequality
     */
    private double calculateGiniCoefficient() {
        // Simplified calculation for demo
        // In reality, this would analyze the distribution of player balances
        if (averageBalance <= 0) return 0.5;
        
        double inequality = Math.min(1.0, totalMoney / (activeUsers * averageBalance * 2));
        return Math.max(0.0, Math.min(1.0, inequality));
    }
    
    /**
     * Calculate economic momentum (rate of change)
     */
    private double calculateEconomicMomentum() {
        // This would be calculated by comparing with previous snapshots
        // For now, base it on transaction volume and market activity
        return (transactionVolume / 100000.0) * marketActivity;
    }
    
    /**
     * Calculate market volatility
     */
    private double calculateMarketVolatility() {
        // Market volatility based on activity patterns
        return Math.abs(marketActivity - 0.5) * 2.0;
    }
    
    /**
     * Get economic stress level (0.0 = no stress, 1.0 = maximum stress)
     */
    public double getEconomicStress() {
        double healthStress = 1.0 - economicHealth;
        double inequalityStress = giniCoefficient;
        double volatilityStress = marketVolatility;
        
        return (healthStress * 0.5 + inequalityStress * 0.3 + volatilityStress * 0.2);
    }
    
    /**
     * Get economic opportunity index (0.0 = no opportunities, 1.0 = maximum opportunities)
     */
    public double getEconomicOpportunityIndex() {
        double healthOpportunity = economicHealth;
        double activityOpportunity = marketActivity;
        double momentumOpportunity = Math.min(1.0, economicMomentum);
        
        return (healthOpportunity * 0.4 + activityOpportunity * 0.4 + momentumOpportunity * 0.2);
    }
    
    /**
     * Determine if intervention is needed
     */
    public boolean requiresIntervention() {
        return economicHealth < 0.4 || inflationRate > 0.1 || inflationRate < -0.05;
    }
    
    /**
     * Get recommended intervention type
     */
    public InterventionType getRecommendedIntervention() {
        if (economicHealth < 0.3) {
            return InterventionType.EMERGENCY_STIMULUS;
        } else if (inflationRate > 0.08) {
            return InterventionType.MONETARY_TIGHTENING;
        } else if (inflationRate < -0.03) {
            return InterventionType.MONETARY_EASING;
        } else if (marketActivity < 0.2) {
            return InterventionType.MARKET_STIMULATION;
        } else if (giniCoefficient > 0.8) {
            return InterventionType.WEALTH_REDISTRIBUTION;
        }
        
        return InterventionType.NONE;
    }
    
    public enum InterventionType {
        NONE,
        MONETARY_EASING,        // Inject money, lower interest rates
        MONETARY_TIGHTENING,    // Remove money, raise interest rates  
        MARKET_STIMULATION,     // Encourage trading activity
        EMERGENCY_STIMULUS,     // Crisis response - major intervention
        WEALTH_REDISTRIBUTION   // Address inequality
    }
    
    /**
     * Generate human-readable economic report
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Economic Snapshot Report ===\n");
        report.append("Timestamp: ").append(timestamp).append("\n");
        report.append("Economic Cycle: ").append(cycle).append("\n");
        report.append("Economic Health: ").append(String.format("%.1f%%", economicHealth * 100)).append("\n");
        report.append("Inflation Rate: ").append(String.format("%.2f%%", inflationRate * 100)).append("\n");
        report.append("Total Money Supply: $").append(String.format("%.2f", totalMoney)).append("\n");
        report.append("Average Balance: $").append(String.format("%.2f", averageBalance)).append("\n");
        report.append("Active Users: ").append(activeUsers).append("\n");
        report.append("Transaction Volume: $").append(String.format("%.2f", transactionVolume)).append("\n");
        report.append("Market Activity: ").append(String.format("%.1f%%", marketActivity * 100)).append("\n");
        report.append("Velocity of Money: ").append(String.format("%.2f", velocityOfMoney)).append("\n");
        report.append("Wealth Inequality (Gini): ").append(String.format("%.3f", giniCoefficient)).append("\n");
        report.append("Economic Momentum: ").append(String.format("%.2f", economicMomentum)).append("\n");
        report.append("Market Volatility: ").append(String.format("%.2f", marketVolatility)).append("\n");
        report.append("Economic Stress: ").append(String.format("%.1f%%", getEconomicStress() * 100)).append("\n");
        report.append("Opportunity Index: ").append(String.format("%.1f%%", getEconomicOpportunityIndex() * 100)).append("\n");
        
        InterventionType intervention = getRecommendedIntervention();
        if (intervention != InterventionType.NONE) {
            report.append("⚠️ Intervention Needed: ").append(intervention).append("\n");
        } else {
            report.append("✅ Economic conditions stable\n");
        }
        
        report.append("=== End Report ===");
        return report.toString();
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getTotalMoney() { return totalMoney; }
    public double getAverageBalance() { return averageBalance; }
    public int getActiveUsers() { return activeUsers; }
    public double getTransactionVolume() { return transactionVolume; }
    public double getMarketActivity() { return marketActivity; }
    public EconomicIntelligenceEngine.EconomicCycle getCycle() { return cycle; }
    public double getEconomicHealth() { return economicHealth; }
    public double getInflationRate() { return inflationRate; }
    public double getVelocityOfMoney() { return velocityOfMoney; }
    public double getGiniCoefficient() { return giniCoefficient; }
    public double getEconomicMomentum() { return economicMomentum; }
    public double getMarketVolatility() { return marketVolatility; }
}