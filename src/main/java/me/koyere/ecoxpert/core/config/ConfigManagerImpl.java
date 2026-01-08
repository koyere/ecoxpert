package me.koyere.ecoxpert.core.config;

import me.koyere.ecoxpert.EcoXpertPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of configuration management system
 * 
 * Provides centralized configuration handling with auto-update
 * capabilities and preservation of user customizations.
 */
@Singleton
public class ConfigManagerImpl implements ConfigManager {
    
    private final EcoXpertPlugin plugin;
    private final Map<String, FileConfiguration> moduleConfigs = new HashMap<>();
    
    // Configuration values cache
    private boolean metricsEnabled = true;
    private boolean updateCheckEnabled = true;
    private String databaseType = "sqlite";
    private String language = "en";
    private boolean debugEnabled = false;
    private boolean simpleMode = false;
    private MySqlConfig mySqlConfig;
    
    @Inject
    public ConfigManagerImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing configuration system...");
        
        // Save default config if not exists
        plugin.saveDefaultConfig();
        
        // Load main configuration
        loadMainConfig();
        
        // Create module config directories
        createModuleDirectories();
        
        // Load module configurations
        loadModuleConfigs();
        
        plugin.getLogger().info("Configuration system initialized");
    }
    
    @Override
    public void reload() {
        plugin.reloadConfig();
        loadMainConfig();
        
        // Reload module configs
        moduleConfigs.clear();
        loadModuleConfigs();
        
        plugin.getLogger().info("Configuration reloaded");
    }
    
    @Override
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    
    @Override
    public FileConfiguration getModuleConfig(String moduleName) {
        return moduleConfigs.computeIfAbsent(moduleName, this::loadModuleConfig);
    }
    
    @Override
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    @Override
    public boolean isUpdateCheckEnabled() {
        return updateCheckEnabled;
    }
    
    @Override
    public String getDatabaseType() {
        return databaseType;
    }
    
    @Override
    public String getLanguage() {
        return language;
    }
    
    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public boolean isSimpleMode() {
        return simpleMode;
    }
    
    @Override
    public void saveAll() {
        // Save main config
        plugin.saveConfig();
        
        // Save module configs
        moduleConfigs.forEach(this::saveModuleConfig);
    }
    
    /**
     * Load main configuration values into cache
     */
    private void loadMainConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // Prefer values under plugin.* with fallback to legacy root keys
        this.metricsEnabled = config.getBoolean("plugin.metrics.enabled", 
            config.getBoolean("metrics.enabled", true));
        this.updateCheckEnabled = config.getBoolean("plugin.updates.check-enabled", 
            config.getBoolean("updates.check-enabled", true));
        this.databaseType = config.getString("database.type", "sqlite");
        this.language = config.getString("plugin.language", 
            config.getString("language", "en"));
        this.debugEnabled = config.getBoolean("plugin.debug", 
            config.getBoolean("debug", false));
        String mode = config.getString("plugin.config_mode", "advanced");
        this.simpleMode = "simple".equalsIgnoreCase(mode);

        loadMySqlConfig(config);
    }

    @Override
    public MySqlConfig getMySqlConfig() {
        return mySqlConfig;
    }
    
    /**
     * Create module configuration directories
     */
    private void createModuleDirectories() {
        File modulesDir = new File(plugin.getDataFolder(), "modules");
        if (!modulesDir.exists()) {
            modulesDir.mkdirs();
        }
    }
    
    /**
     * Load all module configurations
     */
    private void loadModuleConfigs() {
        String[] modules = {"market", "bank", "loans", "events", "professions", "inflation", "integrations"};

        for (String module : modules) {
            // Use getModuleConfig to properly populate the cache via computeIfAbsent
            getModuleConfig(module);
        }
    }

    private void loadMySqlConfig(FileConfiguration config) {
        var mysqlSection = config.getConfigurationSection("database.mysql");
        if (mysqlSection == null) {
            this.mySqlConfig = new MySqlConfig(
                "localhost",
                3306,
                "ecoxpert",
                "username",
                "password",
                false,
                true,
                new DatabasePoolSettings(null, null, null, null, null)
            );
            return;
        }

        String host = mysqlSection.getString("host", "localhost");
        int port = mysqlSection.getInt("port", 3306);
        String databaseName = mysqlSection.getString("database", "ecoxpert");
        String usernameValue = mysqlSection.getString("username", "username");
        String passwordValue = mysqlSection.getString("password", "password");
        boolean useSsl = mysqlSection.getBoolean("use-ssl", false);
        boolean allowPublicKeyRetrieval = mysqlSection.getBoolean("allow-public-key-retrieval", true);

        Integer maximumPoolSize = null;
        Integer minimumIdle = null;
        Long connectionTimeout = null;
        Long idleTimeout = null;
        Long maxLifetime = null;

        var poolSection = mysqlSection.getConfigurationSection("pool");
        if (poolSection != null) {
            if (poolSection.isSet("maximum-pool-size")) {
                maximumPoolSize = poolSection.getInt("maximum-pool-size");
            }
            if (poolSection.isSet("minimum-idle")) {
                minimumIdle = poolSection.getInt("minimum-idle");
            }
            if (poolSection.isSet("connection-timeout")) {
                connectionTimeout = poolSection.getLong("connection-timeout");
            }
            if (poolSection.isSet("idle-timeout")) {
                idleTimeout = poolSection.getLong("idle-timeout");
            }
            if (poolSection.isSet("max-lifetime")) {
                maxLifetime = poolSection.getLong("max-lifetime");
            }
        }

        this.mySqlConfig = new MySqlConfig(
            host,
            port,
            databaseName,
            usernameValue,
            passwordValue,
            useSsl,
            allowPublicKeyRetrieval,
            new DatabasePoolSettings(maximumPoolSize, minimumIdle, connectionTimeout, idleTimeout, maxLifetime)
        );
    }
    
    /**
     * Load a specific module configuration
     *
     * This method is designed to be used as a mapping function for computeIfAbsent().
     * It should NOT modify moduleConfigs directly - computeIfAbsent handles the put.
     *
     * @param moduleName Module name
     * @return Module configuration
     */
    private FileConfiguration loadModuleConfig(String moduleName) {
        File configFile = new File(plugin.getDataFolder(), "modules/" + moduleName + ".yml");

        // Create default config if not exists
        if (!configFile.exists()) {
            createDefaultModuleConfig(moduleName, configFile);
        }

        // Return config - computeIfAbsent will handle putting it in the map
        return YamlConfiguration.loadConfiguration(configFile);
    }
    
    /**
     * Create default module configuration file
     * 
     * @param moduleName Module name
     * @param configFile Configuration file
     */
    private void createDefaultModuleConfig(String moduleName, File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            // Try to copy default from resources if available
            try (java.io.InputStream in = plugin.getResource("modules/" + moduleName + ".yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
            // Fallback: create minimal file
            configFile.createNewFile();
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.options().setHeader(List.of("EcoXpert Pro - " + moduleName.substring(0, 1).toUpperCase() +
                    moduleName.substring(1) + " Module Configuration"));
            config.set("enabled", true);
            config.set("version", 1);
            config.save(configFile);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create default config for module: " + moduleName);
        }
    }
    
    /**
     * Save a module configuration
     * 
     * @param moduleName Module name
     * @param config Configuration to save
     */
    private void saveModuleConfig(String moduleName, FileConfiguration config) {
        try {
            File configFile = new File(plugin.getDataFolder(), "modules/" + moduleName + ".yml");
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save config for module: " + moduleName);
        }
    }
}
