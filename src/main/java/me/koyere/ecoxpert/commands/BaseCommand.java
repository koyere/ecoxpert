package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base command class with common functionality
 * 
 * Provides validation, permission checking, and utility methods
 * for all economy commands.
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {
    
    protected final EconomyManager economyManager;
    protected final TranslationManager translationManager;
    protected final Logger logger;
    
    protected BaseCommand(EconomyManager economyManager, TranslationManager translationManager) {
        this.economyManager = economyManager;
        this.translationManager = translationManager;
        this.logger = Bukkit.getLogger();
    }
    
    /**
     * Send translated message to sender
     */
    protected void sendMessage(CommandSender sender, String key, Object... args) {
        String message = translationManager.getMessage(key, args);
        sender.sendMessage(message);
    }
    
    /**
     * Check if sender has permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sendMessage(sender, "error.no_permission");
        return false;
    }
    
    /**
     * Get player from sender, ensuring it's a player
     */
    protected Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "error.player_only");
            return null;
        }
        return (Player) sender;
    }
    
    /**
     * Parse and validate monetary amount
     */
    protected BigDecimal parseAmount(CommandSender sender, String amountStr) {
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                sendMessage(sender, "error.invalid_amount");
                return null;
            }
            return BigDecimal.valueOf(amount);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error.invalid_number", amountStr);
            return null;
        }
    }
    
    /**
     * Find target player (online or offline)
     */
    protected OfflinePlayer findPlayer(CommandSender sender, String playerName) {
        Player online = Bukkit.getPlayer(playerName);
        if (online != null) {
            return online;
        }
        
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        if (!offline.hasPlayedBefore()) {
            sendMessage(sender, "error.player_not_found", playerName);
            return null;
        }
        
        return offline;
    }
    
    /**
     * Ensure player has an economy account
     * Uses database constraints to prevent race conditions
     */
    protected CompletableFuture<Void> ensureAccount(UUID playerUuid) {
        // Use INSERT OR IGNORE to prevent race conditions
        return economyManager.hasAccount(playerUuid).thenCompose(hasAccount -> {
            if (!hasAccount) {
                return economyManager.createAccount(playerUuid, economyManager.getStartingBalance());
            }
            return CompletableFuture.completedFuture(null);
        }).exceptionally(throwable -> {
            logger.log(Level.SEVERE, "ECOXPERT ERROR - ensureAccount failed for UUID: " + playerUuid, throwable);
            return null;
        });
    }
    
    /**
     * Handle command execution with comprehensive error logging
     */
    protected void handleCommandSafely(CommandSender sender, String commandName, Runnable command) {
        try {
            logger.info("ECOXPERT DEBUG - Executing command: " + commandName + " for sender: " + sender.getName());
            command.run();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ECOXPERT ERROR - Command execution failed: " + commandName, e);
            sendMessage(sender, "error.database_error");
        }
    }
}