package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main /eco command with subcommands
 * 
 * Provides comprehensive economy management through subcommands:
 * - balance: Check player balance
 * - pay: Transfer money to another player
 * - set: Set player balance (admin)
 * - add: Add money to player (admin)
 * - remove: Remove money from player (admin)
 */
public class EcoCommand extends BaseCommand {
    
    public EcoCommand(EconomyManager economyManager, TranslationManager translationManager) {
        super(economyManager, translationManager);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (subcommand) {
            case "balance":
            case "bal":
                return handleBalance(sender, subArgs);
                
            case "pay":
                return handlePay(sender, subArgs);
                
            case "set":
                return handleSet(sender, subArgs);
                
            case "add":
            case "give":
                return handleAdd(sender, subArgs);
                
            case "remove":
            case "take":
                return handleRemove(sender, subArgs);
                
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }
    
    private boolean handleBalance(CommandSender sender, String[] args) {
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
    
    private boolean handlePay(CommandSender sender, String[] args) {
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
    
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.set.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.setBalance(
                target.getUniqueId(), 
                amount, 
                "Admin set by " + sender.getName()
        ).thenRun(() -> {
            String formattedAmount = economyManager.formatMoney(amount);
            sendMessage(sender, "commands.set.success", target.getName(), formattedAmount);
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.add.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.addMoney(
                target.getUniqueId(), 
                amount, 
                "Admin add by " + sender.getName()
        ).thenRun(() -> {
            String formattedAmount = economyManager.formatMoney(amount);
            sendMessage(sender, "commands.add.success", formattedAmount, target.getName());
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.remove.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.removeMoney(
                target.getUniqueId(), 
                amount, 
                "Admin remove by " + sender.getName()
        ).thenAccept(success -> {
            String formattedAmount = economyManager.formatMoney(amount);
            if (success) {
                sendMessage(sender, "commands.remove.success", formattedAmount, target.getName());
            } else {
                sendMessage(sender, "error.insufficient_funds");
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, "commands.help.header");
        sendMessage(sender, "commands.help.balance");
        sendMessage(sender, "commands.help.pay");
        
        if (sender.hasPermission("ecoxpert.admin.economy")) {
            sendMessage(sender, "commands.help.admin_header");
            sendMessage(sender, "commands.help.set");
            sendMessage(sender, "commands.help.add");
            sendMessage(sender, "commands.help.remove");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subcommands = Arrays.asList("balance", "pay", "help");
            if (sender.hasPermission("ecoxpert.admin.economy")) {
                subcommands = Arrays.asList("balance", "pay", "set", "add", "remove", "help");
            }
            
            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && !args[0].equalsIgnoreCase("help")) {
            // Player names
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }
}