package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AdminGuiCommand implements CommandExecutor, Listener {
    private final EcoXpertPlugin plugin;
    private final TranslationManager tm;
    private Inventory inv;

    public AdminGuiCommand(EcoXpertPlugin plugin, TranslationManager tm) {
        this.plugin = plugin;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only"); return true; }
        if (!p.hasPermission("ecoxpert.admin")) { p.sendMessage(tm.getMessage("error.no-permission")); return true; }
        inv = Bukkit.createInventory(null, 27, tm.getMessage("admin.gui.title"));
        inv.setItem(11, item(Material.NETHER_STAR, tm.getMessage("admin.gui.events")));
        inv.setItem(13, item(Material.EMERALD_BLOCK, tm.getMessage("admin.gui.market")));
        inv.setItem(15, item(Material.GOLD_BLOCK, tm.getMessage("admin.gui.loans")));
        inv.setItem(22, item(Material.BOOK, tm.getMessage("admin.gui.economy")));
        Bukkit.getPluginManager().registerEvents(this, plugin);
        p.openInventory(inv);
        return true;
    }

    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (inv == null || e.getInventory() == null || !e.getInventory().equals(inv)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;
        String name = it.getItemMeta().getDisplayName();
        if (name.contains(tm.getMessage("admin.gui.events"))) {
            p.performCommand("ecoxpert events status");
        } else if (name.contains(tm.getMessage("admin.gui.market"))) {
            p.performCommand("market stats");
        } else if (name.contains(tm.getMessage("admin.gui.loans"))) {
            p.performCommand("ecoxpert economy loans stats");
        } else if (name.contains(tm.getMessage("admin.gui.economy"))) {
            p.performCommand("ecoxpert economy status");
        }
    }
}

