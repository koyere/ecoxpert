package me.koyere.ecoxpert.core.platform;

/**
 * Platform detection and capability management interface
 * 
 * Provides cross-platform compatibility detection for:
 * - Server implementations (Spigot, Paper, Purpur, Folia)
 * - Cross-platform players (Java + Bedrock via GeyserMC)
 * - Available APIs and optimization opportunities
 */
public interface PlatformManager {
    
    /**
     * Detect the current server platform and capabilities
     */
    void detectPlatform();
    
    /**
     * Get the detected platform name
     * 
     * @return Platform name (e.g., "Paper", "Purpur", "Spigot")
     */
    String getPlatformName();
    
    /**
     * Get the detected platform type
     * 
     * @return Platform type enum
     */
    ServerPlatform getPlatform();
    
    /**
     * Check if Paper API features are available
     * 
     * @return true if Paper API can be used
     */
    boolean isPaperAvailable();
    
    /**
     * Check if Folia's regional threading is available
     * 
     * @return true if Folia features can be used
     */
    boolean isFoliaAvailable();
    
    /**
     * Check if GeyserMC is installed for Bedrock support
     * 
     * @return true if Bedrock players are supported
     */
    boolean isGeyserAvailable();
    
    /**
     * Check if there are currently Bedrock players online
     * 
     * @return true if Bedrock players are detected
     */
    boolean hasBedrockPlayers();
    
    /**
     * Get the Minecraft server version
     * 
     * @return Version string (e.g., "1.19.4")
     */
    String getMinecraftVersion();
    
    /**
     * Check if async scheduler should be used
     * 
     * @return true if modern async scheduling is available
     */
    boolean supportsAsyncScheduler();
    
    /**
     * Check if Adventure API is available for rich text
     * 
     * @return true if Adventure API can be used
     */
    boolean supportsAdventure();
}