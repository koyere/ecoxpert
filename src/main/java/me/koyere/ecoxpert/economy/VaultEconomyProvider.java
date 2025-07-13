package me.koyere.ecoxpert.economy;

/**
 * Vault economy provider interface
 * 
 * Integrates EcoXpert with Vault's economy API
 * to provide economy services to other plugins.
 */
public interface VaultEconomyProvider {
    
    /**
     * Register the economy provider with Vault
     * 
     * @return true if registration successful
     */
    boolean register();
    
    /**
     * Unregister the economy provider from Vault
     */
    void unregister();
    
    /**
     * Check if the provider is registered
     * 
     * @return true if registered with Vault
     */
    boolean isRegistered();
    
    /**
     * Get the economy provider name
     * 
     * @return Provider name for Vault
     */
    String getName();
}