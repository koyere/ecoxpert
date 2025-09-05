package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.professions.ProfessionsGUI;
import me.koyere.ecoxpert.modules.professions.ProfessionsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ProfessionGuiCommand implements CommandExecutor {
    private final EcoXpertPlugin plugin;
    private final ProfessionsManager professionsManager;
    private final TranslationManager tm;

    public ProfessionGuiCommand(EcoXpertPlugin plugin, ProfessionsManager professionsManager, TranslationManager tm) {
        this.plugin = plugin;
        this.professionsManager = professionsManager;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        new ProfessionsGUI(plugin, professionsManager, tm).open(p);
        return true;
    }
}

