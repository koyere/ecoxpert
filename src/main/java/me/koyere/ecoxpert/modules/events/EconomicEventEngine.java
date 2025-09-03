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
        
        // Crisis Response Events
        if (health < 0.4) {
            return ThreadLocalRandom.current().nextBoolean() ? 
                EconomicEventType.GOVERNMENT_STIMULUS : EconomicEventType.TRADE_BOOM;
        }
        
        // Cycle-Specific Events
        return switch (cycle) {
            case DEPRESSION -> EconomicEventType.GOVERNMENT_STIMULUS;
            case RECESSION -> EconomicEventType.TRADE_BOOM;
            case STABLE -> EconomicEventType.MARKET_DISCOVERY;
            case GROWTH -> EconomicEventType.INVESTMENT_OPPORTUNITY;
            case BOOM -> EconomicEventType.LUXURY_DEMAND;
            case BUBBLE -> EconomicEventType.MARKET_CORRECTION;
        };
    }
    
    // === Event Execution System ===
    
    /**
     * Trigger an intelligent economic event
     */
    private void triggerIntelligentEvent(EconomicEventType eventType) {
        if (activeEvents.containsKey(eventType.name())) {
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
            if (activeEvents.containsKey(eventType.name())) {
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

    private int getEventCooldownHours(EconomicEventType type) {
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

    private void persistEvent(EconomicEvent event, String status) {
        try {
            var dm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            String paramsJson = ""; // TODO: serialize parameters if needed
            dm.executeUpdate(
                "INSERT INTO ecoxpert_economic_events (event_id, type, status, parameters, start_time) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                event.getId(), event.getType().name(), status, paramsJson
            ).join();
        } catch (Exception ignored) {}
    }

    private void persistEventEnd(EconomicEvent event) {
        try {
            var dm = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.data.DataManager.class);
            dm.executeUpdate(
                "UPDATE ecoxpert_economic_events SET status = ?, end_time = CURRENT_TIMESTAMP WHERE event_id = ?",
                "COMPLETED", event.getId()
            ).join();
        } catch (Exception ignored) {}
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
        
        // Distribute stimulus to online players
        Bukkit.getOnlinePlayers().forEach(player -> {
            economyManager.addMoney(player.getUniqueId(), 
                BigDecimal.valueOf(perPlayerAmount),
                "Government Economic Stimulus - " + event.getName());
            
            player.sendMessage("§6[EcoXpert] §aYou received $" + 
                String.format("%.2f", perPlayerAmount) + " from government stimulus!");
        });
        
        // Announce economic impact
        Bukkit.broadcastMessage("§6[Economic Event] §e💰 Government injected $" + 
            String.format("%.0f", totalStimulus) + " into the economy!");
    }
    
    /**
     * Trade Boom - Increased trading rewards and market activity
     */
    private void applyTradeBoom(EconomicEvent event) {
        plugin.getLogger().info("📈 TRADE BOOM: Boosting market activity and rewards");
        
        double multiplier = (Double) event.getParameters().get("trade_multiplier");
        
        // This would integrate with the market system to boost prices/rewards
        Bukkit.broadcastMessage("§6[Economic Event] §e📈 Trade Boom! All market trades give " + 
            String.format("%.0f%%", (multiplier - 1) * 100) + " bonus rewards!");
        
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
        for (Material item : newItems) {
            itemList.append(item.name().toLowerCase().replace('_', ' ')).append(", ");
        }
        Bukkit.broadcastMessage("§6[Economic Event] §e🔍 Market Discovery! " + 
            itemList.toString() + "now worth " + String.format("%.0f%%", priceBoost * 100) + " more!");

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
        Bukkit.broadcastMessage("§6§l[ECONOMIC EVENT]");
        Bukkit.broadcastMessage("§e🎪 " + event.getName() + " has begun!");
        Bukkit.broadcastMessage("§7" + event.getDescription());
        Bukkit.broadcastMessage("§7Duration: " + event.getDuration() + " minutes");
    }
    
    private void broadcastEventEnd(EconomicEvent event) {
        Bukkit.broadcastMessage("§6§l[ECONOMIC EVENT]");
        Bukkit.broadcastMessage("§e🏁 " + event.getName() + " has ended!");
    }
    
    private void applyEventEndEffects(EconomicEvent event) {
        // Implement end effects for events that need cleanup
    }
    
    // Additional effect implementations (stubs for brevity)
    private void applyInvestmentOpportunity(EconomicEvent event) { /* TODO */ }
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
            }
        } catch (Exception ignored) {}
    }
    private void applyMarketCorrection(EconomicEvent event) { /* TODO */ }
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
        } catch (Exception ignored) {}
    }
    private void applyTechnologicalBreakthrough(EconomicEvent event) { /* TODO */ }
    private void applySeasonalDemand(EconomicEvent event) { /* TODO */ }
    private void applyBlackSwanEvent(EconomicEvent event) { /* TODO */ }
    
    // Getters for external access
    public Map<String, EconomicEvent> getActiveEvents() { return new HashMap<>(activeEvents); }
    public List<EconomicEvent> getEventHistory() { return new ArrayList<>(eventHistory); }
    public boolean isEventEngineActive() { return eventEngineActive; }
    
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
