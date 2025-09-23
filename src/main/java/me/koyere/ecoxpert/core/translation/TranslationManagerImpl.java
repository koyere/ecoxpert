package me.koyere.ecoxpert.core.translation;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of translation and internationalization system
 * 
 * Provides complete translation support with per-player language
 * preferences and dynamic message loading.
 */
@Singleton
public class TranslationManagerImpl implements TranslationManager {
    
    private final EcoXpertPlugin plugin;
    private final ConfigManager configManager;
    
    // Language configurations cache
    private final Map<String, FileConfiguration> languageConfigs = new HashMap<>();
    
    // Player language preferences
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    
    // Default language
    private String defaultLanguage;
    
    @Inject
    public TranslationManagerImpl(EcoXpertPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing translation system...");
        
        this.defaultLanguage = configManager.getLanguage();
        
        // Create languages directory
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }
        
        // Load default language files
        loadDefaultLanguageFiles();
        
        // Load all available language configurations
        loadLanguageConfigurations();
        
        plugin.getLogger().info("Translation system initialized with " + 
                               languageConfigs.size() + " languages");
    }
    
    @Override
    public void reload() {
        languageConfigs.clear();
        playerLanguages.clear();
        
        this.defaultLanguage = configManager.getLanguage();
        loadLanguageConfigurations();
        
        plugin.getLogger().info("Translation system reloaded");
    }
    
    @Override
    public String getMessage(String key, Object... args) {
        return getMessage(defaultLanguage, key, args);
    }
    
    @Override
    public String getMessage(String language, String key, Object... args) {
        // Normalize language codes like es_MX -> es, en-US -> en
        String lang = language;
        if (lang != null && lang.length() > 2) {
            String[] parts = lang.split("[-_]");
            if (parts.length > 0 && parts[0].length() == 2) {
                lang = parts[0].toLowerCase();
            }
        }
        FileConfiguration config = languageConfigs.get(lang);
        if (config == null) config = languageConfigs.get(defaultLanguage);
        if (config == null) config = languageConfigs.get("en");
        String message = config != null ? config.getString(key) : null;
        if (message == null) {
            // Fallback: try to load from embedded resource (keeps existing file as-is)
            message = loadFromEmbedded(lang, key);
            if (message == null && !"en".equals(lang)) {
                message = loadFromEmbedded("en", key);
            }
            if (message == null) message = key; // final fallback prints the key
        }
        
        // Apply formatting with arguments
        return formatMessage(message, args);
    }

    private String loadFromEmbedded(String language, String key) {
        try (java.io.InputStream in = plugin.getResource("languages/messages_" + language + ".yml")) {
            if (in == null) return null;
            org.bukkit.configuration.file.FileConfiguration cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in));
            return cfg.getString(key);
        } catch (Exception ignored) { return null; }
    }
    
    @Override
    public String getPlayerMessage(Player player, String key, Object... args) {
        String language = getPlayerLanguage(player);
        return getMessage(language, key, args);
    }
    
    @Override
    public void sendMessage(Player player, String key, Object... args) {
        String message = getPlayerMessage(player, key, args);
        player.sendMessage(message);
    }
    
    @Override
    public void sendConsoleMessage(String key, Object... args) {
        String message = getMessage(key, args);
        plugin.getLogger().info(message);
    }
    
    @Override
    public String[] getAvailableLanguages() {
        return languageConfigs.keySet().toArray(new String[0]);
    }
    
    @Override
    public boolean isLanguageSupported(String language) {
        return languageConfigs.containsKey(language);
    }
    
    @Override
    public String getDefaultLanguage() {
        return defaultLanguage;
    }
    
    @Override
    public String getPlayerLanguage(Player player) {
        return playerLanguages.getOrDefault(player.getUniqueId(), defaultLanguage);
    }
    
    @Override
    public void setPlayerLanguage(Player player, String language) {
        if (isLanguageSupported(language)) {
            playerLanguages.put(player.getUniqueId(), language);
            // TODO: Save to database
        }
    }
    
    /**
     * Load default language files from plugin resources
     */
    private void loadDefaultLanguageFiles() {
        String[] languages = {"en", "es"};
        
        for (String lang : languages) {
            String fileName = "messages_" + lang + ".yml";
            File langFile = new File(plugin.getDataFolder(), "languages/" + fileName);
            
            if (!langFile.exists()) {
                try {
                    InputStream resource = plugin.getResource("languages/" + fileName);
                    if (resource != null) {
                        Files.copy(resource, langFile.toPath());
                        plugin.getLogger().info("Created default language file: " + fileName);
                    } else {
                        createBasicLanguageFile(langFile, lang);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to create language file: " + fileName);
                    createBasicLanguageFile(langFile, lang);
                }
            }
        }
    }
    
    /**
     * Create a basic language file with essential messages
     * 
     * @param file Language file
     * @param language Language code
     */
    private void createBasicLanguageFile(File file, String language) {
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            
            // Add basic messages based on language
            if ("es".equals(language)) {
                config.set("prefix", "&8[&6EcoXpert&8] &f");
                config.set("plugin.enabled", "&aEcoXpert Pro habilitado correctamente");
                config.set("plugin.disabled", "&cEcoXpert Pro deshabilitado");
                config.set("error.no-permission", "&cNo tienes permisos para ejecutar este comando");
                config.set("error.player-only", "&cEste comando solo puede ser ejecutado por jugadores");
                // Basic admin GUI labels
                config.set("admin.gui.title", "Panel Admin EcoXpert");
                config.set("admin.gui.events", "Eventos");
                config.set("admin.gui.market", "Mercado");
                config.set("admin.gui.loans", "Préstamos");
                config.set("admin.gui.economy", "Economía");
            } else {
                config.set("prefix", "&8[&6EcoXpert&8] &f");
                config.set("plugin.enabled", "&aEcoXpert Pro enabled successfully");
                config.set("plugin.disabled", "&cEcoXpert Pro disabled");
                config.set("error.no-permission", "&cYou don't have permission to execute this command");
                config.set("error.player-only", "&cThis command can only be executed by players");
                // Basic admin GUI labels
                config.set("admin.gui.title", "EcoXpert Admin");
                config.set("admin.gui.events", "Events");
                config.set("admin.gui.market", "Market");
                config.set("admin.gui.loans", "Loans");
                config.set("admin.gui.economy", "Economy");
            }
            
            config.save(file);
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create basic language file: " + file.getName());
        }
    }
    
    /**
     * Load all language configurations from files
     */
    private void loadLanguageConfigurations() {
        File languagesDir = new File(plugin.getDataFolder(), "languages");
        
        if (!languagesDir.exists()) {
            return;
        }
        
        File[] files = languagesDir.listFiles((dir, name) -> 
            name.startsWith("messages_") && name.endsWith(".yml"));
        
        if (files != null) {
            for (File file : files) {
                String fileName = file.getName();
                String language = fileName.substring(9, fileName.length() - 4); // Extract from "messages_XX.yml"
                
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                languageConfigs.put(language, config);
                
                plugin.getLogger().info("Loaded language: " + language);
            }
        }
    }
    
    /**
     * Format message with arguments using placeholder replacement
     * 
     * @param message Base message
     * @param args Arguments for replacement
     * @return Formatted message
     */
    private String formatMessage(String message, Object... args) {
        if (args.length == 0) {
            return message.replace('&', '§');
        }
        
        // Replace numbered placeholders {0}, {1}, etc.
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }
        
        // Apply color codes
        return message.replace('&', '§');
    }
}
