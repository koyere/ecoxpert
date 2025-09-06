package me.koyere.ecoxpert.modules.market;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.core.config.ConfigManager;
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
    private final me.koyere.ecoxpert.modules.market.orders.MarketOrderService orderService;
    private final TranslationManager translationManager;
    private final ConfigManager configManager;
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
    private static final int CATEGORY_SLOT = 46;
    private static final int LETTER_SLOT = 47;
    private static final int SELL_HAND_SLOT = 51;
    private static final int ORDERS_SLOT = 52;
    private static final int CLEAR_FILTERS_SLOT = 48;

    // Category cache
    private final java.util.List<String> categoryOrder = new java.util.ArrayList<>();
    private final java.util.Map<String, java.util.Set<Material>> categories = new java.util.HashMap<>();
    
    public MarketGUI(MarketManager marketManager, TranslationManager translationManager, Logger logger) {
        this.marketManager = marketManager;
        this.translationManager = translationManager;
        this.logger = logger;
        this.configManager = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(ConfigManager.class);
        this.orderService = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.market.orders.MarketOrderService.class);
        loadCategories();
    }

    private void loadCategories() {
        try {
            var marketCfg = configManager.getModuleConfig("market");
            var section = marketCfg.getConfigurationSection("categories");
            categoryOrder.clear();
            categories.clear();
            categoryOrder.add("ALL");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    java.util.List<String> mats = marketCfg.getStringList("categories." + key + ".materials");
                    java.util.Set<Material> set = new java.util.HashSet<>();
                    for (String m : mats) {
                        try { set.add(Material.valueOf(m.toUpperCase())); } catch (Exception ignored) {}
                    }
                    categories.put(key.toUpperCase(), set);
                    categoryOrder.add(key.toUpperCase());
                }
            }
        } catch (Exception ignored) {}
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
        // Apply filters
        java.util.List<MarketItem> filtered = applyFilters(items, marketInv);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filtered.size());
        
        // Add market items
        for (int i = startIndex; i < endIndex; i++) {
            MarketItem item = filtered.get(i);
            ItemStack displayItem = createMarketItemStack(item);
            gui.setItem(i - startIndex, displayItem);
        }
        
        // Add navigation and control items
        addNavigationItems(gui, page, filtered.size(), marketInv);

        return gui;
    }

    private java.util.List<MarketItem> applyFilters(java.util.List<MarketItem> items, MarketInventory inv) {
        String cat = inv.getSelectedCategory();
        Character letter = inv.getFilterLetter();
        java.util.stream.Stream<MarketItem> stream = items.stream()
            .filter(mi -> {
                if (cat != null && !"ALL".equals(cat)) {
                    java.util.Set<Material> set = categories.getOrDefault(cat, java.util.Collections.emptySet());
                    if (!set.contains(mi.getMaterial())) return false;
                }
                if (letter != null) {
                    return mi.getMaterial().name().startsWith(String.valueOf(letter));
                }
                return true;
            });
        java.util.List<MarketItem> list = stream.toList();
        // Apply sorting
        list.sort(switch (inv.getSortMode()) {
            case NAME -> java.util.Comparator.comparing(mi -> mi.getMaterial().name());
            case BUY_ASC -> java.util.Comparator.comparing(MarketItem::getCurrentBuyPrice, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case SELL_DESC -> java.util.Comparator.comparing(MarketItem::getCurrentSellPrice, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed();
            case VOLUME_DESC -> java.util.Comparator.comparing(MarketItem::getTotalVolume, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed();
        });
        return list;
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
                // Effective price for this player (role/category/events)
                try {
                    double f = computeEffectiveFactorForItem(getCurrentViewer(), item.getMaterial(), true);
                    java.math.BigDecimal eff = item.getCurrentBuyPrice().multiply(java.math.BigDecimal.valueOf(f))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                    lore.add("§7" + translationManager.getMessage("market.gui.item.effective-buy", formatPrice(eff)));
                } catch (Exception ignored) {}
            } else {
                lore.add("§c" + translationManager.getMessage("market.gui.item.not-buyable"));
            }
            
            if (item.isSellable()) {
                lore.add("§c" + translationManager.getMessage("market.gui.item.sell-price", 
                    formatPrice(item.getCurrentSellPrice())));
                try {
                    double f = computeEffectiveFactorForItem(getCurrentViewer(), item.getMaterial(), false);
                    java.math.BigDecimal eff = item.getCurrentSellPrice().multiply(java.math.BigDecimal.valueOf(f))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                    lore.add("§7" + translationManager.getMessage("market.gui.item.effective-sell", formatPrice(eff)));
                } catch (Exception ignored) {}
                try {
                    var cfg = configManager.getModuleConfig("market");
                    double minFrac = cfg.getDouble("orders.listing.price_bounds_min_base_fraction", 0.10);
                    double maxFrac = cfg.getDouble("orders.listing.price_bounds_max_base_fraction", 10.0);
                    java.math.BigDecimal min = item.getCurrentSellPrice().multiply(new java.math.BigDecimal(String.valueOf(minFrac)))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal max = item.getCurrentSellPrice().multiply(new java.math.BigDecimal(String.valueOf(maxFrac)))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                    lore.add("§7" + translationManager.getMessage("market.gui.info.range", formatPrice(min), formatPrice(max)));
                    lore.add("§7" + translationManager.getMessage("market.gui.item.list-hint"));
                } catch (Exception ignored) {}
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

    private Player getCurrentViewer() {
        // Best-effort: this GUI is per-player; when building items we are in the context of an open GUI.
        // We retrieve any online viewer from openGUIs map.
        if (!openGUIs.isEmpty()) {
            UUID any = openGUIs.keySet().iterator().next();
            Player p = Bukkit.getPlayer(any);
            if (p != null) return p;
        }
        return null;
    }

    private double computeEffectiveFactorForItem(Player player, Material material, boolean isBuy) {
        if (player == null) return 1.0;
        try {
            var sr = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class).getServiceRegistry();
            var pm = sr.getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            var roleOpt = pm.getRole(player.getUniqueId()).join();
            if (roleOpt.isEmpty()) return 1.0;
            String role = roleOpt.get().name().toLowerCase();
            var profCfg = sr.getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class).getModuleConfig("professions");
            int level = pm.getLevel(player.getUniqueId()).join();
            int maxLevel = profCfg.getInt("max_level", 5);
            level = Math.max(1, Math.min(level, maxLevel));
            double base = profCfg.getDouble("roles." + role + "." + (isBuy ? "buy_factor" : "sell_factor"), 1.0);
            double per = profCfg.getDouble("roles." + role + "." + (isBuy ? "buy_bonus_per_level" : "sell_bonus_per_level"), 0.0);
            double v = isBuy ? (base * (1.0 - per * (level - 1))) : (base * (1.0 + per * (level - 1)));
            // Categories: multiply all categories that include the material
            for (var e : categories.entrySet()) {
                if (e.getValue().contains(material)) {
                    String ck = "roles." + role + ".category_bonuses." + e.getKey().toLowerCase() + "." + (isBuy ? "buy_factor" : "sell_factor");
                    v *= profCfg.getDouble(ck, 1.0);
                }
            }
            // Events: multiply for each active event
            var events = sr.getInstance(me.koyere.ecoxpert.modules.events.EconomicEventEngine.class);
            if (events != null) {
                for (var ev : events.getActiveEvents().values()) {
                    String ek = "roles." + role + ".event_bonuses." + ev.getType().name() + "." + (isBuy ? "buy_factor" : "sell_factor");
                    v *= profCfg.getDouble(ek, 1.0);
                }
            }
            if (v < 0.5) v = 0.5; if (v > 1.5) v = 1.5;
            return v;
        } catch (Exception e) {
            return 1.0;
        }
    }
    
    /**
     * Add navigation items to GUI
     */
    private void addNavigationItems(Inventory gui, int currentPage, int totalItems, MarketInventory inv) {
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
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.info.items", totalItems));
            lore.add("§7" + translationManager.getMessage("market.gui.info.page", currentPage + 1, totalPages));

            // Contextual role bonuses
            try {
                var player = inv.getPlayer();
                var ctx = computeContextualBonuses(player, inv.getSelectedCategory());
                // Role total effect (includes level)
                lore.add("" );
                lore.add("§b" + translationManager.getMessage("market.gui.info.role-bonus",
                    formatPercent(ctx.roleBuy), formatPercent(ctx.roleSell)));
                // Category (if not ALL)
                if (inv.getSelectedCategory() != null && !"ALL".equals(inv.getSelectedCategory())) {
                    lore.add("§9" + translationManager.getMessage("market.gui.info.category-bonus",
                        inv.getSelectedCategory(), formatPercent(ctx.catBuy), formatPercent(ctx.catSell)));
                }
                // Active events aggregated
                if (ctx.eventBuy != 1.0 || ctx.eventSell != 1.0) {
                    lore.add("§d" + translationManager.getMessage("market.gui.info.event-bonus",
                        formatPercent(ctx.eventBuy), formatPercent(ctx.eventSell)));
                }
            } catch (Exception ignored) {}

            // Sorting info and hint
            lore.add("");
            lore.add("§7" + translationManager.getMessage("market.gui.info.sort-label", inv.getSortMode().display(translationManager)));
            lore.add("§7" + translationManager.getMessage("market.gui.info.sort-help"));

            lore.add("");
            lore.add("§e" + translationManager.getMessage("market.gui.info.help1"));
            lore.add("§e" + translationManager.getMessage("market.gui.info.help2"));
            lore.add("§e" + translationManager.getMessage("market.gui.info.help3"));
            infoMeta.setLore(lore);
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

        // Category filter button
        ItemStack catBtn = new ItemStack(Material.CHEST);
        ItemMeta catMeta = catBtn.getItemMeta();
        if (catMeta != null) {
            String cat = inv.getSelectedCategory();
            if (cat == null) cat = "ALL";
            catMeta.setDisplayName("§b" + translationManager.getMessage("market.gui.category-label", cat));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.category-help"));
            catMeta.setLore(lore);
            catBtn.setItemMeta(catMeta);
        }
        gui.setItem(CATEGORY_SLOT, catBtn);

        // Letter filter button
        ItemStack letterBtn = new ItemStack(Material.NAME_TAG);
        ItemMeta letterMeta = letterBtn.getItemMeta();
        if (letterMeta != null) {
            String label = inv.getFilterLetter() == null ? "ALL" : inv.getFilterLetter().toString();
            letterMeta.setDisplayName("§b" + translationManager.getMessage("market.gui.letter-label", label));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.letter-help"));
            letterMeta.setLore(lore);
            letterBtn.setItemMeta(letterMeta);
        }
        gui.setItem(LETTER_SLOT, letterBtn);

        // Clear filters (category + letter)
        ItemStack clearFilters = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clearFilters.getItemMeta();
        if (clearMeta != null) {
            clearMeta.setDisplayName("§c" + translationManager.getMessage("market.gui.clear-filters-label"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.clear-filters-help"));
            clearMeta.setLore(lore);
            clearFilters.setItemMeta(clearMeta);
        }
        gui.setItem(CLEAR_FILTERS_SLOT, clearFilters);

        // Sell item in hand
        ItemStack sellHand = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sellMeta = sellHand.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName("§6" + translationManager.getMessage("market.gui.sell-hand-label"));
            sellMeta.setLore(java.util.Arrays.asList(
                "§7" + translationManager.getMessage("market.gui.sell-hand.help1"),
                "§7" + translationManager.getMessage("market.gui.sell-hand.help2")
            ));
            sellHand.setItemMeta(sellMeta);
        }
        gui.setItem(SELL_HAND_SLOT, sellHand);

        // Open Orders GUI button
        ItemStack ordersBtn = new ItemStack(Material.PAPER);
        ItemMeta ordersMeta = ordersBtn.getItemMeta();
        if (ordersMeta != null) {
            ordersMeta.setDisplayName("§6" + translationManager.getMessage("market.gui.orders-button"));
            ordersMeta.setLore(java.util.Arrays.asList(
                "§7" + translationManager.getMessage("market.gui.orders-button-help")
            ));
            ordersBtn.setItemMeta(ordersMeta);
        }
        gui.setItem(ORDERS_SLOT, ordersBtn);
    }

    private static class CtxFactors { double roleBuy=1, roleSell=1, catBuy=1, catSell=1, eventBuy=1, eventSell=1; }

    private CtxFactors computeContextualBonuses(Player player, String selectedCategory) {
        CtxFactors f = new CtxFactors();
        try {
            var sr = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class).getServiceRegistry();
            var pm = sr.getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            var roleOpt = pm.getRole(player.getUniqueId()).join();
            if (roleOpt.isEmpty()) return f;
            String role = roleOpt.get().name().toLowerCase();
            int level = pm.getLevel(player.getUniqueId()).join();
            var profCfg = configManager.getModuleConfig("professions");
            int maxLevel = profCfg.getInt("max_level", 5);
            level = Math.max(1, Math.min(level, maxLevel));

            double baseBuy = profCfg.getDouble("roles." + role + ".buy_factor", 1.0);
            double baseSell = profCfg.getDouble("roles." + role + ".sell_factor", 1.0);
            double perBuy = profCfg.getDouble("roles." + role + ".buy_bonus_per_level", 0.0);
            double perSell = profCfg.getDouble("roles." + role + ".sell_bonus_per_level", 0.0);
            f.roleBuy = clamp( baseBuy * (1.0 - perBuy * (level - 1)) );
            f.roleSell = clamp( baseSell * (1.0 + perSell * (level - 1)) );

            if (selectedCategory != null && !"ALL".equals(selectedCategory)) {
                f.catBuy = profCfg.getDouble("roles." + role + ".category_bonuses." + selectedCategory.toLowerCase() + ".buy_factor", 1.0);
                f.catSell = profCfg.getDouble("roles." + role + ".category_bonuses." + selectedCategory.toLowerCase() + ".sell_factor", 1.0);
            }

            var events = sr.getInstance(me.koyere.ecoxpert.modules.events.EconomicEventEngine.class);
            if (events != null) {
                double eb = 1.0, es = 1.0;
                for (var ev : events.getActiveEvents().values()) {
                    eb *= profCfg.getDouble("roles." + role + ".event_bonuses." + ev.getType().name() + ".buy_factor", 1.0);
                    es *= profCfg.getDouble("roles." + role + ".event_bonuses." + ev.getType().name() + ".sell_factor", 1.0);
                }
                f.eventBuy = clamp(eb); f.eventSell = clamp(es);
            }
        } catch (Exception ignored) {}
        return f;
    }

    private String formatPercent(double factor) {
        double pct = (factor - 1.0) * 100.0;
        String sign = pct > 0 ? "+" : ""; // minus included by default
        return String.format(java.util.Locale.US, "%s%.1f%%", sign, pct);
    }

    private double clamp(double v) { if (v < 0.5) return 0.5; if (v > 1.5) return 1.5; return v; }
    
    /**
     * Handle GUI click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Handle List GUI interactions
        String listTitle = translationManager.getMessage("market.gui.list.title");
        if (event.getView().getTitle().equals(listTitle)) {
            event.setCancelled(true);
            ListState st = listing.get(player.getUniqueId());
            if (st == null) return;
            int slot = event.getRawSlot();
            if (slot == 22) { // cancel
                listing.remove(player.getUniqueId());
                player.closeInventory();
                return;
            }
            if (slot == 16) { // confirm
                int have = marketManager.countItems(player, st.material);
                int qty = Math.max(1, Math.min(st.qty, have));
                orderService.createListing(player, st.material, qty, st.unitPrice, st.hours)
                    .thenAccept(msg -> player.sendMessage(translationManager.getMessage("prefix") + msg));
                listing.remove(player.getUniqueId());
                player.closeInventory();
                return;
            }
            // price adjust (config-driven slots)
            if (st.priceSlots.containsKey(slot)) {
                Integer pctVal = st.priceSlots.get(slot);
                if (pctVal != null && pctVal == Integer.MIN_VALUE) {
                    st.unitPrice = st.basePrice;
                } else {
                    int pct = pctVal != null ? pctVal : 0;
                    java.math.BigDecimal factor = java.math.BigDecimal.valueOf(1.0 + (pct / 100.0));
                    java.math.BigDecimal newPrice = st.unitPrice.multiply(factor).setScale(2, java.math.RoundingMode.HALF_UP);
                    double minFrac = 0.10, maxFrac = 10.0;
                    try {
                        var cfg = configManager.getModuleConfig("market");
                        minFrac = cfg.getDouble("orders.listing.price_bounds_min_base_fraction", 0.10);
                        maxFrac = cfg.getDouble("orders.listing.price_bounds_max_base_fraction", 10.0);
                    } catch (Exception ignored) {}
                    java.math.BigDecimal min = st.basePrice.multiply(new java.math.BigDecimal(String.valueOf(minFrac))).setScale(2, java.math.RoundingMode.HALF_UP);
                    java.math.BigDecimal max = st.basePrice.multiply(new java.math.BigDecimal(String.valueOf(maxFrac))).setScale(2, java.math.RoundingMode.HALF_UP);
                    if (newPrice.compareTo(new java.math.BigDecimal("0.01")) < 0) newPrice = new java.math.BigDecimal("0.01");
                    if (newPrice.compareTo(min) < 0) newPrice = min;
                    if (newPrice.compareTo(max) > 0) newPrice = max;
                    st.unitPrice = newPrice;
                }
                event.getView().getTopInventory().setContents(buildListInventory(player, st).getContents());
                return;
            }
            // price adjust absolute (config-driven slots)
            if (st.absSlots.containsKey(slot)) {
                java.math.BigDecimal delta = st.absSlots.get(slot);
                java.math.BigDecimal newPrice = st.unitPrice.add(delta).setScale(2, java.math.RoundingMode.HALF_UP);
                double minFrac = 0.10, maxFrac = 10.0;
                try {
                    var cfg = configManager.getModuleConfig("market");
                    minFrac = cfg.getDouble("orders.listing.price_bounds_min_base_fraction", 0.10);
                    maxFrac = cfg.getDouble("orders.listing.price_bounds_max_base_fraction", 10.0);
                } catch (Exception ignored) {}
                java.math.BigDecimal min = st.basePrice.multiply(new java.math.BigDecimal(String.valueOf(minFrac))).setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal max = st.basePrice.multiply(new java.math.BigDecimal(String.valueOf(maxFrac))).setScale(2, java.math.RoundingMode.HALF_UP);
                if (newPrice.compareTo(new java.math.BigDecimal("0.01")) < 0) newPrice = new java.math.BigDecimal("0.01");
                if (newPrice.compareTo(min) < 0) newPrice = min;
                if (newPrice.compareTo(max) > 0) newPrice = max;
                st.unitPrice = newPrice;
                event.getView().getTopInventory().setContents(buildListInventory(player, st).getContents());
                return;
            }
            // qty selections
            if (slot >= 10 && slot <= 14) {
                int chosen = switch (slot) { case 10 -> 1; case 11 -> 8; case 12 -> 16; case 13 -> 32; case 14 -> 64; default -> -1; };
                if (chosen > 0) st.qty = chosen;
                event.getView().getTopInventory().setContents(buildListInventory(player, st).getContents());
                return;
            }
            // duration selections (config-driven slots)
            if (st.durationSlots.containsKey(slot)) { st.hours = st.durationSlots.get(slot); event.getView().getTopInventory().setContents(buildListInventory(player, st).getContents()); return; }
            return;
        }

        MarketInventory marketInv = openGUIs.get(player.getUniqueId());
        if (marketInv == null) return;

        // Handle Sell Hand sub-GUI
        String sellTitle = translationManager.getMessage("market.gui.sell-hand.title");
        if (event.getView().getTitle().equals(sellTitle)) {
            event.setCancelled(true);
            int slot = event.getSlot();
            int idx = switch (slot) {
                case 10 -> 0;
                case 11 -> 1;
                case 12 -> 2;
                case 13 -> 3;
                default -> -1;
            };
            if (idx == -1) return;

            java.util.List<BigDecimal> amounts = java.util.Arrays.asList(
                new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("1000"), new BigDecimal("5000")
            );
            BigDecimal target = amounts.get(idx);
            ItemStack inHand = player.getInventory().getItemInMainHand();
            Material mat = inHand.getType();
            // Validate sellable and compute quantity
            marketManager.getItem(mat).whenComplete((opt, thr) -> {
                if (thr != null || opt.isEmpty() || !opt.get().isSellable()) {
                    player.sendMessage(translationManager.getMessage("market.item-not-sellable"));
                    return;
                }
                marketManager.getSellPrice(mat).whenComplete((price, t2) -> {
                    if (t2 != null || price == null || price.signum() <= 0) {
                        player.sendMessage(translationManager.getMessage("market.system-error"));
                        return;
                    }
                    int qty = target.divide(price, 0, java.math.RoundingMode.DOWN).intValue();
                    if (qty < 1) qty = 1;
                    int inHandCount = inHand.getAmount();
                    qty = Math.min(qty, inHandCount);
                    if (qty <= 0) {
                        player.sendMessage(translationManager.getMessage("market.no-items-to-sell", mat.name().toLowerCase()));
                        return;
                    }
                    final int sellQty = qty;
                    marketManager.sellItem(player, mat, sellQty).whenComplete((result, t3) -> {
                        if (t3 != null) {
                            logger.log(Level.SEVERE, "Error in GUI sell-hand transaction", t3);
                            player.sendMessage(translationManager.getMessage("market.system-error"));
                            return;
                        }
                        player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
                        Bukkit.getScheduler().runTask(player.getServer().getPluginManager().getPlugin("EcoXpert"), player::closeInventory);
                    });
                });
            });
            return;
        }

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

        if (slot == CATEGORY_SLOT) {
            // Cycle category
            String current = marketInv.getSelectedCategory();
            int idx = categoryOrder.indexOf(current == null ? "ALL" : current);
            idx = (idx + 1) % categoryOrder.size();
            marketInv.setSelectedCategory(categoryOrder.get(idx));
            marketInv.setCurrentPage(0);
            updateGUI(player, marketInv);
            return;
        }

        if (slot == LETTER_SLOT) {
            // Cycle letter A-Z then ALL
            Character c = marketInv.getFilterLetter();
            if (c == null) {
                marketInv.setFilterLetter('A');
            } else if (c == 'Z') {
                marketInv.setFilterLetter(null);
            } else {
                marketInv.setFilterLetter((char) (c + 1));
            }
            marketInv.setCurrentPage(0);
            updateGUI(player, marketInv);
            return;
        }

        if (slot == SELL_HAND_SLOT) {
            openSellHandGUI(player);
            return;
        }

        if (slot == ORDERS_SLOT) {
            player.performCommand("market orders");
            return;
        }

        if (slot == CLEAR_FILTERS_SLOT) {
            marketInv.setSelectedCategory("ALL");
            marketInv.setFilterLetter(null);
            marketInv.setCurrentPage(0);
            updateGUI(player, marketInv);
            return;
        }
        
        // Handle item clicks (buy/sell/list)
        if (slot < ITEMS_PER_PAGE) {
            int itemIndex = (marketInv.getCurrentPage() * ITEMS_PER_PAGE) + slot;
            if (itemIndex < marketInv.getItems().size()) {
                MarketItem item = marketInv.getItems().get(itemIndex);
                if (event.isRightClick()) {
                    // Right-click: open listing GUI if permission
                    if (!player.hasPermission("ecoxpert.market.list")) {
                        player.sendMessage(translationManager.getMessage("no-permission"));
                        return;
                    }
                    openListGUI(player, item);
                } else {
                    handleItemClick(player, item, event.isShiftClick());
                }
            }
        }

        // Info click cycles sorting
        if (slot == INFO_SLOT) {
            marketInv.cycleSort();
            updateGUI(player, marketInv);
            return;
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

    // === List GUI (Order Book listing) ===

    private final java.util.Map<java.util.UUID, ListState> listing = new java.util.concurrent.ConcurrentHashMap<>();

    private static class ListState {
        final Material material; int qty; int hours; java.math.BigDecimal unitPrice; java.math.BigDecimal basePrice;
        final java.util.Map<Integer,Integer> priceSlots = new java.util.HashMap<>();
        final java.util.Map<Integer,Integer> durationSlots = new java.util.HashMap<>();
        final java.util.Map<Integer,java.math.BigDecimal> absSlots = new java.util.HashMap<>();
        ListState(Material m, int q, int h, java.math.BigDecimal p) { material=m; qty=q; hours=h; unitPrice=p; basePrice=p; }
    }

    private void openListGUI(Player player, MarketItem item) {
        int have = marketManager.countItems(player, item.getMaterial());
        if (have <= 0) {
            player.sendMessage(translationManager.getMessage("market.no-items-to-sell", item.getMaterial().name().toLowerCase()));
            return;
        }
        java.math.BigDecimal base = item.getCurrentSellPrice();
        ListState st = new ListState(item.getMaterial(), Math.min(16, have), 24, base);
        listing.put(player.getUniqueId(), st);
        player.openInventory(buildListInventory(player, st));
    }

    private Inventory buildListInventory(Player player, ListState st) {
        int have = marketManager.countItems(player, st.material);
        Inventory gui = Bukkit.createInventory(null, 27, translationManager.getMessage("market.gui.list.title"));

        // Price adjust controls from config
        st.priceSlots.clear();
        java.util.List<Integer> presets = java.util.List.of(-10, -5, -1, 1, 5, 10);
        try {
            var cfg = configManager.getModuleConfig("market");
            java.util.List<Integer> conf = cfg.getIntegerList("orders.listing.price_adjust_presets_percent");
            if (conf != null && !conf.isEmpty()) presets = conf;
        } catch (Exception ignored) {}
        int[] presetSlots = {1,2,3,5,6,7};
        for (int i = 0; i < Math.min(presets.size(), presetSlots.length); i++) {
            int pct = presets.get(i);
            int slot = presetSlots[i];
            gui.setItem(slot, priceButton(pct > 0, Math.abs(pct)));
            st.priceSlots.put(slot, pct);
        }
        gui.setItem(8, resetPriceButton());
        st.priceSlots.put(8, Integer.MIN_VALUE);

        // Absolute adjust controls from config
        try {
            var cfg = configManager.getModuleConfig("market");
            java.util.List<Integer> absList = cfg.getIntegerList("orders.listing.price_adjust_presets_absolute");
            if (absList != null && !absList.isEmpty()) {
                int[] absSlots = {0,9,15,17};
                int k = Math.min(absList.size(), absSlots.length);
                for (int i = 0; i < k; i++) {
                    int val = absList.get(i);
                    java.math.BigDecimal delta = new java.math.BigDecimal(val);
                    int slot = absSlots[i];
                    gui.setItem(slot, absButton(val >= 0, delta));
                    st.absSlots.put(slot, delta);
                }
            }
        } catch (Exception ignored) {}

        // Header with unit price
        ItemStack head = new ItemStack(st.material);
        ItemMeta hm = head.getItemMeta();
        if (hm != null) {
            hm.setDisplayName("§e" + translationManager.getMessage("market.gui.list.header", st.material.name()));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + translationManager.getMessage("market.gui.list.unit_price", formatPrice(st.unitPrice)));
            hm.setLore(lore);
            head.setItemMeta(hm);
        }
        gui.setItem(4, head);

        // Qty options
        gui.setItem(10, listQtyItem(1));
        gui.setItem(11, listQtyItem(8));
        gui.setItem(12, listQtyItem(16));
        gui.setItem(13, listQtyItem(32));
        gui.setItem(14, listQtyItem(Math.min(64, have)));

        // Duration options from config
        st.durationSlots.clear();
        java.util.List<Integer> hours = java.util.List.of(12, 24, 48);
        try {
            var cfg = configManager.getModuleConfig("market");
            java.util.List<Integer> conf = cfg.getIntegerList("orders.listing.duration_hours_options");
            if (conf != null && !conf.isEmpty()) hours = conf;
        } catch (Exception ignored) {}
        int startSlot = 19;
        for (int i = 0; i < Math.min(hours.size(), 5); i++) {
            int h = hours.get(i);
            int slot = startSlot + i;
            gui.setItem(slot, durationItem(h));
            st.durationSlots.put(slot, h);
        }

        // Confirm / Cancel
        ItemStack confirm = new ItemStack(Material.LIME_DYE);
        ItemMeta cm = confirm.getItemMeta();
        if (cm != null) { cm.setDisplayName("§a" + translationManager.getMessage("market.gui.list.confirm")); confirm.setItemMeta(cm); }
        gui.setItem(16, confirm);
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta ccm = cancel.getItemMeta();
        if (ccm != null) { ccm.setDisplayName("§c" + translationManager.getMessage("market.gui.list.cancel")); cancel.setItemMeta(ccm); }
        gui.setItem(22, cancel);

        return gui;
    }

    private ItemStack priceButton(boolean plus, int pct) {
        ItemStack it = new ItemStack(plus ? Material.EMERALD : Material.REDSTONE);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            String key = plus ? "market.gui.list.price.plus" : "market.gui.list.price.minus";
            im.setDisplayName((plus ? "§a" : "§c") + translationManager.getMessage(key, pct));
            it.setItemMeta(im);
        }
        return it;
    }
    private ItemStack absButton(boolean plus, java.math.BigDecimal amount) {
        ItemStack it = new ItemStack(plus ? Material.LIME_DYE : Material.RED_DYE);
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            String key = plus ? "market.gui.list.price.plus_abs" : "market.gui.list.price.minus_abs";
            im.setDisplayName((plus ? "§a" : "§c") + translationManager.getMessage(key, formatPrice(amount)));
            it.setItemMeta(im);
        }
        return it;
    }
    private ItemStack resetPriceButton() {
        ItemStack it = new ItemStack(Material.SUNFLOWER);
        ItemMeta im = it.getItemMeta();
        if (im != null) { im.setDisplayName("§e" + translationManager.getMessage("market.gui.list.price.reset")); it.setItemMeta(im); }
        return it;
    }

    private ItemStack listQtyItem(int qty) {
        ItemStack it = new ItemStack(Material.CHEST, Math.max(1, Math.min(64, qty)));
        ItemMeta im = it.getItemMeta();
        if (im != null) { im.setDisplayName("§6" + translationManager.getMessage("market.gui.list.qty.select", qty)); it.setItemMeta(im); }
        return it;
    }
    private ItemStack durationItem(int h) {
        ItemStack it = new ItemStack(Material.CLOCK);
        ItemMeta im = it.getItemMeta();
        if (im != null) { im.setDisplayName("§b" + translationManager.getMessage("market.gui.list.duration", h)); it.setItemMeta(im); }
        return it;
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

    private void openSellHandGUI(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) {
            player.sendMessage(translationManager.getMessage("market.no-items-to-sell", "hand"));
            return;
        }
        String title = translationManager.getMessage("market.gui.sell-hand.title");
        Inventory gui = Bukkit.createInventory(null, 27, title);

        java.util.List<BigDecimal> amounts = java.util.Arrays.asList(
            new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("1000"), new BigDecimal("5000")
        );
        int[] slots = {10, 11, 12, 13};
        for (int i = 0; i < amounts.size(); i++) {
            ItemStack it = new ItemStack(Material.GOLD_NUGGET);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6" + translationManager.getMessage("market.gui.sell-hand.amount", formatPrice(amounts.get(i))));
                meta.setLore(java.util.Arrays.asList(
                    "§7" + translationManager.getMessage("market.gui.sell-hand.lore1"),
                    "§7" + translationManager.getMessage("market.gui.sell-hand.lore2")
                ));
                it.setItemMeta(meta);
            }
            gui.setItem(slots[i], it);
        }

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cmeta = close.getItemMeta();
        if (cmeta != null) {
            cmeta.setDisplayName("§c" + translationManager.getMessage("market.gui.close"));
            close.setItemMeta(cmeta);
        }
        gui.setItem(22, close);

        player.openInventory(gui);
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
        private String selectedCategory = "ALL";
        private Character filterLetter = null;
        private SortMode sortMode = SortMode.NAME;
        
        public MarketInventory(Player player, List<MarketItem> items, int currentPage) {
            this.player = player;
            this.items = items;
            this.currentPage = currentPage;
        }
        
        public Player getPlayer() { return player; }
        public List<MarketItem> getItems() { return items; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int page) { this.currentPage = page; }
        public String getSelectedCategory() { return selectedCategory; }
        public void setSelectedCategory(String cat) { this.selectedCategory = cat; }
        public Character getFilterLetter() { return filterLetter; }
        public void setFilterLetter(Character c) { this.filterLetter = c; }
        public SortMode getSortMode() { return sortMode; }
        public void cycleSort() { this.sortMode = this.sortMode.next(); }
    }

    private enum SortMode { NAME, BUY_ASC, SELL_DESC, VOLUME_DESC;
        public SortMode next() {
            return switch (this) {
                case NAME -> BUY_ASC; case BUY_ASC -> SELL_DESC; case SELL_DESC -> VOLUME_DESC; case VOLUME_DESC -> NAME;
            };
        }
        public String display(TranslationManager tm) {
            return switch (this) {
                case NAME -> tm.getMessage("market.gui.sort-mode.name");
                case BUY_ASC -> tm.getMessage("market.gui.sort-mode.buy-asc");
                case SELL_DESC -> tm.getMessage("market.gui.sort-mode.sell-desc");
                case VOLUME_DESC -> tm.getMessage("market.gui.sort-mode.volume-desc");
            };
        }
    }
}
