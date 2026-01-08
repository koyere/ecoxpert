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
    private final me.koyere.ecoxpert.core.platform.PlatformManager platformManager;
    private final me.koyere.ecoxpert.core.bedrock.BedrockFormsManager bedrockFormsManager;

    public LoansGUI(EcoXpertPlugin plugin, LoanManager loanManager, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.loanManager = loanManager;
        this.economyManager = economyManager;
        this.tm = tm;
        var registry = plugin.getServiceRegistry();
        this.platformManager = registry.getInstance(me.koyere.ecoxpert.core.platform.PlatformManager.class);
        this.bedrockFormsManager = registry.getInstance(me.koyere.ecoxpert.core.bedrock.BedrockFormsManager.class);
    }

    /**
     * Open appropriate GUI based on player platform
     */
    @Override
    public void open(Player player) {
        // Use Geyser Forms for Bedrock players if available
        if (platformManager.isBedrockPlayer(player) && bedrockFormsManager != null && bedrockFormsManager.isFormsAvailable()) {
            openBedrockMainMenu(player);
        } else {
            super.open(player); // Chest GUI fallback
        }
    }

    /**
     * Open Bedrock-native Form for loans main menu
     */
    private void openBedrockMainMenu(Player player) {
        // Get loan status
        loanManager.getActiveLoan(player.getUniqueId()).thenAccept(loanOpt -> {
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("loans.gui.bedrock.welcome", "Loans Manager")).append("\n\n");

            if (loanOpt.isPresent()) {
                var loan = loanOpt.get();
                content.append("§7").append(tm.getMessage("loans.gui.bedrock.active", "Active Loan")).append(":\n");
                content.append("§7Outstanding: §c").append(economyManager.formatMoney(loan.getOutstanding())).append("\n");
                content.append("§7Principal: §e").append(economyManager.formatMoney(loan.getPrincipal())).append("\n");
                content.append("§7Rate: §e").append(loan.getInterestRate().multiply(new java.math.BigDecimal("100")).setScale(1)).append("%");
            } else {
                content.append("§7").append(tm.getMessage("loans.gui.bedrock.no-loan", "No active loans"));
            }

            java.util.List<String> buttons = new java.util.ArrayList<>();
            buttons.add("§a" + tm.getMessage("loans.gui.bedrock.btn.request", "Request Loan"));
            if (loanOpt.isPresent()) {
                buttons.add("§e" + tm.getMessage("loans.gui.bedrock.btn.pay", "Make Payment"));
            }
            buttons.add("§b" + tm.getMessage("loans.gui.bedrock.btn.status", "Loan Status"));
            buttons.add("§7" + tm.getMessage("loans.gui.bedrock.btn.close", "Close"));

            bedrockFormsManager.sendSimpleForm(
                player,
                tm.getMessage("loans.gui.bedrock.title", "Loans"),
                content.toString(),
                buttons,
                buttonIndex -> handleLoansMenuSelection(player, buttonIndex, loanOpt.isPresent())
            );
        });
    }

    /**
     * Handle loans main menu selection
     */
    private void handleLoansMenuSelection(Player player, int buttonIndex, boolean hasActiveLoan) {
        if (buttonIndex == 0) {
            // Request loan
            openRequestLoanForm(player);
        } else if (hasActiveLoan && buttonIndex == 1) {
            // Make payment
            openPaymentForm(player);
        } else if ((hasActiveLoan && buttonIndex == 2) || (!hasActiveLoan && buttonIndex == 1)) {
            // Show status
            loanManager.getActiveLoan(player.getUniqueId()).thenAccept(opt -> {
                if (opt.isEmpty()) {
                    player.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.no-active-loans"));
                } else {
                    var loan = opt.get();
                    player.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.status",
                        economyManager.formatMoney(loan.getOutstanding()),
                        economyManager.formatMoney(loan.getPrincipal()),
                        loan.getInterestRate().multiply(new java.math.BigDecimal("100")).setScale(1) + "%"));
                }
            });
        }
        // Close is last button - do nothing
    }

    /**
     * Open loan request form
     */
    private void openRequestLoanForm(Player player) {
        StringBuilder content = new StringBuilder();
        content.append("§7").append(tm.getMessage("loans.gui.bedrock.request.info", "Select loan amount:")).append("\n\n");
        content.append("§7").append(tm.getMessage("loans.gui.bedrock.request.note", "Interest rate and terms based on your credit score"));

        java.util.List<String> buttons = java.util.Arrays.asList(
            "§a$1,000",
            "§a$2,500",
            "§a$5,000",
            "§a$10,000",
            "§a$25,000",
            "§7" + tm.getMessage("loans.gui.bedrock.btn.back", "Back")
        );

        bedrockFormsManager.sendSimpleForm(
            player,
            tm.getMessage("loans.gui.bedrock.request.title", "Request Loan"),
            content.toString(),
            buttons,
            buttonIndex -> {
                if (buttonIndex < 5) {
                    java.math.BigDecimal amount = new java.math.BigDecimal(new int[]{1000, 2500, 5000, 10000, 25000}[buttonIndex]);
                    // Request loan with confirmation
                    loanManager.getOffer(player.getUniqueId(), amount).thenAccept(offer -> {
                        if (offer.approved()) {
                            bedrockFormsManager.sendModalForm(
                                player,
                                tm.getMessage("loans.gui.bedrock.confirm.title", "Confirm Loan"),
                                String.format("§7Amount: §e%s\n§7Rate: §e%.2f%%\n§7Term: §e%d days\n\n§aConfirm loan request?",
                                    economyManager.formatMoney(offer.amount()),
                                    offer.interestRate().multiply(new java.math.BigDecimal("100")).doubleValue(),
                                    offer.termDays()),
                                tm.getMessage("loans.gui.bedrock.confirm.yes", "Confirm"),
                                tm.getMessage("loans.gui.bedrock.confirm.no", "Cancel"),
                                confirmed -> {
                                    if (confirmed) {
                                        loanManager.requestLoanSmart(player.getUniqueId(), offer.amount()).thenAccept(ok -> {
                                            player.sendMessage(tm.getMessage("prefix") +
                                                (ok ? tm.getMessage("loans.loan-approved", economyManager.formatMoney(offer.amount()))
                                                    : tm.getMessage("loans.loan-denied", "")));
                                        });
                                    }
                                }
                            );
                        } else {
                            player.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.loan-denied", offer.reason()));
                        }
                    });
                } else if (buttonIndex == 5) {
                    openBedrockMainMenu(player);
                }
            }
        );
    }

    /**
     * Open payment form
     */
    private void openPaymentForm(Player player) {
        loanManager.getActiveLoan(player.getUniqueId()).thenAccept(loanOpt -> {
            if (loanOpt.isEmpty()) {
                player.sendMessage(tm.getMessage("prefix") + tm.getMessage("loans.no-active-loans"));
                return;
            }

            var loan = loanOpt.get();
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("loans.gui.bedrock.pay.info", "Select payment amount:")).append("\n\n");
            content.append("§7Outstanding: §c").append(economyManager.formatMoney(loan.getOutstanding()));

            java.util.List<String> buttons = java.util.Arrays.asList(
                "§e$500",
                "§e$1,000",
                "§e$2,500",
                "§e$5,000",
                "§a" + tm.getMessage("loans.gui.bedrock.pay.full", "Pay Full Amount"),
                "§7" + tm.getMessage("loans.gui.bedrock.btn.back", "Back")
            );

            bedrockFormsManager.sendSimpleForm(
                player,
                tm.getMessage("loans.gui.bedrock.pay.title", "Make Payment"),
                content.toString(),
                buttons,
                buttonIndex -> {
                    if (buttonIndex < 5) {
                        java.math.BigDecimal amount;
                        if (buttonIndex == 4) {
                            // Pay full
                            amount = loan.getOutstanding();
                        } else {
                            amount = new java.math.BigDecimal(new int[]{500, 1000, 2500, 5000}[buttonIndex]);
                        }

                        loanManager.payLoan(player.getUniqueId(), amount).thenAccept(ok -> {
                            player.sendMessage(tm.getMessage("prefix") +
                                (ok ? tm.getMessage("loans.payment-made", economyManager.formatMoney(amount))
                                    : tm.getMessage("loans.loan-denied", "")));
                            if (ok) {
                                // Re-open menu to show updated balance
                                plugin.getServer().getScheduler().runTaskLater(plugin,
                                    () -> openBedrockMainMenu(player), 20L);
                            }
                        });
                    } else if (buttonIndex == 5) {
                        openBedrockMainMenu(player);
                    }
                }
            );
        });
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
        }
        // Handle offer preview sub-GUI (check both translated and raw key)
        String offerTitle = tm.getMessage("loans.gui.offer-preview.title");
        String viewTitle = e.getView().getTitle();
        if (viewTitle.equals(offerTitle) || viewTitle.equals("loans.gui.offer-preview.title")) {
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
        }
        // Handle schedule sub-GUI (check both translated and raw key)
        String schedTitle = tm.getMessage("loans.gui.schedule.title");
        if (viewTitle.equals(schedTitle) || viewTitle.equals("loans.gui.schedule.title")) {
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
