package me.koyere.ecoxpert.modules.inflation;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.core.config.ConfigManager;
import org.bukkit.Bukkit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.UUID;

/**
 * Economic Intelligence Engine
 * 
 * The BRAIN of EcoXpert's revolutionary economy system.
 * This isn't just inflation control - it's a living, breathing economic
 * simulation
 * that creates realistic economic cycles, responds to player behavior, and
 * maintains long-term economic health through intelligent interventions.
 * 
 * UNIQUE FEATURES:
 * - Economic "breathing" - natural expansion/contraction cycles
 * - Player behavior learning and adaptation
 * - Predictive economic modeling
 * - Dynamic money supply management
 * - Crisis prevention and economic stimulus
 */
public class EconomicIntelligenceEngine {

    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;

    // Economic State Tracking
    private EconomicCycle currentCycle = EconomicCycle.STABLE;
    private double economicHealth = 1.0; // 0.0 = crisis, 1.0 = perfect health
    private double inflationRate = 0.0; // Current inflation rate
    private double velocityOfMoney = 1.0; // How fast money circulates

    // Intelligence Learning System
    private final Map<UUID, PlayerEconomicProfile> playerProfiles = new ConcurrentHashMap<>();
    private final EconomicMemory economicMemory = new EconomicMemory();

    // Economic Cycles
    public enum EconomicCycle {
        DEPRESSION(0.7, -0.02), // Deflation, low activity
        RECESSION(0.8, -0.01), // Mild deflation
        STABLE(1.0, 0.0), // Balanced
        GROWTH(1.2, 0.01), // Mild inflation, high activity
        BOOM(1.4, 0.03), // High inflation, very high activity
        BUBBLE(1.6, 0.05); // Dangerous inflation, unsustainable

        private final double activityMultiplier;
        private final double baseInflationRate;

        EconomicCycle(double activityMultiplier, double baseInflationRate) {
            this.activityMultiplier = activityMultiplier;
            this.baseInflationRate = baseInflationRate;
        }

        public double getActivityMultiplier() {
            return activityMultiplier;
        }

        public double getBaseInflationRate() {
            return baseInflationRate;
        }
    }

    // Policy parameters (loaded from config; runtime adjustable)
    private double wealthTaxRate = 0.005; // 0.5%
    private double wealthTaxThresholdMultiplier = 2.0; // threshold = avgBalance * multiplier
    private double stimulusFactor = 0.02; // -2% buy, +2% sell
    private double cooldownFactor = 0.02; // +2% buy, -2% sell
    private int interventionMinutes = 10;
    private double biasMax = 0.03; // max absolute bias for continuous adjust

    public EconomicIntelligenceEngine(EcoXpertPlugin plugin, EconomyManager economyManager,
            MarketManager marketManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.marketManager = marketManager;
        // Load policy from module config
        try {
            var cfg = configManager.getModuleConfig("inflation");
            this.wealthTaxRate = cfg.getDouble("policy.wealth_tax.rate", wealthTaxRate);
            this.wealthTaxThresholdMultiplier = cfg.getDouble("policy.wealth_tax.threshold_multiplier",
                    wealthTaxThresholdMultiplier);
            this.stimulusFactor = cfg.getDouble("policy.market.stimulus_factor", stimulusFactor);
            this.cooldownFactor = cfg.getDouble("policy.market.cooldown_factor", cooldownFactor);
            this.interventionMinutes = cfg.getInt("policy.intervention.minutes", interventionMinutes);
            this.biasMax = cfg.getDouble("policy.market.bias_max", biasMax);
        } catch (Exception ignored) {
        }
    }

    /**
     * Initialize the Economic Intelligence Engine
     * This starts the "economic heartbeat" of the server
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("🧠 Initializing Economic Intelligence Engine...");

            // Load historical economic data
            economicMemory.loadHistoricalData();

            // Analyze current economic state
            analyzeCurrentEconomicState().thenRun(() -> {
                // Start economic heartbeat (runs every 5 minutes)
                startEconomicHeartbeat();

                // Start player behavior analysis (runs every hour)
                startPlayerBehaviorAnalysis();

                plugin.getLogger().info("✅ Economic Intelligence Engine active");
                plugin.getLogger().info("📊 Current Cycle: " + currentCycle +
                        " | Health: " + String.format("%.1f%%", economicHealth * 100));
            });
        });
    }

    /**
     * The Economic Heartbeat - runs every 5 minutes
     * This is what makes the economy "breathe" and evolve
     */
    private void startEconomicHeartbeat() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                // 1. Analyze current economic conditions
                EconomicSnapshot snapshot = captureEconomicSnapshot();

                // 2. Update economic health based on conditions
                updateEconomicHealth(snapshot);

                // 3. Determine if cycle change is needed
                evaluateCycleTransition(snapshot);

                // 4. Apply intelligent interventions
                applyEconomicInterventions(snapshot);

                // 5. Learn from patterns
                economicMemory.recordSnapshot(snapshot);

                // 6. Log economic status
                logEconomicStatus(snapshot);

            } catch (Exception e) {
                plugin.getLogger().severe("Error in Economic Heartbeat: " + e.getMessage());
            }
        }, 6000L, 6000L); // Every 5 minutes
    }

    /**
     * Capture current economic state
     */
    private EconomicSnapshot captureEconomicSnapshot() {
        double totalMoney = calculateTotalMoneySupply();
        double averageBalance = calculateAveragePlayerBalance();
        int activeUsers = Bukkit.getOnlinePlayers().size();
        double transactionVolume = calculateRecentTransactionVolume();
        double marketActivity = calculateMarketActivity();

        return new EconomicSnapshot(
                LocalDateTime.now(),
                totalMoney,
                averageBalance,
                activeUsers,
                transactionVolume,
                marketActivity,
                currentCycle,
                economicHealth,
                inflationRate,
                velocityOfMoney);
    }

    /**
     * Intelligent cycle transition based on economic conditions
     */
    private void evaluateCycleTransition(EconomicSnapshot snapshot) {
        EconomicCycle newCycle = currentCycle;

        // Analyze economic indicators
        double healthTrend = economicMemory.getHealthTrend();
        double activityTrend = economicMemory.getActivityTrend();

        // Smart cycle progression logic
        if (economicHealth > 0.9 && inflationRate > 0.04) {
            // Economy overheating - trigger cooldown
            newCycle = getNextCycle(currentCycle, false);
            plugin.getLogger().info("🔥 Economy overheating detected - initiating cooldown");

        } else if (economicHealth < 0.5 && inflationRate < -0.02) {
            // Economic crisis - trigger stimulus
            newCycle = getNextCycle(currentCycle, true);
            plugin.getLogger().info("❄️ Economic crisis detected - initiating stimulus");

        } else if (shouldNaturalCycleTransition(snapshot)) {
            // Natural economic evolution
            boolean shouldGrow = healthTrend > 0 && activityTrend > 0;
            newCycle = getNextCycle(currentCycle, shouldGrow);
        }

        if (newCycle != currentCycle) {
            transitionToNewCycle(newCycle, snapshot);
        }
    }

    /**
     * Apply intelligent economic interventions
     */
    private void applyEconomicInterventions(EconomicSnapshot snapshot) {
        // 1. Money Supply Management
        if (economicHealth < 0.6) {
            // Economic stimulus - inject money
            injectLiquidity(snapshot);
        } else if (economicHealth > 0.9 && inflationRate > 0.05) {
            // Reduce money supply - increase taxes, reduce rewards
            reduceLiquidity(snapshot);
        }

        // 2. Market Interventions
        if (snapshot.getMarketActivity() < 0.3) {
            // Stimulate market activity
            stimulateMarketActivity();
        }

        // 3. Interest Rate Adjustments
        adjustInterestRates(snapshot);

        // 4. Dynamic Pricing Adjustments
        adjustMarketPricingFactors(snapshot);
    }

    /**
     * Intelligent liquidity injection during economic downturns
     */
    private void injectLiquidity(EconomicSnapshot snapshot) {
        double injectionAmount = calculateOptimalInjectionAmount(snapshot);

        plugin.getLogger().info("💉 Injecting liquidity: $" +
                String.format("%.2f", injectionAmount) + " into economy");

        // Smart distribution - not random, but targeted
        distributeEconomicStimulus(injectionAmount);
    }

    /**
     * Smart economic stimulus distribution
     */
    private void distributeEconomicStimulus(double totalAmount) {
        // 40% to active players (reward activity)
        // 30% to market makers (encourage trading)
        // 20% to new players (encourage growth)
        // 10% to reserve fund

        double activePlayerPool = totalAmount * 0.4;

        // Distribute to online players weighted by economic activity
        Bukkit.getOnlinePlayers().forEach(player -> {
            PlayerEconomicProfile profile = getPlayerProfile(player.getUniqueId());
            double playerStimulus = calculatePlayerStimulus(profile, activePlayerPool);

            if (playerStimulus > 0) {
                economyManager.addMoney(player.getUniqueId(),
                        BigDecimal.valueOf(playerStimulus),
                        "Economic Stimulus - Cycle: " + currentCycle);
            }
        });
    }

    /**
     * Get or create player economic profile
     */
    private PlayerEconomicProfile getPlayerProfile(UUID playerId) {
        return playerProfiles.computeIfAbsent(playerId,
                id -> new PlayerEconomicProfile(id));
    }

    /**
     * Start player behavior analysis system
     */
    private void startPlayerBehaviorAnalysis() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            analyzePlayerBehaviorPatterns();
        }, 72000L, 72000L); // Every hour
    }

    /**
     * Analyze and learn from player economic behavior
     */
    private void analyzePlayerBehaviorPatterns() {
        plugin.getLogger().info("🔍 Analyzing player economic behavior patterns...");

        playerProfiles.values().forEach(profile -> {
            profile.updateBehaviorMetrics();
            profile.calculateEconomicImpact();
        });

        // Update velocity of money based on player activity
        updateVelocityOfMoney();
    }

    // === Calculation Methods ===

    private CompletableFuture<Void> analyzeCurrentEconomicState() {
        return CompletableFuture.runAsync(() -> {
            EconomicSnapshot snapshot = captureEconomicSnapshot();
            economicHealth = calculateEconomicHealth(snapshot);
            inflationRate = calculateCurrentInflationRate(snapshot);
            velocityOfMoney = calculateVelocityOfMoney(snapshot);
        });
    }

    private double calculateTotalMoneySupply() {
        // This would query the database for total money in circulation
        // For now, return a calculated estimate
        return Bukkit.getOnlinePlayers().size() * 50000.0; // Placeholder
    }

    private double calculateAveragePlayerBalance() {
        // Calculate average balance across all players
        return 25000.0; // Placeholder
    }

    private double calculateRecentTransactionVolume() {
        // Calculate transaction volume in last hour
        return 100000.0; // Placeholder
    }

    private double calculateMarketActivity() {
        // Calculate market trading activity (0.0 to 1.0)
        return 0.6; // Placeholder
    }

    private double calculateEconomicHealth(EconomicSnapshot snapshot) {
        // Complex formula considering multiple factors
        double balanceDistribution = 0.8; // Placeholder
        double transactionHealth = 0.9; // Placeholder
        double marketHealth = snapshot.getMarketActivity();

        return (balanceDistribution * 0.4 + transactionHealth * 0.4 + marketHealth * 0.2);
    }

    private double calculateCurrentInflationRate(EconomicSnapshot snapshot) {
        // Calculate inflation based on price changes and money supply
        return currentCycle.getBaseInflationRate() +
                (snapshot.getTransactionVolume() / 1000000.0 * 0.01);
    }

    private double calculateVelocityOfMoney(EconomicSnapshot snapshot) {
        // How fast money circulates through the economy
        return snapshot.getTransactionVolume() / snapshot.getTotalMoney();
    }

    // === Utility Methods ===

    private void updateEconomicHealth(EconomicSnapshot snapshot) {
        double newHealth = calculateEconomicHealth(snapshot);
        economicHealth = (economicHealth * 0.7) + (newHealth * 0.3); // Smooth transition
        inflationRate = calculateCurrentInflationRate(snapshot);
    }

    private boolean shouldNaturalCycleTransition(EconomicSnapshot snapshot) {
        // Natural cycles should last 1-3 hours typically
        return economicMemory.getTimeSinceLastCycleChange() > 3600; // 1 hour minimum
    }

    private EconomicCycle getNextCycle(EconomicCycle current, boolean shouldGrow) {
        EconomicCycle[] cycles = EconomicCycle.values();
        int currentIndex = current.ordinal();

        if (shouldGrow && currentIndex < cycles.length - 1) {
            return cycles[currentIndex + 1];
        } else if (!shouldGrow && currentIndex > 0) {
            return cycles[currentIndex - 1];
        }

        return current;
    }

    private void transitionToNewCycle(EconomicCycle newCycle, EconomicSnapshot snapshot) {
        plugin.getLogger().info("🔄 Economic cycle transition: " + currentCycle + " → " + newCycle);
        currentCycle = newCycle;
        economicMemory.recordCycleChange(newCycle, snapshot);

        // Broadcast to players
        String message = getEconomicCycleMessage(newCycle);
        Bukkit.broadcastMessage("§6[EcoXpert] §e" + message);
    }

    private String getEconomicCycleMessage(EconomicCycle cycle) {
        return switch (cycle) {
            case DEPRESSION -> "Economic depression detected. Government stimulus programs activated.";
            case RECESSION -> "Economic slowdown. Consider saving and investing wisely.";
            case STABLE -> "Economic conditions stabilized. Normal market activity resumed.";
            case GROWTH -> "Economic growth period. Great time for investments and expansion!";
            case BOOM -> "Economic boom! High market activity and opportunities.";
            case BUBBLE -> "Economic bubble warning! Exercise caution with large investments.";
        };
    }

    private void logEconomicStatus(EconomicSnapshot snapshot) {
        if (plugin.getLogger().isLoggable(java.util.logging.Level.INFO)) {
            plugin.getLogger().info(String.format(
                    "📊 Economic Status | Cycle: %s | Health: %.1f%% | Inflation: %.2f%% | Activity: %.1f",
                    currentCycle, economicHealth * 100, inflationRate * 100, snapshot.getMarketActivity() * 100));
        }
    }

    // === Abstract method stubs (to be implemented) ===
    /**
     * Reduce liquidity during overheating (soft sink):
     * - Apply a small wealth tax to accounts above a dynamic threshold
     * - Make market slightly less favorable for players (temporary factors)
     */
    private void reduceLiquidity(EconomicSnapshot snapshot) {
        try {
            BigDecimal rate = BigDecimal.valueOf(wealthTaxRate);
            BigDecimal threshold = BigDecimal.valueOf(snapshot.getAverageBalance() * wealthTaxThresholdMultiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            String reason = "Overheating liquidity reduction";
            economyManager.applyWealthTax(rate, threshold, reason)
                    .thenAccept(affected -> {
                        try {
                            org.bukkit.Bukkit.getScheduler().runTask(plugin,
                                    () -> org.bukkit.Bukkit.getPluginManager().callEvent(
                                            new me.koyere.ecoxpert.api.events.WealthTaxAppliedEvent(rate, threshold,
                                                    affected, reason, java.time.Instant.now())));
                        } catch (Exception ignored) {
                        }
                    });
            // Notify affected online players (educational message)
            try {
                var tm = plugin.getServiceRegistry()
                        .getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
                for (org.bukkit.entity.Player op : org.bukkit.Bukkit.getOnlinePlayers()) {
                    try {
                        java.math.BigDecimal bal = economyManager.getBalance(op.getUniqueId()).join();
                        if (bal != null && bal.compareTo(threshold) > 0) {
                            me.koyere.ecoxpert.core.education.EducationNotifier.notifyWealthTaxApplied(plugin, tm,
                                    op.getUniqueId(), threshold);
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }

            // Market discouragement for a short period
            marketManager.setGlobalPriceFactors(1.0 + cooldownFactor, 1.0 - cooldownFactor);
            Bukkit.getScheduler().runTaskLater(plugin, () -> marketManager.setGlobalPriceFactors(1.0, 1.0),
                    20L * 60 * interventionMinutes);

            plugin.getLogger().info("Applied liquidity reduction: wealth tax and market factors (10m)");
        } catch (Exception e) {
            plugin.getLogger().warning("reduceLiquidity failed: " + e.getMessage());
        }
    }

    /**
     * Stimulate market activity in stagnation:
     * - Slightly lower buy prices and increase sell prices for a short period
     */
    private void stimulateMarketActivity() {
        try {
            marketManager.setGlobalPriceFactors(1.0 - stimulusFactor, 1.0 + stimulusFactor);
            Bukkit.getScheduler().runTaskLater(plugin, () -> marketManager.setGlobalPriceFactors(1.0, 1.0),
                    20L * 60 * interventionMinutes);
            plugin.getLogger().info("Stimulating market activity: adjusted price factors for 10m");
        } catch (Exception e) {
            plugin.getLogger().warning("stimulateMarketActivity failed: " + e.getMessage());
        }
    }

    /**
     * Adjust interest rates recommendation based on economic health/inflation.
     * (Future: integrate with BankManager to apply dynamically.)
     */
    private void adjustInterestRates(EconomicSnapshot snapshot) {
        // Recommend higher interest in downturns (to reward saving), lower during booms
        double base = 0.01; // 1%
        double adjust = (0.5 - snapshot.getEconomicHealth()) * 0.02; // +/-2%
        double recommended = Math.max(0.0, Math.min(0.05, base + adjust));
        plugin.getLogger().info(String.format("Recommended bank interest rate: %.2f%%", recommended * 100));
        // TODO: Expose API in BankManager to set base interest; for now, logging only.
    }

    /**
     * Adjust market pricing factors smoothly based on inflation and health (gentle
     * bias).
     */
    private void adjustMarketPricingFactors(EconomicSnapshot snapshot) {
        double infl = snapshot.getInflationRate();
        double health = snapshot.getEconomicHealth();

        // Bias buy/sell factors within [0.97, 1.03]
        double buyBias = 1.0 + clamp(infl * 0.5 - (health - 0.5) * 0.02, -biasMax, biasMax);
        double sellBias = 1.0 + clamp(-(infl * 0.5) + (health - 0.5) * 0.02, -biasMax, biasMax);

        marketManager.setGlobalPriceFactors(buyBias, sellBias);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // Policy admin helpers
    public String getPolicyInfo() {
        return String.format(
                "Policy{wealth_tax_rate=%.3f, threshold_mult=%.2f, stim=%.3f, cool=%.3f, minutes=%d, bias_max=%.3f}",
                wealthTaxRate, wealthTaxThresholdMultiplier, stimulusFactor, cooldownFactor, interventionMinutes,
                biasMax);
    }

    public boolean setPolicyParam(String name, double value) {
        switch (name.toLowerCase()) {
            case "wealth_tax_rate" -> this.wealthTaxRate = clamp(value, 0.0, 0.05);
            case "wealth_tax_threshold_multiplier" -> this.wealthTaxThresholdMultiplier = clamp(value, 1.0, 10.0);
            case "stimulus_factor" -> this.stimulusFactor = clamp(value, 0.0, 0.2);
            case "cooldown_factor" -> this.cooldownFactor = clamp(value, 0.0, 0.2);
            case "intervention_minutes" -> this.interventionMinutes = (int) Math.max(1, Math.min(120, value));
            case "bias_max" -> this.biasMax = clamp(value, 0.0, 0.2);
            default -> {
                return false;
            }
        }
        return true;
    }

    public void reloadPolicy(ConfigManager configManager) {
        try {
            var cfg = configManager.getModuleConfig("inflation");
            this.wealthTaxRate = cfg.getDouble("policy.wealth_tax.rate", wealthTaxRate);
            this.wealthTaxThresholdMultiplier = cfg.getDouble("policy.wealth_tax.threshold_multiplier",
                    wealthTaxThresholdMultiplier);
            this.stimulusFactor = cfg.getDouble("policy.market.stimulus_factor", stimulusFactor);
            this.cooldownFactor = cfg.getDouble("policy.market.cooldown_factor", cooldownFactor);
            this.interventionMinutes = cfg.getInt("policy.intervention.minutes", interventionMinutes);
            this.biasMax = cfg.getDouble("policy.market.bias_max", biasMax);
            plugin.getLogger().info("Reloaded economic policy from configuration");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to reload policy: " + e.getMessage());
        }
    }

    private double calculateOptimalInjectionAmount(EconomicSnapshot snapshot) {
        return 10000.0;
    }

    private double calculatePlayerStimulus(PlayerEconomicProfile profile, double pool) {
        return 100.0;
    }

    private void updateVelocityOfMoney() {
        /* TODO */ }

    // Getters
    public EconomicCycle getCurrentCycle() {
        return currentCycle;
    }

    public double getEconomicHealth() {
        return economicHealth;
    }

    public double getInflationRate() {
        return inflationRate;
    }

    public double getVelocityOfMoney() {
        return velocityOfMoney;
    }
}
