package me.koyere.ecoxpert.core.platform;

/**
 * Enumeration of supported server platforms
 * 
 * Defines the hierarchy and capabilities of different
 * Minecraft server implementations.
 */
public enum ServerPlatform {
    
    /**
     * Vanilla Spigot - Base compatibility layer
     */
    SPIGOT("Spigot", false, false, false),
    
    /**
     * PaperMC - Enhanced performance and APIs
     */
    PAPER("Paper", true, true, false),
    
    /**
     * Purpur - Paper fork with additional features
     */
    PURPUR("Purpur", true, true, false),
    
    /**
     * Folia - Paper fork with regional threading
     */
    FOLIA("Folia", true, true, true),
    
    /**
     * Unknown platform - fallback to Spigot compatibility
     */
    UNKNOWN("Unknown", false, false, false);
    
    private final String displayName;
    private final boolean paperCompatible;
    private final boolean adventureSupport;
    private final boolean regionalThreading;
    
    ServerPlatform(String displayName, boolean paperCompatible, 
                  boolean adventureSupport, boolean regionalThreading) {
        this.displayName = displayName;
        this.paperCompatible = paperCompatible;
        this.adventureSupport = adventureSupport;
        this.regionalThreading = regionalThreading;
    }
    
    /**
     * Get the human-readable platform name
     * 
     * @return Display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Check if Paper API features are available
     * 
     * @return true if Paper-compatible
     */
    public boolean isPaperCompatible() {
        return paperCompatible;
    }
    
    /**
     * Check if Adventure API is supported
     * 
     * @return true if Adventure API available
     */
    public boolean hasAdventureSupport() {
        return adventureSupport;
    }
    
    /**
     * Check if regional threading (Folia) is available
     * 
     * @return true if Folia features available
     */
    public boolean hasRegionalThreading() {
        return regionalThreading;
    }
    
    /**
     * Detect platform from server implementation name
     * 
     * @param serverName Server implementation name
     * @return Detected platform
     */
    public static ServerPlatform fromServerName(String serverName) {
        if (serverName == null) {
            return UNKNOWN;
        }
        
        String name = serverName.toLowerCase();
        
        if (name.contains("folia")) {
            return FOLIA;
        } else if (name.contains("purpur")) {
            return PURPUR;
        } else if (name.contains("paper")) {
            return PAPER;
        } else if (name.contains("spigot")) {
            return SPIGOT;
        }
        
        return UNKNOWN;
    }
}