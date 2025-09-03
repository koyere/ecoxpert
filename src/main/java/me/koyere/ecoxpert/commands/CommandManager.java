package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.bank.BankManager;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import me.koyere.ecoxpert.commands.LoansCommand;

import java.util.Arrays;

public class CommandManager {

    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final BankManager bankManager;
    private final TranslationManager translationManager;
    private final EconomicEventEngine eventEngine;
    private final LoanManager loanManager;
    private MarketCommand marketCommand;

    public CommandManager(EcoXpertPlugin plugin, EconomyManager economyManager, MarketManager marketManager, BankManager bankManager, TranslationManager translationManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.marketManager = marketManager;
        this.bankManager = bankManager;
        this.translationManager = translationManager;
        // Fetch event engine from service registry to wire into commands without changing constructor API
        this.eventEngine = plugin.getServiceRegistry().getInstance(EconomicEventEngine.class);
        this.loanManager = plugin.getServiceRegistry().getInstance(LoanManager.class);
    }
    
    /**
     * Register all plugin commands
     */
    public void registerCommands() {
        // Main /eco command with subcommands
        // Main /ecoxpert command with subcommands (economy + admin + events)
        EcoCommand ecoCommand = new EcoCommand(economyManager, translationManager, eventEngine);
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
        
        // Bank commands
        BankCommand bankCommand = new BankCommand(bankManager, economyManager, translationManager);
        plugin.getCommand("bank").setExecutor(bankCommand);
        plugin.getCommand("bank").setTabCompleter(bankCommand);

        // Loans commands
        LoansCommand loansCommand = new LoansCommand(loanManager, economyManager, translationManager);
        if (plugin.getCommand("loans") != null) {
            plugin.getCommand("loans").setExecutor(loansCommand);
            plugin.getCommand("loans").setTabCompleter(loansCommand);
        }
        
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
