package me.koyere.ecoxpert.modules.loans;

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

public class LoansGUI extends BaseGUI {
    private final LoanManager loanManager;
    private final EconomyManager economyManager;
    private final TranslationManager tm;

    public LoansGUI(EcoXpertPlugin plugin, LoanManager loanManager, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.loanManager = loanManager;
        this.economyManager = economyManager;
        this.tm = tm;
    }

    @Override
    protected Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, tm.getMessage("loans.gui.title"));
        inv.setItem(10, button(Material.EMERALD, tm.getMessage("loans.gui.offer", "+1000")));
        inv.setItem(11, button(Material.EMERALD, tm.getMessage("loans.gui.offer", "+5000")));
        inv.setItem(13, button(Material.PAPER, tm.getMessage("loans.gui.status")));
        inv.setItem(15, button(Material.GOLD_INGOT, tm.getMessage("loans.gui.pay", "500")));
        inv.setItem(16, button(Material.GOLD_INGOT, tm.getMessage("loans.gui.pay", "1000")));
        inv.setItem(22, button(Material.BOOK, tm.getMessage("loans.gui.schedule")));
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
            loanManager.requestLoanSmart(p.getUniqueId(), new BigDecimal("1000")).thenAccept(ok ->
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("loans.loan-approved", economyManager.formatMoney(new BigDecimal("1000"))) : tm.getMessage("loans.loan-denied", ""))));
        } else if (name.contains("+5000")) {
            loanManager.requestLoanSmart(p.getUniqueId(), new BigDecimal("5000")).thenAccept(ok ->
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("loans.loan-approved", economyManager.formatMoney(new BigDecimal("5000"))) : tm.getMessage("loans.loan-denied", ""))));
        } else if (name.contains(tm.getMessage("loans.gui.status"))) {
            loanManager.getActiveLoan(p.getUniqueId()).thenAccept(opt -> {
                if (opt.isEmpty()) p.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.no-active-loans"));
                else {
                    var loan = opt.get();
                    p.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.status",
                        economyManager.formatMoney(loan.getOutstanding()),
                        economyManager.formatMoney(loan.getPrincipal()),
                        loan.getInterestRate().multiply(new BigDecimal("100")).setScale(1) + "%"));
                }
            });
        } else if (name.contains("500")) {
            loanManager.payLoan(p.getUniqueId(), new BigDecimal("500")).thenAccept(ok ->
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("loans.payment-made", economyManager.formatMoney(new BigDecimal("500"))) : tm.getMessage("loans.loan-denied", ""))));
        } else if (name.contains("1000")) {
            loanManager.payLoan(p.getUniqueId(), new BigDecimal("1000")).thenAccept(ok ->
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("loans.payment-made", economyManager.formatMoney(new BigDecimal("1000"))) : tm.getMessage("loans.loan-denied", ""))));
        } else if (name.contains(tm.getMessage("loans.gui.schedule"))) {
            loanManager.getSchedule(p.getUniqueId()).thenAccept(list -> {
                p.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.schedule.header"));
                int shown = 0;
                for (var sched : list) {
                    if (shown++ > 15) { p.sendMessage("§7..."); break; }
                    p.sendMessage(tm.getMessage("loans.schedule.item", sched.installmentNo(), sched.dueDate(), economyManager.formatMoney(sched.amountDue()), sched.status()));
                }
            });
        }
    }
}

