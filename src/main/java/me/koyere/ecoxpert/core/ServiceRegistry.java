package me.koyere.ecoxpert.core;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.api.EcoXpertAPI;
import me.koyere.ecoxpert.api.EcoXpertAPIImpl;
import me.koyere.ecoxpert.commands.CommandManager;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.config.ConfigManagerImpl;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.DataManagerImpl;
import me.koyere.ecoxpert.core.dependencies.DependencyManager;
import me.koyere.ecoxpert.core.dependencies.DependencyManagerImpl;
import me.koyere.ecoxpert.core.failsafe.EconomyFailsafeManager;
import me.koyere.ecoxpert.core.failsafe.EconomyFailsafeManagerImpl;
import me.koyere.ecoxpert.core.platform.PlatformManager;
import me.koyere.ecoxpert.core.platform.PlatformManagerImpl;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.core.translation.TranslationManagerImpl;
import me.koyere.ecoxpert.core.update.UpdateChecker;
import me.koyere.ecoxpert.core.update.UpdateCheckerImpl;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.economy.EconomyManagerImpl;
import me.koyere.ecoxpert.economy.VaultEconomyProvider;
import me.koyere.ecoxpert.economy.VaultEconomyProviderImpl;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.market.MarketManagerImpl;
import me.koyere.ecoxpert.modules.bank.BankManager;
import me.koyere.ecoxpert.modules.bank.BankManagerImpl;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.inflation.InflationManagerImpl;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import me.koyere.ecoxpert.modules.loans.LoanManagerImpl;
import me.koyere.ecoxpert.core.safety.SafeModeManager;
import me.koyere.ecoxpert.core.safety.SafeModeManagerImpl;
import me.koyere.ecoxpert.core.safety.RateLimitManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Manual dependency injection service registry
 * 
 * Replaces Guice with a lightweight manual factory pattern to reduce JAR size.
 * Provides singleton instance management and dependency resolution.
 */
public class ServiceRegistry {
    
    private final EcoXpertPlugin plugin;
    private final ConcurrentMap<Class<?>, Object> singletonInstances = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Supplier<?>> factoryMethods = new ConcurrentHashMap<>();
    
    public ServiceRegistry(EcoXpertPlugin plugin) {
        this.plugin = plugin;
        registerFactories();
    }
    
    /**
     * Get a singleton instance of the specified service
     */
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> serviceClass) {
        // Check if already instantiated
        T instance = (T) singletonInstances.get(serviceClass);
        if (instance != null) {
            return instance;
        }
        
        // Get factory and create instance
        Supplier<?> factory = factoryMethods.get(serviceClass);
        if (factory == null) {
            throw new IllegalArgumentException("No factory registered for: " + serviceClass.getName());
        }
        
        // Create and cache instance (thread-safe)
        synchronized (singletonInstances) {
            instance = (T) singletonInstances.get(serviceClass);
            if (instance == null) {
                instance = (T) factory.get();
                singletonInstances.put(serviceClass, instance);
            }
        }
        
        return instance;
    }
    
    /**
     * Check if a service is already instantiated
     */
    public boolean isInstantiated(Class<?> serviceClass) {
        return singletonInstances.containsKey(serviceClass);
    }
    
    /**
     * Clear all singleton instances (for testing or cleanup)
     */
    public void clearInstances() {
        singletonInstances.clear();
    }
    
    /**
     * Register all factory methods for service creation
     */
    private void registerFactories() {
        // Register plugin instance
        singletonInstances.put(EcoXpertPlugin.class, plugin);
        
        // Core services - order matters for dependencies
        factoryMethods.put(PlatformManager.class, this::createPlatformManager);
        factoryMethods.put(ConfigManager.class, this::createConfigManager);
        factoryMethods.put(TranslationManager.class, this::createTranslationManager);
        factoryMethods.put(DependencyManager.class, this::createDependencyManager);
        factoryMethods.put(DataManager.class, this::createDataManager);
        factoryMethods.put(EconomyFailsafeManager.class, this::createEconomyFailsafeManager);
        factoryMethods.put(UpdateChecker.class, this::createUpdateChecker);
        
        // Economy services
        factoryMethods.put(EconomyManager.class, this::createEconomyManager);
        factoryMethods.put(VaultEconomyProvider.class, this::createVaultEconomyProvider);
        
        // Market services
        factoryMethods.put(MarketManager.class, this::createMarketManager);
        
        // Bank services
        factoryMethods.put(BankManager.class, this::createBankManager);
        
        // Inflation Intelligence System
        factoryMethods.put(InflationManager.class, this::createInflationManager);

        // Dynamic Economic Events Engine
        factoryMethods.put(EconomicEventEngine.class, this::createEconomicEventEngine);

        // Loans module
        factoryMethods.put(LoanManager.class, this::createLoanManager);
        
        // Command system
        factoryMethods.put(CommandManager.class, this::createCommandManager);
        
        // Public API
        factoryMethods.put(EcoXpertAPI.class, this::createAPI);
        
        // Safety
        factoryMethods.put(SafeModeManager.class, this::createSafeModeManager);
        factoryMethods.put(RateLimitManager.class, this::createRateLimitManager);
    }
    
    // Factory methods with dependency injection
    
    private PlatformManager createPlatformManager() {
        return new PlatformManagerImpl(plugin);
    }
    
    private ConfigManager createConfigManager() {
        return new ConfigManagerImpl(plugin);
    }
    
    private TranslationManager createTranslationManager() {
        return new TranslationManagerImpl(
            plugin,
            getInstance(ConfigManager.class)
        );
    }
    
    private DependencyManager createDependencyManager() {
        return new DependencyManagerImpl(plugin);
    }
    
    private DataManager createDataManager() {
        return new DataManagerImpl(
            plugin,
            getInstance(ConfigManager.class)
        );
    }
    
    private EconomyFailsafeManager createEconomyFailsafeManager() {
        return new EconomyFailsafeManagerImpl(
            plugin,
            getInstance(DataManager.class)
        );
    }
    
    private UpdateChecker createUpdateChecker() {
        return new UpdateCheckerImpl(
            plugin,
            getInstance(TranslationManager.class)
        );
    }
    
    private EconomyManager createEconomyManager() {
        return new EconomyManagerImpl(
            plugin,
            getInstance(ConfigManager.class),
            getInstance(DataManager.class)
        );
    }
    
    private VaultEconomyProvider createVaultEconomyProvider() {
        return new VaultEconomyProviderImpl(
            plugin,
            getInstance(EconomyManager.class)
        );
    }
    
    private CommandManager createCommandManager() {
        return new CommandManager(
            plugin,
            getInstance(EconomyManager.class),
            getInstance(MarketManager.class),
            getInstance(BankManager.class),
            getInstance(TranslationManager.class)
        );
    }
    
    private MarketManager createMarketManager() {
        return new MarketManagerImpl(
            plugin,
            getInstance(DataManager.class),
            getInstance(EconomyManager.class),
            getInstance(TranslationManager.class),
            getInstance(ConfigManager.class)
        );
    }
    
    private BankManager createBankManager() {
        return new BankManagerImpl(
            plugin,
            getInstance(DataManager.class),
            getInstance(EconomyManager.class),
            getInstance(InflationManager.class)
        );
    }
    
    private InflationManager createInflationManager() {
        return new InflationManagerImpl(
            plugin,
            getInstance(EconomyManager.class),
            getInstance(MarketManager.class),
            getInstance(ConfigManager.class)
        );
    }
    
    private EconomicEventEngine createEconomicEventEngine() {
        // Event engine integrates core managers for cross-system effects
        return new EconomicEventEngine(
            plugin,
            getInstance(EconomyManager.class),
            getInstance(MarketManager.class),
            getInstance(InflationManager.class)
        );
    }
    
    private LoanManager createLoanManager() {
        return new LoanManagerImpl(
            plugin,
            getInstance(DataManager.class),
            getInstance(EconomyManager.class)
        );
    }
    
    private EcoXpertAPI createAPI() {
        return new EcoXpertAPIImpl(plugin);
    }
    
    private SafeModeManager createSafeModeManager() {
        return new SafeModeManagerImpl(
            plugin,
            getInstance(DataManager.class),
            getInstance(ConfigManager.class)
        );
    }

    private RateLimitManager createRateLimitManager() {
        int opsPerSecond = 5;
        try {
            opsPerSecond = Math.max(1, plugin.getConfig().getInt("security.anti-exploit.ops_per_second", 5));
        } catch (Exception ignored) {}
        return new RateLimitManager(opsPerSecond);
    }
}
