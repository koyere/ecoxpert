package me.koyere.ecoxpert.core.translation;

import org.bukkit.entity.Player;

/**
 * Translation and internationalization management interface
 * 
 * Provides comprehensive text translation system with
 * support for multiple languages and dynamic content.
 */
public interface TranslationManager {
    
    /**
     * Initialize the translation system
     */
    void initialize();
    
    /**
     * Reload all translation files
     */
    void reload();
    
    /**
     * Get translated message for default language
     * 
     * @param key Message key
     * @param args Replacement arguments
     * @return Translated message
     */
    String getMessage(String key, Object... args);
    
    /**
     * Get translated message for specific language
     * 
     * @param language Language code
     * @param key Message key
     * @param args Replacement arguments
     * @return Translated message
     */
    String getMessage(String language, String key, Object... args);
    
    /**
     * Get translated message for player's language
     * 
     * @param player Target player
     * @param key Message key
     * @param args Replacement arguments
     * @return Translated message
     */
    String getPlayerMessage(Player player, String key, Object... args);
    
    /**
     * Send translated message to player
     * 
     * @param player Target player
     * @param key Message key
     * @param args Replacement arguments
     */
    void sendMessage(Player player, String key, Object... args);
    
    /**
     * Send translated message to console
     * 
     * @param key Message key
     * @param args Replacement arguments
     */
    void sendConsoleMessage(String key, Object... args);
    
    /**
     * Get available language codes
     * 
     * @return Array of language codes
     */
    String[] getAvailableLanguages();
    
    /**
     * Check if a language is supported
     * 
     * @param language Language code
     * @return true if language is available
     */
    boolean isLanguageSupported(String language);
    
    /**
     * Get the default language code
     * 
     * @return Default language
     */
    String getDefaultLanguage();
    
    /**
     * Get player's preferred language
     * 
     * @param player Target player
     * @return Player's language code
     */
    String getPlayerLanguage(Player player);
    
    /**
     * Set player's preferred language
     * 
     * @param player Target player
     * @param language Language code
     */
    void setPlayerLanguage(Player player, String language);
}