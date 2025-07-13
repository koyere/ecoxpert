package me.koyere.ecoxpert.core.failsafe;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable record of an economy transaction for audit and recovery purposes
 */
public class TransactionRecord {
    private final String transactionId;
    private final UUID fromPlayer;
    private final UUID toPlayer;
    private final BigDecimal amount;
    private final String type;
    private final String reason;
    private final Instant timestamp;
    private final BigDecimal fromBalanceBefore;
    private final BigDecimal fromBalanceAfter;
    private final BigDecimal toBalanceBefore;
    private final BigDecimal toBalanceAfter;
    private final boolean successful;
    private final String errorMessage;
    
    private TransactionRecord(Builder builder) {
        this.transactionId = builder.transactionId;
        this.fromPlayer = builder.fromPlayer;
        this.toPlayer = builder.toPlayer;
        this.amount = builder.amount;
        this.type = builder.type;
        this.reason = builder.reason;
        this.timestamp = builder.timestamp;
        this.fromBalanceBefore = builder.fromBalanceBefore;
        this.fromBalanceAfter = builder.fromBalanceAfter;
        this.toBalanceBefore = builder.toBalanceBefore;
        this.toBalanceAfter = builder.toBalanceAfter;
        this.successful = builder.successful;
        this.errorMessage = builder.errorMessage;
    }
    
    public String getTransactionId() { return transactionId; }
    public UUID getFromPlayer() { return fromPlayer; }
    public UUID getToPlayer() { return toPlayer; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }
    public BigDecimal getFromBalanceBefore() { return fromBalanceBefore; }
    public BigDecimal getFromBalanceAfter() { return fromBalanceAfter; }
    public BigDecimal getToBalanceBefore() { return toBalanceBefore; }
    public BigDecimal getToBalanceAfter() { return toBalanceAfter; }
    public boolean isSuccessful() { return successful; }
    public String getErrorMessage() { return errorMessage; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String transactionId;
        private UUID fromPlayer;
        private UUID toPlayer;
        private BigDecimal amount;
        private String type;
        private String reason;
        private Instant timestamp = Instant.now();
        private BigDecimal fromBalanceBefore;
        private BigDecimal fromBalanceAfter;
        private BigDecimal toBalanceBefore;
        private BigDecimal toBalanceAfter;
        private boolean successful = true;
        private String errorMessage;
        
        public Builder transactionId(String transactionId) { this.transactionId = transactionId; return this; }
        public Builder fromPlayer(UUID fromPlayer) { this.fromPlayer = fromPlayer; return this; }
        public Builder toPlayer(UUID toPlayer) { this.toPlayer = toPlayer; return this; }
        public Builder amount(BigDecimal amount) { this.amount = amount; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder fromBalanceBefore(BigDecimal balance) { this.fromBalanceBefore = balance; return this; }
        public Builder fromBalanceAfter(BigDecimal balance) { this.fromBalanceAfter = balance; return this; }
        public Builder toBalanceBefore(BigDecimal balance) { this.toBalanceBefore = balance; return this; }
        public Builder toBalanceAfter(BigDecimal balance) { this.toBalanceAfter = balance; return this; }
        public Builder successful(boolean successful) { this.successful = successful; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        
        public TransactionRecord build() {
            if (transactionId == null || transactionId.trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction ID is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Amount must be non-negative");
            }
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("Transaction type is required");
            }
            
            return new TransactionRecord(this);
        }
    }
}