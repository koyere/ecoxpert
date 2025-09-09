package me.koyere.ecoxpert.api.events;

import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.time.Instant;

/**
 * Fired when the server economic cycle changes (e.g., GROWTH -> BOOM).
 */
public class EconomyCycleChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final EconomicIntelligenceEngine.EconomicCycle oldCycle;
    private final EconomicIntelligenceEngine.EconomicCycle newCycle;
    private final Instant timestamp;

    public EconomyCycleChangeEvent(EconomicIntelligenceEngine.EconomicCycle oldCycle,
                                   EconomicIntelligenceEngine.EconomicCycle newCycle,
                                   Instant timestamp) {
        this.oldCycle = oldCycle;
        this.newCycle = newCycle;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public EconomicIntelligenceEngine.EconomicCycle getOldCycle() { return oldCycle; }
    public EconomicIntelligenceEngine.EconomicCycle getNewCycle() { return newCycle; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
