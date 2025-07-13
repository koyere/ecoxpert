package me.koyere.ecoxpert.modules.market;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Professional market GUI system
 * 
 * Provides chest-based market interface with pagination,
 * item browsing, and interactive trading functionality.
 */
public class MarketGUI implements Listener {
    
    private final MarketManager marketManager;
    private final TranslationManager translationManager;
    private final Logger logger;
    
    // GUI tracking
    private final Map<UUID, MarketInventory> openGUIs = new ConcurrentHashMap<>();
    
    // GUI configuration
    private static final int ITEMS_PER_PAGE = 45; // 5 rows for items
    private static final int INVENTORY_SIZE = 54; // 6 rows total
    private static final String GUI_TITLE = "Market";
    
    // Special item slots
    private static final int PREV_PAGE_SLOT = 45;
    private static final int NEXT_PAGE_SLOT = 53;
    private static final int INFO_SLOT = 49;
    private static final int CLOSE_SLOT = 50;
    
    public MarketGUI(MarketManager marketManager, TranslationManager translationManager, Logger logger) {
        this.marketManager = marketManager;
        this.translationManager = translationManager;
        this.logger = logger;
    }
    
    /**
     * Open market GUI for player
     */
    public void openMarketGUI(Player player) {
        try {
            // Get market items
            marketManager.getAllItems().whenComplete((items, throwable) -> {
                if (throwable != null) {
                    logger.log(Level.SEVERE, "Error loading market items for GUI", throwable);
                    player.sendMessage(translationManager.getMessage("market.system-error"));
                    return;
                }
                
                // Filter active items
                List<MarketItem> activeItems = items.stream()
                    .filter(MarketItem::isActivelyTraded)
                    .sorted(Comparator.comparing(item -> item.getMaterial().name()))
                    .toList();
                
                // Create and open GUI
                Bukkit.getScheduler().runTask(player.getServer().getPluginManager().getPlugin("EcoXpert"), () -> {
                    MarketInventory marketInv = new MarketInventory(player, activeItems, 0);
                    openGUIs.put(player.getUniqueId(), marketInv);
                    
                    Inventory gui = createMarketPage(marketInv);
                    player.openInventory(gui);
                });
            });
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error opening market GUI", e);
            player.sendMessage(translationManager.getMessage("market.system-error"));
        }
    }
    
    /**
     * Create market page inventory
     */
    private Inventory createMarketPage(MarketInventory marketInv) {
        Inventory gui = Bukkit.createInventory(null, INVENTORY_SIZE, 
            translationManager.getMessage("market.gui.title"));
        
        List<MarketItem> items = marketInv.getItems();
        int page = marketInv.getCurrentPage();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());
        
        // Add market items
        for (int i = startIndex; i < endIndex; i++) {
            MarketItem item = items.get(i);
            ItemStack displayItem = createMarketItemStack(item);
            gui.setItem(i - startIndex, displayItem);
        }
        
        // Add navigation and control items
        addNavigationItems(gui, page, items.size());
        
        return gui;
    }
    
    /**
     * Create ItemStack for market item display
     */
    private ItemStack createMarketItemStack(MarketItem item) {
        ItemStack stack = new ItemStack(item.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        
        if (meta != null) {
            // Set display name
            String itemName = item.getMaterial().name().toLowerCase().replace('_', ' ');
            itemName = capitalizeWords(itemName);
            meta.setDisplayName("§e" + itemName);
            
            // Create lore with price information
            List<String> lore = new ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.item.header"));
            lore.add("");
            
            if (item.isBuyable()) {
                lore.add("§a" + translationManager.getMessage("market.gui.item.buy-price", 
                    formatPrice(item.getCurrentBuyPrice())));
            } else {
                lore.add("§c" + translationManager.getMessage("market.gui.item.not-buyable"));
            }
            
            if (item.isSellable()) {
                lore.add("§c" + translationManager.getMessage("market.gui.item.sell-price", 
                    formatPrice(item.getCurrentSellPrice())));
            } else {
                lore.add("§7" + translationManager.getMessage("market.gui.item.not-sellable"));
            }
            
            lore.add("");
            lore.add("§7" + translationManager.getMessage("market.gui.item.volume", 
                item.getTotalVolume()));
            
            // Add trend information if available
            marketManager.getItemTrend(item.getMaterial()).whenComplete((trend, throwable) -> {
                if (throwable == null && trend != null) {
                    String trendColor = getTrendColor(trend.getDirection());
                    lore.add("§7" + translationManager.getMessage("market.gui.item.trend", 
                        trendColor + trend.getDirection().getDisplayName()));
                }
            });
            
            lore.add("");
            lore.add("§e" + translationManager.getMessage("market.gui.item.click-buy"));
            lore.add("§e" + translationManager.getMessage("market.gui.item.shift-click-sell"));
            
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        
        return stack;
    }
    
    /**
     * Add navigation items to GUI
     */
    private void addNavigationItems(Inventory gui, int currentPage, int totalItems) {
        int totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a" + translationManager.getMessage("market.gui.prev-page"));
                meta.setLore(Arrays.asList(
                    "§7" + translationManager.getMessage("market.gui.current-page", currentPage + 1, totalPages)
                ));
                prevPage.setItemMeta(meta);
            }
            gui.setItem(PREV_PAGE_SLOT, prevPage);
        }
        
        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§a" + translationManager.getMessage("market.gui.next-page"));
                meta.setLore(Arrays.asList(
                    "§7" + translationManager.getMessage("market.gui.current-page", currentPage + 1, totalPages)
                ));
                nextPage.setItemMeta(meta);
            }
            gui.setItem(NEXT_PAGE_SLOT, nextPage);
        }
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6" + translationManager.getMessage("market.gui.info.title"));
            infoMeta.setLore(Arrays.asList(
                "§7" + translationManager.getMessage("market.gui.info.items", totalItems),
                "§7" + translationManager.getMessage("market.gui.info.page", currentPage + 1, totalPages),
                "",
                "§e" + translationManager.getMessage("market.gui.info.help1"),
                "§e" + translationManager.getMessage("market.gui.info.help2")
            ));
            info.setItemMeta(infoMeta);
        }
        gui.setItem(INFO_SLOT, info);
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c" + translationManager.getMessage("market.gui.close"));
            close.setItemMeta(closeMeta);
        }
        gui.setItem(CLOSE_SLOT, close);
    }
    
    /**
     * Handle GUI click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        MarketInventory marketInv = openGUIs.get(player.getUniqueId());
        if (marketInv == null) return;
        
        event.setCancelled(true); // Prevent item movement
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        int slot = event.getSlot();
        
        // Handle navigation clicks
        if (slot == PREV_PAGE_SLOT && marketInv.getCurrentPage() > 0) {
            marketInv.setCurrentPage(marketInv.getCurrentPage() - 1);
            updateGUI(player, marketInv);
            return;
        }
        
        if (slot == NEXT_PAGE_SLOT) {
            int totalPages = (int) Math.ceil((double) marketInv.getItems().size() / ITEMS_PER_PAGE);
            if (marketInv.getCurrentPage() < totalPages - 1) {
                marketInv.setCurrentPage(marketInv.getCurrentPage() + 1);
                updateGUI(player, marketInv);
            }
            return;
        }
        
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        
        // Handle item clicks (buy/sell)
        if (slot < ITEMS_PER_PAGE) {
            int itemIndex = (marketInv.getCurrentPage() * ITEMS_PER_PAGE) + slot;
            if (itemIndex < marketInv.getItems().size()) {
                MarketItem item = marketInv.getItems().get(itemIndex);
                handleItemClick(player, item, event.isShiftClick());
            }
        }
    }
    
    /**
     * Handle market item click (buy/sell)
     */
    private void handleItemClick(Player player, MarketItem item, boolean isShiftClick) {
        if (isShiftClick) {
            // Shift-click = sell
            if (!item.isSellable()) {
                player.sendMessage(translationManager.getMessage("market.item-not-sellable"));
                return;
            }
            
            // Sell all items of this type
            int quantity = marketManager.countItems(player, item.getMaterial());
            if (quantity <= 0) {
                player.sendMessage(translationManager.getMessage("market.no-items-to-sell", 
                    item.getMaterial().name().toLowerCase().replace('_', ' ')));
                return;
            }
            
            // Execute sell
            marketManager.sellItem(player, item.getMaterial(), quantity).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.log(Level.SEVERE, "Error in GUI sell transaction", throwable);
                    player.sendMessage(translationManager.getMessage("market.system-error"));
                    return;
                }
                
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            });
            
        } else {
            // Normal click = buy single item
            if (!item.isBuyable()) {
                player.sendMessage(translationManager.getMessage("market.item-not-buyable"));
                return;
            }
            
            // Execute buy
            marketManager.buyItem(player, item.getMaterial(), 1).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.log(Level.SEVERE, "Error in GUI buy transaction", throwable);
                    player.sendMessage(translationManager.getMessage("market.system-error"));
                    return;
                }
                
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            });
        }
        
        // Close GUI after transaction
        player.closeInventory();
    }
    
    /**
     * Handle GUI close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            openGUIs.remove(player.getUniqueId());
        }
    }
    
    /**
     * Update GUI display
     */
    private void updateGUI(Player player, MarketInventory marketInv) {
        Inventory newGui = createMarketPage(marketInv);
        player.getOpenInventory().getTopInventory().setContents(newGui.getContents());
    }
    
    /**
     * Get color for trend direction
     */
    private String getTrendColor(MarketTrend.TrendDirection direction) {
        return switch (direction) {
            case STRONG_UPWARD, UPWARD -> "§a";
            case STRONG_DOWNWARD, DOWNWARD -> "§c";
            case VOLATILE -> "§6";
            case STABLE -> "§7";
        };
    }
    
    /**
     * Format price for display
     */
    private String formatPrice(BigDecimal price) {
        return "$" + price.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
    
    /**
     * Capitalize words in string
     */
    private String capitalizeWords(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Close all open GUIs
     */
    public void closeAllGUIs() {
        for (UUID playerId : openGUIs.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.closeInventory();
            }
        }
        openGUIs.clear();
    }
    
    /**
     * Inner class for tracking market inventory state
     */
    private static class MarketInventory {
        private final Player player;
        private final List<MarketItem> items;
        private int currentPage;
        
        public MarketInventory(Player player, List<MarketItem> items, int currentPage) {
            this.player = player;
            this.items = items;
            this.currentPage = currentPage;
        }
        
        public Player getPlayer() { return player; }
        public List<MarketItem> getItems() { return items; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int page) { this.currentPage = page; }
    }
}