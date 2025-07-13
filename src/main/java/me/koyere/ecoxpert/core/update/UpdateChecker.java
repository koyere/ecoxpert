package me.koyere.ecoxpert.core.update;

/**
 * Update checking and notification interface
 * 
 * Provides automatic update checking from SpigotMC API
 * with config file preservation and admin notifications.
 */
public interface UpdateChecker {
    
    /**
     * Check for available updates asynchronously
     */
    void checkForUpdates();
    
    /**
     * Get the current plugin version
     * 
     * @return Current version string
     */
    String getCurrentVersion();
    
    /**
     * Get the latest available version
     * 
     * @return Latest version string or null if unknown
     */
    String getLatestVersion();
    
    /**
     * Check if an update is available
     * 
     * @return true if update available
     */
    boolean isUpdateAvailable();
    
    /**
     * Get the update download URL
     * 
     * @return Download URL or null if no update
     */
    String getDownloadUrl();
    
    /**
     * Notify administrators about available updates
     */
    void notifyAdmins();
}