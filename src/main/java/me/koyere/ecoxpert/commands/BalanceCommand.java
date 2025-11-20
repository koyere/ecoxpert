package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standalone balance command
 * 
 * Provides quick access to balance checking without subcommands.
 * Supports checking own balance or other players (with permission).
 */
public class BalanceCommand extends BaseCommand {
    
    public BalanceCommand(EconomyManager economyManager, TranslationManager translationManager, me.koyere.ecoxpert.core.config.ConfigManager configManager) {
        super(economyManager, translationManager, configManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        handleCommandSafely(sender, "balance", () -> {
            debug("BalanceCommand started for: " + sender.getName());
            
            if (!hasPermission(sender, "ecoxpert.economy.balance")) {
                debug("Permission denied for: " + sender.getName());
                return;
            }
            
            OfflinePlayer target;
            
            if (args.length == 0) {
                // Check own balance
                Player player = requirePlayer(sender);
                if (player == null) {
                    debug("requirePlayer returned null");
                    return;
                }
                target = player;
                debug("Checking own balance for: " + player.getName());
            } else {
                // Check another player's balance
                if (!hasPermission(sender, "ecoxpert.admin.economy")) {
                    debug("Admin permission denied for: " + sender.getName());
                    return;
                }
                target = findPlayer(sender, args[0]);
                if (target == null) {
                    debug("findPlayer returned null for: " + args[0]);
                    return;
                }
                debug("Checking balance for other player: " + target.getName());
            }
            
            debug("About to ensureAccount for UUID: " + target.getUniqueId());
            ensureAccount(target.getUniqueId()).thenRun(() -> {
                debug("ensureAccount completed, getting balance for: " + target.getUniqueId());
                
                economyManager.getBalance(target.getUniqueId()).thenAccept(balance -> {
                    debug("getBalance returned: " + balance + " for: " + target.getName());
                    String formattedBalance = economyManager.formatMoney(balance);
                    if (target.equals(sender)) {
                        sendMessage(sender, "commands.balance.own", formattedBalance);
                    } else {
                        sendMessage(sender, "commands.balance.other", target.getName(), formattedBalance);
                    }
                    debug("Balance command completed successfully");
                }).exceptionally(throwable -> {
                    logger.log(java.util.logging.Level.SEVERE, "ECOXPERT ERROR - getBalance failed for: " + target.getName(), throwable);
                    sendMessage(sender, "error.database_error");
                    return null;
                });
            }).exceptionally(throwable -> {
                logger.log(java.util.logging.Level.SEVERE, "ECOXPERT ERROR - ensureAccount failed in command for: " + target.getName(), throwable);
                sendMessage(sender, "error.database_error");
                return null;
            });
        });
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("ecoxpert.admin.economy")) {
            // Player names for admins
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
