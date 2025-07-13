package me.koyere.ecoxpert;

import me.koyere.ecoxpert.core.ServiceRegistry;
import me.koyere.ecoxpert.core.platform.PlatformManager;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.core.update.UpdateChecker;
import me.koyere.ecoxpert.core.dependencies.DependencyManager;
import me.koyere.ecoxpert.core.failsafe.EconomyFailsafeManager;
import me.koyere.ecoxpert.api.EcoXpertAPI;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.economy.VaultEconomyProvider;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.commands.CommandManager;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * EcoXpert Pro - Premium Minecraft Economy Plugin
 * 
 * Professional economy management system with advanced features:
 * - Dynamic market pricing with real-time analytics
 * - Banking system with interest calculations
 * - Loan management with credit scoring
 * - Economic events and automation
 * - Professional role specializations
 * - Anti-exploitation and inflation control
 * 
 * @author Koyere
 * @version 1.0.0
 * @since 1.19.4
 */
public final class EcoXpertPlugin extends JavaPlugin {
    
    // Service registry for dependency management
    private ServiceRegistry serviceRegistry;
    
    // Core managers
    private PlatformManager platformManager;
    private ConfigManager configManager;
    private DataManager dataManager;
    private TranslationManager translationManager;
    private UpdateChecker updateChecker;
    private DependencyManager dependencyManager;
    private EconomyFailsafeManager failsafeManager;
    private EconomyManager economyManager;
    private MarketManager marketManager;
    private VaultEconomyProvider vaultProvider;
    private CommandManager commandManager;
    
    // Plugin metrics
    private Metrics metrics;
    
    // API instance
    private static EcoXpertAPI api;
    
    @Override
    public void onLoad() {
        try {
            getLogger().info("Loading EcoXpert Pro v" + getDescription().getVersion());
            
            // Initialize service registry
            initializeServiceRegistry();
            
            getLogger().info("EcoXpert Pro service registry initialized");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to load EcoXpert Pro", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onEnable() {
        try {
            getLogger().info("Enabling EcoXpert Pro...");
            
            // Get service instances
            getServiceInstances();
            
            // Initialize core systems in dependency order
            initializeCoreManagers();
            
            // Register commands
            registerCommands();
            
            // Register Vault economy provider
            registerVaultProvider();
            
            // Initialize bStats metrics
            initializeMetrics();
            
            // Check for updates
            scheduleUpdateCheck();
            
            // Initialize API
            api = serviceRegistry.getInstance(EcoXpertAPI.class);
            
            getLogger().info("EcoXpert Pro v" + getDescription().getVersion() + 
                           " enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable EcoXpert Pro", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        try {
            getLogger().info("Disabling EcoXpert Pro...");
            
            // Unregister commands
            if (commandManager != null) {
                commandManager.unregisterCommands();
            }
            
            // Shutdown market manager
            if (marketManager != null) {
                marketManager.shutdown();
            }
            
            // Shutdown economy manager
            if (economyManager != null) {
                economyManager.shutdown();
            }
            
            // Shutdown failsafe manager
            if (failsafeManager != null) {
                failsafeManager.shutdown();
            }
            
            // Shutdown data manager (close database connections)
            if (dataManager != null) {
                dataManager.shutdown();
            }
            
            // Shutdown dependency manager
            if (dependencyManager != null) {
                dependencyManager.shutdown();
            }
            
            // Clear API reference
            api = null;
            
            getLogger().info("EcoXpert Pro disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error during plugin shutdown", e);
        }
    }
    
    /**
     * Initialize service registry for dependency management
     */
    private void initializeServiceRegistry() {
        this.serviceRegistry = new ServiceRegistry(this);
    }
    
    /**
     * Get service instances from registry
     */
    private void getServiceInstances() {
        this.platformManager = serviceRegistry.getInstance(PlatformManager.class);
        this.configManager = serviceRegistry.getInstance(ConfigManager.class);
        this.dataManager = serviceRegistry.getInstance(DataManager.class);
        this.translationManager = serviceRegistry.getInstance(TranslationManager.class);
        this.updateChecker = serviceRegistry.getInstance(UpdateChecker.class);
        this.dependencyManager = serviceRegistry.getInstance(DependencyManager.class);
        this.failsafeManager = serviceRegistry.getInstance(EconomyFailsafeManager.class);
        this.economyManager = serviceRegistry.getInstance(EconomyManager.class);
        this.marketManager = serviceRegistry.getInstance(MarketManager.class);
        this.vaultProvider = serviceRegistry.getInstance(VaultEconomyProvider.class);
        this.commandManager = serviceRegistry.getInstance(CommandManager.class);
    }
    
    /**
     * Initialize core managers in proper dependency order
     */
    private void initializeCoreManagers() {
        // 1. Platform detection
        platformManager.detectPlatform();
        getLogger().info("Platform detected: " + platformManager.getPlatformName());
        
        // 2. Configuration system
        configManager.initialize();
        
        // 3. Translation system
        translationManager.initialize();
        
        // 4. Dependency management
        dependencyManager.initialize();
        
        // 5. Database system
        dataManager.initialize();
        
        // 6. Failsafe system
        failsafeManager.initialize();
        
        // 7. Economy system
        economyManager.initialize();
        
        // 8. Market system
        marketManager.initialize();
        
        getLogger().info("Core managers initialized successfully");
    }
    
    /**
     * Register all plugin commands
     */
    private void registerCommands() {
        commandManager.registerCommands();
        getLogger().info("Plugin commands registered successfully");
    }
    
    /**
     * Register Vault economy provider if Vault is available
     */
    private void registerVaultProvider() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            vaultProvider.register();
            getLogger().info("Vault economy provider registered");
        } else {
            getLogger().warning("Vault not found - economy features limited");
        }
    }
    
    /**
     * Initialize bStats metrics collection
     */
    private void initializeMetrics() {
        if (configManager.isMetricsEnabled()) {
            this.metrics = new Metrics(this, 26446);
            
            // Add custom metrics charts
            addCustomMetrics();
            
            getLogger().info("bStats metrics initialized");
        } else {
            getLogger().info("Metrics collection disabled in config");
        }
    }
    
    /**
     * Add custom metrics charts for bStats
     */
    private void addCustomMetrics() {
        // Server platform detection
        metrics.addCustomChart(new SimplePie("server_platform", () -> 
            platformManager.getPlatformName()));
            
        // Cross-platform player count
        metrics.addCustomChart(new SimplePie("bedrock_players", () -> 
            platformManager.hasBedrockPlayers() ? "Yes" : "No"));
    }
    
    /**
     * Schedule periodic update checks
     */
    private void scheduleUpdateCheck() {
        if (configManager.isUpdateCheckEnabled()) {
            // Check for updates every 6 hours
            getServer().getScheduler().runTaskTimerAsynchronously(this, 
                updateChecker::checkForUpdates, 0L, 20L * 60 * 60 * 6);
        }
    }
    
    /**
     * Get the EcoXpert API instance
     * 
     * @return API instance for external plugins
     */
    public static EcoXpertAPI getAPI() {
        if (api == null) {
            throw new IllegalStateException("EcoXpert API not initialized - plugin may not be loaded");
        }
        return api;
    }
    
    /**
     * Get the service registry
     * 
     * @return ServiceRegistry instance
     */
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }
}