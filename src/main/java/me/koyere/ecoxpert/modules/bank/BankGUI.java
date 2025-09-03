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
        inv.setItem(11, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +100",
            new String[]{"§7Depósita una cantidad fija en tu cuenta bancaria.", "§7Seguro, con límites diarios."}));
        inv.setItem(12, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +1000",
            new String[]{"§7Atajo para depósitos mayores.", "§7Afecta tus límites diarios."}));
        inv.setItem(13, button(Material.PAPER, tm.getMessage("bank.gui.balance"),
            new String[]{"§7Muestra el saldo actual de tu cuenta bancaria."}));
        inv.setItem(14, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -100",
            new String[]{"§7Retira una cantidad fija desde tu cuenta bancaria.", "§7Se transfiere a tu saldo general."}));
        inv.setItem(15, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -1000",
            new String[]{"§7Atajo para retiros mayores.", "§7Respeta límites y seguridad."}));
        return inv;
    }

    private ItemStack button(Material mat, String name, String[] lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        if (lore != null) meta.setLore(java.util.Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }
    private ItemStack button(Material mat, String name) { return button(mat, name, null); }

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
