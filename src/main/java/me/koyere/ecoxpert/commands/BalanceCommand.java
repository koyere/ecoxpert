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
    
    public BalanceCommand(EconomyManager economyManager, TranslationManager translationManager) {
        super(economyManager, translationManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasPermission(sender, "ecoxpert.economy.balance")) {
            return true;
        }
        
        OfflinePlayer target;
        
        if (args.length == 0) {
            // Check own balance
            Player player = requirePlayer(sender);
            if (player == null) return true;
            target = player;
        } else {
            // Check another player's balance
            if (!hasPermission(sender, "ecoxpert.admin.economy")) {
                return true;
            }
            target = findPlayer(sender, args[0]);
            if (target == null) return true;
        }
        
        ensureAccount(target.getUniqueId());
        
        economyManager.getBalance(target.getUniqueId()).thenAccept(balance -> {
            String formattedBalance = economyManager.formatMoney(balance);
            if (target.equals(sender)) {
                sendMessage(sender, "commands.balance.own", formattedBalance);
            } else {
                sendMessage(sender, "commands.balance.other", target.getName(), formattedBalance);
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
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