package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;

/**
 * Economic events management service
 */
public interface EventsService {
    /**
     * Get all currently active events
     * @return List of active events
     */
    java.util.List<EventInfo> getActiveEvents();

    /**
     * Get event by ID
     * @param eventId Event UUID
     * @return Event information or null
     */
    java.util.Optional<EventInfo> getEventById(java.util.UUID eventId);

    /**
     * Check if a specific event type is active
     * @param type Event type
     * @return true if event is active
     */
    boolean isEventActive(EventType type);

    /**
     * Get remaining cooldowns for all event types
     * @return Map of event types to cooldown time in milliseconds
     */
    java.util.Map<EventType, Long> getRemainingCooldowns();

    /**
     * Get event statistics
     * @param days Number of days to analyze
     * @return Event statistics
     */
    EventStatistics getEventStatistics(int days);
}
