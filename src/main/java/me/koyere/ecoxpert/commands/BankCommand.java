package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.bank.BankManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class BankCommand extends BaseCommand {

    private final BankManager bankManager;
    
    public BankCommand(BankManager bankManager, EconomyManager economyManager, TranslationManager translationManager, ConfigManager configManager) {
        super(economyManager, translationManager, configManager);
        this.bankManager = bankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "error.player_only");
            return true;
        }
        
        Player player = (Player) sender;

        if (args.length == 0) {
            showBalance(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "balance":
                showBalance(player);
                break;
            case "deposit":
                handleDeposit(player, args);
                break;
            case "withdraw":
                handleWithdraw(player, args);
                break;
            case "transfer":
                handleTransfer(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                sendMessage(sender, "command.usage.bank");
                break;
        }
        return true;
    }

    private void showBalance(Player player) {
        bankManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
            player.sendMessage(translationManager.getMessage("bank.balance", economyManager.formatMoney(balance)));
        }).exceptionally(ex -> {
            player.sendMessage(translationManager.getMessage("bank.error.generic"));
            return null;
        });
    }

    private void handleDeposit(Player player, String[] args) {
        var safe = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        if (safe != null && safe.isActive()) {
            player.sendMessage(translationManager.getMessage("bank.error.safe-mode"));
            return;
        }
        var limiter = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
        if (limiter != null && !limiter.allow(player.getUniqueId(), "bank.deposit")) {
            player.sendMessage(translationManager.getMessage("errors.rate_limited"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("command.usage.bank.deposit"));
            return;
        }
        
        BigDecimal amount = parseAmount(player, args[1]);
        if (amount == null) return;
        
        bankManager.deposit(player, amount).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendMessage(translationManager.getMessage("bank.deposit.success", economyManager.formatMoney(amount), economyManager.formatMoney(result.getNewBalance())));
            } else {
                player.sendMessage(result.getFormattedMessage());
            }
        });
    }

    private void handleWithdraw(Player player, String[] args) {
        var safe = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        if (safe != null && safe.isActive()) {
            player.sendMessage(translationManager.getMessage("bank.error.safe-mode"));
            return;
        }
        var limiter = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
        if (limiter != null && !limiter.allow(player.getUniqueId(), "bank.withdraw")) {
            player.sendMessage(translationManager.getMessage("errors.rate_limited"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("command.usage.bank.withdraw"));
            return;
        }
        
        BigDecimal amount = parseAmount(player, args[1]);
        if (amount == null) return;
        
        bankManager.withdraw(player, amount).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendMessage(translationManager.getMessage("bank.withdraw.success", economyManager.formatMoney(amount), economyManager.formatMoney(result.getNewBalance())));
            } else {
                player.sendMessage(result.getFormattedMessage());
            }
        });
    }

    private void handleTransfer(Player player, String[] args) {
        var safe = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        if (safe != null && safe.isActive()) {
            player.sendMessage(translationManager.getMessage("bank.error.safe-mode"));
            return;
        }
        var limiter = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
        if (limiter != null && !limiter.allow(player.getUniqueId(), "bank.transfer")) {
            player.sendMessage(translationManager.getMessage("errors.rate_limited"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(translationManager.getMessage("command.usage.bank.transfer"));
            return;
        }
        
        BigDecimal amount = parseAmount(player, args[2]);
        if (amount == null) return;
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(translationManager.getMessage("command.error.player-not-found"));
            return;
        }
        
        bankManager.transfer(player, target.getUniqueId(), amount).thenAccept(result -> {
            if (result.isSuccess()) {
                player.sendMessage(translationManager.getMessage("bank.transfer.success.sender", economyManager.formatMoney(amount), target.getName(), economyManager.formatMoney(result.getNewBalance())));
                target.sendMessage(translationManager.getMessage("bank.transfer.success.receiver", economyManager.formatMoney(amount), player.getName()));
            } else {
                player.sendMessage(result.getFormattedMessage());
            }
        });
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== Bank Commands ===");
        player.sendMessage("§e/bank balance §7- View your bank balance");
        player.sendMessage("§e/bank deposit <amount> §7- Deposit money");
        player.sendMessage("§e/bank withdraw <amount> §7- Withdraw money");
        player.sendMessage("§e/bank transfer <player> <amount> §7- Transfer money");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("balance", "deposit", "withdraw", "transfer", "help");
        }
        return Collections.emptyList();
    }
}
