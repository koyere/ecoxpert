package me.koyere.ecoxpert.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

/**
 * Vault economy provider implementation
 * 
 * Provides EcoXpert economy services through Vault API
 * for compatibility with other plugins.
 */
@Singleton
public class VaultEconomyProviderImpl implements VaultEconomyProvider, Economy {
    
    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private boolean registered = false;
    
    @Inject
    public VaultEconomyProviderImpl(EcoXpertPlugin plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }
    
    @Override
    public boolean register() {
        if (!registered) {
            plugin.getServer().getServicesManager().register(
                Economy.class, this, plugin, ServicePriority.Highest);
            registered = true;
            plugin.getLogger().info("Registered as Vault economy provider");
        }
        return registered;
    }
    
    @Override
    public void unregister() {
        if (registered) {
            plugin.getServer().getServicesManager().unregister(Economy.class, this);
            registered = false;
            plugin.getLogger().info("Unregistered Vault economy provider");
        }
    }
    
    @Override
    public boolean isRegistered() {
        return registered;
    }
    
    @Override
    public String getName() {
        return "EcoXpert Pro";
    }
    
    // Vault Economy Implementation (Basic stubs for compilation)
    
    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }
    
    @Override
    public String format(double amount) {
        return economyManager.formatMoney(BigDecimal.valueOf(amount));
    }
    
    @Override
    public String currencyNamePlural() {
        return economyManager.getCurrencyNamePlural();
    }
    
    @Override
    public String currencyNameSingular() {
        return economyManager.getCurrencyNameSingular();
    }
    
    @Override
    public boolean hasAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(player);
    }
    
    @Override
    public boolean hasAccount(OfflinePlayer player) {
        try {
            return economyManager.hasAccount(player.getUniqueId()).join();
        } catch (CompletionException e) {
            plugin.getLogger().warning("Error checking account for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }
    
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }
    
    @Override
    public double getBalance(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return getBalance(player);
    }
    
    @Override
    public double getBalance(OfflinePlayer player) {
        try {
            BigDecimal balance = economyManager.getBalance(player.getUniqueId()).join();
            return balance.doubleValue();
        } catch (CompletionException e) {
            plugin.getLogger().warning("Error getting balance for player " + player.getName() + ": " + e.getMessage());
            return 0.0;
        }
    }
    
    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }
    
    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }
    
    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }
    
    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }
    
    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }
    
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), 
                    EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount");
        }
        
        try {
            BigDecimal withdrawAmount = BigDecimal.valueOf(amount);
            boolean success = economyManager.removeMoney(
                    player.getUniqueId(), 
                    withdrawAmount, 
                    "Vault withdrawal"
            ).join();
            
            double newBalance = getBalance(player);
            
            if (success) {
                return new EconomyResponse(amount, newBalance, 
                        EconomyResponse.ResponseType.SUCCESS, null);
            } else {
                return new EconomyResponse(0, newBalance, 
                        EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
            }
        } catch (CompletionException e) {
            plugin.getLogger().warning("Error withdrawing from player " + player.getName() + ": " + e.getMessage());
            return new EconomyResponse(0, getBalance(player), 
                    EconomyResponse.ResponseType.FAILURE, "Database error: " + e.getMessage());
        }
    }
    
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }
    
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (amount < 0) {
            return new EconomyResponse(0, getBalance(player), 
                    EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount");
        }
        
        try {
            BigDecimal depositAmount = BigDecimal.valueOf(amount);
            economyManager.addMoney(
                    player.getUniqueId(), 
                    depositAmount, 
                    "Vault deposit"
            ).join();
            
            double newBalance = getBalance(player);
            
            return new EconomyResponse(amount, newBalance, 
                    EconomyResponse.ResponseType.SUCCESS, null);
        } catch (CompletionException e) {
            plugin.getLogger().warning("Error depositing to player " + player.getName() + ": " + e.getMessage());
            return new EconomyResponse(0, getBalance(player), 
                    EconomyResponse.ResponseType.FAILURE, "Database error: " + e.getMessage());
        }
    }
    
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }
    
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }
    
    @Override
    public EconomyResponse createBank(String name, String player) {
        // TODO: Implement bank creation
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        // TODO: Implement bank creation
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse deleteBank(String name) {
        // TODO: Implement bank deletion
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse bankBalance(String name) {
        // TODO: Implement bank balance check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse bankHas(String name, double amount) {
        // TODO: Implement bank balance check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        // TODO: Implement bank withdrawal
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        // TODO: Implement bank deposit
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        // TODO: Implement bank ownership check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        // TODO: Implement bank ownership check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        // TODO: Implement bank membership check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        // TODO: Implement bank membership check
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, 
                                 "Banks not yet implemented");
    }
    
    @Override
    public List<String> getBanks() {
        // TODO: Return list of banks
        return List.of();
    }
    
    @Override
    public boolean createPlayerAccount(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        return createPlayerAccount(player);
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        try {
            if (!hasAccount(player)) {
                economyManager.createAccount(
                        player.getUniqueId(), 
                        economyManager.getStartingBalance()
                ).join();
            }
            return true;
        } catch (CompletionException e) {
            plugin.getLogger().warning("Error creating account for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return createPlayerAccount(playerName);
    }
    
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player);
    }
    
    @Override
    public int fractionalDigits() {
        return 2;
    }
    
    @Override
    public boolean hasBankSupport() {
        return false; // TODO: Enable when banks are implemented
    }
}