package me.koyere.ecoxpert.api;

import java.util.concurrent.CompletableFuture;

/**
 * EcoXpert Pro Public API
 * 
 * Professional economy plugin API providing comprehensive
 * access to all economic systems and analytics.
 */
public interface EcoXpertAPI {
    
    /**
     * Get the API version
     * 
     * @return API version string
     */
    String getAPIVersion();
    
    /**
     * Get the plugin version
     * 
     * @return Plugin version string
     */
    String getPluginVersion();
    
    /**
     * Check if the API is ready for use
     * 
     * @return true if API is initialized
     */
    boolean isReady();
    
    /**
     * Get economy service for basic operations
     * 
     * @return Economy service instance
     */
    EconomyService getEconomyService();
    
    /**
     * Get market analytics service
     * 
     * @return Market service instance
     */
    MarketService getMarketService();
    
    /**
     * Get banking service
     * 
     * @return Banking service instance
     */
    BankingService getBankingService();
    
    /**
     * Get loan management service
     * 
     * @return Loan service instance
     */
    LoanService getLoanService();
    
    /**
     * Get economic events service
     * 
     * @return Events service instance
     */
    EventsService getEventsService();
    
    /**
     * Get profession management service
     * 
     * @return Profession service instance
     */
    ProfessionService getProfessionService();
    
    /**
     * Get inflation control service
     * 
     * @return Inflation service instance
     */
    InflationService getInflationService();

    /**
     * Get a lightweight snapshot of server economics for quick dashboards.
     */
    ServerEconomySnapshot getServerEconomics();

    /**
     * Forecast the economic cycle for a given horizon using the intelligence engine
     * and recent metrics. Returned object contains the predicted cycle and confidence.
     */
    CycleForecast forecastCycle(java.time.Duration horizon);

    /**
     * Get extended player economy view with risk score and wealth percentile.
     */
    PlayerEconomyView getPlayerEconomyView(java.util.UUID playerId);
}

/**
 * Basic economy operations service
 */
interface EconomyService {
    // TODO: Define economy operations
}

/**
 * Market analytics and pricing service
 */
interface MarketService {
    // TODO: Define market operations
}

/**
 * Banking and interest management service
 */
interface BankingService {
    // TODO: Define banking operations
}

/**
 * Loan and credit management service
 */
interface LoanService {
    // TODO: Define loan operations
}

/**
 * Economic events management service
 */
interface EventsService {
    // TODO: Define events operations
}

/**
 * Profession and role management service
 */
interface ProfessionService {
    // TODO: Define profession operations
}

/**
 * Inflation control and monitoring service
 */
interface InflationService {
    // TODO: Define inflation operations
}

/** Lightweight snapshot for dashboards and integrations */
class ServerEconomySnapshot {
    private final String cycle;
    private final double economicHealth; // 0..1
    private final double inflationRate;  // fraction, e.g., 0.02
    private final double marketActivity; // 0..1
    private final int activeEvents;

    public ServerEconomySnapshot(String cycle, double economicHealth, double inflationRate, double marketActivity, int activeEvents) {
        this.cycle = cycle;
        this.economicHealth = Math.max(0.0, Math.min(1.0, economicHealth));
        this.inflationRate = inflationRate;
        this.marketActivity = Math.max(0.0, Math.min(1.0, marketActivity));
        this.activeEvents = Math.max(0, activeEvents);
    }

    public String getCycle() { return cycle; }
    public double getEconomicHealth() { return economicHealth; }
    public double getInflationRate() { return inflationRate; }
    public double getMarketActivity() { return marketActivity; }
    public int getActiveEvents() { return activeEvents; }
}
