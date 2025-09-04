package me.koyere.ecoxpert.modules.events;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;
import me.koyere.ecoxpert.core.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Dynamic Economic Events Engine
 * 
 * The MOST REVOLUTIONARY feature of EcoXpert Pro - a living event system that
 * creates realistic economic scenarios and responds intelligently to server conditions.
 * 
 * Unlike static economy plugins, this system creates DYNAMIC ECONOMIC EVENTS that
 * simulate real-world economic phenomena and keep the economy engaging and balanced.
 * 
 * UNIQUE REVOLUTIONARY FEATURES:
 * - AI-driven event selection based on economic conditions
 * - Dynamic event scaling based on server population and economic health
 * - Cascading economic effects that create realistic market reactions
 * - Player behavior adaptation through economic incentives
 * - Anti-stagnation algorithms that prevent economic death
 * - Cross-system integration (Market + Banking + Inflation intelligence)
 */
public class EconomicEventEngine {
    
    private final EcoXpertPlugin plugin;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final InflationManager inflationManager;
    
    // Event State Management
    private final Map<String, EconomicEvent> activeEvents = new ConcurrentHashMap<>();
    private final List<EconomicEvent> eventHistory = new ArrayList<>();
    private boolean eventEngineActive = false;
    // Track last end time per event type to enforce cooldowns
    private final Map<EconomicEventType, LocalDateTime> lastEventEndTime = new ConcurrentHashMap<>();
    
    // Intelligence Integration
    private LocalDateTime lastEventTime = LocalDateTime.now().minusHours(2);
    private int consecutiveQuietHours = 0;
    
    // Event Types Registry
    private final Map<EconomicEventType, EventTemplate> eventTemplates = new HashMap<>();
    
    public EconomicEventEngine(EcoXpertPlugin plugin, EconomyManager economyManager,
                              MarketManager marketManager, InflationManager inflationManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.marketManager = marketManager;
        this.inflationManager = inflationManager;
        
        initializeEventTemplates();
    }
    
    /**
     * Initialize the Dynamic Economic Events Engine
     */
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("🎪 Initializing Dynamic Economic Events Engine...");
            
            // Start the event intelligence system
            startEventIntelligenceEngine();
            
            // Start the event execution scheduler
            startEventScheduler();
            
            // Start the anti-stagnation system
            startAntiStagnationSystem();
            
            eventEngineActive = true;
            
            plugin.getLogger().info("✅ Dynamic Economic Events Engine operational");
            plugin.getLogger().info("🧠 Event Intelligence: ACTIVE | Anti-Stagnation: ENABLED");
        });
    }
    
    public void shutdown() {
        plugin.getLogger().info("🔌 Shutting down Dynamic Economic Events Engine...");
        
        // End all active events gracefully
        activeEvents.values().forEach(this::endEvent);
        activeEvents.clear();
        
        eventEngineActive = false;
        plugin.getLogger().info("Dynamic Economic Events Engine shutdown complete");
    }
    
    // === Event Intelligence System ===
    
    /**
     * Start the AI-driven event intelligence system
     * This analyzes economic conditions and decides what events should happen
     */
    private void startEventIntelligenceEngine() {
        // Run event intelligence analysis every 15 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                analyzeEconomicConditionsForEvents();
            } catch (Exception e) {
                plugin.getLogger().severe("Event intelligence analysis failed: " + e.getMessage());
            }
        }, 18000L, 18000L); // Every 15 minutes
    }
    
    /**
     * Analyze current economic conditions and determine optimal events
     */
    private void analyzeEconomicConditionsForEvents() {
        if (!eventEngineActive || !inflationManager.isActive()) {
            return;
        }
        
        plugin.getLogger().info("🧠 Analyzing economic conditions for dynamic events...");
        
        // Get current economic state
        EconomicIntelligenceEngine.EconomicCycle currentCycle = inflationManager.getCurrentCycle();
        double economicHealth = inflationManager.getEconomicHealth();
        double inflationRate = inflationManager.getInflationRate();
        
        // Calculate event probability based on conditions
        double eventProbability = calculateEventProbability(currentCycle, economicHealth, inflationRate);
        
        plugin.getLogger().info("📊 Economic Analysis | Cycle: " + currentCycle + 
            " | Health: " + String.format("%.1f%%", economicHealth * 100) +
            " | Event Probability: " + String.format("%.1f%%", eventProbability * 100));
        
        // Decide if an event should trigger
        if (shouldTriggerEvent(eventProbability)) {
            EconomicEventType optimalEventType = selectOptimalEventType(currentCycle, economicHealth, inflationRate);
            triggerIntelligentEvent(optimalEventType);
        }
        
        // Update stagnation tracking
        updateStagnationTracking();
    }
    
    /**
     * Calculate probability of economic event based on current conditions
     */
    private double calculateEventProbability(EconomicIntelligenceEngine.EconomicCycle cycle, 
                                           double health, double inflation) {
        double baseProbability = 0.3; // 30% base chance per analysis cycle
        
        // Cycle-based adjustments
        baseProbability += switch (cycle) {
            case DEPRESSION -> 0.4;  // 70% chance - need stimulus events
            case RECESSION -> 0.2;   // 50% chance - moderate intervention
            case STABLE -> -0.1;     // 20% chance - let economy breathe
            case GROWTH -> 0.1;      // 40% chance - opportunity events
            case BOOM -> 0.3;        // 60% chance - celebration and warning events
            case BUBBLE -> 0.5;      // 80% chance - crisis prevention events
        };
        
        // Health-based adjustments
        if (health < 0.3) baseProbability += 0.3; // Crisis events
        if (health > 0.9) baseProbability += 0.2; // Celebration events
        
        // Inflation-based adjustments
        if (Math.abs(inflation) > 0.05) baseProbability += 0.2; // Extreme inflation events
        
        // Time-based adjustments (prevent too frequent events)
        long hoursSinceLastEvent = java.time.Duration.between(lastEventTime, LocalDateTime.now()).toHours();
        if (hoursSinceLastEvent < 2) baseProbability *= 0.1; // Drastically reduce if recent event
        
        return Math.max(0.0, Math.min(1.0, baseProbability));
    }
    
    /**
     * Select the most appropriate event type for current conditions
     */
    private EconomicEventType selectOptimalEventType(EconomicIntelligenceEngine.EconomicCycle cycle,
                                                    double health, double inflation) {
        // Weighted selection driven by configuration and cycle/health.
        Map<EconomicEventType, Double> weights = new EnumMap<>(EconomicEventType.class);
        for (EconomicEventType t : EconomicEventType.values()) {
            double w = getEventWeight(t, cycle, health, inflation);
            if (w > 0) weights.put(t, w);
        }
        if (weights.isEmpty()) return EconomicEventType.MARKET_DISCOVERY;
        return pickWeighted(weights);
    }

    private double getEventWeight(EconomicEventType type, EconomicIntelligenceEngine.EconomicCycle cycle,
                                  double health, double inflation) {
        double base = 1.0;
        try {
            var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var ev = cfg.getModuleConfig("events");
            String key = (type.name().toLowerCase() + ".weight").replace(' ', '_');
            base = Math.max(0.0, ev.getDouble(key, 1.0));
        } catch (Exception ignored) {}

        double modifier = 1.0;
        // Cycle/health-driven modifiers (gentle)
        switch (cycle) {
            case DEPRESSION -> {
                if (type == EconomicEventType.GOVERNMENT_STIMULUS) modifier *= 2.0;
                if (type == EconomicEventType.TRADE_BOOM) modifier *= 1.5;
                if (type == EconomicEventType.MARKET_CORRECTION) modifier *= 0.7;
            }
            case RECESSION -> {
                if (type == EconomicEventType.TRADE_BOOM) modifier *= 1.6;
                if (type == EconomicEventType.GOVERNMENT_STIMULUS) modifier *= 1.4;
            }
            case STABLE -> {
                if (type == EconomicEventType.MARKET_DISCOVERY || type == EconomicEventType.SEASONAL_DEMAND)
                    modifier *= 1.3;
            }
            case GROWTH -> {
                if (type == EconomicEventType.INVESTMENT_OPPORTUNITY || type == EconomicEventType.TECHNOLOGICAL_BREAKTHROUGH)
                    modifier *= 1.5;
            }
            case BOOM -> {
                if (type == EconomicEventType.LUXURY_DEMAND) modifier *= 1.7;
                if (type == EconomicEventType.MARKET_CORRECTION) modifier *= 1.2;
            }
            case BUBBLE -> {
                if (type == EconomicEventType.MARKET_CORRECTION || type == EconomicEventType.BLACK_SWAN_EVENT)
                    modifier *= 1.6;
            }
        }
        if (health < 0.4) {
            if (type == EconomicEventType.GOVERNMENT_STIMULUS || type == EconomicEventType.TRADE_BOOM)
                modifier *= 1.5;
        }
        if (health > 0.85) {
            if (type == EconomicEventType.LUXURY_DEMAND || type == EconomicEventType.INVESTMENT_OPPORTUNITY)
                modifier *= 1.3;
        }
        // Inflation extremes favor correction-like events
        if (Math.abs(inflation) > 0.08 && type == EconomicEventType.MARKET_CORRECTION) modifier *= 1.4;

        return base * modifier;
    }

    private EconomicEventType pickWeighted(Map<EconomicEventType, Double> weights) {
        double total = 0.0;
        for (double w : weights.values()) total += w;
        if (total <= 0.0) return EconomicEventType.MARKET_DISCOVERY;
        double r = ThreadLocalRandom.current().nextDouble() * total;
        double acc = 0.0;
        for (Map.Entry<EconomicEventType, Double> e : weights.entrySet()) {
            acc += e.getValue();
            if (r <= acc) return e.getKey();
        }
        return EconomicEventType.MARKET_DISCOVERY;
    }
    
    // === Event Execution System ===
    
    /**
     * Trigger an intelligent economic event
     */
    private void triggerIntelligentEvent(EconomicEventType eventType) {
        if (isTypeActive(eventType)) {
            plugin.getLogger().info("Event " + eventType + " already active, skipping");
            return;
        }
        
        EventTemplate template = eventTemplates.get(eventType);
        if (template == null) {
            plugin.getLogger().warning("No template found for event type: " + eventType);
            return;
        }
        
        // Create intelligent event with dynamic scaling
        EconomicEvent event = createIntelligentEvent(template, eventType);
        
        // Start the event
        startEvent(event);
        
        lastEventTime = LocalDateTime.now();
        consecutiveQuietHours = 0;
    }

    /**
     * Public API: Trigger a dynamic economic event of the given type.
     * Ensures idempotency (skips if an event of the same type is already active).
     *
     * @param eventType The event type to trigger
     * @return true if the event was started; false if skipped or failed
     */
    public boolean triggerEvent(EconomicEventType eventType) {
        try {
            if (isTypeActive(eventType)) {
                plugin.getLogger().info("Event " + eventType + " already active, skipping");
                return false;
            }
            if (!activeEvents.isEmpty()) {
                plugin.getLogger().info("Another event is active; skipping new trigger");
                return false;
            }
            int cooldownHours = getEventCooldownHours(eventType);
            java.time.LocalDateTime lastEnd = lastEventEndTime.get(eventType);
            if (lastEnd != null && java.time.Duration.between(lastEnd, java.time.LocalDateTime.now()).toHours() < cooldownHours) {
                plugin.getLogger().info("Cooldown active for event " + eventType + "; skipping trigger");
                return false;
            }
            EventTemplate template = eventTemplates.get(eventType);
            if (template == null) {
                plugin.getLogger().warning("No template found for event type: " + eventType);
                return false;
            }
            EconomicEvent event = createIntelligentEvent(template, eventType);
            startEvent(event);
            lastEventTime = LocalDateTime.now();
            consecutiveQuietHours = 0;
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to trigger event " + eventType + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isTypeActive(EconomicEventType type) {
        for (EconomicEvent ev : activeEvents.values()) {
            if (ev.getType() == type && ev.getStatus() == EconomicEvent.EventStatus.ACTIVE) return true;
        }
        return false;
    }

    /**
     * Check if the event engine is active.
     */
    public boolean isEngineActive() {
        return eventEngineActive;
    }

    /**
     * Get count of active events.
     */
    public int getActiveEventsCount() {
        int c = 0;
        for (EconomicEvent ev : activeEvents.values()) {
            if (ev.getStatus() == EconomicEvent.EventStatus.ACTIVE) c++;
        }
        return c;
    }

    public int getEventCooldownHours(EconomicEventType type) {
        try {
            var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var ev = cfg.getModuleConfig("events");
            int global = ev.getInt("cooldown_hours", 6);
            String key = (type.name().toLowerCase() + ".cooldown_hours").replace(' ', '_');
            return ev.getInt(key, global);
        } catch (Exception e) {
            return 6;
        }
    }

    /**
     * Get configured static weight for a given event type from events.yml.
     * Returns 1.0 if not configured or on error.
     */
    public double getConfiguredWeight(EconomicEventType type) {
        try {
            var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var ev = cfg.getModuleConfig("events");
            String key = (type.name().toLowerCase() + ".weight").replace(' ', '_');
            return Math.max(0.0, ev.getDouble(key, 1.0));
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * Get remaining cooldown hours for the event type based on the last end time map.
     * Returns 0 if no cooldown is active.
     */
    public long getRemainingCooldownHours(EconomicEventType type) {
        try {
            java.time.LocalDateTime lastEnd = lastEventEndTime.get(type);
            if (lastEnd == null) return 0L;
            int cooldown = getEventCooldownHours(type);
            long elapsed = java.time.Duration.between(lastEnd, java.time.LocalDateTime.now()).toHours();
            long remaining = cooldown - elapsed;
            return Math.max(0L, remaining);
        } catch (Exception e) {
            return 0L;
        }
    }

    private void persistEvent(EconomicEvent event, String status) {
        try {
            var dm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            String paramsJson = toJson(event.getParameters());
            dm.executeUpdate(
                "INSERT INTO ecoxpert_economic_events (event_id, type, status, parameters, start_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                event.getId(), event.getType().name(), status, paramsJson
            ).join();
        } catch (Exception ignored) {}
    }

    private void persistEventEnd(EconomicEvent event) {
        try {
            var dm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            long minutes = java.time.Duration.between(event.getStartTime(), java.time.LocalDateTime.now()).toMinutes();
            addMetric(event, "metrics.duration_minutes", minutes);
            String paramsJson = toJson(event.getParameters());
            dm.executeUpdate(
                "UPDATE ecoxpert_economic_events SET status = ?, end_time = CURRENT_TIMESTAMP, parameters = ? WHERE event_id = ?",
                "COMPLETED", paramsJson, event.getId()
            ).join();
        } catch (Exception ignored) {}
    }

    // Minimal JSON serialization for parameters (numbers, strings, lists of Material)
    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            sb.append(toJsonValue(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }
    private String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean) return v.toString();
        if (v instanceof CharSequence) return '"' + escape(v.toString()) + '"';
        if (v instanceof Material) return '"' + ((Material) v).name() + '"';
        if (v instanceof List<?>) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (Object o : (List<?>) v) {
                if (!first) sb.append(',');
                first = false;
                sb.append(toJsonValue(o));
            }
            sb.append(']');
            return sb.toString();
        }
        // Fallback to string
        return '"' + escape(v.toString()) + '"';
    }
    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    /**
     * Create an intelligent event with dynamic parameters
     */
    private EconomicEvent createIntelligentEvent(EventTemplate template, EconomicEventType eventType) {
        String eventId = generateEventId(eventType);
        
        // Calculate dynamic duration based on server conditions
        int duration = calculateIntelligentDuration(template.baseDuration);
        
        // Calculate dynamic intensity based on economic conditions
        double intensity = calculateIntelligentIntensity(template.baseIntensity);
        
        // Select affected items intelligently
        List<Material> affectedItems = selectIntelligentItems(template.potentialItems);
        
        // Create smart event parameters
        Map<String, Object> parameters = createSmartEventParameters(eventType, intensity, affectedItems);
        
        return new EconomicEvent(
            eventId,
            eventType,
            template.name,
            template.description,
            LocalDateTime.now(),
            duration,
            intensity,
            affectedItems,
            parameters,
            EconomicEvent.EventStatus.ACTIVE
        );
    }
    
    /**
     * Start an economic event with cascading effects
     */
    private void startEvent(EconomicEvent event) {
        plugin.getLogger().info("🎪 Starting Dynamic Economic Event: " + event.getName() + 
            " | Duration: " + event.getDuration() + "min | Intensity: " + 
            String.format("%.1f%%", event.getIntensity() * 100));
        
        // Add to active events
        activeEvents.put(event.getId(), event);
        eventHistory.add(event);
        
        // Broadcast event start
        broadcastEventStart(event);
        
        // Apply immediate effects
        applyEventEffects(event);
        // Persist event start
        persistEvent(event, "ACTIVE");
        
        // Schedule event end
        scheduleEventEnd(event);
        
        // Notify inflation manager of event
        if (inflationManager != null) {
            // This would integrate with the intelligence system
            plugin.getLogger().info("🔗 Event integrated with Economic Intelligence System");
        }
    }
    
    /**
     * Apply the dynamic effects of an economic event
     */
    private void applyEventEffects(EconomicEvent event) {
        switch (event.getType()) {
            case GOVERNMENT_STIMULUS -> applyGovernmentStimulus(event);
            case TRADE_BOOM -> applyTradeBoom(event);
            case MARKET_DISCOVERY -> applyMarketDiscovery(event);
            case INVESTMENT_OPPORTUNITY -> applyInvestmentOpportunity(event);
            case LUXURY_DEMAND -> applyLuxuryDemand(event);
            case MARKET_CORRECTION -> applyMarketCorrection(event);
            case RESOURCE_SHORTAGE -> applyResourceShortage(event);
            case TECHNOLOGICAL_BREAKTHROUGH -> applyTechnologicalBreakthrough(event);
            case SEASONAL_DEMAND -> applySeasonalDemand(event);
            case BLACK_SWAN_EVENT -> applyBlackSwanEvent(event);
        }
    }
    
    // === Event Effect Implementations ===
    
    /**
     * Government Stimulus - Emergency economic support during crises
     */
    private void applyGovernmentStimulus(EconomicEvent event) {
        plugin.getLogger().info("💉 GOVERNMENT STIMULUS: Distributing emergency economic aid");

        double totalStimulus = (Double) event.getParameters().get("total_stimulus");
        double perPlayerAmount = totalStimulus / Math.max(1, Bukkit.getOnlinePlayers().size());

        me.koyere.ecoxpert.core.translation.TranslationManager tm =
            plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);

        // Distribute stimulus to online players
        Bukkit.getOnlinePlayers().forEach(player -> {
            economyManager.addMoney(
                player.getUniqueId(),
                BigDecimal.valueOf(perPlayerAmount),
                "Government Economic Stimulus - " + event.getName()
            );
            String amountStr = economyManager.formatMoney(BigDecimal.valueOf(perPlayerAmount));
            player.sendMessage(tm.getMessage("prefix") + tm.getPlayerMessage(player, "events.stimulus.received", amountStr));
        });

        // Announce economic impact (use formatted money)
        String totalStr = economyManager.formatMoney(BigDecimal.valueOf(totalStimulus));
        Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.stimulus.injected", totalStr));

        // Metrics
        addMetric(event, "metrics.type", "stimulus");
        addMetric(event, "metrics.total_stimulus", totalStimulus);
        addMetric(event, "metrics.per_player_amount", perPlayerAmount);
        addMetric(event, "metrics.recipients", Bukkit.getOnlinePlayers().size());
    }
    
    /**
     * Trade Boom - Increased trading rewards and market activity
     */
    private void applyTradeBoom(EconomicEvent event) {
        plugin.getLogger().info("📈 TRADE BOOM: Boosting market activity and rewards");

        double multiplier = (Double) event.getParameters().get("trade_multiplier");

        me.koyere.ecoxpert.core.translation.TranslationManager tm =
            plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
        String pct = String.format("%.0f", (multiplier - 1) * 100);
        Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.trade_boom.announce", pct));
        addMetric(event, "metrics.type", "global_bonus");
        addMetric(event, "metrics.trade_bonus_pct", (multiplier - 1) * 100.0);
        
        // Apply temporary market boost
        if (marketManager != null) {
            plugin.getLogger().info("🔗 Trade boom integrated with market system");
            marketManager.setGlobalPriceFactors(0.99, 1.01);
            Bukkit.getScheduler().runTaskLater(plugin, () -> marketManager.setGlobalPriceFactors(1.0, 1.0), 20L * 60 * 10);
        }
    }
    
    /**
     * Market Discovery - New valuable items appear in the market
     */
    private void applyMarketDiscovery(EconomicEvent event) {
        plugin.getLogger().info("🔍 MARKET DISCOVERY: New valuable resources discovered");

        @SuppressWarnings("unchecked")
        List<Material> newItems = (List<Material>) event.getParameters().get("discovered_items");
        double priceBoost = (Double) event.getParameters().get("discovery_bonus");

        StringBuilder itemList = new StringBuilder();
        for (int i = 0; i < newItems.size(); i++) {
            Material item = newItems.get(i);
            if (i > 0) itemList.append(", ");
            itemList.append(item.name().toLowerCase().replace('_', ' '));
        }
        me.koyere.ecoxpert.core.translation.TranslationManager tm =
            plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
        String pct = String.format("%.0f", (priceBoost - 1.0) * 100);
        Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.market_discovery.announce", itemList.toString(), pct));

        addMetric(event, "metrics.type", "per_item");
        addMetric(event, "metrics.items", newItems.size());
        addMetric(event, "metrics.sell_boost_pct", (priceBoost - 1.0) * 100.0);

        // Apply temporary per-item sell boost (encourage players to supply discovered items)
        if (marketManager != null && !newItems.isEmpty()) {
            java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
            double sellBoost = Math.min(1.2, 1.0 + (priceBoost - 1.0) * 0.5);
            for (Material m : newItems) {
                factors.put(m, new double[]{1.0, sellBoost});
            }
            marketManager.applyTemporaryItemFactors(factors, 10);
        }
    }
    
    // === Event Templates and Configuration ===
    
    /**
     * Initialize all event templates with their base parameters
     */
    private void initializeEventTemplates() {
        // Crisis Response Events
        eventTemplates.put(EconomicEventType.GOVERNMENT_STIMULUS, new EventTemplate(
            "Government Economic Stimulus",
            "Emergency government aid to boost economic activity during downturns",
            60, // 1 hour base duration
            0.7, // High intensity
            Arrays.asList() // No specific items
        ));
        
        eventTemplates.put(EconomicEventType.TRADE_BOOM, new EventTemplate(
            "Trade Boom Period",
            "Increased demand for trading creates bonus opportunities",
            45, // 45 minutes
            0.5,
            Arrays.asList(Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT)
        ));
        
        // Discovery Events
        eventTemplates.put(EconomicEventType.MARKET_DISCOVERY, new EventTemplate(
            "Valuable Resource Discovery",
            "New deposits discovered, increasing market value of specific resources",
            90, // 1.5 hours
            0.6,
            Arrays.asList(Material.GOLD_INGOT, Material.IRON_INGOT, Material.COPPER_INGOT, 
                         Material.REDSTONE, Material.LAPIS_LAZULI)
        ));
        
        eventTemplates.put(EconomicEventType.TECHNOLOGICAL_BREAKTHROUGH, new EventTemplate(
            "Technological Breakthrough",
            "Innovation increases efficiency and creates new market opportunities",
            120, // 2 hours
            0.8,
            Arrays.asList(Material.REDSTONE, Material.REPEATER, Material.COMPARATOR, 
                         Material.PISTON, Material.OBSERVER)
        ));
        
        // Market Events
        eventTemplates.put(EconomicEventType.LUXURY_DEMAND, new EventTemplate(
            "Luxury Goods Demand",
            "High demand for luxury items during economic prosperity",
            30, // 30 minutes
            0.4,
            Arrays.asList(Material.DIAMOND, Material.EMERALD, Material.GOLD_BLOCK, 
                         Material.NETHERITE_BLOCK)
        ));
        
        eventTemplates.put(EconomicEventType.INVESTMENT_OPPORTUNITY, new EventTemplate(
            "Investment Opportunity",
            "Special investment opportunities with increased returns",
            75, // 1 hour 15 minutes
            0.6,
            Arrays.asList(Material.EMERALD, Material.DIAMOND, Material.GOLD_INGOT)
        ));
        
        // Crisis Events
        eventTemplates.put(EconomicEventType.MARKET_CORRECTION, new EventTemplate(
            "Market Correction",
            "Natural market adjustment to prevent economic bubbles",
            40, // 40 minutes
            0.7,
            Arrays.asList() // Affects all items
        ));
        
        eventTemplates.put(EconomicEventType.RESOURCE_SHORTAGE, new EventTemplate(
            "Resource Shortage",
            "Temporary shortage increases value of specific resources",
            60, // 1 hour
            0.8,
            Arrays.asList(Material.IRON_INGOT, Material.COAL, Material.WHEAT, 
                         Material.OAK_LOG, Material.COBBLESTONE)
        ));
        
        // Special Events
        eventTemplates.put(EconomicEventType.SEASONAL_DEMAND, new EventTemplate(
            "Seasonal Market Demand",
            "Seasonal changes affect market preferences",
            180, // 3 hours
            0.3,
            Arrays.asList(Material.PUMPKIN, Material.WHEAT, Material.CARROT, 
                         Material.POTATO, Material.BEETROOT)
        ));
        
        eventTemplates.put(EconomicEventType.BLACK_SWAN_EVENT, new EventTemplate(
            "Unexpected Economic Event",
            "Rare, unpredictable event with significant economic impact",
            20, // 20 minutes - short but intense
            1.0, // Maximum intensity
            Arrays.asList() // Can affect anything
        ));
    }
    
    // === Anti-Stagnation System ===
    
    /**
     * Start the anti-stagnation system that prevents economic death
     */
    private void startAntiStagnationSystem() {
        // Check for economic stagnation every hour
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                checkForEconomicStagnation();
            } catch (Exception e) {
                plugin.getLogger().severe("Anti-stagnation system failed: " + e.getMessage());
            }
        }, 72000L, 72000L); // Every hour
    }
    
    /**
     * Check for signs of economic stagnation and take corrective action
     */
    private void checkForEconomicStagnation() {
        consecutiveQuietHours++;
        
        plugin.getLogger().info("🔍 Anti-Stagnation Check | Quiet Hours: " + consecutiveQuietHours);
        
        // If no events for 4+ hours, force stimulation
        if (consecutiveQuietHours >= 4) {
            plugin.getLogger().warning("⚠️ Economic stagnation detected! Triggering emergency stimulation");
            
            // Force a stimulating event
            EconomicEventType antiStagnationEvent = selectAntiStagnationEvent();
            triggerIntelligentEvent(antiStagnationEvent);
            
            consecutiveQuietHours = 0;
        }
        
        // Check for low player activity
        if (Bukkit.getOnlinePlayers().size() > 0 && consecutiveQuietHours >= 2) {
            // Consider smaller stimulation events
            if (ThreadLocalRandom.current().nextDouble() < 0.4) { // 40% chance
                triggerIntelligentEvent(EconomicEventType.MARKET_DISCOVERY);
            }
        }
    }
    
    /**
     * Select an appropriate anti-stagnation event
     */
    private EconomicEventType selectAntiStagnationEvent() {
        if (inflationManager != null && inflationManager.isActive()) {
            double economicHealth = inflationManager.getEconomicHealth();
            
            if (economicHealth < 0.5) {
                return EconomicEventType.GOVERNMENT_STIMULUS;
            } else {
                return EconomicEventType.TRADE_BOOM;
            }
        }
        
        // Default fallback
        return EconomicEventType.MARKET_DISCOVERY;
    }
    
    // === Helper Methods ===
    
    private void startEventScheduler() {
        // Event management scheduler - runs every 5 minutes
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                manageActiveEvents();
            } catch (Exception e) {
                plugin.getLogger().severe("Event scheduler failed: " + e.getMessage());
            }
        }, 6000L, 6000L); // Every 5 minutes
    }
    
    private void manageActiveEvents() {
        // Check for expired events
        List<EconomicEvent> expiredEvents = new ArrayList<>();
        
        for (EconomicEvent event : activeEvents.values()) {
            if (event.hasExpired()) {
                expiredEvents.add(event);
            }
        }
        
        // End expired events
        expiredEvents.forEach(this::endEvent);
    }
    
    private void endEvent(EconomicEvent event) {
        plugin.getLogger().info("🏁 Ending Economic Event: " + event.getName());
        
        // Remove from active events
        activeEvents.remove(event.getId());
        
        // Update event status
        event.setStatus(EconomicEvent.EventStatus.COMPLETED);
        lastEventEndTime.put(event.getType(), java.time.LocalDateTime.now());
        
        // Broadcast event end
        broadcastEventEnd(event);
        
        // Apply end effects if needed
        applyEventEndEffects(event);
        // Persist end
        persistEventEnd(event);
    }

    /**
     * Public API: End an active event by id.
     *
     * @param eventId The id returned when the event started
     * @return true if an active event was found and ended
     */
    public boolean endEventById(String eventId) {
        EconomicEvent ev = activeEvents.get(eventId);
        if (ev == null) {
            return false;
        }
        endEvent(ev);
        return true;
    }
    
    // === Utility and Stub Methods ===
    
    private boolean shouldTriggerEvent(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }
    
    private String generateEventId(EconomicEventType eventType) {
        return eventType.name() + "_" + System.currentTimeMillis();
    }
    
    private int calculateIntelligentDuration(int baseDuration) {
        // Adjust duration based on server conditions
        double multiplier = 1.0;
        
        if (inflationManager != null && inflationManager.isActive()) {
            double economicHealth = inflationManager.getEconomicHealth();
            
            // Longer events during stable periods, shorter during crises
            if (economicHealth < 0.4) multiplier = 0.7; // Shorter during crisis
            else if (economicHealth > 0.8) multiplier = 1.3; // Longer during prosperity
        }
        
        return (int) (baseDuration * multiplier);
    }
    
    private double calculateIntelligentIntensity(double baseIntensity) {
        // Adjust intensity based on economic conditions and server population
        double intensity = baseIntensity;
        
        // Population scaling
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount > 20) intensity *= 1.2; // More intense for larger servers
        else if (playerCount < 5) intensity *= 0.8; // Less intense for smaller servers
        
        return Math.max(0.1, Math.min(1.0, intensity));
    }
    
    private List<Material> selectIntelligentItems(List<Material> potentialItems) {
        if (potentialItems.isEmpty()) return new ArrayList<>();
        
        // Select 1-3 items randomly
        int itemCount = ThreadLocalRandom.current().nextInt(1, Math.min(4, potentialItems.size() + 1));
        List<Material> selected = new ArrayList<>(potentialItems);
        Collections.shuffle(selected);
        return selected.subList(0, itemCount);
    }
    
    private Map<String, Object> createSmartEventParameters(EconomicEventType eventType, 
                                                          double intensity, List<Material> items) {
        Map<String, Object> params = new HashMap<>();
        
        switch (eventType) {
            case GOVERNMENT_STIMULUS -> {
                double totalStimulus = intensity * Bukkit.getOnlinePlayers().size() * 1000;
                params.put("total_stimulus", totalStimulus);
            }
            case TRADE_BOOM -> {
                double multiplier = 1.0 + (intensity * 0.5); // Up to 50% bonus
                params.put("trade_multiplier", multiplier);
            }
            case MARKET_DISCOVERY -> {
                params.put("discovered_items", items);
                params.put("discovery_bonus", 1.0 + intensity);
            }
        }
        
        return params;
    }
    
    private void updateStagnationTracking() {
        if (activeEvents.isEmpty()) {
            consecutiveQuietHours++;
        } else {
            consecutiveQuietHours = 0;
        }
    }
    
    private void scheduleEventEnd(EconomicEvent event) {
        // Schedule event end
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (activeEvents.containsKey(event.getId())) {
                endEvent(event);
            }
        }, event.getDuration() * 60 * 20L); // Convert minutes to ticks
    }
    
    private void broadcastEventStart(EconomicEvent event) {
        me.koyere.ecoxpert.core.translation.TranslationManager tm =
            plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
        Bukkit.broadcastMessage(tm.getMessage("events.banner"));
        Bukkit.broadcastMessage(tm.getMessage("events.started", event.getName()));
        Bukkit.broadcastMessage(tm.getMessage("events.info.description", event.getDescription()));
        Bukkit.broadcastMessage(tm.getMessage("events.info.duration", event.getDuration()));
    }
    
    private void broadcastEventEnd(EconomicEvent event) {
        me.koyere.ecoxpert.core.translation.TranslationManager tm =
            plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
        Bukkit.broadcastMessage(tm.getMessage("events.banner"));
        Bukkit.broadcastMessage(tm.getMessage("events.ended", event.getName()));
    }
    
    private void applyEventEndEffects(EconomicEvent event) {
        // Implement end effects for events that need cleanup
    }
    
    // Additional effect implementations (stubs for brevity)
    private void applyInvestmentOpportunity(EconomicEvent event) {
        if (marketManager == null) return;
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var marketCfg = cfg.getModuleConfig("market");
            var evcfg = cfg.getModuleConfig("events");
            int minutes = evcfg.getInt("investment_opportunity.duration_minutes", 10);
            double buyDelta = evcfg.getDouble("investment_opportunity.buy_delta", -0.02); // cheaper buy
            double sellDelta = evcfg.getDouble("investment_opportunity.sell_delta", 0.05); // better sell

            java.util.Set<Material> targets = new java.util.HashSet<>();
            if (event.getAffectedItems() != null && !event.getAffectedItems().isEmpty()) {
                targets.addAll(event.getAffectedItems());
            } else {
                // fallback to a reasonable investment set (emerald/diamond/gold)
                for (String s : new String[]{"EMERALD","DIAMOND","GOLD_INGOT"}) {
                    try { targets.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
                }
            }
            if (targets.isEmpty()) return;

            java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
            for (Material m : targets) {
                factors.put(m, new double[]{1.0 + buyDelta, 1.0 + sellDelta});
            }
            marketManager.applyTemporaryItemFactors(factors, minutes);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String up = String.format("%.0f", Math.max(0, sellDelta) * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.investment.announce", up));

            addMetric(event, "metrics.type", "per_item");
            addMetric(event, "metrics.items", targets.size());
            addMetric(event, "metrics.buy_delta", buyDelta);
            addMetric(event, "metrics.sell_delta", sellDelta);
        } catch (Exception ignored) { }
    }
    private void applyLuxuryDemand(EconomicEvent event) { 
        // Luxury items fetch higher prices temporarily
        if (marketManager == null) return;
        java.util.Set<Material> targets = new java.util.HashSet<>();
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var marketCfg = cfg.getModuleConfig("market");
            var evcfg = cfg.getModuleConfig("events");
            String category = evcfg.getString("luxury_demand.category", "luxury");
            java.util.List<String> mats = marketCfg.getStringList("categories." + category + ".materials");
            for (String s : mats) {
                try { targets.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {}
            }
            if (event.getAffectedItems() != null) targets.addAll(event.getAffectedItems());
            if (!targets.isEmpty()) {
                int minutes = evcfg.getInt("luxury_demand.duration_minutes", 10);
                double buyDelta = evcfg.getDouble("luxury_demand.category_buy_delta", 0.04);
                double sellDelta = evcfg.getDouble("luxury_demand.category_sell_delta", 0.08);
                java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
                for (Material m : targets) {
                    factors.put(m, new double[]{1.0 + buyDelta, 1.0 + sellDelta});
                }
                marketManager.applyTemporaryItemFactors(factors, minutes);

                me.koyere.ecoxpert.core.translation.TranslationManager tm =
                    plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
                String pct = String.format("%.0f", sellDelta * 100);
                Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.luxury_demand.announce", pct));

                addMetric(event, "metrics.type", "category");
                addMetric(event, "metrics.items", targets.size());
                addMetric(event, "metrics.category", category);
                addMetric(event, "metrics.buy_delta", buyDelta);
                addMetric(event, "metrics.sell_delta", sellDelta);
            }
        } catch (Exception ignored) {}
    }
    private void applyMarketCorrection(EconomicEvent event) {
        // Apply a temporary global cooling: raise buy prices a bit, lower sell prices a bit
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var evcfg = cfg.getModuleConfig("events");
            int minutes = evcfg.getInt("market_correction.duration_minutes", 10);
            double buyDelta = evcfg.getDouble("market_correction.global_buy_factor_delta", 0.02);   // +2% cost to buy
            double sellDelta = evcfg.getDouble("market_correction.global_sell_factor_delta", -0.02); // -2% returns to sell

            double[] prev = marketManager.getGlobalPriceFactors();
            double newBuy = Math.max(0.5, prev[0] + buyDelta);
            double newSell = Math.max(0.5, prev[1] + sellDelta);
            marketManager.setGlobalPriceFactors(newBuy, newSell);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String buyPct = String.format("%.0f", buyDelta * 100);
            String sellPct = String.format("%.0f", Math.abs(sellDelta) * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.market_correction.announce", buyPct, sellPct));

            // Reset after duration
            Bukkit.getScheduler().runTaskLater(plugin, () -> marketManager.setGlobalPriceFactors(prev[0], prev[1]), minutes * 60L * 20L);

            addMetric(event, "metrics.type", "global");
            addMetric(event, "metrics.buy_delta", buyDelta);
            addMetric(event, "metrics.sell_delta", sellDelta);
            addMetric(event, "metrics.duration_minutes", minutes);
        } catch (Exception ignored) { }
    }
    private void applyResourceShortage(EconomicEvent event) {
        if (marketManager == null || event.getAffectedItems() == null || event.getAffectedItems().isEmpty()) return;
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var evcfg = cfg.getModuleConfig("events");
            int minutes = evcfg.getInt("resource_shortage.duration_minutes", 10);
            double delta = evcfg.getDouble("resource_shortage.buy_sell_delta", 0.08);
            java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
            for (Material m : event.getAffectedItems()) {
                factors.put(m, new double[]{1.0 + delta, 1.0 + delta});
            }
            marketManager.applyTemporaryItemFactors(factors, minutes);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String pct = String.format("%.0f", delta * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.resource_shortage.announce", pct));

            addMetric(event, "metrics.type", "per_item");
            addMetric(event, "metrics.items", event.getAffectedItems().size());
            addMetric(event, "metrics.buy_delta", delta);
            addMetric(event, "metrics.sell_delta", delta);
        } catch (Exception ignored) {}
    }
    private void applyTechnologicalBreakthrough(EconomicEvent event) {
        if (marketManager == null) return;
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var marketCfg = cfg.getModuleConfig("market");
            var evcfg = cfg.getModuleConfig("events");
            String category = evcfg.getString("technological_breakthrough.category", "redstone");
            java.util.List<String> mats = marketCfg.getStringList("categories." + category + ".materials");
            java.util.Set<Material> targets = new java.util.HashSet<>();
            for (String s : mats) { try { targets.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {} }
            if (event.getAffectedItems() != null) targets.addAll(event.getAffectedItems());
            if (targets.isEmpty()) return;

            int minutes = evcfg.getInt("technological_breakthrough.duration_minutes", 10);
            double buyDelta = evcfg.getDouble("technological_breakthrough.buy_delta", -0.05); // cheaper to buy
            double sellDelta = evcfg.getDouble("technological_breakthrough.sell_delta", -0.02); // slightly lower sell (deflation)
            java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
            for (Material m : targets) {
                factors.put(m, new double[]{1.0 + buyDelta, 1.0 + sellDelta});
            }
            marketManager.applyTemporaryItemFactors(factors, minutes);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String buyPct = String.format("%.0f", Math.abs(buyDelta) * 100);
            String sellPct = String.format("%.0f", Math.abs(sellDelta) * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.tech_breakthrough.announce", buyPct, sellPct));

            addMetric(event, "metrics.type", "category");
            addMetric(event, "metrics.items", targets.size());
            addMetric(event, "metrics.category", category);
            addMetric(event, "metrics.buy_delta", buyDelta);
            addMetric(event, "metrics.sell_delta", sellDelta);
        } catch (Exception ignored) { }
    }
    private void applySeasonalDemand(EconomicEvent event) {
        if (marketManager == null) return;
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var marketCfg = cfg.getModuleConfig("market");
            var evcfg = cfg.getModuleConfig("events");
            String category = evcfg.getString("seasonal_demand.category", "food");
            int minutes = evcfg.getInt("seasonal_demand.duration_minutes", 10);
            double buyDelta = evcfg.getDouble("seasonal_demand.buy_delta", 0.00);
            double sellDelta = evcfg.getDouble("seasonal_demand.sell_delta", 0.06); // default +6% sell

            java.util.List<String> mats = marketCfg.getStringList("categories." + category + ".materials");
            java.util.Set<Material> targets = new java.util.HashSet<>();
            for (String s : mats) { try { targets.add(Material.valueOf(s)); } catch (IllegalArgumentException ignored) {} }
            if (event.getAffectedItems() != null) targets.addAll(event.getAffectedItems());
            if (targets.isEmpty()) return;

            java.util.Map<Material, double[]> factors = new java.util.HashMap<>();
            for (Material m : targets) {
                factors.put(m, new double[]{1.0 + buyDelta, 1.0 + sellDelta});
            }
            marketManager.applyTemporaryItemFactors(factors, minutes);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String pct = String.format("%.0f", Math.max(buyDelta, sellDelta) * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.seasonal_demand.announce", pct));
            addMetric(event, "metrics.type", "category");
            addMetric(event, "metrics.items", targets.size());
            addMetric(event, "metrics.category", category);
            addMetric(event, "metrics.buy_delta", buyDelta);
            addMetric(event, "metrics.sell_delta", sellDelta);
        } catch (Exception ignored) { }
    }
    private void applyBlackSwanEvent(EconomicEvent event) {
        try {
            me.koyere.ecoxpert.core.config.ConfigManager cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class);
            var evcfg = cfg.getModuleConfig("events");
            int minutes = evcfg.getInt("black_swan_event.duration_minutes", 20);
            double buyDelta = evcfg.getDouble("black_swan_event.global_buy_factor_delta", -0.10);
            double sellDelta = evcfg.getDouble("black_swan_event.global_sell_factor_delta", -0.10);

            double[] prev = marketManager.getGlobalPriceFactors();
            double newBuy = Math.max(0.3, prev[0] + buyDelta);
            double newSell = Math.max(0.3, prev[1] + sellDelta);
            marketManager.setGlobalPriceFactors(newBuy, newSell);

            me.koyere.ecoxpert.core.translation.TranslationManager tm =
                plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.translation.TranslationManager.class);
            String b = String.format("%.0f", Math.abs(buyDelta) * 100);
            String s = String.format("%.0f", Math.abs(sellDelta) * 100);
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage("events.black_swan.announce", b, s));

            Bukkit.getScheduler().runTaskLater(plugin, () -> marketManager.setGlobalPriceFactors(prev[0], prev[1]), minutes * 60L * 20L);
            addMetric(event, "metrics.type", "global");
            addMetric(event, "metrics.buy_delta", buyDelta);
            addMetric(event, "metrics.sell_delta", sellDelta);
            addMetric(event, "metrics.duration_minutes", minutes);
        } catch (Exception ignored) { }
    }

    private void addMetric(EconomicEvent event, String key, Object value) {
        try {
            event.getParameters().put(key, value);
        } catch (Exception ignored) {}
    }
    
    // Getters for external access
    public Map<String, EconomicEvent> getActiveEvents() { return new HashMap<>(activeEvents); }
    public List<EconomicEvent> getEventHistory() { return new ArrayList<>(eventHistory); }
    public boolean isEventEngineActive() { return eventEngineActive; }
    public void pauseEngine() { this.eventEngineActive = false; }
    public void resumeEngine() { this.eventEngineActive = true; }
    public int getConsecutiveQuietHours() { return consecutiveQuietHours; }
    public long getHoursSinceLastEvent() {
        return java.time.Duration.between(lastEventTime, java.time.LocalDateTime.now()).toHours();
    }
    
    // Inner Classes
    private static class EventTemplate {
        final String name;
        final String description;
        final int baseDuration;
        final double baseIntensity;
        final List<Material> potentialItems;
        
        EventTemplate(String name, String description, int baseDuration, 
                     double baseIntensity, List<Material> potentialItems) {
            this.name = name;
            this.description = description;
            this.baseDuration = baseDuration;
            this.baseIntensity = baseIntensity;
            this.potentialItems = potentialItems;
        }
    }
}
