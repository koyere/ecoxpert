package me.koyere.ecoxpert;

import me.koyere.ecoxpert.core.ServiceRegistry;
import me.koyere.ecoxpert.core.platform.PlatformManager;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.economy.EconomySyncManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.core.update.UpdateChecker;
import me.koyere.ecoxpert.core.dependencies.DependencyManager;
import me.koyere.ecoxpert.core.failsafe.EconomyFailsafeManager;
import me.koyere.ecoxpert.api.EcoXpertAPI;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.economy.VaultEconomyProvider;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
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
 * @version 1.1.0
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
    private me.koyere.ecoxpert.modules.bank.BankManager bankManager;
    private MarketManager marketManager;
    private EconomicEventEngine eventEngine;
    private me.koyere.ecoxpert.core.safety.SafeModeManager safeModeManager;
    private me.koyere.ecoxpert.core.economy.EconomySyncService economySyncService;
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

            // Start periodic balance sync with fallback providers (EssentialsX/CMI) if configured
            try {
                economySyncService.start();
            } catch (Exception e) {
                getLogger().warning("Failed to start economy sync service: " + e.getMessage());
            }
            
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
            if (economySyncService != null) {
                economySyncService.stop();
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
        this.bankManager = serviceRegistry.getInstance(me.koyere.ecoxpert.modules.bank.BankManager.class);
        this.marketManager = serviceRegistry.getInstance(MarketManager.class);
        this.eventEngine = serviceRegistry.getInstance(EconomicEventEngine.class);
        this.safeModeManager = serviceRegistry.getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        this.economySyncService = serviceRegistry.getInstance(me.koyere.ecoxpert.core.economy.EconomySyncService.class);
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
        // Simple mode banner (usability demo)
        try {
            if (configManager.isSimpleMode()) {
                var root = configManager.getConfig();
                double mc = root.getDouble("simple.market.max_price_change", 0.15);
                double vd = root.getDouble("simple.market.volatility_damping", 0.90);
                int th = root.getInt("simple.market.trend_analysis_hours", 24);
                double infl = root.getDouble("simple.inflation.target", 1.02);
                double wtax = root.getDouble("simple.policy.wealth_tax_rate", 0.005);
                getLogger().info("================ SIMPLE MODE ================");
                getLogger().info(String.format("Market: max_price_change=%.2f, volatility_damping=%.2f, trend_hours=%d", mc, vd, th));
                getLogger().info(String.format("Inflation target: %.2f (multiplier)", infl));
                getLogger().info(String.format("Wealth tax rate: %.3f", wtax));
                getLogger().info("(Switch to advanced via plugin.config_mode=\"advanced\")");
                getLogger().info("============================================");
            }
        } catch (Exception ignored) {}
        
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
        
        // 9. Dynamic Economic Events Engine (async initialize)
        try {
            eventEngine.initialize();
        } catch (Exception e) {
            getLogger().warning("Dynamic Economic Events Engine failed to initialize: " + e.getMessage());
        }

        // 10. Safe Mode monitor
        try {
            safeModeManager.initialize();
        } catch (Exception e) {
            getLogger().warning("Safe Mode monitor failed to initialize: " + e.getMessage());
        }

        // 11. Loans delinquency scheduler
        try {
            new me.koyere.ecoxpert.modules.loans.LoanDelinquencyScheduler(this,
                dataManager, economyManager).start();
        } catch (Exception e) {
            getLogger().warning("Loan delinquency scheduler failed to start: " + e.getMessage());
        }

        // 12. PlaceholderAPI integration
        try {
            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new me.koyere.ecoxpert.modules.integrations.PlaceholderProvider(
                    this,
                    economyManager,
                    bankManager,
                    marketManager,
                    serviceRegistry.getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class),
                    eventEngine,
                    serviceRegistry.getInstance(me.koyere.ecoxpert.modules.loans.LoanManager.class),
                    serviceRegistry.getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class),
                    translationManager
                ).register();
                getLogger().info("PlaceholderAPI placeholders registered (identifier: ecox)");
            }
        } catch (Exception e) {
            getLogger().warning("PlaceholderAPI registration failed: " + e.getMessage());
        }

        // 12.5. Integrations detection banner (ensure early detection/logging)
        try {
            // Force initialization so detection status is logged at startup
            serviceRegistry.getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class);
        } catch (Exception e) {
            getLogger().fine("Integrations manager init skipped: " + e.getMessage());
        }

        // 13. Jobs Reborn integration (soft hook)
        try {
            new me.koyere.ecoxpert.modules.integrations.jobs.JobsIntegration(this).registerIfPresent();
        } catch (Exception e) {
            getLogger().fine("Jobs integration not available: " + e.getMessage());
        }

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
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found - economy features limited");
            return;
        }

        // Nota informativa para admins: conflicto de comandos y alias recomendado
        boolean hasEssentials = getServer().getPluginManager().getPlugin("Essentials") != null
                || getServer().getPluginManager().getPlugin("EssentialsX") != null;
        boolean hasCMI = getServer().getPluginManager().getPlugin("CMI") != null;
        if (hasEssentials) {
            getLogger().info("EssentialsX detected: use /ecox or /ecoxpert to avoid /eco conflicts");
        }

        boolean importOnStartup = getConfig().getBoolean("economy.migration.import_on_startup", true);
        boolean backupBeforeImport = getConfig().getBoolean("economy.migration.backup_before_import", true);

        // If Essentials/CMI are present, defer import and registration to ensure their API is ready
        if (importOnStartup && (hasEssentials || hasCMI)) {
            getServer().getScheduler().runTaskLaterAsynchronously(this, () -> {
                try {
                    // Skip import if we already have EcoXpert accounts to avoid overwriting balances after deposits/bank ops
                    if (hasExistingEcoxpertAccounts()) {
                        getLogger().info("Startup balance import skipped: EcoXpert accounts already exist (set economy.migration.import_on_startup=false to silence).");
                        getServer().getScheduler().runTask(this, () -> {
                            vaultProvider.register();
                            getLogger().info("Vault economy provider registered successfully");
                        });
                        return;
                    }
                    var registration = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                    if (registration != null) {
                        String providerPlugin = registration.getPlugin() != null ? registration.getPlugin().getName() : "Unknown";
                        if (providerPlugin != null && !providerPlugin.toLowerCase().contains("ecoxpert")) {
                            if (backupBeforeImport) {
                                try {
                                    java.nio.file.Path backups = getDataFolder().toPath().resolve("backups");
                                    java.nio.file.Files.createDirectories(backups);
                                    String ts = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
                                    java.nio.file.Path backupPath = backups.resolve("pre_import-" + ts + ".db");
                                    dataManager.exportDatabase(backupPath).join();
                                    getLogger().info("Database backup created: " + backupPath.getFileName());
                                } catch (Exception e) {
                                    getLogger().warning("Could not create backup before import: " + e.getMessage());
                                }
                            }
                            getLogger().info("Importing balances from existing provider: " + providerPlugin + "...");
                            EconomySyncManager tempSync = new EconomySyncManager(this, economyManager, registration.getProvider());
                            int imported = tempSync.importBalancesFromFallback().join();
                            getLogger().info("Import completed. Imported accounts: " + imported);
                        }
                    }
                } catch (Exception e) {
                    getLogger().warning("Error during startup balance import: " + e.getMessage());
                } finally {
                    // Register our provider on the main thread after import completes
                    getServer().getScheduler().runTask(this, () -> {
                        vaultProvider.register();
                        getLogger().info("Vault economy provider registered successfully");
                    });
                }
            }, 60L); // ~3 segundos tras el enable
        } else {
            // Register immediately if no relevant providers detected or import is disabled
            vaultProvider.register();
            getLogger().info("Vault economy provider registered successfully");
        }
    }

    private boolean hasExistingEcoxpertAccounts() {
        try (me.koyere.ecoxpert.core.data.QueryResult qr = dataManager.executeQuery(
            "SELECT COUNT(*) AS cnt FROM ecoxpert_accounts"
        ).join()) {
            if (qr.next()) {
                Long cnt = qr.getLong("cnt");
                return cnt != null && cnt > 0;
            }
        } catch (Exception e) {
            getLogger().warning("Could not check existing EcoXpert accounts before import: " + e.getMessage());
        }
        return false;
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
            // Initial check
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                updateChecker.checkForUpdates();
                updateChecker.notifyAdmins();
            });
            // Periodic checks (6 hours)
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                updateChecker.checkForUpdates();
                updateChecker.notifyAdmins();
            }, 20L * 60 * 60 * 6, 20L * 60 * 60 * 6);
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
