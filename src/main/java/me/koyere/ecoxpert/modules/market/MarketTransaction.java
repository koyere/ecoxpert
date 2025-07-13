package me.koyere.ecoxpert.modules.market;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a market transaction record
 * 
 * Immutable data class for tracking buy/sell transactions
 * in the market system with comprehensive audit trail.
 */
public final class MarketTransaction {
    
    private final long transactionId;
    private final UUID playerUuid;
    private final String playerName;
    private final Material material;
    private final TransactionType type;
    private final int quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalAmount;
    private final LocalDateTime timestamp;
    private final String description;
    
    /**
     * Transaction type enumeration
     */
    public enum TransactionType {
        BUY("buy"),
        SELL("sell");
        
        private final String displayName;
        
        TransactionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static TransactionType fromString(String type) {
            for (TransactionType t : values()) {
                if (t.displayName.equalsIgnoreCase(type)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown transaction type: " + type);
        }
    }
    
    /**
     * Constructor for market transaction
     */
    public MarketTransaction(long transactionId, UUID playerUuid, String playerName,
                           Material material, TransactionType type, int quantity,
                           BigDecimal unitPrice, BigDecimal totalAmount,
                           LocalDateTime timestamp, String description) {
        this.transactionId = transactionId;
        this.playerUuid = Objects.requireNonNull(playerUuid, "Player UUID cannot be null");
        this.playerName = Objects.requireNonNull(playerName, "Player name cannot be null");
        this.material = Objects.requireNonNull(material, "Material cannot be null");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null");
        this.quantity = Math.max(1, quantity);
        this.unitPrice = Objects.requireNonNull(unitPrice, "Unit price cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.description = description != null ? description : generateDefaultDescription();
    }
    
    /**
     * Builder for creating market transactions
     */
    public static Builder builder() {
        return new Builder();
    }
    
    // === Getters ===
    
    public long getTransactionId() {
        return transactionId;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public Material getMaterial() {
        return material;
    }
    
    public TransactionType getType() {
        return type;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getDescription() {
        return description;
    }
    
    // === Utility Methods ===
    
    /**
     * Check if this is a buy transaction
     */
    public boolean isBuyTransaction() {
        return type == TransactionType.BUY;
    }
    
    /**
     * Check if this is a sell transaction
     */
    public boolean isSellTransaction() {
        return type == TransactionType.SELL;
    }
    
    /**
     * Generate default description based on transaction data
     */
    private String generateDefaultDescription() {
        return String.format("%s %dx %s for %s each",
            type.getDisplayName().toUpperCase(),
            quantity,
            material.name().toLowerCase().replace('_', ' '),
            unitPrice
        );
    }
    
    /**
     * Get formatted transaction summary
     */
    public String getFormattedSummary() {
        return String.format("%s: %s %dx %s = %s",
            type.getDisplayName().toUpperCase(),
            playerName,
            quantity,
            material.name().toLowerCase().replace('_', ' '),
            totalAmount
        );
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketTransaction that = (MarketTransaction) obj;
        return transactionId == that.transactionId;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    @Override
    public String toString() {
        return String.format("MarketTransaction{id=%d, player=%s, type=%s, material=%s, quantity=%d, total=%s}",
            transactionId, playerName, type, material, quantity, totalAmount);
    }
    
    /**
     * Builder pattern for creating MarketTransaction instances
     */
    public static class Builder {
        private long transactionId;
        private UUID playerUuid;
        private String playerName;
        private Material material;
        private TransactionType type;
        private int quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalAmount;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String description;
        
        public Builder transactionId(long transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        
        public Builder player(UUID uuid, String name) {
            this.playerUuid = uuid;
            this.playerName = name;
            return this;
        }
        
        public Builder material(Material material) {
            this.material = material;
            return this;
        }
        
        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }
        
        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public Builder unitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
            this.totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
            return this;
        }
        
        public Builder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public MarketTransaction build() {
            return new MarketTransaction(
                transactionId, playerUuid, playerName, material, type,
                quantity, unitPrice, totalAmount, timestamp, description
            );
        }
    }
}