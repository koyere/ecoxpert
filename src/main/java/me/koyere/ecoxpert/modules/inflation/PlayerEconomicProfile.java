package me.koyere.ecoxpert.modules.inflation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player Economic Profile
 * 
 * Creates unique economic profiles for each player based on their behavior.
 * This allows the Economic Intelligence Engine to:
 * - Predict player economic impact
 * - Personalize economic interventions
 * - Reward positive economic behavior
 * - Identify and prevent economic exploitation
 * 
 * REVOLUTIONARY FEATURES:
 * - Economic personality classification
 * - Behavior prediction algorithms
 * - Dynamic credit scoring
 * - Contribution to server economy measurement
 */
public class PlayerEconomicProfile {
    
    private final UUID playerId;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    
    // Economic Behavior Metrics
    private double totalTransactionVolume = 0.0;
    private int transactionCount = 0;
    private double averageTransactionSize = 0.0;
    private double balanceVolatility = 0.0; // How much their balance fluctuates
    
    // Market Behavior
    private int marketTransactions = 0;
    private double marketVolume = 0.0;
    private double marketProfitability = 0.0; // How profitable their trades are
    private boolean isMarketMaker = false; // Do they provide market liquidity?
    
    // Banking Behavior  
    private double totalSaved = 0.0;
    private double interestEarned = 0.0;
    private int loanHistory = 0; // Number of loans taken
    private double creditScore = 700.0; // Starting credit score
    
    // Economic Impact Metrics
    private double economicContribution = 0.0; // Positive impact on server economy
    private double inflationContribution = 0.0; // How much they contribute to inflation
    private double velocityContribution = 1.0; // How much they contribute to money velocity
    
    // Behavioral Classification
    private EconomicPersonality personality = EconomicPersonality.UNKNOWN;
    private List<EconomicBehaviorPattern> behaviorPatterns = new ArrayList<>();
    
    // Predictive Metrics
    private double predictedFutureBalance = 0.0;
    private double riskScore = 0.5; // 0.0 = no risk, 1.0 = high risk
    private double trustScore = 0.5; // 0.0 = no trust, 1.0 = fully trusted
    
    public enum EconomicPersonality {
        UNKNOWN,        // Not enough data
        SAVER,          // Tends to save money, low spending
        SPENDER,        // High spending, low savings
        TRADER,         // Active market participant
        INVESTOR,       // Long-term wealth building
        SPECULATOR,     // High-risk, high-reward behavior
        HOARDER,        // Accumulates wealth, minimal transactions
        PHILANTHROPIST, // Generous, helps other players
        EXPLOITER       // Attempts to exploit economic systems
    }
    
    public enum EconomicBehaviorPattern {
        CONSISTENT_SAVER,     // Regular savings pattern
        PANIC_SELLER,         // Sells during market downturns
        MOMENTUM_TRADER,      // Follows market trends
        CONTRARIAN,           // Goes against market trends
        WHALE,                // Large transaction sizes
        MICRO_TRADER,         // Many small transactions
        SEASONAL_SPENDER,     // Spending varies by time
        DEBT_ACCUMULATOR,     // Takes many loans
        MARKET_MAKER,         // Provides market liquidity
        ECONOMIC_CATALYST     // Actions trigger economic changes
    }
    
    public PlayerEconomicProfile(UUID playerId) {
        this.playerId = playerId;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Update behavior metrics based on recent activity
     */
    public void updateBehaviorMetrics() {
        lastUpdated = LocalDateTime.now();
        
        // Update averages
        if (transactionCount > 0) {
            averageTransactionSize = totalTransactionVolume / transactionCount;
        }
        
        // Update market metrics
        if (marketTransactions > 0) {
            isMarketMaker = marketTransactions > 50 && marketVolume > averageTransactionSize * 20;
        }
        
        // Update behavioral patterns
        updateBehaviorPatterns();
        
        // Update personality classification
        updatePersonalityClassification();
        
        // Update predictive metrics
        updatePredictiveMetrics();
    }
    
    /**
     * Calculate economic impact on server
     */
    public void calculateEconomicImpact() {
        // Positive contributions
        double positiveImpact = 0.0;
        positiveImpact += marketVolume * 0.1; // Market activity boosts economy
        positiveImpact += Math.min(velocityContribution, 2.0) * 1000; // Money circulation
        positiveImpact += totalSaved * 0.05; // Savings provide stability
        
        // Negative contributions  
        double negativeImpact = 0.0;
        negativeImpact += Math.max(0, inflationContribution) * 1000; // Inflation is bad
        negativeImpact += Math.max(0, riskScore - 0.5) * 500; // High risk behavior
        
        economicContribution = positiveImpact - negativeImpact;
    }
    
    /**
     * Update behavior patterns based on activity
     */
    private void updateBehaviorPatterns() {
        behaviorPatterns.clear();
        
        // Analyze transaction patterns
        if (averageTransactionSize > 10000) {
            behaviorPatterns.add(EconomicBehaviorPattern.WHALE);
        } else if (averageTransactionSize < 100 && transactionCount > 100) {
            behaviorPatterns.add(EconomicBehaviorPattern.MICRO_TRADER);
        }
        
        // Analyze saving patterns
        if (totalSaved > totalTransactionVolume * 0.5) {
            behaviorPatterns.add(EconomicBehaviorPattern.CONSISTENT_SAVER);
        }
        
        // Analyze market behavior
        if (isMarketMaker) {
            behaviorPatterns.add(EconomicBehaviorPattern.MARKET_MAKER);
        }
        
        // Analyze economic impact
        if (Math.abs(economicContribution) > 5000) {
            behaviorPatterns.add(EconomicBehaviorPattern.ECONOMIC_CATALYST);
        }
    }
    
    /**
     * Update personality classification using AI-like logic
     */
    private void updatePersonalityClassification() {
        if (transactionCount < 10) {
            personality = EconomicPersonality.UNKNOWN;
            return;
        }
        
        // Calculate personality scores
        double saverScore = calculateSaverScore();
        double traderScore = calculateTraderScore();
        double spenderScore = calculateSpenderScore();
        double investorScore = calculateInvestorScore();
        double speculatorScore = calculateSpeculatorScore();
        double hoarderScore = calculateHoarderScore();
        
        // Find dominant personality
        double maxScore = Math.max(saverScore, Math.max(traderScore, 
            Math.max(spenderScore, Math.max(investorScore, 
            Math.max(speculatorScore, hoarderScore)))));
        
        if (maxScore == saverScore) personality = EconomicPersonality.SAVER;
        else if (maxScore == traderScore) personality = EconomicPersonality.TRADER;
        else if (maxScore == spenderScore) personality = EconomicPersonality.SPENDER;
        else if (maxScore == investorScore) personality = EconomicPersonality.INVESTOR;
        else if (maxScore == speculatorScore) personality = EconomicPersonality.SPECULATOR;
        else if (maxScore == hoarderScore) personality = EconomicPersonality.HOARDER;
        
        // Check for special personalities
        if (riskScore > 0.8 && trustScore < 0.3) {
            personality = EconomicPersonality.EXPLOITER;
        } else if (economicContribution > 10000) {
            personality = EconomicPersonality.PHILANTHROPIST;
        }
    }
    
    /**
     * Update predictive metrics using behavioral analysis
     */
    private void updatePredictiveMetrics() {
        // Predict future balance based on patterns
        double growthRate = calculateExpectedGrowthRate();
        predictedFutureBalance = getCurrentBalance() * (1.0 + growthRate);
        
        // Update risk score based on behavior
        riskScore = calculateRiskScore();
        
        // Update trust score based on positive economic contributions
        trustScore = calculateTrustScore();
        
        // Update credit score
        updateCreditScore();
    }
    
    // === Personality Score Calculations ===
    
    private double calculateSaverScore() {
        double savingsRatio = totalSaved / Math.max(totalTransactionVolume, 1.0);
        double lowSpendingBonus = Math.max(0, 1.0 - (totalTransactionVolume / 100000.0));
        return savingsRatio * 0.7 + lowSpendingBonus * 0.3;
    }
    
    private double calculateTraderScore() {
        double marketActivityRatio = marketVolume / Math.max(totalTransactionVolume, 1.0);
        double frequencyBonus = Math.min(1.0, marketTransactions / 50.0);
        return marketActivityRatio * 0.6 + frequencyBonus * 0.4;
    }
    
    private double calculateSpenderScore() {
        double spendingRatio = totalTransactionVolume / Math.max(totalSaved + totalTransactionVolume, 1.0);
        double velocityBonus = Math.min(1.0, velocityContribution);
        return spendingRatio * 0.7 + velocityBonus * 0.3;
    }
    
    private double calculateInvestorScore() {
        double longTermSavings = totalSaved > 50000 ? 1.0 : totalSaved / 50000.0;
        double consistencyBonus = 1.0 - balanceVolatility; // Low volatility = consistent
        return longTermSavings * 0.6 + consistencyBonus * 0.4;
    }
    
    private double calculateSpeculatorScore() {
        double volatilityScore = balanceVolatility;
        double riskTakingScore = riskScore;
        return volatilityScore * 0.5 + riskTakingScore * 0.5;
    }
    
    private double calculateHoarderScore() {
        if (totalTransactionVolume < 1000) return 1.0; // Very low activity
        double lowActivityScore = 1.0 - Math.min(1.0, transactionCount / 100.0);
        double highBalanceScore = Math.min(1.0, getCurrentBalance() / 100000.0);
        return lowActivityScore * 0.6 + highBalanceScore * 0.4;
    }
    
    // === Predictive Calculations ===
    
    private double calculateExpectedGrowthRate() {
        // Base growth on personality and behavior patterns
        double baseGrowth = switch (personality) {
            case INVESTOR -> 0.1; // 10% expected growth
            case TRADER -> 0.05; // 5% expected growth  
            case SPECULATOR -> 0.15; // 15% expected growth (high risk/reward)
            case SAVER -> 0.02; // 2% expected growth (conservative)
            case SPENDER -> -0.05; // -5% expected (spending more than earning)
            case HOARDER -> 0.01; // 1% minimal growth
            default -> 0.0;
        };
        
        // Adjust based on market activity
        if (isMarketMaker) baseGrowth += 0.03;
        
        // Adjust based on economic contribution
        baseGrowth += economicContribution / 100000.0;
        
        return Math.max(-0.5, Math.min(0.5, baseGrowth)); // Cap at ±50%
    }
    
    private double calculateRiskScore() {
        double behaviorRisk = balanceVolatility;
        double marketRisk = marketTransactions > 0 ? Math.min(1.0, marketVolume / totalTransactionVolume) : 0.0;
        double creditRisk = loanHistory > 5 ? 0.3 : 0.0;
        
        return Math.max(0.0, Math.min(1.0, (behaviorRisk * 0.5 + marketRisk * 0.3 + creditRisk * 0.2)));
    }
    
    private double calculateTrustScore() {
        double contributionScore = Math.max(0.0, Math.min(1.0, economicContribution / 10000.0));
        double consistencyScore = 1.0 - balanceVolatility;
        double longevityScore = Math.min(1.0, java.time.Duration.between(createdAt, LocalDateTime.now()).toDays() / 365.0);
        
        return (contributionScore * 0.4 + consistencyScore * 0.3 + longevityScore * 0.3);
    }
    
    private void updateCreditScore() {
        // Start with base score
        double newScore = 700.0;
        
        // Adjust based on behavior
        newScore += (trustScore - 0.5) * 200; // ±100 points for trust
        newScore -= (riskScore - 0.5) * 150; // ±75 points for risk
        newScore += Math.min(100, economicContribution / 100); // Up to +100 for contribution
        newScore -= loanHistory * 10; // -10 points per loan
        
        // Add interest earned bonus
        newScore += Math.min(50, interestEarned / 100); // Up to +50 for interest earned
        
        creditScore = Math.max(300, Math.min(850, newScore)); // Standard credit score range
    }
    
    // === Utility Methods ===
    
    private double getCurrentBalance() {
        // This would query the actual player balance
        return 25000.0; // Placeholder
    }
    
    /**
     * Record a transaction for behavior analysis
     */
    public void recordTransaction(double amount, String type) {
        totalTransactionVolume += Math.abs(amount);
        transactionCount++;
        
        if ("MARKET".equals(type)) {
            marketTransactions++;
            marketVolume += Math.abs(amount);
        }
        
        // Update volatility (simplified)
        balanceVolatility = Math.min(1.0, balanceVolatility * 0.9 + Math.abs(amount) / 10000.0 * 0.1);
        
        lastUpdated = LocalDateTime.now();
    }
    
    /**
     * Get recommended economic actions for this player
     */
    public List<String> getRecommendedActions() {
        List<String> recommendations = new ArrayList<>();
        
        if (personality == EconomicPersonality.SPENDER && totalSaved < 1000) {
            recommendations.add("Consider saving some money in the bank for future opportunities");
        }
        
        if (personality == EconomicPersonality.HOARDER && transactionCount < 10) {
            recommendations.add("Try participating in the market to grow your wealth");
        }
        
        if (creditScore < 600) {
            recommendations.add("Improve your credit score by making regular transactions and contributing to the economy");
        }
        
        if (isMarketMaker) {
            recommendations.add("Great job providing market liquidity! You're helping the server economy");
        }
        
        return recommendations;
    }
    
    // Getters and Setters
    public UUID getPlayerId() { return playerId; }
    public EconomicPersonality getPersonality() { return personality; }
    public List<EconomicBehaviorPattern> getBehaviorPatterns() { return behaviorPatterns; }
    public double getEconomicContribution() { return economicContribution; }
    public double getCreditScore() { return creditScore; }
    public double getRiskScore() { return riskScore; }
    public double getTrustScore() { return trustScore; }
    public double getPredictedFutureBalance() { return predictedFutureBalance; }
    public double getTotalTransactionVolume() { return totalTransactionVolume; }
    public int getTransactionCount() { return transactionCount; }
    public boolean isMarketMaker() { return isMarketMaker; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
}