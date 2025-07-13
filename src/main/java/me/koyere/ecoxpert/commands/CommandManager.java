package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.market.MarketManager;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Command management system
 * 
 * Handles registration and coordination of all plugin commands
 * with professional validation and permission checking.
 */
@Singleton
public class CommandManager {
    
    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final TranslationManager translationManager;
    
    private MarketCommand marketCommand;
    
    @Inject
    public CommandManager(EcoXpertPlugin plugin, EconomyManager economyManager, 
                         MarketManager marketManager, TranslationManager translationManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.marketManager = marketManager;
        this.translationManager = translationManager;
    }
    
    /**
     * Register all plugin commands
     */
    public void registerCommands() {
        // Main /eco command with subcommands
        EcoCommand ecoCommand = new EcoCommand(economyManager, translationManager);
        plugin.getCommand("ecoxpert").setExecutor(ecoCommand);
        plugin.getCommand("ecoxpert").setTabCompleter(ecoCommand);
        
        // Direct commands for convenience
        BalanceCommand balanceCommand = new BalanceCommand(economyManager, translationManager);
        plugin.getCommand("ecobalance").setExecutor(balanceCommand);
        plugin.getCommand("ecobalance").setTabCompleter(balanceCommand);
        
        PayCommand payCommand = new PayCommand(economyManager, translationManager);
        plugin.getCommand("ecopay").setExecutor(payCommand);
        plugin.getCommand("ecopay").setTabCompleter(payCommand);
        
        // Market commands
        this.marketCommand = new MarketCommand(marketManager, translationManager, plugin.getLogger());
        plugin.getCommand("market").setExecutor(marketCommand);
        plugin.getCommand("market").setTabCompleter(marketCommand);
        
        // Register MarketGUI events
        plugin.getServer().getPluginManager().registerEvents(marketCommand.getMarketGUI(), plugin);
        
        plugin.getLogger().info("Registered economy and market commands");
    }
    
    /**
     * Unregister all commands
     */
    public void unregisterCommands() {
        // Close all market GUIs
        if (marketCommand != null) {
            marketCommand.getMarketGUI().closeAllGUIs();
        }
        
        // Commands are automatically unregistered when plugin disables
        plugin.getLogger().info("Unregistered economy commands");
    }
}