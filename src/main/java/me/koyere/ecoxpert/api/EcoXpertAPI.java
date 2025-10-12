package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
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
