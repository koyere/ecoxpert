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
        try {
            var cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class).getModuleConfig("loans");
            double penalty = cfg.getDouble("policy.late.penalty_rate", 0.01); // 1%
            boolean notify = cfg.getBoolean("policy.late.notify", true);
            // Mark overdue installments to LATE
            java.util.Set<Long> penalizedLoans = new java.util.HashSet<>();
            try (QueryResult qr = dataManager.executeQuery(
                "SELECT s.id as sid, s.loan_id as lid, l.player_uuid as pu, l.outstanding as out, s.amount_due, s.paid_amount " +
                "FROM ecoxpert_loan_schedules s JOIN ecoxpert_loans l ON l.id = s.loan_id " +
                "WHERE s.status = 'PENDING' AND s.due_date < date('now')").join()) {
                while (qr.next()) {
                    long schedId = qr.getLong("sid");
                    long loanId = qr.getLong("lid");
                    String pu = qr.getString("pu");
                    BigDecimal outstanding = qr.getBigDecimal("out");
                    // set status LATE
                    dataManager.executeUpdate("UPDATE ecoxpert_loan_schedules SET status='LATE' WHERE id = ?", schedId).join();
                    // apply penalty on loan outstanding
                    if (outstanding != null && penalty > 0 && !penalizedLoans.contains(loanId)) {
                        BigDecimal newOut = outstanding.multiply(BigDecimal.valueOf(1.0 + penalty));
                        dataManager.executeUpdate("UPDATE ecoxpert_loans SET outstanding = ? WHERE id = ?", newOut, loanId).join();
                        penalizedLoans.add(loanId);
                    }
                    // notify player (if online)
                    if (notify && pu != null) {
                        try {
                            UUID uuid = UUID.fromString(pu);
                            var p = Bukkit.getPlayer(uuid);
                            if (p != null && p.isOnline()) {
                                var tm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
                                p.sendMessage(tm.getMessage("prefix") + tm.getPlayerMessage(p, "loans.payment-overdue"));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Loan scheduler error: " + e.getMessage());
        }
    }
}
