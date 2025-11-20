package me.koyere.ecoxpert.commands;

import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.events.EconomicEvent;
import me.koyere.ecoxpert.modules.events.EconomicEventType;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
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
    private final ConfigManager configManager;
    
    public EcoCommand(EconomyManager economyManager, TranslationManager translationManager, ConfigManager configManager, EconomicEventEngine eventEngine) {
        super(economyManager, translationManager, configManager);
        this.eventEngine = eventEngine;
        this.configManager = configManager;
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
            case "reload":
                return handleReload(sender);
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
            case "integrations":
                return handleIntegrations(sender, subArgs);

            case "migrate":
            case "import":
                return handleMigrate(sender, subArgs);
                
            case "help":
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private boolean handleIntegrations(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ecoxpert.admin.integrations")) {
            sendMessage(sender, "error.no_permission");
            return true;
        }
        var sr = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry();
        var integ = sr.getInstance(me.koyere.ecoxpert.modules.integrations.IntegrationsManager.class);
        var cfg = sr.getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class).getModuleConfig("integrations");
        boolean enabled = cfg.getBoolean("enabled", true);
        sender.sendMessage("§6=== Integrations Status ===");
        sender.sendMessage("§7Enabled: §e" + enabled);
        try {
            sender.sendMessage("§7Jobs: §e" + (integ != null && integ.hasJobs()));
            sender.sendMessage("§7WorldGuard: §e" + (integ != null && integ.hasWorldGuard()));
            sender.sendMessage("§7Lands: §e" + (integ != null && integ.hasLands()));
            sender.sendMessage("§7Towny: §e" + (integ != null && integ.hasTowny()));
            sender.sendMessage("§7Slimefun: §e" + (integ != null && integ.hasSlimefun()));
            sender.sendMessage("§7McMMO: §e" + (integ != null && integ.hasMcMMO()));
            // Jobs dynamic thresholds quick view
            if (cfg.getBoolean("jobs.dynamic.enabled", true)) {
                java.util.List<?> th = cfg.getList("jobs.dynamic.inflation.thresholds");
                sender.sendMessage("§7Jobs dynamic thresholds: §e" + (th != null ? th.size() : 0));
            }
            // Territory rules quick view
            if (cfg.getBoolean("territory.enabled", true)) {
                var wg = cfg.getConfigurationSection("territory.worldguard.rules");
                var ld = cfg.getConfigurationSection("territory.lands");
                var ty = cfg.getConfigurationSection("territory.towny.rules");
                sender.sendMessage("§7Territory WG rules: §e" + (wg != null ? wg.getKeys(false).size() : 0));
                sender.sendMessage("§7Territory Lands entries: §e" + (ld != null ? ld.getKeys(false).size() : 0));
                sender.sendMessage("§7Territory Towny rules: §e" + (ty != null ? ty.getKeys(false).size() : 0));
            }
        } catch (Exception ignored) {}
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!(sender.hasPermission("ecoxpert.admin") || sender.hasPermission("ecoxpert.admin.reload"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }

        // Reload core configs and translations
        try {
            configManager.reload();
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload configuration: " + e.getMessage());
            return true;
        }
        try {
            translationManager.reload();
        } catch (Exception e) {
            sender.sendMessage("§cFailed to reload translations: " + e.getMessage());
            return true;
        }

        // Reload bank limits (tier overrides)
        try {
            me.koyere.ecoxpert.modules.bank.BankManager bankManager =
                org.bukkit.plugin.java.JavaPlugin.getPlugin(EcoXpertPlugin.class)
                    .getServiceRegistry().getInstance(me.koyere.ecoxpert.modules.bank.BankManager.class);
            if (bankManager != null) {
                bankManager.reloadConfig();
            }
        } catch (Exception ignored) { }

        sendMessage(sender, "plugin.reloaded");
        return true;
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
            case "stats":
                return handleEventsStats(sender, args);
            case "recent":
                return handleEventsRecent(sender);
            case "statsdetail":
                return handleEventsStatsDetail(sender, args);
            case "anti-stagnation":
            case "antistagnation":
                return handleEventsAntiStagnation(sender);
            case "pause":
                return handleEventsPause(sender);
            case "resume":
                return handleEventsResume(sender);
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
        boolean engineActive = eventEngine.isEventEngineActive();
        int active = eventEngine.getActiveEvents().size();
        int history = eventEngine.getEventHistory().size();
        sendMessage(sender, "events.admin.status", engineActive, active, history);
        return true;
    }

    private boolean handleEventsRecent(CommandSender sender) {
        try {
            var dm = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            try (me.koyere.ecoxpert.core.data.QueryResult qr = dm.executeQuery(
                    "SELECT event_id, type, status, start_time, end_time FROM ecoxpert_economic_events ORDER BY id DESC LIMIT 10").join()) {
                boolean any = false;
                sendMessage(sender, "events.admin.recent.header");
                while (qr.next()) {
                    any = true;
                    String id = qr.getString("event_id");
                    String type = qr.getString("type");
                    String status = qr.getString("status");
                    String start = String.valueOf(qr.getTimestamp("start_time"));
                    String end = String.valueOf(qr.getTimestamp("end_time"));
                    sendMessage(sender, "events.admin.recent.item", id, type, status, start, end);
                }
                if (!any) sendMessage(sender, "events.admin.recent.none");
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to load recent events");
        }
        return true;
    }

    private boolean handleEventsAntiStagnation(CommandSender sender) {
        long hoursSince = eventEngine.getHoursSinceLastEvent();
        int quiet = eventEngine.getConsecutiveQuietHours();
        sendMessage(sender, "events.admin.antistagnation.info", hoursSince, quiet);
        return true;
    }

    private boolean handleEventsPause(CommandSender sender) {
        eventEngine.pauseEngine();
        sendMessage(sender, "events.admin.engine.paused");
        return true;
    }

    private boolean handleEventsResume(CommandSender sender) {
        eventEngine.resumeEngine();
        sendMessage(sender, "events.admin.engine.resumed");
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
                // Provide hints on common failure reasons
                if (!eventEngine.getActiveEvents().isEmpty()) {
                    sendMessage(sender, "events.admin.active.header");
                    for (var ev : eventEngine.getActiveEvents().values()) {
                        sendMessage(sender, "events.admin.active.item", ev.getId(), ev.getName(), ev.getType().name(), ev.getDuration());
                    }
                    sender.sendMessage("§7Hint: end the active event or wait for cooldown.");
                } else {
                    sendMessage(sender, "events.admin.trigger.failed", type.name());
                }
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

    private boolean handleEventsStats(CommandSender sender, String[] args) {
        int days = 30;
        if (args.length >= 2) {
            try {
                days = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }
        try {
            var dm = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            // Totales por tipo en ventana
            try (me.koyere.ecoxpert.core.data.QueryResult qr = dm.executeQuery(
                "SELECT type, COUNT(*) as cnt FROM ecoxpert_economic_events WHERE start_time >= datetime('now', '-' || ? || ' days') GROUP BY type ORDER BY cnt DESC",
                days).join()) {
                boolean any = false;
                sendMessage(sender, "events.admin.stats.header", days);
                int total = 0;
                while (qr.next()) {
                    any = true;
                    String type = qr.getString("type");
                    Integer cntObj = qr.getInt("cnt");
                    int cnt = cntObj != null ? cntObj : (qr.getLong("cnt") != null ? qr.getLong("cnt").intValue() : 0);
                    total += cnt;
                    sendMessage(sender, "events.admin.stats.item", type, cnt);
                }
                if (!any) {
                    sendMessage(sender, "events.admin.stats.none", days);
                } else {
                    sendMessage(sender, "events.admin.stats.total", total);
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to load event stats");
        }
        return true;
    }

    private boolean handleEventsStatsDetail(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /ecoxpert events statsdetail <TYPE> [days]");
            return true;
        }
        String typeArg = args[1].toUpperCase();
        int days = 30;
        if (args.length >= 3) {
            try { days = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
        }
        try {
            EconomicEventType type = EconomicEventType.valueOf(typeArg);
            var dm = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            try (me.koyere.ecoxpert.core.data.QueryResult qr = dm.executeQuery(
                "SELECT parameters, start_time, end_time FROM ecoxpert_economic_events WHERE type = ? AND start_time >= datetime('now', '-' || ? || ' days')",
                type.name(), days).join()) {
                int count = 0; long durSum = 0; int durCount = 0; int itemsSum = 0; int itemsCount = 0;
                double buySum = 0.0; int buyCount = 0; double sellSum = 0.0; int sellCount = 0; double stimulusSum = 0.0;
                while (qr.next()) {
                    count++;
                    String params = qr.getString("parameters");
                    Long dur = parseJsonLong(params, "metrics.duration_minutes");
                    if (dur == null) {
                        try {
                            var s = qr.getTimestamp("start_time");
                            var e = qr.getTimestamp("end_time");
                            if (s != null && e != null) {
                                long mins = java.time.Duration.between(s.toLocalDateTime(), e.toLocalDateTime()).toMinutes();
                                dur = mins;
                            }
                        } catch (Exception ignored) {}
                    }
                    if (dur != null) { durSum += dur; durCount++; }
                    Integer items = parseJsonInt(params, "metrics.items");
                    if (items != null) { itemsSum += items; itemsCount++; }
                    Double bd = parseJsonDouble(params, "metrics.buy_delta");
                    if (bd != null) { buySum += bd; buyCount++; }
                    Double sd = parseJsonDouble(params, "metrics.sell_delta");
                    if (sd != null) { sellSum += sd; sellCount++; }
                    Double stim = parseJsonDouble(params, "metrics.total_stimulus");
                    if (stim != null) { stimulusSum += stim; }
                }
                sendMessage(sender, "events.admin.statsdetail.header", type.name(), days);
                sendMessage(sender, "events.admin.statsdetail.count", count);
                if (durCount > 0) sendMessage(sender, "events.admin.statsdetail.avg_duration", durSum / Math.max(1, durCount));
                if (itemsCount > 0) sendMessage(sender, "events.admin.statsdetail.avg_items", itemsSum / Math.max(1, itemsCount));
                if (buyCount > 0) sendMessage(sender, "events.admin.statsdetail.avg_buy_delta", String.format("%.2f%%", (buySum / buyCount) * 100.0));
                if (sellCount > 0) sendMessage(sender, "events.admin.statsdetail.avg_sell_delta", String.format("%.2f%%", (sellSum / sellCount) * 100.0));
                if (stimulusSum > 0.0) {
                    String money = economyManager.formatMoney(java.math.BigDecimal.valueOf(stimulusSum));
                    sendMessage(sender, "events.admin.statsdetail.total_stimulus", money);
                }
            }
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "events.admin.unknown_type", args[1]);
        } catch (Exception e) {
            sender.sendMessage("§cFailed to load detailed stats");
        }
        return true;
    }

    private Double parseJsonDouble(String json, String key) {
        try {
            String token = "\"" + key + "\":";
            int i = json.indexOf(token);
            if (i < 0) return null;
            i += token.length();
            int j = i;
            while (j < json.length() && "-+.0123456789".indexOf(json.charAt(j)) >= 0) j++;
            return Double.parseDouble(json.substring(i, j));
        } catch (Exception ignored) {
            return null;
        }
    }
    private Integer parseJsonInt(String json, String key) {
        Double d = parseJsonDouble(json, key);
        return d != null ? (int) Math.round(d) : null;
    }
    private Long parseJsonLong(String json, String key) {
        Double d = parseJsonDouble(json, key);
        return d != null ? d.longValue() : null;
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
            sendMessage(sender, "economy.admin.help.sync");
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
            case "loans":
                return handleEconomyLoans(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "sync":
                return handleEconomySync(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            default:
                sendMessage(sender, "economy.admin.unknown");
                return true;
        }
    }

    private boolean handleEconomySync(CommandSender sender, String[] args) {
        var plugin = JavaPlugin.getPlugin(EcoXpertPlugin.class);
        var syncService = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.economy.EconomySyncService.class);
        if (syncService == null) {
            sender.sendMessage("§cSync service unavailable.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            var status = syncService.getStatus();
            sendMessage(sender, "economy.admin.sync.status",
                status.mode(),
                status.providerName(),
                status.running() ? "running" : "stopped",
                status.trackedPlayers());
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("all")) {
            OfflinePlayer target = findPlayer(sender, args[0]);
            if (target == null) return true;
            sendMessage(sender, "economy.admin.sync.started", "player", target.getName());
            syncService.syncPlayerNow(target).thenAccept(result -> {
                sendMessage(sender, "economy.admin.sync.completed",
                    result.providerName(), result.pulled(), result.pushed(), result.skipped());
            }).exceptionally(ex -> {
                sendMessage(sender, "economy.admin.sync.error", ex.getMessage());
                return null;
            });
            return true;
        }

        sendMessage(sender, "economy.admin.sync.started", "online", "all");
        syncService.syncOnlineNow().thenAccept(result -> {
            if ("None".equalsIgnoreCase(result.providerName())) {
                sendMessage(sender, "economy.admin.sync.no-provider");
                return;
            }
            if ("Disabled".equalsIgnoreCase(result.providerName())) {
                sendMessage(sender, "economy.admin.sync.disabled");
                return;
            }
            sendMessage(sender, "economy.admin.sync.completed",
                result.providerName(), result.pulled(), result.pushed(), result.skipped());
        }).exceptionally(ex -> {
            sendMessage(sender, "economy.admin.sync.error", ex.getMessage());
            return null;
        });
        return true;
    }

    private boolean handleEconomyLoans(CommandSender sender, String[] args) {
        if (!(sender.hasPermission("ecoxpert.admin") || sender.hasPermission("ecoxpert.admin.loans"))) {
            sendMessage(sender, "error.no_permission");
            return true;
        }
        if (args.length == 0) {
            sendMessage(sender, "economy.admin.loans.help.header");
            sendMessage(sender, "economy.admin.loans.help.stats");
            sendMessage(sender, "economy.admin.loans.help.statsdetail");
            return true;
        }
        String sub = args[0].toLowerCase();
        int days = 30;
        if (args.length >= 2) {
            try { days = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
        }
        try {
            var dm = JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            if (sub.equals("stats")) {
                int active = 0; String outstanding = "0"; String avgRate = "0"; int late = 0;
                try (me.koyere.ecoxpert.core.data.QueryResult q1 = dm.executeQuery("SELECT COUNT(*) as c FROM ecoxpert_loans WHERE status='ACTIVE'").join()) {
                    if (q1.next()) { Integer c = q1.getInt("c"); active = c != null ? c : (q1.getLong("c") != null ? q1.getLong("c").intValue() : 0); }
                }
                try (me.koyere.ecoxpert.core.data.QueryResult q2 = dm.executeQuery("SELECT SUM(outstanding) as s, AVG(interest_rate) as r FROM ecoxpert_loans WHERE status='ACTIVE'").join()) {
                    if (q2.next()) {
                        java.math.BigDecimal s = q2.getBigDecimal("s");
                        java.math.BigDecimal r = q2.getBigDecimal("r");
                        outstanding = s != null ? JavaPlugin.getPlugin(EcoXpertPlugin.class).getServiceRegistry().getInstance(me.koyere.ecoxpert.economy.EconomyManager.class).formatMoney(s) : "$0";
                        avgRate = r != null ? r.multiply(new java.math.BigDecimal("100")).setScale(2) + "%" : "0%";
                    }
                }
                try (me.koyere.ecoxpert.core.data.QueryResult q3 = dm.executeQuery("SELECT COUNT(*) as c FROM ecoxpert_loan_schedules WHERE status='LATE' AND due_date >= date('now', '-' || ? || ' days')", days).join()) {
                    if (q3.next()) { Integer c = q3.getInt("c"); late = c != null ? c : (q3.getLong("c") != null ? q3.getLong("c").intValue() : 0); }
                }
                sendMessage(sender, "economy.admin.loans.stats.header", days);
                sendMessage(sender, "economy.admin.loans.stats.active", active);
                sendMessage(sender, "economy.admin.loans.stats.outstanding", outstanding);
                sendMessage(sender, "economy.admin.loans.stats.avg_rate", avgRate);
                sendMessage(sender, "economy.admin.loans.stats.late", late);
                return true;
            } else if (sub.equals("statsdetail")) {
                // Breakdown by buckets of score and term (if desired in future). For now: counts created in window and paid vs pending installments.
                int created = 0, paidInst = 0, pendingInst = 0, lateInst = 0;
                try (me.koyere.ecoxpert.core.data.QueryResult q1 = dm.executeQuery("SELECT COUNT(*) as c FROM ecoxpert_loans WHERE created_at >= datetime('now', '-' || ? || ' days')", days).join()) {
                    if (q1.next()) { Integer c = q1.getInt("c"); created = c != null ? c : (q1.getLong("c") != null ? q1.getLong("c").intValue() : 0); }
                }
                try (me.koyere.ecoxpert.core.data.QueryResult q2 = dm.executeQuery("SELECT status, COUNT(*) as c FROM ecoxpert_loan_schedules WHERE due_date >= date('now', '-' || ? || ' days') GROUP BY status", days).join()) {
                    while (q2.next()) {
                        String st = q2.getString("status");
                        Integer c = q2.getInt("c");
                        int v = c != null ? c : (q2.getLong("c") != null ? q2.getLong("c").intValue() : 0);
                        if ("PAID".equalsIgnoreCase(st)) paidInst = v;
                        else if ("LATE".equalsIgnoreCase(st)) lateInst = v;
                        else pendingInst = v;
                    }
                }
                sendMessage(sender, "economy.admin.loans.statsdetail.header", days);
                sendMessage(sender, "economy.admin.loans.statsdetail.created", created);
                sendMessage(sender, "economy.admin.loans.statsdetail.installments", paidInst + pendingInst + lateInst, paidInst, pendingInst, lateInst);
                return true;
            }
        } catch (Exception e) {
            sender.sendMessage("§cFailed to load loans stats");
        }
        return true;
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

        EcoXpertPlugin plugin = JavaPlugin.getPlugin(EcoXpertPlugin.class);
        var syncService = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.economy.EconomySyncService.class);
        if (syncService == null) {
            sendMessage(sender, "admin.migrate.error", "Sync service unavailable");
            return true;
        }

        sendMessage(sender, "admin.migrate.started", syncService.getStatus().providerName());
        syncService.importAllFromFallback().thenAccept(result -> {
            if ("None".equalsIgnoreCase(result.providerName())) {
                sendMessage(sender, "admin.migrate.none");
                return;
            }
            sendMessage(sender, "admin.migrate.completed", result.imported());
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
