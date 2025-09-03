package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import me.koyere.ecoxpert.modules.loans.LoansGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoansGuiCommand implements CommandExecutor {
    private final EcoXpertPlugin plugin;
    private final LoanManager loanManager;
    private final EconomyManager economyManager;
    private final TranslationManager tm;

    public LoansGuiCommand(EcoXpertPlugin plugin, LoanManager loanManager, EconomyManager economyManager, TranslationManager tm) {
        this.plugin = plugin;
        this.loanManager = loanManager;
        this.economyManager = economyManager;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        new LoansGUI(plugin, loanManager, economyManager, tm).open(p);
        return true;
    }
}

