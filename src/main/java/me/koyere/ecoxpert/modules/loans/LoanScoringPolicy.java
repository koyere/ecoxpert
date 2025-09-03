package me.koyere.ecoxpert.modules.loans;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

class LoanScoringPolicy {
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;

    LoanScoringPolicy(EcoXpertPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
    }

    LoanOffer computeOffer(UUID player, BigDecimal amount) {
        try {
            var cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class).getModuleConfig("loans");
            int minTerm = cfg.getInt("policy.term_days.min", 15);
            int maxTerm = cfg.getInt("policy.term_days.max", 90);
            BigDecimal minRate = new BigDecimal(cfg.getString("policy.rate.min", "0.02"));
            BigDecimal maxRate = new BigDecimal(cfg.getString("policy.rate.max", "0.15"));
            BigDecimal baseRate = new BigDecimal(cfg.getString("policy.rate.base", "0.05"));
            BigDecimal maxAmountMult = new BigDecimal(cfg.getString("policy.max_amount.multiplier_balance", "0.5"));
            BigDecimal maxAmountFloor = new BigDecimal(cfg.getString("policy.max_amount.floor", "500"));

            int score = computeScore(player);
            // Dynamic rate: base adjusted by score (higher score -> lower rate)
            BigDecimal scoreFactor = BigDecimal.valueOf((1000 - Math.min(1000, Math.max(300, score))) / 1000.0);
            BigDecimal rate = baseRate.add(scoreFactor.multiply(new BigDecimal("0.05"))).max(minRate).min(maxRate);

            // Max amount: floor + balance * multiplier
            BigDecimal balance = getBalance(player);
            BigDecimal maxAmount = maxAmountFloor.add(balance.multiply(maxAmountMult));
            if (amount.compareTo(maxAmount) > 0) {
                return new LoanOffer(false, amount, rate, minTerm, score, "Amount exceeds limit");
            }

            // Term: interpolate by score
            int termDays = (int) Math.round(minTerm + (maxTerm - minTerm) * (score / 1000.0));
            termDays = Math.max(minTerm, Math.min(maxTerm, termDays));

            return new LoanOffer(true, amount, rate, termDays, score, "OK");
        } catch (Exception e) {
            return new LoanOffer(false, amount, new BigDecimal("0.00"), 0, 0, "Error computing offer");
        }
    }

    CompletableFuture<Boolean> createScheduleFor(UUID player, BigDecimal amount, BigDecimal rate, int termDays) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Fetch last loan id for player (the one we just inserted)
                try (QueryResult qr = dataManager.executeQuery(
                    "SELECT id FROM ecoxpert_loans WHERE player_uuid = ? AND status='ACTIVE' ORDER BY id DESC LIMIT 1",
                    player.toString()).join()) {
                    if (!qr.next()) return false;
                    long loanId = qr.getLong("id");
                    // Total repay = principal * (1 + rate)
                    BigDecimal totalRepay = amount.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal daily = totalRepay.divide(BigDecimal.valueOf(termDays), 2, RoundingMode.HALF_UP);
                    LocalDate start = LocalDate.now().plusDays(1);
                    for (int i = 1; i <= termDays; i++) {
                        LocalDate due = start.plusDays(i - 1);
                        dataManager.executeUpdate(
                            "INSERT INTO ecoxpert_loan_schedules (loan_id, installment_no, due_date, amount_due, paid_amount, status) VALUES (?, ?, ?, ?, 0, 'PENDING')",
                            loanId, i, java.sql.Date.valueOf(due), daily
                        ).join();
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    List<LoanPayment> getSchedule(UUID player) {
        List<LoanPayment> out = new ArrayList<>();
        try (QueryResult qr = dataManager.executeQuery(
            "SELECT s.id, s.loan_id, s.installment_no, s.due_date, s.amount_due, s.paid_amount, s.status " +
                "FROM ecoxpert_loan_schedules s JOIN ecoxpert_loans l ON l.id = s.loan_id " +
                "WHERE l.player_uuid = ? AND l.status='ACTIVE' ORDER BY s.installment_no",
            player.toString()).join()) {
            while (qr.next()) {
                out.add(new LoanPayment(
                    qr.getLong("id"),
                    qr.getLong("loan_id"),
                    qr.getInt("installment_no"),
                    (qr.getTimestamp("due_date") != null
                        ? qr.getTimestamp("due_date").toLocalDateTime().toLocalDate()
                        : java.time.LocalDate.now()),
                    qr.getBigDecimal("amount_due"),
                    qr.getBigDecimal("paid_amount"),
                    qr.getString("status")
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    int computeScore(UUID player) {
        int score = 600; // base
        try {
            // Balance factor (0..200)
            BigDecimal bal = getBalance(player);
            double bonus = Math.min(200.0, Math.log10(bal.doubleValue() + 10) * 40.0);
            score += (int) bonus;

            // Income last 7 days (0..150)
            BigDecimal income = getIncomeLastDays(player, 7);
            score += Math.min(150, income.divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN).intValue() * 10);

            // Delinquency penalty (0..200)
            int delinq = getDelinquencies(player, 30);
            score -= Math.min(200, delinq * 40);
        } catch (Exception ignored) {}
        return Math.max(300, Math.min(1000, score));
    }

    private BigDecimal getBalance(UUID player) {
        try (QueryResult qr = dataManager.executeQuery(
            "SELECT balance FROM ecoxpert_accounts WHERE player_uuid = ?", player.toString()).join()) {
            if (qr.next()) return qr.getBigDecimal("balance");
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    private BigDecimal getIncomeLastDays(UUID player, int days) {
        try (QueryResult qr = dataManager.executeQuery(
            "SELECT SUM(amount) as total FROM ecoxpert_transactions WHERE to_uuid = ? AND created_at >= datetime('now', '-' || ? || ' days')",
            player.toString(), days).join()) {
            if (qr.next()) {
                BigDecimal sum = qr.getBigDecimal("total");
                return sum != null ? sum : BigDecimal.ZERO;
            }
        } catch (Exception ignored) {}
        return BigDecimal.ZERO;
    }

    private int getDelinquencies(UUID player, int days) {
        try (QueryResult qr = dataManager.executeQuery(
            "SELECT COUNT(*) as cnt FROM ecoxpert_loan_schedules s JOIN ecoxpert_loans l ON l.id = s.loan_id " +
                "WHERE l.player_uuid = ? AND s.status = 'LATE' AND s.due_date >= date('now', '-' || ? || ' days')",
            player.toString(), days).join()) {
            if (qr.next()) {
                Integer cnt = qr.getInt("cnt");
                if (cnt != null) return cnt;
                Long l = qr.getLong("cnt");
                return l != null ? l.intValue() : 0;
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
