package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bank transaction record
 * 
 * Immutable transaction record with complete audit trail,
 * security tracking, and balance verification data.
 */
public class BankTransaction {
    
    private final long transactionId;
    private final UUID accountId;
    private final BankTransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceBefore;
    private final BigDecimal balanceAfter;
    private final LocalDateTime timestamp;
    private final String description;
    private final String reference;
    
    // Related transaction data
    private final UUID relatedAccountId; // For transfers
    private final String adminId; // For admin operations
    private final String ipAddress; // Security tracking
    private final String reason; // For freezes, fees, etc.
    
    // Verification data
    private final String transactionHash; // For integrity verification
    private final boolean verified;
    
    private BankTransaction(Builder builder) {
        this.transactionId = builder.transactionId;
        this.accountId = builder.accountId;
        this.type = builder.type;
        this.amount = builder.amount;
        this.balanceBefore = builder.balanceBefore;
        this.balanceAfter = builder.balanceAfter;
        this.timestamp = builder.timestamp != null ? builder.timestamp : LocalDateTime.now();
        this.description = builder.description;
        this.reference = builder.reference;
        this.relatedAccountId = builder.relatedAccountId;
        this.adminId = builder.adminId;
        this.ipAddress = builder.ipAddress;
        this.reason = builder.reason;
        this.transactionHash = generateTransactionHash();
        this.verified = true;
    }
    
    /**
     * Generate transaction hash for integrity verification
     */
    private String generateTransactionHash() {
        String data = String.format("%d-%s-%s-%s-%s-%s",
            transactionId, accountId, type, amount, balanceBefore, balanceAfter);
        return String.valueOf(data.hashCode());
    }
    
    /**
     * Verify transaction integrity
     */
    public boolean verifyIntegrity() {
        String expectedHash = generateTransactionHash();
        return expectedHash.equals(this.transactionHash);
    }
    
    /**
     * Check if transaction is a credit (increases balance)
     */
    public boolean isCredit() {
        return type.increasesBalance();
    }
    
    /**
     * Check if transaction is a debit (decreases balance)
     */
    public boolean isDebit() {
        return type.decreasesBalance();
    }
    
    /**
     * Get formatted transaction description
     */
    public String getFormattedDescription() {
        if (description != null && !description.isEmpty()) {
            return description;
        }
        
        return switch (type) {
            case DEPOSIT -> "Deposit to bank account";
            case WITHDRAW -> "Withdrawal from bank account";
            case TRANSFER_IN -> "Transfer received from " + (relatedAccountId != null ? relatedAccountId : "unknown");
            case TRANSFER_OUT -> "Transfer sent to " + (relatedAccountId != null ? relatedAccountId : "unknown");
            case INTEREST -> "Interest earned";
            case FEE -> "Bank fee charged";
            case ADMIN_ADJUSTMENT -> "Administrative adjustment" + (reason != null ? ": " + reason : "");
            case FREEZE -> "Account frozen" + (reason != null ? ": " + reason : "");
            case UNFREEZE -> "Account unfrozen";
            case TIER_UPGRADE -> "Account tier upgraded";
        };
    }
    
    /**
     * Get transaction age in days
     */
    public long getAgeInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(timestamp, LocalDateTime.now());
    }
    
    // === Builder Pattern ===
    
    public static class Builder {
        private long transactionId;
        private UUID accountId;
        private BankTransactionType type;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private LocalDateTime timestamp;
        private String description;
        private String reference;
        private UUID relatedAccountId;
        private String adminId;
        private String ipAddress;
        private String reason;
        
        public Builder setTransactionId(long transactionId) {
            this.transactionId = transactionId;
            return this;
        }
        
        public Builder setAccountId(UUID accountId) {
            this.accountId = accountId;
            return this;
        }
        
        public Builder setType(BankTransactionType type) {
            this.type = type;
            return this;
        }
        
        public Builder setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }
        
        public Builder setBalanceBefore(BigDecimal balanceBefore) {
            this.balanceBefore = balanceBefore;
            return this;
        }
        
        public Builder setBalanceAfter(BigDecimal balanceAfter) {
            this.balanceAfter = balanceAfter;
            return this;
        }
        
        public Builder setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }
        
        public Builder setReference(String reference) {
            this.reference = reference;
            return this;
        }
        
        public Builder setRelatedAccountId(UUID relatedAccountId) {
            this.relatedAccountId = relatedAccountId;
            return this;
        }
        
        public Builder setAdminId(String adminId) {
            this.adminId = adminId;
            return this;
        }
        
        public Builder setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }
        
        public Builder setReason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public BankTransaction build() {
            // Validation
            if (accountId == null) throw new IllegalArgumentException("Account ID cannot be null");
            if (type == null) throw new IllegalArgumentException("Transaction type cannot be null");
            if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
            if (balanceBefore == null) throw new IllegalArgumentException("Balance before cannot be null");
            if (balanceAfter == null) throw new IllegalArgumentException("Balance after cannot be null");
            
            // Verify balance calculation
            BigDecimal expectedBalanceAfter = balanceBefore;
            if (type.increasesBalance()) {
                expectedBalanceAfter = balanceBefore.add(amount);
            } else if (type.decreasesBalance()) {
                expectedBalanceAfter = balanceBefore.subtract(amount);
            }
            
            if (expectedBalanceAfter.compareTo(balanceAfter) != 0) {
                throw new IllegalArgumentException("Balance calculation does not match: expected " + 
                    expectedBalanceAfter + ", got " + balanceAfter);
            }
            
            return new BankTransaction(this);
        }
    }
    
    // === Getters ===
    
    public long getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
    public BankTransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public UUID getRelatedAccountId() { return relatedAccountId; }
    public String getAdminId() { return adminId; }
    public String getIpAddress() { return ipAddress; }
    public String getReason() { return reason; }
    public String getTransactionHash() { return transactionHash; }
    public boolean isVerified() { return verified; }
    
    @Override
    public String toString() {
        return String.format("BankTransaction{id=%d, type=%s, amount=%s, account=%s, timestamp=%s}",
            transactionId, type, amount, accountId, timestamp);
    }
}