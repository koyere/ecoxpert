package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Economy Conflict Detector
 * 
 * Safely detects economy provider conflicts and determines
 * the best operation mode without breaking existing functionality.
 */
public class EconomyConflictDetector {
    
    private final EcoXpertPlugin plugin;
    
    public EconomyConflictDetector(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Detect current economy provider status
     * 
     * @return EconomyProviderStatus with detailed information
     */
    public EconomyProviderStatus detectEconomyProvider() {
        try {
            RegisteredServiceProvider<Economy> registration = 
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
            
            if (registration == null) {
                return new EconomyProviderStatus(
                    EconomyProviderStatus.Status.NO_PROVIDER,
                    null,
                    null,
                    "No economy provider found"
                );
            }
            
            Economy provider = registration.getProvider();
            String providerName = provider.getClass().getSimpleName();
            String pluginName = registration.getPlugin().getName();
            
            // Check if we are the active provider
            boolean isEcoXpert = provider.getClass().getName().contains("ecoxpert");
            
            if (isEcoXpert) {
                return new EconomyProviderStatus(
                    EconomyProviderStatus.Status.ECOXPERT_ACTIVE,
                    providerName,
                    pluginName,
                    "EcoXpert is the active economy provider"
                );
            } else {
                return new EconomyProviderStatus(
                    EconomyProviderStatus.Status.OTHER_PROVIDER_ACTIVE,
                    providerName,
                    pluginName,
                    "Another economy provider is active: " + pluginName
                );
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error detecting economy provider: " + e.getMessage());
            return new EconomyProviderStatus(
                EconomyProviderStatus.Status.DETECTION_ERROR,
                null,
                null,
                "Error during detection: " + e.getMessage()
            );
        }
    }
    
    /**
     * Check if specific economy plugins are installed
     * 
     * @return Information about installed economy plugins
     */
    public InstalledEconomyPlugins detectInstalledEconomyPlugins() {
        InstalledEconomyPlugins.Builder builder = new InstalledEconomyPlugins.Builder();
        
        // Check for common economy plugins
        if (plugin.getServer().getPluginManager().getPlugin("Essentials") != null) {
            builder.addPlugin("Essentials", true);
        }
        if (plugin.getServer().getPluginManager().getPlugin("EssentialsX") != null) {
            builder.addPlugin("EssentialsX", true);
        }
        if (plugin.getServer().getPluginManager().getPlugin("CMI") != null) {
            builder.addPlugin("CMI", true);
        }
        if (plugin.getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            builder.addPlugin("GriefPrevention", true);
        }
        if (plugin.getServer().getPluginManager().getPlugin("Towny") != null) {
            builder.addPlugin("Towny", true);
        }
        
        return builder.build();
    }
    
    /**
     * Safely test if we can register as economy provider
     * without actually registering (dry run)
     * 
     * @return true if registration would likely succeed
     */
    public boolean canSafelyRegister() {
        try {
            // Check if Vault is available
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                return false;
            }
            
            // Check if ServicesManager is accessible
            if (plugin.getServer().getServicesManager() == null) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Cannot safely register economy provider: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Economy Provider Status Information
     */
    public static class EconomyProviderStatus {
        public enum Status {
            NO_PROVIDER,
            ECOXPERT_ACTIVE,
            OTHER_PROVIDER_ACTIVE,
            DETECTION_ERROR
        }
        
        private final Status status;
        private final String providerClassName;
        private final String pluginName;
        private final String message;
        
        public EconomyProviderStatus(Status status, String providerClassName, 
                                   String pluginName, String message) {
            this.status = status;
            this.providerClassName = providerClassName;
            this.pluginName = pluginName;
            this.message = message;
        }
        
        // Getters
        public Status getStatus() { return status; }
        public String getProviderClassName() { return providerClassName; }
        public String getPluginName() { return pluginName; }
        public String getMessage() { return message; }
        
        public boolean isEcoXpertActive() {
            return status == Status.ECOXPERT_ACTIVE;
        }
        
        public boolean hasConflict() {
            return status == Status.OTHER_PROVIDER_ACTIVE;
        }
    }
    
    /**
     * Installed Economy Plugins Information
     */
    public static class InstalledEconomyPlugins {
        private final java.util.Map<String, Boolean> plugins;
        
        private InstalledEconomyPlugins(java.util.Map<String, Boolean> plugins) {
            this.plugins = new java.util.HashMap<>(plugins);
        }
        
        public boolean hasPlugin(String pluginName) {
            return plugins.getOrDefault(pluginName, false);
        }
        
        public java.util.Set<String> getInstalledPlugins() {
            return plugins.entrySet().stream()
                .filter(java.util.Map.Entry::getValue)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        }
        
        public int getInstalledCount() {
            return (int) plugins.values().stream().filter(Boolean::booleanValue).count();
        }
        
        public boolean hasEssentials() {
            return hasPlugin("Essentials") || hasPlugin("EssentialsX");
        }
        
        public boolean hasCMI() {
            return hasPlugin("CMI");
        }
        
        public static class Builder {
            private final java.util.Map<String, Boolean> plugins = new java.util.HashMap<>();
            
            public Builder addPlugin(String name, boolean installed) {
                plugins.put(name, installed);
                return this;
            }
            
            public InstalledEconomyPlugins build() {
                return new InstalledEconomyPlugins(plugins);
            }
        }
    }
}