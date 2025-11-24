package me.koyere.ecoxpert.core.platform;

import me.koyere.ecoxpert.EcoXpertPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Implementation of platform detection and capability management
 * 
 * Detects server platform, version, and available features
 * for optimal compatibility and performance optimization.
 */
@Singleton
public class PlatformManagerImpl implements PlatformManager {
    
    private final EcoXpertPlugin plugin;
    private ServerPlatform platform;
    private String platformName;
    private String minecraftVersion;
    private boolean geyserAvailable;
    private boolean floodgateAvailable;
    private Object floodgateApi;
    private Method floodgateIsPlayer;
    
    @Inject
    public PlatformManagerImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void detectPlatform() {
        // Detect server platform
        this.platformName = Bukkit.getVersion();
        this.platform = ServerPlatform.fromServerName(platformName);
        
        // Get Minecraft version
        this.minecraftVersion = Bukkit.getBukkitVersion().split("-")[0];
        
        // Check for GeyserMC (Bedrock support)
        this.geyserAvailable = plugin.getServer().getPluginManager().getPlugin("Geyser-Spigot") != null;
        this.floodgateAvailable = plugin.getServer().getPluginManager().getPlugin("floodgate") != null;
        if (floodgateAvailable) {
            try {
                Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                Method getInstance = apiClass.getMethod("getInstance");
                this.floodgateApi = getInstance.invoke(null);
                this.floodgateIsPlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
                plugin.getLogger().info("Floodgate detected - enhanced Bedrock detection enabled");
            } catch (Exception ex) {
                floodgateAvailable = false;
                floodgateApi = null;
                floodgateIsPlayer = null;
                plugin.getLogger().log(Level.WARNING, "Floodgate detected but API binding failed", ex);
            }
        }
        
        plugin.getLogger().info("Detected platform: " + platform.getDisplayName() + 
                               " (MC " + minecraftVersion + ")");
        
        if (geyserAvailable) {
            plugin.getLogger().info("GeyserMC detected - Bedrock support available");
        }
        
        // Log capabilities
        logCapabilities();
    }
    
    @Override
    public String getPlatformName() {
        return platform != null ? platform.getDisplayName() : "Unknown";
    }
    
    @Override
    public ServerPlatform getPlatform() {
        return platform != null ? platform : ServerPlatform.UNKNOWN;
    }
    
    @Override
    public boolean isPaperAvailable() {
        return platform != null && platform.isPaperCompatible();
    }
    
    @Override
    public boolean isFoliaAvailable() {
        return platform == ServerPlatform.FOLIA;
    }
    
    @Override
    public boolean isGeyserAvailable() {
        return geyserAvailable;
    }
    
    @Override
    public boolean hasBedrockPlayers() {
        if (!geyserAvailable && !floodgateAvailable) return false;
        
        // Check if any online players are Bedrock players
        return Bukkit.getOnlinePlayers().stream()
            .anyMatch(player -> isBedrockPlayer(player.getUniqueId()));
    }

    @Override
    public boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        return isBedrockPlayer(player.getUniqueId());
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) return false;
        if (floodgateAvailable && floodgateApi != null && floodgateIsPlayer != null) {
            try {
                Object result = floodgateIsPlayer.invoke(floodgateApi, uuid);
                if (result instanceof Boolean bool) {
                    return bool;
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed Floodgate bedrock detection", ex);
            }
        }
        if (!geyserAvailable) return false;
        return looksLikeBedrockUuid(uuid.toString());
    }
    
    @Override
    public String getMinecraftVersion() {
        return minecraftVersion != null ? minecraftVersion : "Unknown";
    }
    
    @Override
    public boolean supportsAsyncScheduler() {
        // Paper 1.19.4+ has improved async scheduler
        return isPaperAvailable() && isVersionAtLeast("1.19.4");
    }
    
    @Override
    public boolean supportsAdventure() {
        return platform != null && platform.hasAdventureSupport();
    }
    
    /**
     * Check if Minecraft version is at least the specified version
     * 
     * @param targetVersion Version to check against
     * @return true if current version >= target version
     */
    private boolean isVersionAtLeast(String targetVersion) {
        if (minecraftVersion == null) return false;
        
        try {
            String[] current = minecraftVersion.split("\\.");
            String[] target = targetVersion.split("\\.");
            
            for (int i = 0; i < Math.max(current.length, target.length); i++) {
                int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
                int targetPart = i < target.length ? Integer.parseInt(target[i]) : 0;
                
                if (currentPart > targetPart) return true;
                if (currentPart < targetPart) return false;
            }
            
            return true; // Versions are equal
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if a player UUID indicates a Bedrock player
     * 
     * @param uuid Player UUID string
     * @return true if Bedrock player
     */
    private boolean looksLikeBedrockUuid(String uuid) {
        // Bedrock players typically have UUIDs starting with 00000000
        return uuid.startsWith("00000000");
    }
    
    /**
     * Log detected platform capabilities
     */
    private void logCapabilities() {
        plugin.getLogger().info("Platform capabilities:");
        plugin.getLogger().info("  Paper API: " + isPaperAvailable());
        plugin.getLogger().info("  Adventure API: " + supportsAdventure());
        plugin.getLogger().info("  Async Scheduler: " + supportsAsyncScheduler());
        plugin.getLogger().info("  Folia Threading: " + isFoliaAvailable());
        plugin.getLogger().info("  Bedrock Support: " + (isGeyserAvailable() || floodgateAvailable));
        if (floodgateAvailable) {
            plugin.getLogger().info("  Floodgate API: true");
        }
    }
}
