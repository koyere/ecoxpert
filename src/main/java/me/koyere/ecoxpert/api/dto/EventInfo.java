package me.koyere.ecoxpert.api.dto;

import java.time.Instant;
import java.util.UUID;

/** Event information */
public class EventInfo {
    private final UUID eventId;
    private final EventType type;
    private final String name;
    private final Instant startedAt;
    private final Instant endsAt;
    private final String description;

    public EventInfo(UUID eventId, EventType type, String name, Instant startedAt, Instant endsAt, String description) {
        this.eventId = eventId;
        this.type = type;
        this.name = name;
        this.startedAt = startedAt;
        this.endsAt = endsAt;
        this.description = description;
    }

    public UUID getEventId() { return eventId; }
    public EventType getType() { return type; }
    public String getName() { return name; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndsAt() { return endsAt; }
    public String getDescription() { return description; }
}
