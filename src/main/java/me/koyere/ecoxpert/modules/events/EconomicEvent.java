package me.koyere.ecoxpert.modules.events;

import org.bukkit.Material;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Economic Event
 * 
 * Represents a dynamic economic event in the EcoXpert intelligent economy system.
 * Each event encapsulates all the data and behavior for a specific economic scenario
 * that affects the server economy.
 */
public class EconomicEvent {
    
    private final String id;
    private final EconomicEventType type;
    private final String name;
    private final String description;
    private final LocalDateTime startTime;
    private final int duration; // in minutes
    private final double intensity; // 0.0 to 1.0
    private final List<Material> affectedItems;
    private final Map<String, Object> parameters;
    
    private EventStatus status;
    private LocalDateTime endTime;
    private String endReason;
    
    public EconomicEvent(String id, EconomicEventType type, String name, String description,
                        LocalDateTime startTime, int duration, double intensity,
                        List<Material> affectedItems, Map<String, Object> parameters,
                        EventStatus status) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.duration = duration;
        this.intensity = intensity;
        this.affectedItems = affectedItems;
        this.parameters = parameters;
        this.status = status;
    }
    
    /**
     * Check if this event has expired based on its duration
     */
    public boolean hasExpired() {
        if (status != EventStatus.ACTIVE) {
            return true;
        }
        
        LocalDateTime expectedEndTime = startTime.plusMinutes(duration);
        return LocalDateTime.now().isAfter(expectedEndTime);
    }
    
    /**
     * Get the remaining time for this event in minutes
     */
    public long getRemainingMinutes() {
        if (status != EventStatus.ACTIVE) {
            return 0;
        }
        
        LocalDateTime expectedEndTime = startTime.plusMinutes(duration);
        Duration remaining = Duration.between(LocalDateTime.now(), expectedEndTime);
        
        return Math.max(0, remaining.toMinutes());
    }
    
    /**
     * Get the progress of this event (0.0 to 1.0)
     */
    public double getProgress() {
        if (status != EventStatus.ACTIVE) {
            return 1.0;
        }
        
        Duration elapsed = Duration.between(startTime, LocalDateTime.now());
        double elapsedMinutes = elapsed.toMinutes();
        
        return Math.min(1.0, elapsedMinutes / duration);
    }
    
    /**
     * End this event with a specific reason
     */
    public void end(String reason) {
        this.status = EventStatus.COMPLETED;
        this.endTime = LocalDateTime.now();
        this.endReason = reason;
    }
    
    /**
     * Cancel this event with a specific reason
     */
    public void cancel(String reason) {
        this.status = EventStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
        this.endReason = reason;
    }
    
    /**
     * Get a parameter value with type safety
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Get a parameter value with default fallback
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value != null) {
            try {
                return (T) value;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * Check if this event affects a specific material
     */
    public boolean affects(Material material) {
        return affectedItems.contains(material);
    }
    
    /**
     * Get a formatted string representation of this event
     */
    public String getFormattedInfo() {
        StringBuilder info = new StringBuilder();
        info.append("§6§l").append(name).append("\n");
        info.append("§7").append(description).append("\n");
        info.append("§eType: §f").append(type.getDisplayName()).append("\n");
        info.append("§eStatus: §f").append(getStatusColor()).append(status.getDisplayName()).append("\n");
        info.append("§eIntensity: §f").append(String.format("%.1f%%", intensity * 100)).append("\n");
        
        if (status == EventStatus.ACTIVE) {
            info.append("§eRemaining: §f").append(getRemainingMinutes()).append(" minutes\n");
            info.append("§eProgress: §f").append(String.format("%.1f%%", getProgress() * 100)).append("\n");
        }
        
        if (!affectedItems.isEmpty()) {
            info.append("§eAffected Items: §f");
            for (int i = 0; i < affectedItems.size(); i++) {
                if (i > 0) info.append(", ");
                info.append(affectedItems.get(i).name().toLowerCase().replace('_', ' '));
            }
            info.append("\n");
        }
        
        return info.toString();
    }
    
    /**
     * Get color code based on event status
     */
    private String getStatusColor() {
        return switch (status) {
            case ACTIVE -> "§a";
            case COMPLETED -> "§2";
            case CANCELLED -> "§c";
            case PENDING -> "§e";
        };
    }
    
    // === Getters and Setters ===
    
    public String getId() {
        return id;
    }
    
    public EconomicEventType getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public double getIntensity() {
        return intensity;
    }
    
    public List<Material> getAffectedItems() {
        return affectedItems;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public EventStatus getStatus() {
        return status;
    }
    
    public void setStatus(EventStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public String getEndReason() {
        return endReason;
    }
    
    @Override
    public String toString() {
        return String.format("EconomicEvent{id='%s', type=%s, name='%s', status=%s, intensity=%.2f, duration=%d}",
                id, type, name, status, intensity, duration);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        EconomicEvent that = (EconomicEvent) obj;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /**
     * Event Status enumeration
     */
    public enum EventStatus {
        PENDING("Pending"),
        ACTIVE("Active"),
        COMPLETED("Completed"),
        CANCELLED("Cancelled");
        
        private final String displayName;
        
        EventStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}