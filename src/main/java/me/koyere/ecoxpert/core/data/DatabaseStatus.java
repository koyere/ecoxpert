package me.koyere.ecoxpert.core.data;

import java.time.Instant;

/**
 * Database status and health information
 */
public class DatabaseStatus {
    private final String currentType;
    private final String originalType;
    private final boolean connected;
    private final boolean healthy;
    private final boolean usingFallback;
    private final String fallbackReason;
    private final Instant lastHealthCheck;
    private final int connectionPoolSize;
    private final int activeConnections;
    
    private DatabaseStatus(Builder builder) {
        this.currentType = builder.currentType;
        this.originalType = builder.originalType;
        this.connected = builder.connected;
        this.healthy = builder.healthy;
        this.usingFallback = builder.usingFallback;
        this.fallbackReason = builder.fallbackReason;
        this.lastHealthCheck = builder.lastHealthCheck;
        this.connectionPoolSize = builder.connectionPoolSize;
        this.activeConnections = builder.activeConnections;
    }
    
    public String getCurrentType() { return currentType; }
    public String getOriginalType() { return originalType; }
    public boolean isConnected() { return connected; }
    public boolean isHealthy() { return healthy; }
    public boolean isUsingFallback() { return usingFallback; }
    public String getFallbackReason() { return fallbackReason; }
    public Instant getLastHealthCheck() { return lastHealthCheck; }
    public int getConnectionPoolSize() { return connectionPoolSize; }
    public int getActiveConnections() { return activeConnections; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String currentType;
        private String originalType;
        private boolean connected = false;
        private boolean healthy = false;
        private boolean usingFallback = false;
        private String fallbackReason;
        private Instant lastHealthCheck = Instant.now();
        private int connectionPoolSize = 0;
        private int activeConnections = 0;
        
        public Builder currentType(String currentType) { this.currentType = currentType; return this; }
        public Builder originalType(String originalType) { this.originalType = originalType; return this; }
        public Builder connected(boolean connected) { this.connected = connected; return this; }
        public Builder healthy(boolean healthy) { this.healthy = healthy; return this; }
        public Builder usingFallback(boolean usingFallback) { this.usingFallback = usingFallback; return this; }
        public Builder fallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; return this; }
        public Builder lastHealthCheck(Instant lastHealthCheck) { this.lastHealthCheck = lastHealthCheck; return this; }
        public Builder connectionPoolSize(int connectionPoolSize) { this.connectionPoolSize = connectionPoolSize; return this; }
        public Builder activeConnections(int activeConnections) { this.activeConnections = activeConnections; return this; }
        
        public DatabaseStatus build() {
            return new DatabaseStatus(this);
        }
    }
}