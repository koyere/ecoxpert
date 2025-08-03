package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.VaultEconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Economy Mode Manager
 * 
 * Manages different operation modes based on economy provider conflicts.
 * Ensures safe operation without breaking existing server economies.
 */
public class EconomyModeManager {
    
    public enum EconomyMode {
        /**
         * EcoXpert is the primary economy provider
         * All economy operations go through EcoXpert
         */
        TAKEOVER_MODE,
        
        /**
         * Another plugin is the primary economy provider
         * EcoXpert runs alongside providing advanced features
         */
        COMPATIBILITY_MODE,
        
        /**
         * No economy provider detected or error state
         * EcoXpert operates in standalone mode
         */
        STANDALONE_MODE,
        
        /**
         * Detection or initialization failed
         * EcoXpert operates with minimal functionality
         */
        SAFE_MODE
    }
    
    private final EcoXpertPlugin plugin;
    private final EconomyConflictDetector conflictDetector;
    private final VaultEconomyProvider vaultProvider;
    
    private EconomyMode currentMode;
    private Economy fallbackEconomy;
    private boolean initialized = false;
    
    public EconomyModeManager(EcoXpertPlugin plugin, VaultEconomyProvider vaultProvider) {
        this.plugin = plugin;
        this.vaultProvider = vaultProvider;
        this.conflictDetector = new EconomyConflictDetector(plugin);
        this.currentMode = EconomyMode.SAFE_MODE;
    }
    
    /**
     * Initialize economy mode with safety checks
     * 
     * @return true if initialization successful
     */
    public boolean initialize() {
        if (initialized) {
            plugin.getLogger().warning("EconomyModeManager already initialized");
            return true;
        }
        
        try {
            plugin.getLogger().info("Initializing Economy Mode Manager...");
            
            // Step 1: Detect current state
            EconomyConflictDetector.EconomyProviderStatus status = 
                conflictDetector.detectEconomyProvider();
            
            EconomyConflictDetector.InstalledEconomyPlugins installedPlugins = 
                conflictDetector.detectInstalledEconomyPlugins();
            
            // Step 2: Log detection results
            logDetectionResults(status, installedPlugins);
            
            // Step 3: Determine and set operation mode
            EconomyMode determinedMode = determineOperationMode(status, installedPlugins);
            
            // Step 4: Initialize chosen mode safely
            boolean success = initializeMode(determinedMode);
            
            if (success) {
                this.currentMode = determinedMode;
                this.initialized = true;
                plugin.getLogger().info("Economy Mode Manager initialized successfully in " + 
                    currentMode + " mode");
                return true;
            } else {
                plugin.getLogger().warning("Failed to initialize economy mode, falling back to SAFE_MODE");
                this.currentMode = EconomyMode.SAFE_MODE;
                this.initialized = true;
                return false;
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error during economy mode initialization: " + e.getMessage());
            this.currentMode = EconomyMode.SAFE_MODE;
            this.initialized = true;
            return false;
        }
    }
    
    /**
     * Determine the best operation mode based on detection results
     */
    private EconomyMode determineOperationMode(EconomyConflictDetector.EconomyProviderStatus status,
                                             EconomyConflictDetector.InstalledEconomyPlugins installedPlugins) {
        
        switch (status.getStatus()) {
            case NO_PROVIDER:
                // No economy provider - we can take over safely
                return EconomyMode.TAKEOVER_MODE;
                
            case ECOXPERT_ACTIVE:
                // We're already the provider
                return EconomyMode.TAKEOVER_MODE;
                
            case OTHER_PROVIDER_ACTIVE:
                // Another provider is active - use compatibility mode
                plugin.getLogger().info("Detected active economy provider: " + status.getPluginName());
                plugin.getLogger().info("Operating in compatibility mode alongside " + status.getPluginName());
                return EconomyMode.COMPATIBILITY_MODE;
                
            case DETECTION_ERROR:
            default:
                // Error or unknown state - be safe
                plugin.getLogger().warning("Economy provider detection failed, using safe mode");
                return EconomyMode.SAFE_MODE;
        }
    }
    
    /**
     * Initialize the chosen operation mode
     */
    private boolean initializeMode(EconomyMode mode) {
        try {
            switch (mode) {
                case TAKEOVER_MODE:
                    return initializeTakeoverMode();
                    
                case COMPATIBILITY_MODE:
                    return initializeCompatibilityMode();
                    
                case STANDALONE_MODE:
                    return initializeStandaloneMode();
                    
                case SAFE_MODE:
                default:
                    return initializeSafeMode();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error initializing economy mode " + mode + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize takeover mode - EcoXpert becomes primary economy provider
     */
    private boolean initializeTakeoverMode() {
        if (!conflictDetector.canSafelyRegister()) {
            plugin.getLogger().warning("Cannot safely register as economy provider, falling back");
            return false;
        }
        
        try {
            boolean registered = vaultProvider.register();
            if (registered) {
                plugin.getLogger().info("Successfully registered as primary economy provider");
                
                // Verify we actually became the provider
                Bukkit.getScheduler().runTaskLater(plugin, this::verifyTakeoverSuccess, 20L);
                return true;
            } else {
                plugin.getLogger().warning("Failed to register as economy provider");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error during takeover mode initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize compatibility mode - Work alongside existing economy provider
     */
    private boolean initializeCompatibilityMode() {
        try {
            // Get reference to the active economy provider
            RegisteredServiceProvider<Economy> registration = 
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
            
            if (registration != null) {
                this.fallbackEconomy = registration.getProvider();
                plugin.getLogger().info("Operating in compatibility mode with " + 
                    registration.getPlugin().getName());
                
                // DON'T register our provider - let the other one handle Vault
                // We'll provide our advanced features through our own commands
                return true;
            } else {
                plugin.getLogger().warning("No economy provider found for compatibility mode");
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error during compatibility mode initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize standalone mode - EcoXpert operates independently
     */
    private boolean initializeStandaloneMode() {
        try {
            // Register our provider but don't expect to be the only one
            boolean registered = vaultProvider.register();
            plugin.getLogger().info("Initialized in standalone mode, registered: " + registered);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Error during standalone mode initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize safe mode - Minimal functionality
     */
    private boolean initializeSafeMode() {
        plugin.getLogger().info("Operating in safe mode - providing minimal economy features");
        // Don't register with Vault to avoid conflicts
        return true;
    }
    
    /**
     * Verify that takeover was successful
     */
    private void verifyTakeoverSuccess() {
        EconomyConflictDetector.EconomyProviderStatus status = 
            conflictDetector.detectEconomyProvider();
        
        if (!status.isEcoXpertActive()) {
            plugin.getLogger().warning("⚠️  TAKEOVER VERIFICATION FAILED!");
            plugin.getLogger().warning("Expected EcoXpert as provider, but found: " + 
                status.getPluginName());
            plugin.getLogger().warning("Switching to compatibility mode...");
            
            // Switch to compatibility mode
            this.currentMode = EconomyMode.COMPATIBILITY_MODE;
            initializeCompatibilityMode();
        } else {
            plugin.getLogger().info("✓ Takeover successful - EcoXpert is active economy provider");
        }
    }
    
    /**
     * Log detection results for debugging
     */
    private void logDetectionResults(EconomyConflictDetector.EconomyProviderStatus status,
                                   EconomyConflictDetector.InstalledEconomyPlugins installedPlugins) {
        
        plugin.getLogger().info("=== Economy Detection Results ===");
        plugin.getLogger().info("Provider Status: " + status.getStatus());
        plugin.getLogger().info("Active Provider: " + status.getPluginName());
        plugin.getLogger().info("Provider Class: " + status.getProviderClassName());
        plugin.getLogger().info("Message: " + status.getMessage());
        
        plugin.getLogger().info("Installed Economy Plugins:");
        for (String pluginName : installedPlugins.getInstalledPlugins()) {
            plugin.getLogger().info("  - " + pluginName);
        }
        
        if (installedPlugins.getInstalledCount() == 0) {
            plugin.getLogger().info("  - None detected");
        }
        
        plugin.getLogger().info("=== End Detection Results ===");
    }
    
    // Getters
    public EconomyMode getCurrentMode() {
        return currentMode;
    }
    
    public Economy getFallbackEconomy() {
        return fallbackEconomy;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isTakeoverMode() {
        return currentMode == EconomyMode.TAKEOVER_MODE;
    }
    
    public boolean isCompatibilityMode() {
        return currentMode == EconomyMode.COMPATIBILITY_MODE;
    }
    
    public boolean canUseAdvancedFeatures() {
        // Advanced features available in all modes except safe mode
        return currentMode != EconomyMode.SAFE_MODE;
    }
}