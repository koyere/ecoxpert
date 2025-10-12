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
    
    // ===== SHARED BANK ACCOUNTS API =====
    // These methods will be implemented when shared/group bank accounts are added.
    // Currently, EcoXpert only supports personal player bank accounts via BankManager.
    //
    // FUTURE IMPLEMENTATION NOTES:
    // - Shared banks could be used for Factions, Guilds, Towns (Towny), Lands, etc.
    // - Database schema will need: ecoxpert_shared_banks (id, name, balance, owner_uuid, created_at)
    //                               ecoxpert_shared_bank_members (bank_id, player_uuid, role)
    // - Integration with BankManager to support both personal and shared accounts
    // - Permission system: owner, member, admin roles
    // - Transaction logging for audit trails

    @Override
    public EconomyResponse createBank(String name, String player) {
        // TODO [FUTURE]: Create shared bank account
        // Implementation: Insert into ecoxpert_shared_banks with owner
        // Validate: Check if name is unique, player exists
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented - use /bank for personal accounts");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        // TODO [FUTURE]: Create shared bank account with OfflinePlayer owner
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented - use /bank for personal accounts");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        // TODO [FUTURE]: Delete shared bank account
        // Implementation: Soft delete or hard delete with balance redistribution to owner
        // Validate: Check permissions, handle remaining balance
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        // TODO [FUTURE]: Get shared bank balance
        // Implementation: SELECT balance FROM ecoxpert_shared_banks WHERE name = ?
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        // TODO [FUTURE]: Check if shared bank has sufficient balance
        // Implementation: Compare balance >= amount
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        // TODO [FUTURE]: Withdraw from shared bank
        // Implementation: BankManager-like withdrawal with permission checks
        // Validate: Check member permissions, sufficient balance
        // Log: Record transaction in ecoxpert_shared_bank_transactions
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        // TODO [FUTURE]: Deposit to shared bank
        // Implementation: BankManager-like deposit with permission checks
        // Log: Record transaction in ecoxpert_shared_bank_transactions
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        // TODO [FUTURE]: Check if player is bank owner
        // Implementation: SELECT owner_uuid FROM ecoxpert_shared_banks WHERE name = ?
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        // TODO [FUTURE]: Check if OfflinePlayer is bank owner
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        // TODO [FUTURE]: Check if player is bank member
        // Implementation: SELECT FROM ecoxpert_shared_bank_members WHERE bank_name = ? AND player_uuid = ?
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        // TODO [FUTURE]: Check if OfflinePlayer is bank member
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                                 "Shared bank accounts not yet implemented");
    }
    
    @Override
    public List<String> getBanks() {
        // TODO [FUTURE]: Return list of all shared bank names
        // Implementation: SELECT name FROM ecoxpert_shared_banks
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