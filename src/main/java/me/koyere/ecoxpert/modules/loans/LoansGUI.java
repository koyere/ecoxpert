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
    private final java.util.Map<java.util.UUID, LoanOffer> pendingOffer = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, Integer> schedulePage = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<java.util.UUID, java.util.List<LoanPayment>> scheduleCache = new java.util.concurrent.ConcurrentHashMap<>();

    public LoansGUI(EcoXpertPlugin plugin, LoanManager loanManager, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.loanManager = loanManager;
        this.economyManager = economyManager;
        this.tm = tm;
    }

    @Override
    protected Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, tm.getMessage("loans.gui.title"));
        inv.setItem(10, button(Material.EMERALD, tm.getMessage("loans.gui.offer", "+1000"), new String[]{
            "§7" + tm.getMessage("loans.gui.offer-lore1"),
            "§7" + tm.getMessage("loans.gui.offer-lore2")
        }));
        inv.setItem(11, button(Material.EMERALD, tm.getMessage("loans.gui.offer", "+5000"), new String[]{
            "§7" + tm.getMessage("loans.gui.offer-large-lore1"),
            "§7" + tm.getMessage("loans.gui.offer-large-lore2")
        }));
        inv.setItem(13, button(Material.PAPER, tm.getMessage("loans.gui.status"), new String[]{
            "§7" + tm.getMessage("loans.gui.status-lore1"),
            "§7" + tm.getMessage("loans.gui.status-lore2")
        }));
        inv.setItem(15, button(Material.GOLD_INGOT, tm.getMessage("loans.gui.pay", "500"), new String[]{
            "§7" + tm.getMessage("loans.gui.pay-lore1"),
            "§7" + tm.getMessage("loans.gui.pay-lore2")
        }));
        inv.setItem(16, button(Material.GOLD_INGOT, tm.getMessage("loans.gui.pay", "1000"), new String[]{
            "§7" + tm.getMessage("loans.gui.pay-fast-lore1"),
            "§7" + tm.getMessage("loans.gui.pay-fast-lore2")
        }));
        inv.setItem(22, button(Material.BOOK, tm.getMessage("loans.gui.schedule.button"), new String[]{
            "§7" + tm.getMessage("loans.gui.schedule-lore1"),
            "§7" + tm.getMessage("loans.gui.schedule-lore2")
        }));
        return inv;
    }

    private ItemStack button(Material mat, String name) { return button(mat, name, null); }
    private ItemStack button(Material mat, String name, String[] lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        if (lore != null) meta.setLore(java.util.Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    @Override
    protected void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = it.getItemMeta().getDisplayName();
        // Offer preview flows
        if (name.contains("+1000")) {
            openOfferPreview(p, new BigDecimal("1000"));
        } else if (name.contains("+5000")) {
            openOfferPreview(p, new BigDecimal("5000"));
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
        } else if (name.contains(tm.getMessage("loans.gui.schedule.button"))) {
            openScheduleGUI(p, 0);
        } else if (e.getView().getTitle().equals(tm.getMessage("loans.gui.offer-preview.title"))) {
            e.setCancelled(true);
            if (name.contains(tm.getMessage("loans.gui.confirm"))) {
                LoanOffer offer = pendingOffer.get(p.getUniqueId());
                if (offer != null && offer.approved()) {
                    loanManager.requestLoanSmart(p.getUniqueId(), offer.amount()).thenAccept(ok -> {
                        p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("loans.loan-approved", economyManager.formatMoney(offer.amount())) : tm.getMessage("loans.loan-denied", "")));
                        pendingOffer.remove(p.getUniqueId());
                        p.closeInventory();
                    });
                } else {
                    p.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.loan-denied", ""));
                    p.closeInventory();
                }
            } else if (name.contains(tm.getMessage("loans.gui.cancel"))) {
                pendingOffer.remove(p.getUniqueId());
                p.closeInventory();
            }
        } else if (e.getView().getTitle().equals(tm.getMessage("loans.gui.schedule.title"))) {
            e.setCancelled(true);
            if (it.getType() == Material.ARROW) {
                if (name.contains("Prev")) {
                    int page = schedulePage.getOrDefault(p.getUniqueId(), 0);
                    if (page > 0) openScheduleGUI(p, page - 1);
                } else {
                    int page = schedulePage.getOrDefault(p.getUniqueId(), 0);
                    openScheduleGUI(p, page + 1);
                }
            }
        }
    }

    private void openOfferPreview(Player p, BigDecimal amount) {
        loanManager.getOffer(p.getUniqueId(), amount).thenAccept(offer -> {
            pendingOffer.put(p.getUniqueId(), offer);
            Inventory inv = Bukkit.createInventory(null, 27, tm.getMessage("loans.gui.offer-preview.title"));
            ItemStack info = new ItemStack(Material.BOOK);
            ItemMeta meta = info.getItemMeta();
            meta.setDisplayName("§e" + tm.getMessage("loans.offer.header"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("loans.offer.details",
                economyManager.formatMoney(offer.amount()),
                offer.interestRate().multiply(new BigDecimal("100")).setScale(2) + "%",
                offer.termDays(), offer.score()));
            lore.add(" ");
            lore.add(offer.approved() ? "§a" + tm.getMessage("loans.gui.offer-preview.approved") : "§c" + tm.getMessage("loans.gui.offer-preview.denied", offer.reason()));
            meta.setLore(lore);
            info.setItemMeta(meta);
            inv.setItem(11, info);

            if (offer.approved()) {
                inv.setItem(15, button(Material.LIME_WOOL, tm.getMessage("loans.gui.confirm")));
            }
            inv.setItem(22, button(Material.BARRIER, tm.getMessage("loans.gui.cancel")));
            this.inventory = inv;
            p.openInventory(inv);
        });
    }

    private void openScheduleGUI(Player p, int page) {
        loanManager.getSchedule(p.getUniqueId()).thenAccept(list -> {
            scheduleCache.put(p.getUniqueId(), list);
            schedulePage.put(p.getUniqueId(), page);
            int perPage = 45;
            int totalPages = Math.max(1, (int) Math.ceil(list.size() / (double) perPage));
            int clamped = Math.max(0, Math.min(page, totalPages - 1));
            Inventory inv = Bukkit.createInventory(null, 54, tm.getMessage("loans.gui.schedule.title"));
            int start = clamped * perPage;
            int end = Math.min(start + perPage, list.size());
            int slot = 0;
            for (int i = start; i < end; i++) {
                LoanPayment s = list.get(i);
                ItemStack paper = new ItemStack(Material.PAPER);
                ItemMeta meta = paper.getItemMeta();
                meta.setDisplayName("§e#" + s.installmentNo() + " - " + s.status());
                meta.setLore(java.util.Arrays.asList(
                    "§7" + tm.getMessage("loans.gui.schedule-item.due", s.dueDate()),
                    "§7" + tm.getMessage("loans.gui.schedule-item.amount", economyManager.formatMoney(s.amountDue())),
                    "§7" + tm.getMessage("loans.gui.schedule-item.paid", economyManager.formatMoney(s.paidAmount()))
                ));
                paper.setItemMeta(meta);
                inv.setItem(slot++, paper);
            }
            // Nav arrows
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta pm = prev.getItemMeta();
            pm.setDisplayName("§a" + tm.getMessage("loans.gui.schedule-item.prev"));
            prev.setItemMeta(pm);
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§a" + tm.getMessage("loans.gui.schedule-item.next"));
            next.setItemMeta(nm);
            inv.setItem(45, prev);
            inv.setItem(53, next);
            this.inventory = inv;
            p.openInventory(inv);
        });
    }
}
