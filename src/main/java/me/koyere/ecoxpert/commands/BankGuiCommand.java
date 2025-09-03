package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.bank.BankGUI;
import me.koyere.ecoxpert.modules.bank.BankManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BankGuiCommand implements CommandExecutor {
    private final EcoXpertPlugin plugin;
    private final BankManager bankManager;
    private final EconomyManager economyManager;
    private final TranslationManager tm;

    public BankGuiCommand(EcoXpertPlugin plugin, BankManager bankManager, EconomyManager economyManager, TranslationManager tm) {
        this.plugin = plugin;
        this.bankManager = bankManager;
        this.economyManager = economyManager;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        new BankGUI(plugin, bankManager, economyManager, tm).open(p);
        return true;
    }
}

