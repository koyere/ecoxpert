package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.core.economy.EconomySyncManager;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.events.EconomicEvent;
import me.koyere.ecoxpert.modules.events.EconomicEventType;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main /eco command with subcommands
 * 
 * Provides comprehensive economy management through subcommands:
 * - balance: Check player balance
 * - pay: Transfer money to another player
 * - set: Set player balance (admin)
 * - add: Add money to player (admin)
 * - remove: Remove money from player (admin)
 */
public class EcoCommand extends BaseCommand {
    // Dynamic events engine used for /ecoxpert events admin operations
    private final EconomicEventEngine eventEngine;
    
    public EcoCommand(EconomyManager economyManager, TranslationManager translationManager, EconomicEventEngine eventEngine) {
        super(economyManager, translationManager);
        this.eventEngine = eventEngine;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        
        switch (subcommand) {
            case "balance":
            case "bal":
                return handleBalance(sender, subArgs);
                
            case "pay":
                return handlePay(sender, subArgs);
                
            case "set":
                return handleSet(sender, subArgs);
                
            case "add":
            case "give":
                return handleAdd(sender, subArgs);
                
            case "remove":
            case "take":
                return handleRemove(sender, subArgs);
            
            case "events":
                return handleEvents(sender, subArgs);

            case "economy":
                return handleEconomy(sender, subArgs);

            case "migrate":
            case "import":
                return handleMigrate(sender, subArgs);
                
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    /**
     * Handle admin events subcommands
     * Syntax: /ecoxpert events status|active|history|trigger <TYPE>|end <ID>
     */
    private boolean handleEvents(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ecoxpert.admin.events")) {
            sendMessage(sender, "error.no_permission");
            return true;
        }

        if (args.length == 0) {
            // Show basic help
            sendMessage(sender, "events.admin.help.header");
            sendMessage(sender, "events.admin.help.status");
            sendMessage(sender, "events.admin.help.active");
            sendMessage(sender, "events.admin.help.history");
            sendMessage(sender, "events.admin.help.trigger");
            sendMessage(sender, "events.admin.help.end");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status":
                return handleEventsStatus(sender);
            case "active":
                return handleEventsActive(sender);
            case "history":
                return handleEventsHistory(sender);
            case "trigger":
                return handleEventsTrigger(sender, args);
            case "end":
                return handleEventsEnd(sender, args);
            default:
                sendMessage(sender, "events.admin.unknown");
                return true;
        }
    }

    private boolean handleEventsStatus(CommandSender sender) {
        int active = eventEngine.getActiveEvents().size();
        int history = eventEngine.getEventHistory().size();
        sendMessage(sender, "events.admin.status", active, history);
        return true;
    }

    private boolean handleEventsActive(CommandSender sender) {
        var active = eventEngine.getActiveEvents();
        if (active.isEmpty()) {
            sendMessage(sender, "events.admin.active.none");
            return true;
        }
        sendMessage(sender, "events.admin.active.header");
        for (EconomicEvent ev : active.values()) {
            sendMessage(sender, "events.admin.active.item", ev.getId(), ev.getName(), ev.getType().name(), ev.getDuration());
        }
        return true;
    }

    private boolean handleEventsHistory(CommandSender sender) {
        var history = eventEngine.getEventHistory();
        if (history.isEmpty()) {
            sendMessage(sender, "events.admin.history.none");
            return true;
        }
        sendMessage(sender, "events.admin.history.header");
        // Show last 10 items max
        int start = Math.max(0, history.size() - 10);
        for (int i = history.size() - 1; i >= start; i--) {
            EconomicEvent ev = history.get(i);
            sendMessage(sender, "events.admin.history.item", ev.getId(), ev.getName(), ev.getType().name(), ev.getStatus().name());
        }
        return true;
    }

    private boolean handleEventsTrigger(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "events.admin.trigger.usage");
            return true;
        }
        try {
            EconomicEventType type = EconomicEventType.valueOf(args[1].toUpperCase());
            boolean ok = eventEngine.triggerEvent(type);
            if (ok) {
                sendMessage(sender, "events.admin.triggered", type.name());
            } else {
                sendMessage(sender, "events.admin.trigger.failed", type.name());
            }
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "events.admin.unknown_type", args[1]);
        }
        return true;
    }

    private boolean handleEventsEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "events.admin.end.usage");
            return true;
        }
        String id = args[1];
        boolean ended = eventEngine.endEventById(id);
        if (ended) {
            sendMessage(sender, "events.admin.ended", id);
        } else {
            sendMessage(sender, "events.admin.not_found", id);
        }
        return true;
    }

    /**
     * Economy status and diagnostics for admins.
     * Syntax: /ecoxpert economy status|diagnostics
     */
    private boolean handleEconomy(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("ecoxpert.admin") || sender.hasPermission("ecoxpert.admin.economy"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }

        if (args.length == 0) {
            sendMessage(sender, "economy.admin.help.header");
            sendMessage(sender, "economy.admin.help.status");
            sendMessage(sender, "economy.admin.help.diagnostics");
            sendMessage(sender, "economy.admin.help.policy");
            sender.sendMessage("§7Use §e/ecoxpert economy health §7to view CPI and score");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "status":
                return handleEconomyStatus(sender);
            case "diagnostics":
                return handleEconomyDiagnostics(sender);
            case "health":
                return handleEconomyHealth(sender);
            case "policy":
                return handleEconomyPolicy(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            default:
                sendMessage(sender, "economy.admin.unknown");
                return true;
        }
    }

    private boolean handleEconomyHealth(CommandSender sender) {
        if (!(sender.hasPermission("ecoxpert.admin") || sender.hasPermission("ecoxpert.admin.economy"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }
        var plugin = JavaPlugin.getPlugin(EcoXpertPlugin.class);
        var services = plugin.getServiceRegistry();
        var cfg = services.getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
        var dm = services.getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
        var econ = services.getInstance(me.koyere.ecoxpert.economy.EconomyManager.class);

        var inflCfg = cfg.getModuleConfig("inflation");
        double targetInflation = inflCfg.getDouble("targets.inflation", 1.02);
        int windowHours = inflCfg.getInt("metrics.cpi_window_hours", 72);

        var marketCfg = cfg.getModuleConfig("market");
        java.util.List<String> basket = marketCfg.getStringList("metrics.basket");
        if (basket == null || basket.isEmpty()) {
            basket = java.util.List.of("WHEAT","BREAD","APPLE","COAL","IRON_INGOT","GOLD_INGOT");
        }

        try {
            java.math.BigDecimal cpiSum = java.math.BigDecimal.ZERO;
            int cpiCount = 0;
            java.math.BigDecimal windowInflationSum = java.math.BigDecimal.ZERO;
            int inflCount = 0;

            for (String mat : basket) {
                try (me.koyere.ecoxpert.core.data.QueryResult qr = dm.executeQuery(
                        "SELECT base_price, current_buy_price FROM ecoxpert_market_items WHERE material = ?",
                        mat).join()) {
                    if (qr.next()) {
                        var base = qr.getBigDecimal("base_price");
                        var cur = qr.getBigDecimal("current_buy_price");
                        if (base != null && cur != null && base.compareTo(java.math.BigDecimal.ZERO) > 0) {
                            cpiSum = cpiSum.add(cur.divide(base, 6, java.math.RoundingMode.HALF_UP));
                            cpiCount++;
                        }
                    }
                } catch (Exception ignored) {}

                try (me.koyere.ecoxpert.core.data.QueryResult qwin = dm.executeQuery(
                        "SELECT buy_price, snapshot_time FROM ecoxpert_market_price_history WHERE material = ? AND snapshot_time >= datetime('now', '-' || ? || ' hours') ORDER BY snapshot_time ASC",
                        mat, windowHours).join()) {
                    java.math.BigDecimal first = null;
                    java.math.BigDecimal last = null;
                    while (qwin.next()) {
                        if (first == null) first = qwin.getBigDecimal("buy_price");
                        last = qwin.getBigDecimal("buy_price");
                    }
                    if (first != null && last != null && first.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        windowInflationSum = windowInflationSum.add(last.divide(first, 6, java.math.RoundingMode.HALF_UP));
                        inflCount++;
                    }
                } catch (Exception ignored) {}
            }

            double cpi = cpiCount > 0 ? cpiSum.divide(java.math.BigDecimal.valueOf(cpiCount), 4, java.math.RoundingMode.HALF_UP).doubleValue() : 1.0;
            double windowInflation = inflCount > 0 ? windowInflationSum.divide(java.math.BigDecimal.valueOf(inflCount), 6, java.math.RoundingMode.HALF_UP).doubleValue() : 1.0;
            double annualized = windowHours > 0 ? Math.pow(windowInflation, (365.0*24.0)/windowHours) - 1.0 : 0.0;

            double deviation = Math.abs(cpi - targetInflation);
            double tolerance = 0.05;
            int healthScore = (int) Math.max(0, 100 - Math.min(100, (deviation / tolerance) * 100));

            sendMessage(sender, "economy.admin.health.header");
            sender.sendMessage(String.format("§7CPI Index: §e%.3f", cpi));
            sender.sendMessage(String.format("§7Annualized Inflation: §e%.2f%%", annualized * 100));
            sender.sendMessage(String.format("§7Target Inflation: §e%.3f", targetInflation));
            sender.sendMessage(String.format("§7Health Score: §e%d/100", healthScore));

        } catch (Exception e) {
            sender.sendMessage("§cFailed to compute economy health");
        }
        return true;
    }

    private boolean handleEconomyPolicy(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("ecoxpert.admin") || sender.hasPermission("ecoxpert.admin.economy"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }
        InflationManager infl = JavaPlugin.getPlugin(EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
        if (infl == null) {
            sender.sendMessage("Policy not available");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("show")) {
            sendMessage(sender, "economy.admin.policy.header");
            sender.sendMessage(infl.getPolicyInfo());
            double[] f = infl.getMarketFactors();
            sender.sendMessage(String.format("§7Global factors: buy=%.3f, sell=%.3f", f[0], f[1]));
            return true;
        }
        if (args[0].equalsIgnoreCase("set") && args.length >= 3) {
            String name = args[1];
            try {
                double value = Double.parseDouble(args[2]);
                boolean ok = infl.setPolicyParam(name, value);
                if (ok) sendMessage(sender, "economy.admin.policy.updated", name, value);
                else sendMessage(sender, "economy.admin.policy.unknown_param", name);
            } catch (NumberFormatException e) {
                sendMessage(sender, "economy.admin.policy.invalid_value", args[2]);
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            infl.reloadPolicy();
            sendMessage(sender, "economy.admin.policy.reloaded");
            return true;
        }
        sendMessage(sender, "economy.admin.policy.usage");
        return true;
    }

    private boolean handleEconomyStatus(CommandSender sender) {
        // Provider detection
        me.koyere.ecoxpert.core.economy.EconomyConflictDetector detector =
            new me.koyere.ecoxpert.core.economy.EconomyConflictDetector(JavaPlugin.getPlugin(EcoXpertPlugin.class));
        var status = detector.detectEconomyProvider();
        var plugins = detector.detectInstalledEconomyPlugins();

        // DB status
        var dm = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry()
            .getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
        var dbStatus = dm.getStatus();

        sendMessage(sender, "economy.admin.status.header");
        sendMessage(sender, "economy.admin.status.provider", status.getStatus().name(), String.valueOf(status.getPluginName()));
        sendMessage(sender, "economy.admin.status.plugins", plugins.getInstalledCount());
        sendMessage(sender, "economy.admin.status.db", dbStatus.getCurrentType(), dbStatus.isConnected(), dbStatus.isHealthy());

        // Accounts summary (total money, average, count)
        try (me.koyere.ecoxpert.core.data.QueryResult qr = dm.executeQuery(
                "SELECT COUNT(*) as cnt, COALESCE(SUM(balance),0) as total, COALESCE(AVG(balance),0) as avg FROM ecoxpert_accounts"
        ).join()) {
            if (qr.next()) {
                long cnt = qr.getLong("cnt");
                java.math.BigDecimal total = qr.getBigDecimal("total");
                java.math.BigDecimal avg = qr.getBigDecimal("avg");
                sender.sendMessage(String.format("§7Accounts: §e%d §7| Total: §e%s §7| Avg: §e%s",
                        cnt,
                        JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.economy.EconomyManager.class).formatMoney(total),
                        JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.economy.EconomyManager.class).formatMoney(avg)));
            }
        } catch (Exception ignored) {}

        // Market global factors (from InflationManager)
        var infl = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry()
            .getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
        if (infl != null) {
            double[] f = infl.getMarketFactors();
            sender.sendMessage(String.format("§7Market factors: buy=%.3f, sell=%.3f", f[0], f[1]));
        }
        return true;
    }

    private boolean handleEconomyDiagnostics(CommandSender sender) {
        EcoXpertPlugin plugin = JavaPlugin.getPlugin(EcoXpertPlugin.class);
        me.koyere.ecoxpert.core.economy.EconomySystemTestRunner runner =
            new me.koyere.ecoxpert.core.economy.EconomySystemTestRunner(plugin);
        var results = runner.runSafeTests();
        sender.sendMessage(results.toString());
        return true;
    }

    private boolean handleMigrate(CommandSender sender, String[] args) {
        // Admin-only: allow either database or economy admin perms
        if (!(sender.hasPermission("ecoxpert.admin.database") || sender.hasPermission("ecoxpert.admin.economy"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }

        // Optional arg: balances
        if (args.length > 0 && !args[0].equalsIgnoreCase("balances")) {
            sendMessage(sender, "commands.migrate.usage");
            return true;
        }

        // Get current Vault provider
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (registration == null || registration.getProvider() == null) {
            sendMessage(sender, "admin.migrate.none");
            return true;
        }

        String providerPlugin = registration.getPlugin() != null ? registration.getPlugin().getName() : "Unknown";
        if (providerPlugin.toLowerCase().contains("ecoxpert")) {
            // We are the active provider; nothing to import
            sendMessage(sender, "admin.migrate.none");
            return true;
        }

        sendMessage(sender, "admin.migrate.started", providerPlugin);

        // Use plugin instance to run the importer
        EcoXpertPlugin plugin = JavaPlugin.getPlugin(EcoXpertPlugin.class);
        EconomySyncManager sync = new EconomySyncManager(plugin, economyManager, registration.getProvider());
        sync.importBalancesFromFallback().thenAccept(imported -> {
            sendMessage(sender, "admin.migrate.completed", imported);
        }).exceptionally(ex -> {
            sendMessage(sender, "admin.migrate.error", ex.getMessage());
            return null;
        });

        return true;
    }
    
    private boolean handleBalance(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.economy.balance")) {
            return true;
        }
        
        OfflinePlayer target;
        
        if (args.length == 0) {
            // Check own balance
            Player player = requirePlayer(sender);
            if (player == null) return true;
            target = player;
        } else {
            // Check another player's balance
            if (!hasPermission(sender, "ecoxpert.admin.economy")) {
                return true;
            }
            target = findPlayer(sender, args[0]);
            if (target == null) return true;
        }
        
        ensureAccount(target.getUniqueId());
        
        economyManager.getBalance(target.getUniqueId()).thenAccept(balance -> {
            String formattedBalance = economyManager.formatMoney(balance);
            if (target.equals(sender)) {
                sendMessage(sender, "commands.balance.own", formattedBalance);
            } else {
                sendMessage(sender, "commands.balance.other", target.getName(), formattedBalance);
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handlePay(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.economy.pay")) {
            return true;
        }
        
        Player player = requirePlayer(sender);
        if (player == null) return true;
        
        if (args.length < 2) {
            sendMessage(sender, "commands.pay.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sendMessage(sender, "error.cannot_pay_self");
            return true;
        }
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(player.getUniqueId());
        ensureAccount(target.getUniqueId());
        
        economyManager.transferMoney(
                player.getUniqueId(), 
                target.getUniqueId(), 
                amount, 
                "Player payment from " + player.getName()
        ).thenAccept(success -> {
            String formattedAmount = economyManager.formatMoney(amount);
            if (success) {
                sendMessage(sender, "commands.pay.success", formattedAmount, target.getName());
                
                // Notify target if online
                if (target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    sendMessage(targetPlayer, "commands.pay.received", formattedAmount, player.getName());
                }
            } else {
                sendMessage(sender, "error.insufficient_funds");
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handleSet(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.set.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.setBalance(
                target.getUniqueId(), 
                amount, 
                "Admin set by " + sender.getName()
        ).thenRun(() -> {
            String formattedAmount = economyManager.formatMoney(amount);
            sendMessage(sender, "commands.set.success", target.getName(), formattedAmount);
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.add.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.addMoney(
                target.getUniqueId(), 
                amount, 
                "Admin add by " + sender.getName()
        ).thenRun(() -> {
            String formattedAmount = economyManager.formatMoney(amount);
            sendMessage(sender, "commands.add.success", formattedAmount, target.getName());
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "ecoxpert.admin.economy")) {
            return true;
        }
        
        if (args.length < 2) {
            sendMessage(sender, "commands.remove.usage");
            return true;
        }
        
        OfflinePlayer target = findPlayer(sender, args[0]);
        if (target == null) return true;
        
        BigDecimal amount = parseAmount(sender, args[1]);
        if (amount == null) return true;
        
        ensureAccount(target.getUniqueId());
        
        economyManager.removeMoney(
                target.getUniqueId(), 
                amount, 
                "Admin remove by " + sender.getName()
        ).thenAccept(success -> {
            String formattedAmount = economyManager.formatMoney(amount);
            if (success) {
                sendMessage(sender, "commands.remove.success", formattedAmount, target.getName());
            } else {
                sendMessage(sender, "error.insufficient_funds");
            }
        }).exceptionally(throwable -> {
            sendMessage(sender, "error.database_error");
            return null;
        });
        
        return true;
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sendMessage(sender, "commands.help.header");
        sendMessage(sender, "commands.help.balance");
        sendMessage(sender, "commands.help.pay");
        
        if (sender.hasPermission("ecoxpert.admin.economy")) {
            sendMessage(sender, "commands.help.admin_header");
            sendMessage(sender, "commands.help.set");
            sendMessage(sender, "commands.help.add");
            sendMessage(sender, "commands.help.remove");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Subcommands
            List<String> subcommands = Arrays.asList("balance", "pay", "help");
            if (sender.hasPermission("ecoxpert.admin.economy")) {
                subcommands = Arrays.asList("balance", "pay", "set", "add", "remove", "help");
            }
            
            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        if (args.length == 2 && !args[0].equalsIgnoreCase("help")) {
            // Player names
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return completions;
    }
}
