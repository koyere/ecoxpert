package me.koyere.ecoxpert.modules.loans;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.UUID;

public class LoanDelinquencyScheduler {
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final java.util.concurrent.ConcurrentHashMap<java.util.UUID, Long> lastNotify = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(false);

    public LoanDelinquencyScheduler(EcoXpertPlugin plugin, DataManager dataManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
    }

    public void start() {
        try {
            var cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class).getModuleConfig("loans");
            boolean enabled = cfg.getBoolean("enabled", true);
            if (!enabled) return;
            int intervalMinutes = cfg.getInt("scheduler.interval_minutes", 60);
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::runOnce, 20L * 10, 20L * 60 * intervalMinutes);
            plugin.getLogger().info("Loan delinquency scheduler started (" + intervalMinutes + " min)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to start loan scheduler: " + e.getMessage());
        }
    }

    private void runOnce() {
        if (!running.compareAndSet(false, true)) {
            plugin.getLogger().fine("Loan scheduler: previous run still active, skipping iteration");
            return;
        }
        try {
            var cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class).getModuleConfig("loans");
            double penalty = cfg.getDouble("policy.late.penalty_rate", 0.01); // 1%
            boolean notify = cfg.getBoolean("policy.late.notify", true);
            double capFraction = cfg.getDouble("policy.late.penalty_cap_fraction", 0.50); // 50% of principal cap on penalties
            int notifyCooldownMin = cfg.getInt("policy.late.notify_cooldown_minutes", 120);
            // Optional: skip in Safe Mode
            try {
                var safe = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.safety.SafeModeManager.class);
                if (safe != null && safe.isActive()) {
                    plugin.getLogger().fine("Loan scheduler: Safe Mode active, skipping");
                    return;
                }
            } catch (Exception ignored) {}
            // Mark overdue installments to LATE
            java.util.Set<Long> penalizedLoans = new java.util.HashSet<>();
            try (QueryResult qr = dataManager.executeQuery(
                "SELECT s.id as sid, s.loan_id as lid, l.player_uuid as pu, l.outstanding as out, l.principal as principal, s.amount_due, s.paid_amount " +
                "FROM ecoxpert_loan_schedules s JOIN ecoxpert_loans l ON l.id = s.loan_id " +
                "WHERE s.status = 'PENDING' AND s.due_date < date('now')").join()) {
                while (qr.next()) {
                    long schedId = qr.getLong("sid");
                    long loanId = qr.getLong("lid");
                    String pu = qr.getString("pu");
                    BigDecimal outstanding = qr.getBigDecimal("out");
                    BigDecimal principal = qr.getBigDecimal("principal");
                    // set status LATE
                    dataManager.executeUpdate("UPDATE ecoxpert_loan_schedules SET status='LATE' WHERE id = ?", schedId).join();
                    // apply penalty on loan outstanding
                    if (outstanding != null && penalty > 0 && !penalizedLoans.contains(loanId)) {
                        BigDecimal newOut = outstanding.multiply(BigDecimal.valueOf(1.0 + penalty));
                        if (principal != null && capFraction > 0) {
                            BigDecimal cap = principal.add(principal.multiply(BigDecimal.valueOf(capFraction)));
                            if (newOut.compareTo(cap) > 0) newOut = cap;
                        }
                        dataManager.executeUpdate("UPDATE ecoxpert_loans SET outstanding = ? WHERE id = ?", newOut, loanId).join();
                        penalizedLoans.add(loanId);
                    }
                    // notify player (if online)
                    if (notify && pu != null) {
                        try {
                            UUID uuid = UUID.fromString(pu);
                            long now = System.currentTimeMillis();
                            long last = lastNotify.getOrDefault(uuid, 0L);
                            if (now - last >= notifyCooldownMin * 60_000L) {
                                var p = Bukkit.getPlayer(uuid);
                                if (p != null && p.isOnline()) {
                                    var tm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
                                    p.sendMessage(tm.getMessage("prefix") + tm.getPlayerMessage(p, "loans.overdue-summary", 1));
                                    lastNotify.put(uuid, now);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Loan scheduler error: " + e.getMessage());
        } finally {
            running.set(false);
        }
    }
}
