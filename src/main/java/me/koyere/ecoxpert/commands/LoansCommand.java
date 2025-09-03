package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.loans.Loan;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Loans command: /loans request <amount> | pay <amount> | status
 *
 * Permissions:
 * - ecoxpert.loans.request for requesting loans
 * - ecoxpert.loans.pay for paying loans
 */
public class LoansCommand extends BaseCommand {

    private final LoanManager loanManager;

    public LoansCommand(LoanManager loanManager, EconomyManager economyManager, TranslationManager translationManager) {
        super(economyManager, translationManager);
        this.loanManager = loanManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "request":
                return handleRequest(player, args);
            case "pay":
                return handlePay(player, args);
            case "status":
                return handleStatus(player);
            default:
                showHelp(player);
                return true;
        }
    }

    private boolean handleRequest(Player player, String[] args) {
        if (!player.hasPermission("ecoxpert.loans.request")) {
            sendMessage(player, "error.no_permission");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("loans.usage.request"));
            return true;
        }
        BigDecimal amount = parseAmount(player, args[1]);
        if (amount == null) return true;

        // Basic interest rate for MVP (e.g., 5%)
        BigDecimal rate = new BigDecimal("0.05");
        loanManager.requestLoan(player.getUniqueId(), amount, rate).thenAccept(success -> {
            if (success) {
                player.sendMessage(translationManager.getMessage("loans.request-sent", economyManager.formatMoney(amount)));
                player.sendMessage(translationManager.getMessage("loans.loan-approved", economyManager.formatMoney(amount)));
            } else {
                player.sendMessage(translationManager.getMessage("loans.loan-denied", "Active loan exists or invalid amount"));
            }
        }).exceptionally(ex -> {
            player.sendMessage(translationManager.getMessage("errors.database-error"));
            return null;
        });
        return true;
    }

    private boolean handlePay(Player player, String[] args) {
        if (!player.hasPermission("ecoxpert.loans.pay")) {
            sendMessage(player, "error.no_permission");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("loans.usage.pay"));
            return true;
        }
        BigDecimal amount = parseAmount(player, args[1]);
        if (amount == null) return true;

        loanManager.payLoan(player.getUniqueId(), amount).thenAccept(success -> {
            if (success) {
                player.sendMessage(translationManager.getMessage("loans.payment-made", economyManager.formatMoney(amount)));
            } else {
                player.sendMessage(translationManager.getMessage("loans.loan-denied", "Insufficient funds or no active loan"));
            }
        }).exceptionally(ex -> {
            player.sendMessage(translationManager.getMessage("errors.database-error"));
            return null;
        });
        return true;
    }

    private boolean handleStatus(Player player) {
        loanManager.getActiveLoan(player.getUniqueId()).thenAccept(opt -> {
            if (opt.isEmpty()) {
                player.sendMessage(translationManager.getMessage("loans.no-active-loans"));
                return;
            }
            Loan loan = opt.get();
            player.sendMessage(translationManager.getMessage("loans.status",
                economyManager.formatMoney(loan.getOutstanding()),
                economyManager.formatMoney(loan.getPrincipal()),
                loan.getInterestRate().multiply(new BigDecimal("100")).setScale(1) + "%"));
        }).exceptionally(ex -> {
            player.sendMessage(translationManager.getMessage("errors.database-error"));
            return null;
        });
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(translationManager.getMessage("loans.help.header"));
        player.sendMessage(translationManager.getMessage("loans.help.request"));
        player.sendMessage(translationManager.getMessage("loans.help.pay"));
        player.sendMessage(translationManager.getMessage("loans.help.status"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : new String[]{"request", "pay", "status"}) {
                if (s.startsWith(args[0].toLowerCase())) out.add(s);
            }
        } else if (args.length == 2 && ("request".equalsIgnoreCase(args[0]) || "pay".equalsIgnoreCase(args[0]))) {
            for (String s : new String[]{"100", "500", "1000", "5000"}) {
                if (s.startsWith(args[1])) out.add(s);
            }
        }
        return out;
    }
}

