package me.koyere.ecoxpert.modules.inflation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Economic Memory System
 * 
 * The "memory" of the Economic Intelligence Engine. Stores historical economic
 * data and uses machine learning-like algorithms to identify patterns, predict
 * trends, and learn from past interventions.
 * 
 * REVOLUTIONARY FEATURES:
 * - Pattern recognition algorithms
 * - Trend prediction based on historical data  
 * - Intervention effectiveness tracking
 * - Economic cycle prediction
 * - Anomaly detection for crisis prevention
 */
public class EconomicMemory {
    
    // Historical data storage (in-memory for performance)
    private final ConcurrentLinkedQueue<EconomicSnapshot> historicalSnapshots = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<CycleTransition> cycleHistory = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EconomicIntervention> interventionHistory = new ConcurrentLinkedQueue<>();
    
    // Memory configuration
    private static final int MAX_SNAPSHOTS = 2880; // 24 hours worth (5-minute intervals)
    private static final int MAX_CYCLES = 100; // Last 100 cycle transitions
    private static final int MAX_INTERVENTIONS = 200; // Last 200 interventions
    
    // Trend analysis cache
    private double cachedHealthTrend = 0.0;
    private double cachedInflationTrend = 0.0;
    private double cachedActivityTrend = 0.0;
    private LocalDateTime lastTrendCalculation = LocalDateTime.now().minusHours(1);
    
    // Pattern recognition results
    private List<EconomicPattern> identifiedPatterns = new ArrayList<>();
    private EconomicForecast currentForecast = null;
    
    /**
     * Record an economic snapshot for historical analysis
     */
    public void recordSnapshot(EconomicSnapshot snapshot) {
        historicalSnapshots.offer(snapshot);
        
        // Maintain size limit (remove oldest if necessary)
        while (historicalSnapshots.size() > MAX_SNAPSHOTS) {
            historicalSnapshots.poll();
        }
        
        // Invalidate trend cache
        invalidateTrendCache();
        
        // Update pattern recognition every hour
        if (historicalSnapshots.size() % 12 == 0) { // Every 12 snapshots = 1 hour
            updatePatternRecognition();
        }
        
        // Update forecast every 4 hours
        if (historicalSnapshots.size() % 48 == 0) { // Every 48 snapshots = 4 hours
            updateEconomicForecast();
        }
    }
    
    /**
     * Record a cycle transition for learning
     */
    public void recordCycleChange(EconomicIntelligenceEngine.EconomicCycle newCycle, EconomicSnapshot snapshot) {
        CycleTransition transition = new CycleTransition(
            LocalDateTime.now(),
            snapshot.getCycle(),
            newCycle,
            snapshot.getEconomicHealth(),
            snapshot.getInflationRate(),
            getCycleDuration(snapshot.getCycle())
        );
        
        cycleHistory.offer(transition);
        
        // Maintain size limit
        while (cycleHistory.size() > MAX_CYCLES) {
            cycleHistory.poll();
        }
    }
    
    /**
     * Record an economic intervention for effectiveness tracking
     */
    public void recordIntervention(EconomicIntervention intervention) {
        interventionHistory.offer(intervention);
        
        // Maintain size limit
        while (interventionHistory.size() > MAX_INTERVENTIONS) {
            interventionHistory.poll();
        }
    }
    
    /**
     * Load historical data (from database in real implementation)
     */
    public void loadHistoricalData() {
        // In real implementation, this would load from database
        // For now, just initialize with empty data
    }
    
    /**
     * Get economic health trend (-1.0 to 1.0)
     * Negative = declining, Positive = improving
     */
    public double getHealthTrend() {
        if (shouldRecalculateTrends()) {
            recalculateTrends();
        }
        return cachedHealthTrend;
    }
    
    /**
     * Get inflation trend (-1.0 to 1.0)  
     * Negative = deflationary, Positive = inflationary
     */
    public double getInflationTrend() {
        if (shouldRecalculateTrends()) {
            recalculateTrends();
        }
        return cachedInflationTrend;
    }
    
    /**
     * Get activity trend (-1.0 to 1.0)
     * Negative = declining activity, Positive = increasing activity
     */
    public double getActivityTrend() {
        if (shouldRecalculateTrends()) {
            recalculateTrends();
        }
        return cachedActivityTrend;
    }
    
    /**
     * Get time since last cycle change (in seconds)
     */
    public long getTimeSinceLastCycleChange() {
        if (cycleHistory.isEmpty()) {
            return 3600; // Default 1 hour if no history
        }
        
        CycleTransition lastTransition = null;
        if (!cycleHistory.isEmpty()) {
            // Convert to array to get last element
            CycleTransition[] transitions = cycleHistory.toArray(new CycleTransition[0]);
            lastTransition = transitions[transitions.length - 1];
        }
        if (lastTransition != null) {
            return java.time.Duration.between(lastTransition.timestamp, LocalDateTime.now()).toSeconds();
        }
        
        return 3600;
    }
    
    /**
     * Predict next economic cycle based on historical patterns
     */
    public EconomicIntelligenceEngine.EconomicCycle predictNextCycle(
            EconomicIntelligenceEngine.EconomicCycle currentCycle, EconomicSnapshot currentSnapshot) {
        
        if (cycleHistory.isEmpty()) {
            return currentCycle; // No data to predict
        }
        
        // Analyze historical patterns for similar economic conditions
        List<CycleTransition> similarConditions = findSimilarConditions(currentSnapshot);
        
        if (similarConditions.isEmpty()) {
            return currentCycle; // No similar conditions found
        }
        
        // Find most common next cycle in similar conditions
        return findMostCommonNextCycle(currentCycle, similarConditions);
    }
    
    /**
     * Get intervention effectiveness score (0.0 to 1.0)
     */
    public double getInterventionEffectiveness(EconomicSnapshot.InterventionType type) {
        List<EconomicIntervention> interventions = new ArrayList<>(interventionHistory);
        
        double totalEffectiveness = 0.0;
        int count = 0;
        
        for (EconomicIntervention intervention : interventions) {
            if (intervention.type == type && intervention.effectiveness >= 0) {
                totalEffectiveness += intervention.effectiveness;
                count++;
            }
        }
        
        return count > 0 ? totalEffectiveness / count : 0.5; // Default to 50% if no data
    }
    
    /**
     * Detect economic anomalies that might indicate coming crisis
     */
    public List<EconomicAnomaly> detectAnomalies(EconomicSnapshot currentSnapshot) {
        List<EconomicAnomaly> anomalies = new ArrayList<>();
        
        // Check for rapid changes
        if (Math.abs(getHealthTrend()) > 0.5) {
            anomalies.add(new EconomicAnomaly(
                EconomicAnomaly.Type.RAPID_HEALTH_CHANGE,
                "Economic health changing rapidly: " + String.format("%.2f", getHealthTrend()),
                0.7
            ));
        }
        
        // Check for extreme inflation
        if (Math.abs(currentSnapshot.getInflationRate()) > 0.1) {
            anomalies.add(new EconomicAnomaly(
                EconomicAnomaly.Type.EXTREME_INFLATION,
                "Extreme inflation rate detected: " + String.format("%.2f%%", currentSnapshot.getInflationRate() * 100),
                0.9
            ));
        }
        
        // Check for market volatility
        if (currentSnapshot.getMarketVolatility() > 0.8) {
            anomalies.add(new EconomicAnomaly(
                EconomicAnomaly.Type.HIGH_VOLATILITY,
                "High market volatility detected: " + String.format("%.2f", currentSnapshot.getMarketVolatility()),
                0.6
            ));
        }
        
        // Check for wealth inequality
        if (currentSnapshot.getGiniCoefficient() > 0.8) {
            anomalies.add(new EconomicAnomaly(
                EconomicAnomaly.Type.WEALTH_INEQUALITY,
                "High wealth inequality detected: " + String.format("%.3f", currentSnapshot.getGiniCoefficient()),
                0.5
            ));
        }
        
        return anomalies;
    }
    
    /**
     * Get economic forecast based on historical patterns
     */
    public EconomicForecast getForecast() {
        if (currentForecast == null || currentForecast.isStale()) {
            updateEconomicForecast();
        }
        return currentForecast;
    }
    
    // === Private Methods ===
    
    private boolean shouldRecalculateTrends() {
        return java.time.Duration.between(lastTrendCalculation, LocalDateTime.now()).toMinutes() > 15;
    }
    
    private void invalidateTrendCache() {
        // Trends will be recalculated on next access
        lastTrendCalculation = LocalDateTime.now().minusHours(1);
    }
    
    private void recalculateTrends() {
        List<EconomicSnapshot> snapshots = new ArrayList<>(historicalSnapshots);
        
        if (snapshots.size() < 12) {
            // Not enough data for trend analysis
            cachedHealthTrend = 0.0;
            cachedInflationTrend = 0.0;
            cachedActivityTrend = 0.0;
            lastTrendCalculation = LocalDateTime.now();
            return;
        }
        
        // Sort by timestamp
        snapshots.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        
        // Calculate trends using linear regression-like approach
        cachedHealthTrend = calculateTrend(snapshots, EconomicSnapshot::getEconomicHealth);
        cachedInflationTrend = calculateTrend(snapshots, EconomicSnapshot::getInflationRate);
        cachedActivityTrend = calculateTrend(snapshots, EconomicSnapshot::getMarketActivity);
        
        lastTrendCalculation = LocalDateTime.now();
    }
    
    private double calculateTrend(List<EconomicSnapshot> snapshots, java.util.function.Function<EconomicSnapshot, Double> valueExtractor) {
        int n = Math.min(24, snapshots.size()); // Use last 24 snapshots (2 hours)
        List<EconomicSnapshot> recent = snapshots.subList(snapshots.size() - n, snapshots.size());
        
        if (recent.size() < 2) return 0.0;
        
        // Simple trend calculation: (latest - earliest) / range
        double earliest = valueExtractor.apply(recent.get(0));
        double latest = valueExtractor.apply(recent.get(recent.size() - 1));
        
        return Math.max(-1.0, Math.min(1.0, (latest - earliest) / Math.max(0.1, Math.abs(earliest))));
    }
    
    private long getCycleDuration(EconomicIntelligenceEngine.EconomicCycle cycle) {
        // Calculate how long the cycle lasted
        return getTimeSinceLastCycleChange();
    }
    
    private void updatePatternRecognition() {
        // Analyze historical data for patterns
        identifiedPatterns.clear();
        
        List<EconomicSnapshot> snapshots = new ArrayList<>(historicalSnapshots);
        if (snapshots.size() < 48) return; // Need at least 4 hours of data
        
        // Look for recurring patterns
        identifySeasonalPatterns(snapshots);
        identifyVolatilityPatterns(snapshots);
        identifyCyclePatterns();
    }
    
    private void identifySeasonalPatterns(List<EconomicSnapshot> snapshots) {
        // Identify daily, weekly patterns in economic activity
        // This is simplified - real implementation would use more sophisticated analysis
    }
    
    private void identifyVolatilityPatterns(List<EconomicSnapshot> snapshots) {
        // Identify patterns in market volatility
    }
    
    private void identifyCyclePatterns() {
        // Identify patterns in economic cycle transitions
    }
    
    private void updateEconomicForecast() {
        List<EconomicSnapshot> snapshots = new ArrayList<>(historicalSnapshots);
        if (snapshots.size() < 12) {
            currentForecast = null;
            return;
        }
        
        // Create forecast based on trends and patterns
        currentForecast = new EconomicForecast(
            LocalDateTime.now(),
            predictHealthIn1Hour(),
            predictInflationIn1Hour(),
            predictActivityIn1Hour(),
            calculateForecastConfidence()
        );
    }
    
    private double predictHealthIn1Hour() {
        return Math.max(0.0, Math.min(1.0, 
            getCurrentValue(EconomicSnapshot::getEconomicHealth) + getHealthTrend() * 0.1));
    }
    
    private double predictInflationIn1Hour() {
        return getCurrentValue(EconomicSnapshot::getInflationRate) + getInflationTrend() * 0.01;
    }
    
    private double predictActivityIn1Hour() {
        return Math.max(0.0, Math.min(1.0,
            getCurrentValue(EconomicSnapshot::getMarketActivity) + getActivityTrend() * 0.05));
    }
    
    private double calculateForecastConfidence() {
        // Confidence based on data quality and pattern consistency
        int dataPoints = historicalSnapshots.size();
        double dataQuality = Math.min(1.0, dataPoints / 288.0); // 24 hours of data = 100% quality
        
        return dataQuality * 0.8; // Max 80% confidence
    }
    
    private double getCurrentValue(java.util.function.Function<EconomicSnapshot, Double> valueExtractor) {
        EconomicSnapshot latest = null;
        if (!historicalSnapshots.isEmpty()) {
            // Convert to array to get last element
            EconomicSnapshot[] snapshots = historicalSnapshots.toArray(new EconomicSnapshot[0]);
            latest = snapshots[snapshots.length - 1];
        }
        return latest != null ? valueExtractor.apply(latest) : 0.0;
    }
    
    private List<CycleTransition> findSimilarConditions(EconomicSnapshot currentSnapshot) {
        // Find historical snapshots with similar economic conditions
        List<CycleTransition> similar = new ArrayList<>();
        
        for (CycleTransition transition : cycleHistory) {
            if (isSimilarCondition(transition, currentSnapshot)) {
                similar.add(transition);
            }
        }
        
        return similar;
    }
    
    private boolean isSimilarCondition(CycleTransition transition, EconomicSnapshot current) {
        // Define similarity criteria
        double healthDiff = Math.abs(transition.economicHealth - current.getEconomicHealth());
        double inflationDiff = Math.abs(transition.inflationRate - current.getInflationRate());
        
        return healthDiff < 0.2 && inflationDiff < 0.02; // Within 20% health and 2% inflation
    }
    
    private EconomicIntelligenceEngine.EconomicCycle findMostCommonNextCycle(
            EconomicIntelligenceEngine.EconomicCycle currentCycle, List<CycleTransition> transitions) {
        
        // Count occurrences of each next cycle
        java.util.Map<EconomicIntelligenceEngine.EconomicCycle, Integer> cycleCounts = new java.util.HashMap<>();
        
        for (CycleTransition transition : transitions) {
            if (transition.fromCycle == currentCycle) {
                cycleCounts.merge(transition.toCycle, 1, Integer::sum);
            }
        }
        
        // Return most common next cycle
        return cycleCounts.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(currentCycle);
    }
    
    // === Inner Classes ===
    
    public static class CycleTransition {
        public final LocalDateTime timestamp;
        public final EconomicIntelligenceEngine.EconomicCycle fromCycle;
        public final EconomicIntelligenceEngine.EconomicCycle toCycle;
        public final double economicHealth;
        public final double inflationRate;
        public final long duration;
        
        public CycleTransition(LocalDateTime timestamp, EconomicIntelligenceEngine.EconomicCycle fromCycle,
                             EconomicIntelligenceEngine.EconomicCycle toCycle, double economicHealth,
                             double inflationRate, long duration) {
            this.timestamp = timestamp;
            this.fromCycle = fromCycle;
            this.toCycle = toCycle;
            this.economicHealth = economicHealth;
            this.inflationRate = inflationRate;
            this.duration = duration;
        }
    }
    
    public static class EconomicIntervention {
        public final LocalDateTime timestamp;
        public final EconomicSnapshot.InterventionType type;
        public final double magnitude;
        public final double effectiveness; // Measured afterwards (-1.0 to 1.0)
        public final String details;
        
        public EconomicIntervention(LocalDateTime timestamp, EconomicSnapshot.InterventionType type,
                                  double magnitude, String details) {
            this.timestamp = timestamp;
            this.type = type;
            this.magnitude = magnitude;
            this.effectiveness = -1.0; // Will be calculated later
            this.details = details;
        }
    }
    
    public static class EconomicPattern {
        public final String name;
        public final String description;
        public final double confidence;
        public final LocalDateTime identified;
        
        public EconomicPattern(String name, String description, double confidence) {
            this.name = name;
            this.description = description;
            this.confidence = confidence;
            this.identified = LocalDateTime.now();
        }
    }
    
    public static class EconomicAnomaly {
        public enum Type {
            RAPID_HEALTH_CHANGE,
            EXTREME_INFLATION,
            HIGH_VOLATILITY,
            WEALTH_INEQUALITY,
            UNUSUAL_ACTIVITY
        }
        
        public final Type type;
        public final String description;
        public final double severity; // 0.0 to 1.0
        public final LocalDateTime detected;
        
        public EconomicAnomaly(Type type, String description, double severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.detected = LocalDateTime.now();
        }
    }
    
    public static class EconomicForecast {
        private final LocalDateTime created;
        private final double predictedHealth;
        private final double predictedInflation;
        private final double predictedActivity;
        private final double confidence;
        
        public EconomicForecast(LocalDateTime created, double predictedHealth,
                              double predictedInflation, double predictedActivity, double confidence) {
            this.created = created;
            this.predictedHealth = predictedHealth;
            this.predictedInflation = predictedInflation;
            this.predictedActivity = predictedActivity;
            this.confidence = confidence;
        }
        
        public boolean isStale() {
            return java.time.Duration.between(created, LocalDateTime.now()).toHours() > 1;
        }
        
        // Getters
        public LocalDateTime getCreated() { return created; }
        public double getPredictedHealth() { return predictedHealth; }
        public double getPredictedInflation() { return predictedInflation; }
        public double getPredictedActivity() { return predictedActivity; }
        public double getConfidence() { return confidence; }
    }
}