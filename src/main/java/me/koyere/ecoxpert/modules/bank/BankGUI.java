package me.koyere.ecoxpert.modules.bank;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.gui.BaseGUI;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;

public class BankGUI extends BaseGUI {
    private final BankManager bankManager;
    private final EconomyManager economyManager;
    private final TranslationManager tm;

    public BankGUI(EcoXpertPlugin plugin, BankManager bankManager, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.bankManager = bankManager;
        this.economyManager = economyManager;
        this.tm = tm;
    }

    @Override
    protected Inventory create(Player player) {
        String title = tm.getMessage("bank.gui.title");
        Inventory inv = Bukkit.createInventory(null, 27, title);
        inv.setItem(11, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +100"));
        inv.setItem(12, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +1000"));
        inv.setItem(13, button(Material.PAPER, tm.getMessage("bank.gui.balance")));
        inv.setItem(14, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -100"));
        inv.setItem(15, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -1000"));
        return inv;
    }

    private ItemStack button(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    protected void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = it.getItemMeta().getDisplayName();
        if (name.contains("+1000")) {
            bankManager.deposit(p, new BigDecimal("1000")).thenAccept(r -> p.sendMessage(tm.getMessage("prefix") + r.getMessage()));
        } else if (name.contains("+100")) {
            bankManager.deposit(p, new BigDecimal("100")).thenAccept(r -> p.sendMessage(tm.getMessage("prefix") + r.getMessage()));
        } else if (name.contains("-1000")) {
            bankManager.withdraw(p, new BigDecimal("1000")).thenAccept(r -> p.sendMessage(tm.getMessage("prefix") + r.getMessage()));
        } else if (name.contains("-100")) {
            bankManager.withdraw(p, new BigDecimal("100")).thenAccept(r -> p.sendMessage(tm.getMessage("prefix") + r.getMessage()));
        } else if (name.contains(tm.getMessage("bank.gui.balance"))) {
            bankManager.getBalance(p.getUniqueId()).thenAccept(b -> p.sendMessage(tm.getMessage("prefix") + tm.getMessage("bank.balance", economyManager.formatMoney(b))));
        }
    }
}

