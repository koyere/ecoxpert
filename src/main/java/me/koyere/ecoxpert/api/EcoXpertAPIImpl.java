package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.api.services.*;
import me.koyere.ecoxpert.api.dto.*;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.UUID;

/**
 * Professional implementation of EcoXpert Pro Public API
 *
 * Provides access to all plugin services through a unified professional API interface.
 * Implementation uses separate service classes for maintainability and clarity.
 */
@Singleton
public class EcoXpertAPIImpl implements EcoXpertAPI {

    private final EcoXpertPlugin plugin;

    @Inject
    public EcoXpertAPIImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAPIVersion() {
        return "1.0.0";
    }

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isReady() {
        return plugin.isEnabled();
    }

    @Override
    public EconomyService getEconomyService() {
        try {
            var economyManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.economy.EconomyManager.class);
            return new EconomyServiceImpl(economyManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get EconomyService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public MarketService getMarketService() {
        try {
            var marketManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.market.MarketManager.class);
            return new MarketServiceImpl(marketManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get MarketService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public BankingService getBankingService() {
        try {
            var bankManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.bank.BankManager.class);
            return new BankingServiceImpl(bankManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get BankingService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public LoanService getLoanService() {
        try {
            var loanManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.loans.LoanManager.class);
            return new LoanServiceImpl(loanManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get LoanService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public EventsService getEventsService() {
        try {
            var eventEngine = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.events.EconomicEventEngine.class);
            return new EventsServiceImpl(eventEngine);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get EventsService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ProfessionService getProfessionService() {
        try {
            var professionsManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.professions.ProfessionsManager.class);
            return new ProfessionServiceImpl(professionsManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get ProfessionService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public InflationService getInflationService() {
        try {
            var inflationManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
            var dataManager = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            return new InflationServiceImpl(inflationManager, dataManager);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get InflationService: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ServerEconomySnapshot getServerEconomics() {
        try {
            var sr = plugin.getServiceRegistry();
            var inflationManager = sr.getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
            var marketManager = sr.getInstance(me.koyere.ecoxpert.modules.market.MarketManager.class);
            var eventEngine = sr.getInstance(me.koyere.ecoxpert.modules.events.EconomicEventEngine.class);

            double health = inflationManager.getEconomicHealth();
            double inflRate = inflationManager.getInflationRate();
            var cycle = inflationManager.getCurrentCycle();

            double activity = 0.0;
            try {
                var stats = marketManager.getMarketStatistics().join();
                activity = stats.getMarketActivity();
            } catch (Exception ignored) {}

            int activeEvents = 0;
            try {
                activeEvents = eventEngine.getActiveEventsCount();
            } catch (Exception ignored) {}

            return new ServerEconomySnapshot(
                cycle != null ? cycle.name() : "UNKNOWN",
                health,
                inflRate,
                activity,
                activeEvents
            );
        } catch (Exception e) {
            return new ServerEconomySnapshot("UNKNOWN", 0.0, 0.0, 0.0, 0);
        }
    }

    @Override
    public CycleForecast forecastCycle(Duration horizon) {
        try {
            var sr = plugin.getServiceRegistry();
            var inflationManager = sr.getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
            var configManager = sr.getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);

            var cfg = configManager.getModuleConfig("inflation");
            double targetInfl = cfg.getDouble("targets.inflation", 1.02);

            var forecast = inflationManager.getEconomicForecast();
            if (forecast == null) {
                return new CycleForecast(
                    me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.STABLE,
                    0.5,
                    horizon
                );
            }

            double health = forecast.getPredictedHealth();
            double inflRate = forecast.getPredictedInflation();
            double confidence = forecast.getConfidence();
            var cycle = classifyCycle(health, inflRate, targetInfl);

            return new CycleForecast(cycle, confidence, horizon);
        } catch (Exception e) {
            return new CycleForecast(
                me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.STABLE,
                0.0,
                horizon
            );
        }
    }

    @Override
    public PlayerEconomyView getPlayerEconomyView(UUID playerId) {
        try {
            var sr = plugin.getServiceRegistry();
            var economyManager = sr.getInstance(me.koyere.ecoxpert.economy.EconomyManager.class);
            var inflationManager = sr.getInstance(me.koyere.ecoxpert.modules.inflation.InflationManager.class);
            var dataManager = sr.getInstance(me.koyere.ecoxpert.core.data.DataManager.class);

            java.math.BigDecimal balance = economyManager.getBalance(playerId).join();
            if (balance == null) balance = java.math.BigDecimal.ZERO;

            // Calculate wealth percentile
            double percentile = 0.0;
            try (var qr = dataManager.executeQuery(
                "SELECT COUNT(*) AS t, SUM(CASE WHEN balance <= ? THEN 1 ELSE 0 END) AS le FROM ecoxpert_accounts",
                balance
            ).join()) {
                if (qr.next()) {
                    int total = 0;
                    int lessOrEqual = 0;

                    Integer ti = qr.getInt("t");
                    if (ti != null) total = ti;
                    else {
                        Long tl = qr.getLong("t");
                        total = tl != null ? tl.intValue() : 0;
                    }

                    Integer lei = qr.getInt("le");
                    if (lei != null) lessOrEqual = lei;
                    else {
                        Long lel = qr.getLong("le");
                        lessOrEqual = lel != null ? lel.intValue() : 0;
                    }

                    if (total > 0) {
                        percentile = Math.max(0.0, Math.min(1.0, (double) lessOrEqual / (double) total));
                    }
                }
            } catch (Exception ignored) {}

            var profile = inflationManager.getPlayerProfile(playerId);
            double risk = profile != null ? profile.getRiskScore() : 0.5;
            double futureBalance = profile != null ? profile.getPredictedFutureBalance() : balance.doubleValue();

            return new PlayerEconomyView(playerId, balance, percentile, risk, futureBalance);
        } catch (Exception e) {
            return new PlayerEconomyView(playerId, java.math.BigDecimal.ZERO, 0.0, 0.5, 0.0);
        }
    }

    /**
     * Classify economic cycle based on health and inflation
     */
    private me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle classifyCycle(
        double health, double inflRate, double targetInfl) {

        if (health >= 0.85) {
            if (inflRate >= targetInfl + 0.05)
                return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.BUBBLE;
            if (inflRate >= targetInfl + 0.01)
                return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.BOOM;
            return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.GROWTH;
        }

        if (health >= 0.6) {
            if (inflRate >= targetInfl + 0.03)
                return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.BOOM;
            if (inflRate <= targetInfl - 0.03)
                return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.RECESSION;
            return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.STABLE;
        }

        if (health >= 0.4) {
            if (inflRate <= targetInfl - 0.05)
                return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.RECESSION;
            return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.RECESSION;
        }

        if (inflRate <= targetInfl - 0.08)
            return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.DEPRESSION;

        return me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine.EconomicCycle.RECESSION;
    }
}
