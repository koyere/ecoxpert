package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Standalone pay command
 * 
 * Provides quick money transfers between players.
 * Includes validation, notifications, and error handling.
 */
public class PayCommand extends BaseCommand {
    
    public PayCommand(EconomyManager economyManager, TranslationManager translationManager, ConfigManager configManager) {
        super(economyManager, translationManager, configManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            var plugin = me.koyere.ecoxpert.EcoXpertPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class);
            var services = plugin.getServiceRegistry();
            var limiter = services.getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
            if (limiter != null && !limiter.allow(((Player) sender).getUniqueId(), "pay")) {
                sendMessage(sender, "errors.command-error");
                return true;
            }
        }
        if (!hasPermission(sender, "ecoxpert.economy.pay")) {
            return true;
        }
        
        Player player = requirePlayer(sender);
        if (player == null) return true;
        
        if (args.length < 2) {
            sendMessage(sender, "commands.pay.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendMessage(sender, "error.cannot_pay_self");
            return true;
        }
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(player.getUniqueId());
        ensureAccount(target.getUniqueId());
        
        economyManager.transferMoney(
                player.getUniqueId(), 
                target.getUniqueId(), 
                amount, 
                "Player payment from " + player.getName()
        ).thenAccept(success -> {
            String formattedAmount = economyManager.formatMoney(amount);
            if (success) {
                sendMessage(sender, "commands.pay.success", formattedAmount, target.getName());
                
                // Notify target if online
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    sendMessage(targetPlayer, "commands.pay.received", formattedAmount, player.getName());
                }
            } else {
                sendMessage(sender, "error.insufficient_funds");
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Player names (exclude self)
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> !name.equals(sender.getName()))
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2) {
            // Amount suggestions
            List<String> amounts = new ArrayList<>();
            amounts.add("10");
            amounts.add("50");
            amounts.add("100");
            amounts.add("500");
            amounts.add("1000");
            
            return amounts.stream()
                    .filter(amount -> amount.startsWith(args[1]))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
