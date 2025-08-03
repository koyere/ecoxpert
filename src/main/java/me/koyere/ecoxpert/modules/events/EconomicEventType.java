package me.koyere.ecoxpert.modules.events;

/**
 * Economic Event Types
 * 
 * Defines all types of dynamic economic events that can occur in the EcoXpert
 * intelligent economy system. Each event type represents a different economic
 * scenario that affects the server economy in realistic ways.
 */
public enum EconomicEventType {
    
    // === Crisis Response Events ===
    /**
     * Government Economic Stimulus
     * Triggered during economic downturns to inject liquidity and boost activity
     */
    GOVERNMENT_STIMULUS("Government Stimulus", "Crisis Response"),
    
    /**
     * Trade Boom Period
     * Increased trading activity with bonus rewards and incentives
     */
    TRADE_BOOM("Trade Boom", "Economic Recovery"),
    
    // === Discovery and Innovation Events ===
    /**
     * Market Discovery
     * New valuable resources or opportunities discovered
     */
    MARKET_DISCOVERY("Market Discovery", "Innovation"),
    
    /**
     * Technological Breakthrough
     * Innovation events that create new market opportunities
     */
    TECHNOLOGICAL_BREAKTHROUGH("Tech Breakthrough", "Innovation"),
    
    // === Market Events ===
    /**
     * Investment Opportunity
     * Special investment opportunities with enhanced returns
     */
    INVESTMENT_OPPORTUNITY("Investment Opportunity", "Market"),
    
    /**
     * Luxury Goods Demand
     * High demand for luxury items during prosperity periods
     */
    LUXURY_DEMAND("Luxury Demand", "Market"),
    
    /**
     * Market Correction
     * Natural market adjustment to prevent bubbles
     */
    MARKET_CORRECTION("Market Correction", "Market Adjustment"),
    
    // === Resource Events ===
    /**
     * Resource Shortage
     * Temporary scarcity that increases specific resource values
     */
    RESOURCE_SHORTAGE("Resource Shortage", "Supply Chain"),
    
    /**
     * Seasonal Market Demand
     * Cyclical demand changes for specific goods
     */
    SEASONAL_DEMAND("Seasonal Demand", "Natural Cycle"),
    
    // === Special Events ===
    /**
     * Black Swan Event
     * Rare, unpredictable events with major economic impact
     */
    BLACK_SWAN_EVENT("Black Swan Event", "Crisis");
    
    private final String displayName;
    private final String category;
    
    EconomicEventType(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCategory() {
        return category;
    }
    
    /**
     * Check if this event type is a crisis response event
     */
    public boolean isCrisisResponse() {
        return this == GOVERNMENT_STIMULUS || this == TRADE_BOOM;
    }
    
    /**
     * Check if this event type is a positive economic event
     */
    public boolean isPositive() {
        return this != MARKET_CORRECTION && this != RESOURCE_SHORTAGE && this != BLACK_SWAN_EVENT;
    }
    
    /**
     * Get the typical impact level of this event type
     */
    public ImpactLevel getImpactLevel() {
        return switch (this) {
            case BLACK_SWAN_EVENT -> ImpactLevel.EXTREME;
            case GOVERNMENT_STIMULUS, MARKET_CORRECTION -> ImpactLevel.HIGH;
            case TRADE_BOOM, TECHNOLOGICAL_BREAKTHROUGH, RESOURCE_SHORTAGE -> ImpactLevel.MEDIUM;
            case INVESTMENT_OPPORTUNITY, LUXURY_DEMAND, MARKET_DISCOVERY -> ImpactLevel.MEDIUM;
            case SEASONAL_DEMAND -> ImpactLevel.LOW;
        };
    }
    
    /**
     * Impact levels for economic events
     */
    public enum ImpactLevel {
        LOW(0.3),
        MEDIUM(0.6),
        HIGH(0.8),
        EXTREME(1.0);
        
        private final double intensity;
        
        ImpactLevel(double intensity) {
            this.intensity = intensity;
        }
        
        public double getIntensity() {
            return intensity;
        }
    }
}