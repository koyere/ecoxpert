package me.koyere.ecoxpert.core.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuration management interface
 * 
 * Handles loading, saving, and auto-updating of all
 * configuration files with preservation of user customizations.
 */
public interface ConfigManager {
    
    /**
     * Initialize the configuration system
     */
    void initialize();
    
    /**
     * Reload all configuration files
     */
    void reload();
    
    /**
     * Get the main plugin configuration
     * 
     * @return Main config.yml
     */
    FileConfiguration getConfig();
    
    /**
     * Get a module-specific configuration
     * 
     * @param moduleName Module name (e.g., "market", "bank")
     * @return Module configuration file
     */
    FileConfiguration getModuleConfig(String moduleName);
    
    /**
     * Check if metrics collection is enabled
     * 
     * @return true if bStats metrics allowed
     */
    boolean isMetricsEnabled();
    
    /**
     * Check if update checking is enabled
     * 
     * @return true if update checks allowed
     */
    boolean isUpdateCheckEnabled();
    
    /**
     * Get the configured database type
     * 
     * @return Database type ("sqlite" or "mysql")
     */
    String getDatabaseType();
    
    /**
     * Get the configured language code
     * 
     * @return Language code (e.g., "en", "es")
     */
    String getLanguage();
    
    /**
     * Check if debug logging is enabled
     * 
     * @return true if debug mode active
     */
    boolean isDebugEnabled();

    /**
     * Returns true if plugin runs in simple configuration mode.
     */
    boolean isSimpleMode();
    
    /**
     * Save all configuration files
     */
    void saveAll();
}
