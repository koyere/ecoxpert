package me.koyere.ecoxpert.modules.baltop;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.gui.BaseGUI;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Baltop (Leaderboard) GUI
 *
 * Displays top player balances with pagination.
 * Supports both Java Edition (chest GUI) and Bedrock Edition (Geyser Forms).
 *
 * Features:
 * - 45 entries per page (5 rows)
 * - Player skulls with balance info
 * - Navigation buttons (prev/next page)
 * - Highlights current player
 * - Bedrock-native Forms for mobile/console
 */
public class BaltopGUI extends BaseGUI {

    private static final int ENTRIES_PER_PAGE = 45;
    private static final int INVENTORY_SIZE = 54; // 6 rows

    private final EconomyManager economyManager;
    private final TranslationManager tm;
    private final me.koyere.ecoxpert.core.platform.PlatformManager platformManager;
    private final me.koyere.ecoxpert.core.bedrock.BedrockFormsManager bedrockFormsManager;

    private int currentPage = 1;
    private List<EconomyManager.TopBalanceEntry> cachedEntries = new ArrayList<>();

    public BaltopGUI(EcoXpertPlugin plugin, EconomyManager economyManager, TranslationManager tm) {
        super(plugin);
        this.economyManager = economyManager;
        this.tm = tm;
        var registry = plugin.getServiceRegistry();
        this.platformManager = registry.getInstance(me.koyere.ecoxpert.core.platform.PlatformManager.class);
        this.bedrockFormsManager = registry.getInstance(me.koyere.ecoxpert.core.bedrock.BedrockFormsManager.class);
    }

    /**
     * Open GUI for specific page
     */
    public void open(Player player, int page) {
        this.currentPage = Math.max(1, page);

        // Use Geyser Forms for Bedrock players if available
        if (platformManager.isBedrockPlayer(player) && bedrockFormsManager != null && bedrockFormsManager.isFormsAvailable()) {
            openBedrockLeaderboard(player);
        } else {
            super.open(player); // Chest GUI fallback
        }
    }

    /**
     * Open Bedrock-native SimpleForm leaderboard
     */
    private void openBedrockLeaderboard(Player player) {
        // Calculate required entries
        int limit = currentPage * ENTRIES_PER_PAGE + 10; // Fetch extra to detect next page

        economyManager.getTopBalances(limit).thenAccept(entries -> {
            cachedEntries = entries;

            // Calculate pagination
            int totalEntries = entries.size();
            int totalPages = (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE);
            int startIndex = (currentPage - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalEntries);

            // Validate page
            if (startIndex >= totalEntries) {
                player.sendMessage(tm.getMessage("prefix") + tm.getMessage("baltop.page-out-of-range", currentPage, totalPages));
                return;
            }

            // Build content
            StringBuilder content = new StringBuilder();
            content.append("§7").append(tm.getMessage("baltop.gui.bedrock.subtitle", "Top Players - Page {0}/{1}", currentPage, totalPages)).append("\n\n");

            // Get player's own rank
            economyManager.getBalanceRank(player.getUniqueId()).thenAccept(playerRank -> {
                if (playerRank > 0) {
                    economyManager.getBalance(player.getUniqueId()).thenAccept(playerBalance -> {
                        content.append("§e").append(tm.getMessage("baltop.gui.bedrock.your-rank", "Your Rank: #{0}", playerRank))
                               .append(" §7- §a").append(economyManager.formatMoney(playerBalance)).append("\n\n");

                        // Build buttons list
                        List<String> buttons = new ArrayList<>();

                        // Add entry buttons (limit to first 20 for Forms UI clarity)
                        int displayLimit = Math.min(endIndex, startIndex + 20);
                        for (int i = startIndex; i < displayLimit; i++) {
                            EconomyManager.TopBalanceEntry entry = entries.get(i);
                            int rank = i + 1;
                            String playerName = getPlayerName(entry.playerUuid());
                            String balance = economyManager.formatMoney(entry.balance());

                            boolean isSelf = player.getUniqueId().equals(entry.playerUuid());
                            String prefix = isSelf ? "§e★ " : "§7";
                            String suffix = isSelf ? " §7(You)" : "";

                            buttons.add(prefix + "#" + rank + " " + playerName + suffix + " §8- §a" + balance);
                        }

                        // Navigation buttons
                        if (currentPage > 1) {
                            buttons.add("§a« " + tm.getMessage("baltop.gui.bedrock.btn.prev", "Previous Page"));
                        }
                        if (endIndex < totalEntries) {
                            buttons.add("§a» " + tm.getMessage("baltop.gui.bedrock.btn.next", "Next Page"));
                        }
                        buttons.add("§c" + tm.getMessage("baltop.gui.bedrock.btn.close", "Close"));

                        // Send form
                        bedrockFormsManager.sendSimpleForm(
                            player,
                            tm.getMessage("baltop.gui.bedrock.title", "Top Balances"),
                            content.toString(),
                            buttons,
                            buttonIndex -> handleBedrockSelection(player, buttonIndex, buttons.size(), displayLimit - startIndex)
                        );
                    });
                }
            });
        });
    }

    /**
     * Handle Bedrock form button selection
     */
    private void handleBedrockSelection(Player player, int buttonIndex, int totalButtons, int displayedEntries) {
        // Check if navigation buttons
        int navStartIndex = displayedEntries;

        if (buttonIndex >= navStartIndex) {
            int navIndex = buttonIndex - navStartIndex;

            // Determine which nav button based on page state
            boolean hasPrev = currentPage > 1;
            boolean hasNext = cachedEntries.size() > currentPage * ENTRIES_PER_PAGE;

            if (hasPrev && navIndex == 0) {
                // Previous page
                currentPage--;
                openBedrockLeaderboard(player);
                return;
            }

            if (hasNext) {
                int nextButtonIndex = hasPrev ? 1 : 0;
                if (navIndex == nextButtonIndex) {
                    // Next page
                    currentPage++;
                    openBedrockLeaderboard(player);
                    return;
                }
            }

            // Close button (last one)
            // Do nothing, form closes automatically
        }
        // Entry buttons don't do anything - informational only
    }

    /**
     * Create chest GUI inventory
     */
    @Override
    protected Inventory create(Player player) {
        String title = tm.getMessage("baltop.gui.title", "Top Balances - Page {0}", currentPage);
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Fetch leaderboard data
        int limit = currentPage * ENTRIES_PER_PAGE + 10;

        economyManager.getTopBalances(limit).thenAccept(entries -> {
            cachedEntries = entries;

            int totalEntries = entries.size();
            int startIndex = (currentPage - 1) * ENTRIES_PER_PAGE;
            int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalEntries);

            // Populate entries (slots 0-44)
            for (int i = startIndex; i < endIndex; i++) {
                EconomyManager.TopBalanceEntry entry = entries.get(i);
                int rank = i + 1;
                int slot = i - startIndex;

                if (slot >= 45) break; // Safety check

                boolean isSelf = player.getUniqueId().equals(entry.playerUuid());
                ItemStack item = createEntryItem(entry, rank, isSelf);
                inv.setItem(slot, item);
            }

            // Navigation buttons (row 6)
            if (currentPage > 1) {
                inv.setItem(48, createNavButton(Material.ARROW, tm.getMessage("baltop.gui.prev", "« Previous Page")));
            }

            if (endIndex < totalEntries) {
                inv.setItem(50, createNavButton(Material.ARROW, tm.getMessage("baltop.gui.next", "Next Page »")));
            }

            // Info button (center bottom)
            economyManager.getBalanceRank(player.getUniqueId()).thenAccept(playerRank -> {
                if (playerRank > 0) {
                    economyManager.getBalance(player.getUniqueId()).thenAccept(balance -> {
                        ItemStack infoItem = createInfoItem(playerRank, balance);
                        inv.setItem(49, infoItem);
                    });
                }
            });

            // Close button
            inv.setItem(53, createCloseButton());

        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Failed to load baltop GUI: " + throwable.getMessage());
            player.sendMessage(tm.getMessage("prefix") + tm.getMessage("error.database_error"));
            player.closeInventory();
            return null;
        });

        return inv;
    }

    /**
     * Handle inventory clicks
     */
    @Override
    protected void onClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        // Previous page
        if (slot == 48 && currentPage > 1) {
            currentPage--;
            refresh(player);
        }

        // Next page
        if (slot == 50 && cachedEntries.size() > currentPage * ENTRIES_PER_PAGE) {
            currentPage++;
            refresh(player);
        }

        // Close button
        if (slot == 53) {
            player.closeInventory();
        }
    }

    /**
     * Refresh GUI
     */
    private void refresh(Player player) {
        player.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> open(player, currentPage), 1L);
    }

    /**
     * Create leaderboard entry item
     */
    private ItemStack createEntryItem(EconomyManager.TopBalanceEntry entry, int rank, boolean isSelf) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        String playerName = getPlayerName(entry.playerUuid());
        String rankDisplay = isSelf ? "§e§l#" + rank : "§7#" + rank;
        String nameDisplay = isSelf ? "§e§l" + playerName + " §7(You)" : "§f" + playerName;

        meta.setDisplayName(rankDisplay + " " + nameDisplay);

        List<String> lore = new ArrayList<>();
        lore.add("§7" + tm.getMessage("baltop.gui.balance", "Balance") + ": §a" + economyManager.formatMoney(entry.balance()));

        if (isSelf) {
            lore.add("");
            lore.add("§e✦ " + tm.getMessage("baltop.gui.your-position", "This is your position!"));
        }

        meta.setLore(lore);

        // Set skull owner
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.playerUuid());
        meta.setOwningPlayer(offlinePlayer);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create navigation button
     */
    private ItemStack createNavButton(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§a" + name);
        meta.setLore(List.of("§7" + tm.getMessage("baltop.gui.nav-hint", "Click to navigate")));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create player info button
     */
    private ItemStack createInfoItem(int rank, java.math.BigDecimal balance) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + tm.getMessage("baltop.gui.your-stats", "Your Statistics"));

        List<String> lore = new ArrayList<>();
        lore.add("§7" + tm.getMessage("baltop.gui.your-rank", "Your Rank") + ": §e#" + rank);
        lore.add("§7" + tm.getMessage("baltop.gui.your-balance", "Your Balance") + ": §a" + economyManager.formatMoney(balance));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create close button
     */
    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c" + tm.getMessage("baltop.gui.close", "Close"));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Get player name with fallback
     */
    private String getPlayerName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : "§7Unknown";
    }
}
