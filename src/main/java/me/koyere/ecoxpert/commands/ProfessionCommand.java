package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.professions.ProfessionRole;
import me.koyere.ecoxpert.modules.professions.ProfessionsManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfessionCommand implements TabExecutor {

    private final ProfessionsManager professionsManager;
    private final TranslationManager tm;

    public ProfessionCommand(ProfessionsManager professionsManager, TranslationManager tm) {
        this.professionsManager = professionsManager;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(tm.getMessage("player-only")); return true; }
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            professionsManager.getRole(p.getUniqueId()).thenAccept(opt -> {
                String current = opt.map(Enum::name).orElse("NONE");
                String available = String.join(", ", Arrays.stream(ProfessionRole.values()).map(Enum::name).toList());
                p.sendMessage(tm.getMessage("prefix") + "§7" + tm.getMessage("professions.current", current));
                p.sendMessage("§7" + tm.getMessage("professions.available", available));
            });
            return true;
        }
        if (args[0].equalsIgnoreCase("select")) {
            if (!p.hasPermission("ecoxpert.professions.select")) { p.sendMessage(tm.getMessage("no-permission")); return true; }
            if (args.length < 2) { p.sendMessage(tm.getMessage("prefix") + "§cUsage: /profession select <role>"); return true; }
            ProfessionRole role;
            try { role = ProfessionRole.fromString(args[1]); } catch (Exception e) { p.sendMessage(tm.getMessage("prefix") + "§cUnknown role"); return true; }
            var r = role;
            professionsManager.setRole(p.getUniqueId(), role).thenAccept(ok -> {
                if (ok) p.sendMessage(tm.getMessage("prefix") + tm.getMessage("professions.selected", r.name()));
                else p.sendMessage(tm.getMessage("prefix") + tm.getMessage("errors.command-error"));
            });
            return true;
        }
        if (args[0].equalsIgnoreCase("level")) {
            professionsManager.getLevel(p.getUniqueId()).thenAccept(lv ->
                p.sendMessage(tm.getMessage("prefix") + "§7Level: §e" + lv));
            return true;
        }
        if (args[0].equalsIgnoreCase("levelup")) {
            if (!p.hasPermission("ecoxpert.professions.levelup")) { p.sendMessage(tm.getMessage("no-permission")); return true; }
            professionsManager.getLevel(p.getUniqueId()).thenCompose(lv -> professionsManager.setLevel(p.getUniqueId(), lv + 1))
                .thenAccept(ok -> p.sendMessage(tm.getMessage("prefix") + (ok ? "§aLevel up!" : tm.getMessage("errors.command-error"))));
            return true;
        }
        p.sendMessage(tm.getMessage("prefix") + "§cUsage: /profession [info|select <role>|level|levelup]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.add("info"); list.add("select"); list.add("level"); list.add("levelup");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("select")) {
            for (ProfessionRole r : ProfessionRole.values()) list.add(r.name());
        }
        return list;
    }
}
