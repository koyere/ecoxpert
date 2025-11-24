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
    private final me.koyere.ecoxpert.core.platform.PlatformManager platformManager;
    private final me.koyere.ecoxpert.core.bedrock.BedrockFormsManager bedrockFormsManager;

    public BankGUI(EcoXpertPlugin plugin, BankManager bankManager, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.bankManager = bankManager;
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
     * Open Bedrock-native Form for bank main menu
     */
    private void openBedrockMainMenu(Player player) {
        // Get current bank balance
        bankManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.welcome", "Welcome to the Bank!")).append("\n\n");
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.balance", "Bank Balance")).append(": §e")
                   .append(economyManager.formatMoney(balance)).append("\n");

            economyManager.getBalance(player.getUniqueId()).thenAccept(walletBalance -> {
                content.append("§7").append(tm.getMessage("bank.gui.bedrock.wallet", "Wallet")).append(": §e")
                       .append(economyManager.formatMoney(walletBalance));

                java.util.List<String> buttons = java.util.Arrays.asList(
                    "§a" + tm.getMessage("bank.gui.bedrock.btn.deposit", "Deposit Money"),
                    "§c" + tm.getMessage("bank.gui.bedrock.btn.withdraw", "Withdraw Money"),
                    "§e" + tm.getMessage("bank.gui.bedrock.btn.balance", "Check Balance"),
                    "§7" + tm.getMessage("bank.gui.bedrock.btn.close", "Close")
                );

                bedrockFormsManager.sendSimpleForm(
                    player,
                    tm.getMessage("bank.gui.bedrock.title", "Bank"),
                    content.toString(),
                    buttons,
                    buttonIndex -> handleBankMenuSelection(player, buttonIndex)
                );
            });
        });
    }

    /**
     * Handle bank main menu selection
     */
    private void handleBankMenuSelection(Player player, int buttonIndex) {
        switch (buttonIndex) {
            case 0 -> openDepositForm(player);
            case 1 -> openWithdrawForm(player);
            case 2 -> {
                bankManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
                    player.sendMessage(tm.getMessage("prefix") +
                        tm.getMessage("bank.balance", economyManager.formatMoney(balance)));
                });
            }
            // case 3 is close - do nothing
        }
    }

    /**
     * Open deposit amount selection form
     */
    private void openDepositForm(Player player) {
        economyManager.getBalance(player.getUniqueId()).thenAccept(walletBalance -> {
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.deposit.info", "Select amount to deposit:")).append("\n\n");
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.wallet", "Wallet")).append(": §e")
                   .append(economyManager.formatMoney(walletBalance));

            java.util.List<String> buttons = java.util.Arrays.asList(
                "§a+$100",
                "§a+$500",
                "§a+$1,000",
                "§a+$5,000",
                "§a+$10,000",
                "§7" + tm.getMessage("bank.gui.bedrock.btn.back", "Back")
            );

            bedrockFormsManager.sendSimpleForm(
                player,
                tm.getMessage("bank.gui.bedrock.deposit.title", "Deposit"),
                content.toString(),
                buttons,
                buttonIndex -> {
                    if (buttonIndex < 5) {
                        BigDecimal amount = new BigDecimal(new int[]{100, 500, 1000, 5000, 10000}[buttonIndex]);
                        bankManager.deposit(player, amount).thenAccept(result -> {
                            sendResult(player, result);
                            if (result.isSuccess()) {
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

    /**
     * Open withdraw amount selection form
     */
    private void openWithdrawForm(Player player) {
        bankManager.getBalance(player.getUniqueId()).thenAccept(bankBalance -> {
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.withdraw.info", "Select amount to withdraw:")).append("\n\n");
            content.append("§7").append(tm.getMessage("bank.gui.bedrock.balance", "Bank Balance")).append(": §e")
                   .append(economyManager.formatMoney(bankBalance));

            java.util.List<String> buttons = java.util.Arrays.asList(
                "§c-$100",
                "§c-$500",
                "§c-$1,000",
                "§c-$5,000",
                "§c-$10,000",
                "§7" + tm.getMessage("bank.gui.bedrock.btn.back", "Back")
            );

            bedrockFormsManager.sendSimpleForm(
                player,
                tm.getMessage("bank.gui.bedrock.withdraw.title", "Withdraw"),
                content.toString(),
                buttons,
                buttonIndex -> {
                    if (buttonIndex < 5) {
                        BigDecimal amount = new BigDecimal(new int[]{100, 500, 1000, 5000, 10000}[buttonIndex]);
                        bankManager.withdraw(player, amount).thenAccept(result -> {
                            sendResult(player, result);
                            if (result.isSuccess()) {
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
        String title = tm.getMessage("bank.gui.title");
        Inventory inv = Bukkit.createInventory(null, 27, title);
        inv.setItem(11, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +100",
            new String[]{"§7" + tm.getMessage("bank.gui.deposit-lore1"), "§7" + tm.getMessage("bank.gui.deposit-lore2")}));
        inv.setItem(12, button(Material.LIME_WOOL, tm.getMessage("bank.gui.deposit") + " +1000",
            new String[]{"§7" + tm.getMessage("bank.gui.deposit-fast-lore1"), "§7" + tm.getMessage("bank.gui.deposit-fast-lore2")}));
        inv.setItem(13, button(Material.PAPER, tm.getMessage("bank.gui.balance"),
            new String[]{"§7" + tm.getMessage("bank.gui.balance-lore")}));
        inv.setItem(14, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -100",
            new String[]{"§7" + tm.getMessage("bank.gui.withdraw-lore1"), "§7" + tm.getMessage("bank.gui.withdraw-lore2")}));
        inv.setItem(15, button(Material.RED_WOOL, tm.getMessage("bank.gui.withdraw") + " -1000",
            new String[]{"§7" + tm.getMessage("bank.gui.withdraw-fast-lore1"), "§7" + tm.getMessage("bank.gui.withdraw-fast-lore2")}));
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
            bankManager.deposit(p, new BigDecimal("1000")).thenAccept(r -> sendResult(p, r));
        } else if (name.contains("+100")) {
            bankManager.deposit(p, new BigDecimal("100")).thenAccept(r -> sendResult(p, r));
        } else if (name.contains("-1000")) {
            bankManager.withdraw(p, new BigDecimal("1000")).thenAccept(r -> sendResult(p, r));
        } else if (name.contains("-100")) {
            bankManager.withdraw(p, new BigDecimal("100")).thenAccept(r -> sendResult(p, r));
        } else if (name.contains(tm.getMessage("bank.gui.balance"))) {
            bankManager.getBalance(p.getUniqueId()).thenAccept(b -> p.sendMessage(tm.getMessage("prefix") + tm.getMessage("bank.balance", economyManager.formatMoney(b))));
        }
    }

    private void sendResult(Player player, me.koyere.ecoxpert.modules.bank.BankOperationResult result) {
        if (result.isSuccess()) {
            // Choose message based on new balance availability
            if (result.getNewBalance() != null && result.getAmount() != null) {
                String key = result.getMessage().toLowerCase().contains("withdraw") ? "bank.withdraw.success" : "bank.deposit.success";
                player.sendMessage(tm.getMessage("prefix") + tm.getMessage(key,
                    economyManager.formatMoney(result.getAmount()),
                    economyManager.formatMoney(result.getNewBalance())));
            } else {
                player.sendMessage(tm.getMessage("prefix") + "§a" + result.getMessage());
            }
        } else {
            player.sendMessage(tm.getMessage("prefix") + result.getFormattedMessage());
        }
    }
}
