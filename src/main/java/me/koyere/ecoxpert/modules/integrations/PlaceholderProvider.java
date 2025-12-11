package me.koyere.ecoxpert.modules.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.bank.BankAccountTier;
import me.koyere.ecoxpert.modules.bank.BankManager;
import me.koyere.ecoxpert.modules.bank.BankTransactionType;
import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.loans.Loan;
import me.koyere.ecoxpert.modules.loans.LoanManager;
import me.koyere.ecoxpert.modules.loans.LoanPayment;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.market.MarketStatistics;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * PlaceholderAPI provider for EcoXpert Pro
 *
 * Identifier: %ecox_<key>%
 * Supported keys (global):
 * - economy_health -> 0..100
 * - inflation_rate -> percent 0..100
 * - cycle -> current economic cycle
 * - market_activity -> 0..100
 * - events_active -> number of active events
 *
 * Player keys:
 * - balance -> formatted balance
 * - loans_outstanding -> formatted outstanding of active loan (or 0)
 */
public class PlaceholderProvider extends PlaceholderExpansion {

    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final BankManager bankManager;
    private final MarketManager marketManager;
    private final InflationManager inflationManager;
    private final EconomicEventEngine eventEngine;
    private final LoanManager loanManager;
    private final me.koyere.ecoxpert.modules.integrations.IntegrationsManager integrations;

    private static final Pattern BALTOP_BALANCE_PATTERN = Pattern
            .compile("(?:essentials_)?baltop_balance(?:_(commas|fixed|formatted))?_(\\d+)");
    private static final Pattern BALTOP_PLAYER_PATTERN = Pattern
            .compile("(?:essentials_)?baltop_player(?:_stripped)?_(\\d+)");

    private volatile List<EconomyManager.TopBalanceEntry> baltopCache = List.of();
    private volatile long baltopCacheTimestamp = 0L;

    public PlaceholderProvider(EcoXpertPlugin plugin,
            EconomyManager economyManager,
            BankManager bankManager,
            MarketManager marketManager,
            InflationManager inflationManager,
            EconomicEventEngine eventEngine,
            LoanManager loanManager,
            me.koyere.ecoxpert.modules.integrations.IntegrationsManager integrations,
            TranslationManager tm) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.bankManager = bankManager;
        this.marketManager = marketManager;
        this.inflationManager = inflationManager;
        this.eventEngine = eventEngine;
        this.loanManager = loanManager;
        this.integrations = integrations;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ecox";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Koyere";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        String key = params.toLowerCase(Locale.ROOT);
        try {
            Matcher balanceMatcher = BALTOP_BALANCE_PATTERN.matcher(key);
            if (balanceMatcher.matches()) {
                int rank = parseRank(balanceMatcher.group(2));
                if (rank <= 0)
                    return "";
                EconomyManager.TopBalanceEntry entry = getTopEntry(rank);
                String variant = balanceMatcher.group(1);
                return formatBaltopBalance(entry, variant);
            }

            Matcher playerMatcher = BALTOP_PLAYER_PATTERN.matcher(key);
            if (playerMatcher.matches()) {
                int rank = parseRank(playerMatcher.group(1));
                if (rank <= 0)
                    return "";
                EconomyManager.TopBalanceEntry entry = getTopEntry(rank);
                if (entry == null)
                    return "";
                return getPlayerName(entry.playerUuid());
            }

            if ("baltop_rank".equals(key) || "essentials_baltop_rank".equals(key)) {
                if (player == null)
                    return "0";
                int rank = economyManager.getBalanceRank(player.getUniqueId()).join();
                return Integer.toString(Math.max(rank, 0));
            }

            switch (key) {
                case "bank_balance": {
                    if (player == null || bankManager == null)
                        return "0";
                    BigDecimal bal = bankManager.getBalance(player.getUniqueId()).join();
                    return formatMoneySafe(bal);
                }
                case "bank_tier": {
                    if (player == null || bankManager == null)
                        return "";
                    BankAccountTier tier = bankManager.getAccountTier(player.getUniqueId()).join();
                    return tier != null ? tier.name() : "";
                }
                case "bank_interest_rate": {
                    if (player == null || bankManager == null)
                        return "0";
                    BankAccountTier tier = bankManager.getAccountTier(player.getUniqueId()).join();
                    if (tier == null)
                        return "0";
                    return formatPercent(tier.getAnnualInterestRate());
                }
                case "bank_daily_deposit_remaining": {
                    if (player == null || bankManager == null)
                        return "0";
                    BigDecimal remaining = bankManager
                            .getDailyLimitRemaining(player.getUniqueId(), BankTransactionType.DEPOSIT).join();
                    return formatMoneySafe(remaining);
                }
                case "bank_daily_withdraw_remaining": {
                    if (player == null || bankManager == null)
                        return "0";
                    BigDecimal remaining = bankManager
                            .getDailyLimitRemaining(player.getUniqueId(), BankTransactionType.WITHDRAW).join();
                    return formatMoneySafe(remaining);
                }
                case "bank_daily_transfer_remaining": {
                    if (player == null || bankManager == null)
                        return "0";
                    BigDecimal remaining = bankManager
                            .getDailyLimitRemaining(player.getUniqueId(), BankTransactionType.TRANSFER_OUT).join();
                    return formatMoneySafe(remaining);
                }
                case "loan_status": {
                    if (player == null)
                        return "NONE";
                    var opt = loanManager.getActiveLoan(player.getUniqueId()).join();
                    return opt.map(Loan::getStatus).orElse("NONE");
                }
                case "loan_principal": {
                    if (player == null)
                        return "0";
                    var opt = loanManager.getActiveLoan(player.getUniqueId()).join();
                    return formatMoneySafe(opt.map(Loan::getPrincipal).orElse(BigDecimal.ZERO));
                }
                case "loan_outstanding_raw": {
                    if (player == null)
                        return "0";
                    var opt = loanManager.getActiveLoan(player.getUniqueId()).join();
                    return opt.map(loan -> loan.getOutstanding().toPlainString()).orElse("0");
                }
                case "loan_interest_rate": {
                    if (player == null)
                        return "0";
                    var opt = loanManager.getActiveLoan(player.getUniqueId()).join();
                    return opt.map(loan -> formatPercent(loan.getInterestRate())).orElse("0");
                }
                case "loan_next_due_amount": {
                    if (player == null)
                        return "0";
                    var summary = getLoanScheduleSummary(player.getUniqueId());
                    return formatMoneySafe(summary.nextAmount());
                }
                case "loan_next_due_date": {
                    if (player == null)
                        return "";
                    var summary = getLoanScheduleSummary(player.getUniqueId());
                    return summary.nextDate() != null ? summary.nextDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            : "";
                }
                case "loan_next_due_in_days": {
                    if (player == null)
                        return "0";
                    var summary = getLoanScheduleSummary(player.getUniqueId());
                    if (summary.nextDate() == null)
                        return "0";
                    long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), summary.nextDate());
                    return Long.toString(days);
                }
                case "loan_installments_left": {
                    if (player == null)
                        return "0";
                    var summary = getLoanScheduleSummary(player.getUniqueId());
                    return Integer.toString(summary.installmentsLeft());
                }
            }

            switch (key) {
                case "velocity": {
                    double v = inflationManager.getVelocityOfMoney();
                    return String.format(Locale.US, "%.2f", v);
                }
                case "total_money": {
                    var snap = inflationManager.getCurrentSnapshot().join();
                    return String.format(Locale.US, "%.0f", snap.getTotalMoney());
                }
                case "avg_balance": {
                    var snap = inflationManager.getCurrentSnapshot().join();
                    return String.format(Locale.US, "%.0f", snap.getAverageBalance());
                }
                case "gini": {
                    var snap = inflationManager.getCurrentSnapshot().join();
                    return String.format(Locale.US, "%.3f", snap.getGiniCoefficient());
                }
                case "has_worldguard": {
                    return integrations != null && integrations.hasWorldGuard() ? "true" : "false";
                }
                case "has_lands": {
                    return integrations != null && integrations.hasLands() ? "true" : "false";
                }
                case "has_jobs": {
                    return integrations != null && integrations.hasJobs() ? "true" : "false";
                }
                case "has_towny": {
                    return integrations != null && integrations.hasTowny() ? "true" : "false";
                }
                case "towny_town": {
                    if (player == null)
                        return "";
                    return integrations != null ? integrations.getTownyTown(player.getPlayer()) : "";
                }
                case "has_slimefun": {
                    return integrations != null && integrations.hasSlimefun() ? "true" : "false";
                }
                case "has_mcmmo": {
                    return integrations != null && integrations.hasMcMMO() ? "true" : "false";
                }
                case "economy_health": {
                    double h = inflationManager.getEconomicHealth();
                    return String.format(Locale.US, "%.0f", h * 100.0);
                }
                case "inflation_rate": {
                    double r = inflationManager.getInflationRate();
                    return String.format(Locale.US, "%.2f", r * 100.0);
                }
                case "cycle": {
                    EconomicIntelligenceEngine.EconomicCycle c = inflationManager.getCurrentCycle();
                    return c != null ? c.name() : "UNKNOWN";
                }
                case "market_activity": {
                    MarketStatistics st = marketManager.getMarketStatistics().join();
                    return String.format(Locale.US, "%.0f", st.getMarketActivity() * 100.0);
                }
                case "events_active": {
                    return Integer.toString(eventEngine.getActiveEventsCount());
                }
                case "balance": {
                    if (player == null)
                        return "0";
                    BigDecimal bal = economyManager.getBalance(player.getUniqueId()).join();
                    return economyManager.formatMoney(bal != null ? bal : BigDecimal.ZERO);
                }
                case "loans_outstanding": {
                    if (player == null)
                        return "0";
                    var opt = loanManager.getActiveLoan(player.getUniqueId()).join();
                    BigDecimal out = opt.map(me.koyere.ecoxpert.modules.loans.Loan::getOutstanding)
                            .orElse(BigDecimal.ZERO);
                    return economyManager.formatMoney(out);
                }
                case "role": {
                    if (player == null)
                        return "";
                    try {
                        var prof = plugin.getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class)
                                .getRole(player.getUniqueId()).join();
                        return prof.map(Enum::name).orElse("");
                    } catch (Exception ignored) {
                        return "";
                    }
                }
                case "role_level": {
                    if (player == null)
                        return "1";
                    try {
                        var pm = plugin.getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
                        return Integer.toString(pm.getLevel(player.getUniqueId()).join());
                    } catch (Exception ignored) {
                        return "1";
                    }
                }
                case "role_xp": {
                    if (player == null)
                        return "0";
                    try {
                        var pm = plugin.getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
                        return Integer.toString(pm.getXp(player.getUniqueId()).join());
                    } catch (Exception ignored) {
                        return "0";
                    }
                }
                case "role_progress": {
                    if (player == null)
                        return "0";
                    try {
                        var cfg = plugin.getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class)
                                .getModuleConfig("professions");
                        var pm = plugin.getServiceRegistry()
                                .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
                        int level = pm.getLevel(player.getUniqueId()).join();
                        int xp = pm.getXp(player.getUniqueId()).join();
                        int maxLevel = Math.max(1, cfg.getInt("max_level", 5));
                        java.util.List<Integer> thresholds = cfg.getIntegerList("xp.level_thresholds");
                        if (thresholds == null || thresholds.isEmpty())
                            thresholds = java.util.Arrays.asList(0, 100, 250, 500, 1000, 2000);
                        int curIdx = Math.max(0, Math.min(level - 1, thresholds.size() - 1));
                        int curBase = thresholds.get(curIdx);
                        if (level >= maxLevel)
                            return "100";
                        int nextIdx = Math.min(level, thresholds.size() - 1);
                        int nextReq = thresholds.get(nextIdx);
                        int num = Math.max(0, xp - curBase);
                        int den = Math.max(1, nextReq - curBase);
                        int progress = (int) Math.max(0, Math.min(100, Math.floor((num * 100.0) / den)));
                        return Integer.toString(progress);
                    } catch (Exception ignored) {
                        return "0";
                    }
                }
                case "role_bonus_buy": {
                    if (player == null)
                        return "0";
                    try {
                        return roleBonus(player.getUniqueId(), true);
                    } catch (Exception ignored) {
                        return "0";
                    }
                }
                case "role_bonus_sell": {
                    if (player == null)
                        return "0";
                    try {
                        return roleBonus(player.getUniqueId(), false);
                    } catch (Exception ignored) {
                        return "0";
                    }
                }
                case "wg_regions": {
                    if (player == null)
                        return "";
                    return integrations != null ? integrations.getWorldGuardRegions(player.getPlayer()) : "";
                }
                case "lands_land": {
                    if (player == null)
                        return "";
                    return integrations != null ? integrations.getLandsLand(player.getPlayer()) : "";
                }
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String roleBonus(java.util.UUID uuid, boolean isBuy) {
        try {
            var pm = plugin.getServiceRegistry()
                    .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            var roleOpt = pm.getRole(uuid).join();
            if (roleOpt.isEmpty())
                return "0";
            var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class)
                    .getModuleConfig("professions");
            int level = pm.getLevel(uuid).join();
            int maxLevel = cfg.getInt("max_level", 5);
            level = Math.max(1, Math.min(level, maxLevel));
            double perLevel = cfg.getDouble("roles." + roleOpt.get().name().toLowerCase() + "."
                    + (isBuy ? "buy_bonus_per_level" : "sell_bonus_per_level"), 0.0);
            double total = perLevel * (level - 1);
            return String.format(java.util.Locale.US, "%.3f", total);
        } catch (Exception e) {
            return "0";
        }
    }

    private EconomyManager.TopBalanceEntry getTopEntry(int rank) {
        try {
            List<EconomyManager.TopBalanceEntry> cache = refreshBaltopCache(rank);
            if (rank <= cache.size()) {
                return cache.get(rank - 1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<EconomyManager.TopBalanceEntry> refreshBaltopCache(int requiredRank) {
        long now = System.currentTimeMillis();
        if ((now - baltopCacheTimestamp) > 30_000 || baltopCache.size() < requiredRank) {
            int limit = Math.max(requiredRank, 25);
            baltopCache = new ArrayList<>(economyManager.getTopBalances(limit).join());
            baltopCacheTimestamp = now;
        }
        return baltopCache;
    }

    private int parseRank(String raw) {
        try {
            int rank = Integer.parseInt(raw);
            return rank > 0 ? rank : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatBaltopBalance(EconomyManager.TopBalanceEntry entry, String variant) {
        if (entry == null) {
            return "0";
        }
        BigDecimal balance = entry.balance();
        if (balance == null) {
            return "0";
        }

        if (variant == null) {
            return balance.toPlainString();
        }

        switch (variant) {
            case "commas": {
                DecimalFormat df = new DecimalFormat("#,##0.##");
                df.setRoundingMode(RoundingMode.HALF_UP);
                return df.format(balance);
            }
            case "fixed": {
                return balance.setScale(2, RoundingMode.HALF_UP).toPlainString();
            }
            case "formatted": {
                return economyManager.formatMoney(balance);
            }
            default:
                return balance.toPlainString();
        }
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        String name = off != null ? off.getName() : null;
        return name != null ? name : "";
    }

    private String formatMoneySafe(BigDecimal value) {
        if (value == null)
            return "0";
        try {
            return economyManager.formatMoney(value);
        } catch (Exception e) {
            return value.toPlainString();
        }
    }

    private String formatPercent(BigDecimal rate) {
        if (rate == null)
            return "0";
        return String.format(Locale.US, "%.2f", rate.multiply(BigDecimal.valueOf(100)));
    }

    private LoanScheduleSummary getLoanScheduleSummary(UUID uuid) {
        List<LoanPayment> schedule = loanManager.getSchedule(uuid).join();
        LoanPayment next = null;
        int remaining = 0;
        for (LoanPayment p : schedule) {
            boolean paid = "PAID".equalsIgnoreCase(p.status()) || p.paidAmount().compareTo(p.amountDue()) >= 0;
            if (!paid) {
                remaining++;
                if (next == null) {
                    next = p;
                }
            }
        }
        BigDecimal nextAmount = next != null ? next.amountDue().subtract(next.paidAmount()) : BigDecimal.ZERO;
        LocalDate nextDate = next != null ? next.dueDate() : null;
        return new LoanScheduleSummary(nextAmount, nextDate, remaining);
    }

    private record LoanScheduleSummary(BigDecimal nextAmount, LocalDate nextDate, int installmentsLeft) {
    }
}
