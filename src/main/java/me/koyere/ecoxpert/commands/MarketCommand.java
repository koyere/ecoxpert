package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.market.MarketItem;
import me.koyere.ecoxpert.modules.market.MarketStatistics;
import me.koyere.ecoxpert.modules.market.MarketTransactionResult;
import me.koyere.ecoxpert.modules.market.MarketGUI;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main market command with subcommands
 * 
 * Handles all market-related commands with professional validation,
 * async operations, and comprehensive error handling.
 */
public class MarketCommand implements TabExecutor {
    
    private final MarketManager marketManager;
    private final TranslationManager translationManager;
    private final Logger logger;
    private final MarketGUI marketGUI;
    
    public MarketCommand(MarketManager marketManager, TranslationManager translationManager, Logger logger) {
        this.marketManager = marketManager;
        this.translationManager = translationManager;
        this.logger = logger;
        this.marketGUI = new MarketGUI(marketManager, translationManager, logger);
    }
    
    /**
     * Get MarketGUI instance for event registration
     */
    public MarketGUI getMarketGUI() {
        return marketGUI;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Market commands require player context
        if (!(sender instanceof Player)) {
            sender.sendMessage(translationManager.getMessage("player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        // Check market system availability
        if (!marketManager.isMarketOpen()) {
            player.sendMessage(translationManager.getMessage("market.market-closed"));
            return true;
        }
        
        // Handle subcommands
        if (args.length == 0) {
            // Open market GUI
            openMarketGUI(player);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "buy":
                return handleBuyCommand(player, args);
            case "sell":
                return handleSellCommand(player, args);
            case "prices":
                return handlePricesCommand(player, args);
            case "stats":
                return handleStatsCommand(player);
            case "help":
                return handleHelpCommand(player);
            default:
                player.sendMessage(translationManager.getMessage("market.unknown-command", subcommand));
                return true;
        }
    }
    
    /**
     * Handle market buy command
     */
    private boolean handleBuyCommand(Player player, String[] args) {
        var safe = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        if (safe != null && safe.isActive()) {
            player.sendMessage(translationManager.getMessage("market.error.safe-mode"));
            return true;
        }
        var limiter = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
        if (limiter != null && !limiter.allow(player.getUniqueId(), "market.buy")) {
            player.sendMessage(translationManager.getMessage("errors.rate_limited"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("market.buy-usage"));
            return true;
        }
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(translationManager.getMessage("market.invalid-item", args[1]));
            return true;
        }
        
        // Parse quantity (default 1)
        int quantity = 1;
        if (args.length >= 3) {
            try {
                quantity = Integer.parseInt(args[2]);
                if (quantity <= 0) {
                    player.sendMessage(translationManager.getMessage("market.invalid-quantity"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(translationManager.getMessage("market.invalid-quantity"));
                return true;
            }
        }
        
        // Execute buy operation asynchronously
        CompletableFuture<MarketTransactionResult> buyFuture = 
            marketManager.buyItem(player, material, quantity);
        
        buyFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error processing buy command", throwable);
                player.sendMessage(translationManager.getMessage("market.system-error"));
                return;
            }
            
            if (result.isSuccess()) {
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            } else {
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            }
        });
        
        return true;
    }
    
    /**
     * Handle market sell command
     */
    private boolean handleSellCommand(Player player, String[] args) {
        var safe = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
        if (safe != null && safe.isActive()) {
            player.sendMessage(translationManager.getMessage("market.error.safe-mode"));
            return true;
        }
        var limiter = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.RateLimitManager.class);
        if (limiter != null && !limiter.allow(player.getUniqueId(), "market.sell")) {
            player.sendMessage(translationManager.getMessage("errors.rate_limited"));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(translationManager.getMessage("market.sell-usage"));
            return true;
        }
        
        // Parse material
        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(translationManager.getMessage("market.invalid-item", args[1]));
            return true;
        }
        
        // Parse quantity (default: all available)
        int quantity;
        if (args.length >= 3) {
            try {
                quantity = Integer.parseInt(args[2]);
                if (quantity <= 0) {
                    player.sendMessage(translationManager.getMessage("market.invalid-quantity"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(translationManager.getMessage("market.invalid-quantity"));
                return true;
            }
        } else {
            // Sell all available items of this type
            quantity = marketManager.countItems(player, material);
            if (quantity <= 0) {
                player.sendMessage(translationManager.getMessage("market.no-items-to-sell", 
                    material.name().toLowerCase().replace('_', ' ')));
                return true;
            }
        }
        
        // Execute sell operation asynchronously
        CompletableFuture<MarketTransactionResult> sellFuture = 
            marketManager.sellItem(player, material, quantity);
        
        sellFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error processing sell command", throwable);
                player.sendMessage(translationManager.getMessage("market.system-error"));
                return;
            }
            
            if (result.isSuccess()) {
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            } else {
                player.sendMessage(translationManager.getMessage("prefix") + result.getMessage());
            }
        });
        
        return true;
    }
    
    /**
     * Handle market prices command
     */
    private boolean handlePricesCommand(Player player, String[] args) {
        if (args.length >= 2) {
            // Show prices for specific item
            Material material;
            try {
                material = Material.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(translationManager.getMessage("market.invalid-item", args[1]));
                return true;
            }
            
            showItemPrices(player, material);
        } else {
            // Show general market prices
            showMarketPrices(player);
        }
        
        return true;
    }
    
    /**
     * Handle market stats command
     */
    private boolean handleStatsCommand(Player player) {
        CompletableFuture<MarketStatistics> statsFuture = marketManager.getMarketStatistics();
        
        statsFuture.whenComplete((stats, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error getting market statistics", throwable);
                player.sendMessage(translationManager.getMessage("market.system-error"));
                return;
            }
            
            displayMarketStatistics(player, stats);
        });
        
        return true;
    }
    
    /**
     * Handle help command
     */
    private boolean handleHelpCommand(Player player) {
        player.sendMessage(translationManager.getMessage("market.help.header"));
        player.sendMessage(translationManager.getMessage("market.help.buy"));
        player.sendMessage(translationManager.getMessage("market.help.sell"));
        player.sendMessage(translationManager.getMessage("market.help.prices"));
        player.sendMessage(translationManager.getMessage("market.help.stats"));
        player.sendMessage(translationManager.getMessage("market.help.gui"));
        player.sendMessage(translationManager.getMessage("market.help.footer"));
        return true;
    }
    
    /**
     * Open market GUI for player
     */
    private void openMarketGUI(Player player) {
        marketGUI.openMarketGUI(player);
    }
    
    /**
     * Show prices for specific item
     */
    private void showItemPrices(Player player, Material material) {
        CompletableFuture<Optional<MarketItem>> itemFuture = marketManager.getItem(material);
        
        itemFuture.whenComplete((itemOpt, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error getting item prices", throwable);
                player.sendMessage(translationManager.getMessage("market.system-error"));
                return;
            }
            
            if (itemOpt.isEmpty()) {
                player.sendMessage(translationManager.getMessage("market.item-not-found", 
                    material.name().toLowerCase().replace('_', ' ')));
                return;
            }
            
            MarketItem item = itemOpt.get();
            String itemName = material.name().toLowerCase().replace('_', ' ');
            
            player.sendMessage(translationManager.getMessage("market.item-prices.header", itemName));
            
            if (item.isBuyable()) {
                player.sendMessage(translationManager.getMessage("market.item-prices.buy", 
                    formatPrice(item.getCurrentBuyPrice())));
            }
            
            if (item.isSellable()) {
                player.sendMessage(translationManager.getMessage("market.item-prices.sell", 
                    formatPrice(item.getCurrentSellPrice())));
            }
            
            // Show trading statistics
            player.sendMessage(translationManager.getMessage("market.item-prices.volume", 
                item.getTotalVolume()));
                
            // Show trend if available
            marketManager.getItemTrend(material).whenComplete((trend, trendThrowable) -> {
                if (trendThrowable == null && trend != null) {
                    player.sendMessage(translationManager.getMessage("market.item-prices.trend", 
                        trend.getDirection().getDisplayName(), 
                        formatPercentage(trend.getPriceChangePercentage())));
                }
            });
        });
    }
    
    /**
     * Show general market prices
     */
    private void showMarketPrices(Player player) {
        CompletableFuture<List<MarketItem>> itemsFuture = marketManager.getAllItems();
        
        itemsFuture.whenComplete((items, throwable) -> {
            if (throwable != null) {
                logger.log(Level.SEVERE, "Error getting market prices", throwable);
                player.sendMessage(translationManager.getMessage("market.system-error"));
                return;
            }
            
            player.sendMessage(translationManager.getMessage("market.prices-header"));
            
            // Show first 10 items (pagination can be added later)
            int count = 0;
            for (MarketItem item : items) {
                if (count >= 10) break;
                if (!item.isActivelyTraded()) continue;
                
                String itemName = item.getMaterial().name().toLowerCase().replace('_', ' ');
                String buyPrice = item.isBuyable() ? formatPrice(item.getCurrentBuyPrice()) : "N/A";
                String sellPrice = item.isSellable() ? formatPrice(item.getCurrentSellPrice()) : "N/A";
                
                player.sendMessage(translationManager.getMessage("market.price-format", 
                    itemName, buyPrice, sellPrice));
                    
                count++;
            }
            
            if (items.size() > 10) {
                player.sendMessage(translationManager.getMessage("market.prices-more", 
                    items.size() - 10));
            }
        });
    }
    
    /**
     * Display market statistics
     */
    private void displayMarketStatistics(Player player, MarketStatistics stats) {
        player.sendMessage(translationManager.getMessage("market.stats.header"));
        player.sendMessage(translationManager.getMessage("market.stats.items", 
            stats.getActiveItems(), stats.getTotalItems()));
        player.sendMessage(translationManager.getMessage("market.stats.transactions", 
            stats.getTotalTransactions()));
        player.sendMessage(translationManager.getMessage("market.stats.volume", 
            formatPrice(stats.getTotalVolume())));
        player.sendMessage(translationManager.getMessage("market.stats.activity", 
            stats.getActivityLevel()));
        player.sendMessage(translationManager.getMessage("market.stats.daily", 
            stats.getDailyTransactions(), formatPrice(stats.getDailyVolume())));
    }
    
    /**
     * Format price for display
     */
    private String formatPrice(BigDecimal price) {
        // Use simple formatting for now - can be enhanced later
        return "$" + price.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
    
    /**
     * Format percentage for display
     */
    private String formatPercentage(BigDecimal percentage) {
        String sign = percentage.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + percentage.setScale(1, BigDecimal.ROUND_HALF_UP) + "%";
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subcommands = Arrays.asList("buy", "sell", "prices", "stats", "help");
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            
            if ("buy".equals(subcommand) || "sell".equals(subcommand) || "prices".equals(subcommand)) {
                // Material completions - show common materials
                List<String> materials = Arrays.asList(
                    "DIAMOND", "IRON_INGOT", "GOLD_INGOT", "EMERALD", "COAL", 
                    "STONE", "COBBLESTONE", "DIRT", "SAND", "GRAVEL",
                    "WHEAT", "BREAD", "APPLE", "COOKED_BEEF", "COOKED_PORKCHOP"
                );
                
                for (String material : materials) {
                    if (material.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(material);
                    }
                }
            }
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();
            
            if ("buy".equals(subcommand) || "sell".equals(subcommand)) {
                // Quantity completions
                List<String> quantities = Arrays.asList("1", "10", "32", "64");
                for (String qty : quantities) {
                    if (qty.startsWith(args[2])) {
                        completions.add(qty);
                    }
                }
            }
        }
        
        return completions;
    }
}