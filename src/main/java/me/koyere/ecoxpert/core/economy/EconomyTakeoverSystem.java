package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.economy.VaultEconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * NON-INVASIVE Economy Takeover System
 * 
 * This system operates COMPLETELY INDEPENDENTLY without modifying
 * existing code. It provides intelligent economy integration that can:
 * 
 * 1. Take control when possible (no conflicts)
 * 2. Operate in compatibility mode alongside other economy plugins
 * 3. Provide migration tools for server owners
 * 4. Never break existing server functionality
 */
public class EconomyTakeoverSystem {
    
    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final VaultEconomyProvider vaultProvider;
    
    private EconomyModeManager modeManager;
    private EconomyConflictDetector conflictDetector;
    private EconomySyncManager syncManager;
    
    private boolean initialized = false;
    private boolean active = false;
    
    public EconomyTakeoverSystem(EcoXpertPlugin plugin, EconomyManager economyManager, 
                               VaultEconomyProvider vaultProvider) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.vaultProvider = vaultProvider;
    }
    
    /**
     * Initialize the takeover system safely
     * 
     * @return true if initialization successful
     */
    public boolean initialize() {
        if (initialized) {
            plugin.getLogger().info("Economy Takeover System already initialized");
            return true;
        }
        
        try {
            plugin.getLogger().info("Initializing Economy Takeover System...");
            
            // Step 1: Create components
            this.conflictDetector = new EconomyConflictDetector(plugin);
            this.modeManager = new EconomyModeManager(plugin, vaultProvider);
            
            // Step 2: Initialize mode manager
            boolean modeInitialized = modeManager.initialize();
            if (!modeInitialized) {
                plugin.getLogger().warning("Failed to initialize economy mode manager");
                return false;
            }
            
            // Step 3: Setup sync if in compatibility mode
            if (modeManager.isCompatibilityMode()) {
                Economy fallbackEconomy = modeManager.getFallbackEconomy();
                if (fallbackEconomy != null) {
                    this.syncManager = new EconomySyncManager(plugin, economyManager, fallbackEconomy);
                    syncManager.startSync();
                    plugin.getLogger().info("Economy synchronization enabled with " + 
                        fallbackEconomy.getClass().getSimpleName());
                }
            }
            
            this.initialized = true;
            this.active = true;
            
            plugin.getLogger().info("✓ Economy Takeover System initialized successfully");
            plugin.getLogger().info("Mode: " + modeManager.getCurrentMode());
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Economy Takeover System: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shutdown the takeover system safely
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        try {
            plugin.getLogger().info("Shutting down Economy Takeover System...");
            
            // Stop sync manager
            if (syncManager != null) {
                syncManager.stopSync();
                syncManager = null;
            }
            
            // Reset components
            modeManager = null;
            conflictDetector = null;
            
            this.active = false;
            this.initialized = false;
            
            plugin.getLogger().info("Economy Takeover System shutdown complete");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during Economy Takeover System shutdown: " + e.getMessage());
        }
    }
    
    /**
     * Get current system status
     * 
     * @return SystemStatus with detailed information
     */
    public SystemStatus getStatus() {
        if (!initialized) {
            return new SystemStatus(false, false, null, null, null, "Not initialized");
        }
        
        try {
            EconomyConflictDetector.EconomyProviderStatus providerStatus = 
                conflictDetector.detectEconomyProvider();
            
            EconomyConflictDetector.InstalledEconomyPlugins installedPlugins = 
                conflictDetector.detectInstalledEconomyPlugins();
            
            EconomySyncManager.SyncStatistics syncStats = 
                syncManager != null ? syncManager.getStatistics() : null;
            
            return new SystemStatus(
                true, 
                active,
                modeManager.getCurrentMode(),
                providerStatus,
                installedPlugins,
                syncStats != null ? syncStats.toString() : "No sync active"
            );
            
        } catch (Exception e) {
            return new SystemStatus(true, false, null, null, null, 
                "Error getting status: " + e.getMessage());
        }
    }
    
    /**
     * Force a specific operation mode (for admin control)
     * 
     * @param mode Mode to force
     * @return true if successful
     */
    public boolean forceMode(EconomyModeManager.EconomyMode mode) {
        if (!initialized) {
            plugin.getLogger().warning("Cannot force mode - system not initialized");
            return false;
        }
        
        plugin.getLogger().info("Admin forcing economy mode to: " + mode);
        
        // This would require extending EconomyModeManager with a forceMode method
        // For now, just log the request
        plugin.getLogger().info("Mode forcing requested but not yet implemented");
        return false;
    }
    
    /**
     * Import balances from another economy plugin
     * 
     * @return number of accounts imported
     */
    public int importBalances() {
        if (!initialized || syncManager == null) {
            plugin.getLogger().warning("Cannot import balances - sync manager not available");
            return 0;
        }
        
        try {
            return syncManager.importBalancesFromFallback().join();
        } catch (Exception e) {
            plugin.getLogger().severe("Error importing balances: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Run system diagnostics
     * 
     * @return diagnostic results
     */
    public String runDiagnostics() {
        if (!initialized) {
            return "Economy Takeover System not initialized";
        }
        
        EconomySystemTestRunner testRunner = new EconomySystemTestRunner(plugin);
        EconomySystemTestRunner.TestResults results = testRunner.runSafeTests();
        
        return results.toString();
    }
    
    // Getters
    public boolean isInitialized() { return initialized; }
    public boolean isActive() { return active; }
    public EconomyModeManager getModeManager() { return modeManager; }
    public EconomyConflictDetector getConflictDetector() { return conflictDetector; }
    public EconomySyncManager getSyncManager() { return syncManager; }
    
    /**
     * System Status Information
     */
    public static class SystemStatus {
        private final boolean initialized;
        private final boolean active;
        private final EconomyModeManager.EconomyMode currentMode;
        private final EconomyConflictDetector.EconomyProviderStatus providerStatus;
        private final EconomyConflictDetector.InstalledEconomyPlugins installedPlugins;
        private final String additionalInfo;
        
        public SystemStatus(boolean initialized, boolean active, 
                          EconomyModeManager.EconomyMode currentMode,
                          EconomyConflictDetector.EconomyProviderStatus providerStatus,
                          EconomyConflictDetector.InstalledEconomyPlugins installedPlugins,
                          String additionalInfo) {
            this.initialized = initialized;
            this.active = active;
            this.currentMode = currentMode;
            this.providerStatus = providerStatus;
            this.installedPlugins = installedPlugins;
            this.additionalInfo = additionalInfo;
        }
        
        // Getters
        public boolean isInitialized() { return initialized; }
        public boolean isActive() { return active; }
        public EconomyModeManager.EconomyMode getCurrentMode() { return currentMode; }
        public EconomyConflictDetector.EconomyProviderStatus getProviderStatus() { return providerStatus; }
        public EconomyConflictDetector.InstalledEconomyPlugins getInstalledPlugins() { return installedPlugins; }
        public String getAdditionalInfo() { return additionalInfo; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Economy Takeover System Status ===\n");
            sb.append("Initialized: ").append(initialized).append("\n");
            sb.append("Active: ").append(active).append("\n");
            sb.append("Current Mode: ").append(currentMode).append("\n");
            
            if (providerStatus != null) {
                sb.append("Provider Status: ").append(providerStatus.getStatus()).append("\n");
                sb.append("Active Provider: ").append(providerStatus.getPluginName()).append("\n");
            }
            
            if (installedPlugins != null) {
                sb.append("Installed Economy Plugins: ").append(installedPlugins.getInstalledCount()).append("\n");
                for (String plugin : installedPlugins.getInstalledPlugins()) {
                    sb.append("  - ").append(plugin).append("\n");
                }
            }
            
            sb.append("Additional Info: ").append(additionalInfo).append("\n");
            sb.append("=== End Status ===");
            
            return sb.toString();
        }
    }
}