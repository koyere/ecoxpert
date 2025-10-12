package me.koyere.ecoxpert.api.dto;

import java.util.Map;

/** Event statistics */
public class EventStatistics {
    private final int totalEvents;
    private final Map<EventType, Integer> eventCounts;
    private final Map<EventType, Double> averageDurations;

    public EventStatistics(int totalEvents, Map<EventType, Integer> eventCounts, Map<EventType, Double> averageDurations) {
        this.totalEvents = totalEvents;
        this.eventCounts = eventCounts;
        this.averageDurations = averageDurations;
    }

    public int getTotalEvents() { return totalEvents; }
    public Map<EventType, Integer> getEventCounts() { return eventCounts; }
    public Map<EventType, Double> getAverageDurations() { return averageDurations; }
}
