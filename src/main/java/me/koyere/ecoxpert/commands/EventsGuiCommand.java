package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.events.EventsAdminGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EventsGuiCommand implements CommandExecutor {
    private final EcoXpertPlugin plugin;
    private final EconomicEventEngine engine;
    private final TranslationManager tm;

    public EventsGuiCommand(EcoXpertPlugin plugin, EconomicEventEngine engine, TranslationManager tm) {
        this.plugin = plugin;
        this.engine = engine;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only"); return true; }
        if (!p.hasPermission("ecoxpert.admin.events")) { p.sendMessage(tm.getMessage("error.no-permission")); return true; }
        new EventsAdminGUI(plugin, engine, tm).open(p);
        return true;
    }
}

