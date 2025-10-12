package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.events.EconomicEventEngine;
import me.koyere.ecoxpert.modules.events.EconomicEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Professional implementation of EventsService
 * Delegates to EconomicEventEngine with proper mapping
 */
public class EventsServiceImpl implements EventsService {

    private final EconomicEventEngine eventEngine;

    public EventsServiceImpl(EconomicEventEngine eventEngine) {
        this.eventEngine = eventEngine;
    }

    @Override
    public List<EventInfo> getActiveEvents() {
        Map<String, EconomicEvent> activeEventsMap = eventEngine.getActiveEvents();
        return activeEventsMap.values().stream()
            .map(event -> new EventInfo(
                UUID.nameUUIDFromBytes(event.getId().getBytes()),
                mapEventType(event.getType()),
                event.getName(),
                event.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                event.getEndTime() != null ? event.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
                event.getDescription()
            ))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<EventInfo> getEventById(UUID eventId) {
        Map<String, EconomicEvent> activeEventsMap = eventEngine.getActiveEvents();
        return activeEventsMap.values().stream()
            .filter(e -> UUID.nameUUIDFromBytes(e.getId().getBytes()).equals(eventId))
            .map(event -> new EventInfo(
                UUID.nameUUIDFromBytes(event.getId().getBytes()),
                mapEventType(event.getType()),
                event.getName(),
                event.getStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant(),
                event.getEndTime() != null ? event.getEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant() : null,
                event.getDescription()
            ))
            .findFirst();
    }

    @Override
    public boolean isEventActive(EventType type) {
        me.koyere.ecoxpert.modules.events.EconomicEventType internalType = unmapEventType(type);
        Map<String, EconomicEvent> activeEvents = eventEngine.getActiveEvents();
        return activeEvents.values().stream()
            .anyMatch(e -> e.getType() == internalType);
    }

    @Override
    public Map<EventType, Long> getRemainingCooldowns() {
        Map<EventType, Long> cooldowns = new HashMap<>();
        for (EventType type : EventType.values()) {
            me.koyere.ecoxpert.modules.events.EconomicEventType internalType = unmapEventType(type);
            long remainingHours = eventEngine.getRemainingCooldownHours(internalType);
            cooldowns.put(type, remainingHours * 3600000); // Convert hours to milliseconds
        }
        return cooldowns;
    }

    @Override
    public EventStatistics getEventStatistics(int days) {
        List<EconomicEvent> history = eventEngine.getEventHistory();

        // Calculate statistics from history
        Map<EventType, Integer> counts = new HashMap<>();
        Map<EventType, Double> avgDurations = new HashMap<>();

        for (EconomicEvent event : history) {
            EventType apiType = mapEventType(event.getType());
            counts.put(apiType, counts.getOrDefault(apiType, 0) + 1);

            // Calculate duration if available
            if (event.getEndTime() != null && event.getStartTime() != null) {
                long duration = java.time.Duration.between(
                    event.getStartTime(),
                    event.getEndTime()
                ).toMinutes();

                avgDurations.merge(apiType, (double) duration, (old, newVal) -> (old + newVal) / 2);
            }
        }

        return new EventStatistics(history.size(), counts, avgDurations);
    }

    private EventType mapEventType(me.koyere.ecoxpert.modules.events.EconomicEventType internal) {
        // Map internal event types to API event types
        return switch (internal) {
            case BLACK_SWAN_EVENT -> EventType.BLACK_SWAN;
            case TECHNOLOGICAL_BREAKTHROUGH -> EventType.TECHNOLOGICAL_ADVANCEMENT;
            case RESOURCE_SHORTAGE -> EventType.RESOURCE_SCARCITY;
            case MARKET_CORRECTION -> EventType.MARKET_CRASH;
            case MARKET_DISCOVERY -> EventType.MARKET_CRASH; // Closest match
            case GOVERNMENT_STIMULUS -> EventType.GOVERNMENT_STIMULUS;
            case TRADE_BOOM -> EventType.TRADE_BOOM;
            case INVESTMENT_OPPORTUNITY -> EventType.INVESTMENT_OPPORTUNITY;
            case LUXURY_DEMAND -> EventType.LUXURY_DEMAND;
            case SEASONAL_DEMAND -> EventType.SEASONAL_DEMAND;
        };
    }

    private me.koyere.ecoxpert.modules.events.EconomicEventType unmapEventType(EventType external) {
        // Map API event types to internal event types
        return switch (external) {
            case BLACK_SWAN -> me.koyere.ecoxpert.modules.events.EconomicEventType.BLACK_SWAN_EVENT;
            case TECHNOLOGICAL_ADVANCEMENT -> me.koyere.ecoxpert.modules.events.EconomicEventType.TECHNOLOGICAL_BREAKTHROUGH;
            case RESOURCE_SCARCITY -> me.koyere.ecoxpert.modules.events.EconomicEventType.RESOURCE_SHORTAGE;
            case MARKET_CRASH -> me.koyere.ecoxpert.modules.events.EconomicEventType.MARKET_CORRECTION;
            case GOVERNMENT_STIMULUS -> me.koyere.ecoxpert.modules.events.EconomicEventType.GOVERNMENT_STIMULUS;
            case TRADE_BOOM -> me.koyere.ecoxpert.modules.events.EconomicEventType.TRADE_BOOM;
            case INVESTMENT_OPPORTUNITY -> me.koyere.ecoxpert.modules.events.EconomicEventType.INVESTMENT_OPPORTUNITY;
            case LUXURY_DEMAND -> me.koyere.ecoxpert.modules.events.EconomicEventType.LUXURY_DEMAND;
            case SEASONAL_DEMAND -> me.koyere.ecoxpert.modules.events.EconomicEventType.SEASONAL_DEMAND;
        };
    }
}
