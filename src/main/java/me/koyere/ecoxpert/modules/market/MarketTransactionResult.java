package me.koyere.ecoxpert.modules.market;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of a market transaction operation
 * 
 * Immutable result class that indicates success/failure
 * and provides relevant transaction data.
 */
public final class MarketTransactionResult {
    
    private final boolean success;
    private final String message;
    private final MarketTransaction transaction;
    private final TransactionError error;
    
    /**
     * Transaction error enumeration
     */
    public enum TransactionError {
        INSUFFICIENT_FUNDS("market.error.insufficient-funds"),
        INSUFFICIENT_ITEMS("market.error.insufficient-items"),
        MARKET_CLOSED("market.error.market-closed"),
        ITEM_NOT_AVAILABLE("market.error.item-not-available"),
        ITEM_NOT_BUYABLE("market.error.item-not-buyable"),
        ITEM_NOT_SELLABLE("market.error.item-not-sellable"),
        INVENTORY_FULL("market.error.inventory-full"),
        INVALID_QUANTITY("market.error.invalid-quantity"),
        PRICE_CHANGED("market.error.price-changed"),
        SYSTEM_ERROR("market.error.system-error");
        
        private final String messageKey;
        
        TransactionError(String messageKey) {
            this.messageKey = messageKey;
        }
        
        public String getMessageKey() {
            return messageKey;
        }
    }
    
    private MarketTransactionResult(boolean success, String message, 
                                  MarketTransaction transaction, TransactionError error) {
        this.success = success;
        this.message = message;
        this.transaction = transaction;
        this.error = error;
    }
    
    /**
     * Create successful transaction result
     */
    public static MarketTransactionResult success(MarketTransaction transaction, String message) {
        return new MarketTransactionResult(true, message, transaction, null);
    }
    
    /**
     * Create failed transaction result
     */
    public static MarketTransactionResult failure(TransactionError error, String message) {
        return new MarketTransactionResult(false, message, null, error);
    }
    
    /**
     * Create failed transaction result with custom message
     */
    public static MarketTransactionResult failure(String message) {
        return new MarketTransactionResult(false, message, null, TransactionError.SYSTEM_ERROR);
    }
    
    // === Getters ===
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Optional<MarketTransaction> getTransaction() {
        return Optional.ofNullable(transaction);
    }
    
    public Optional<TransactionError> getError() {
        return Optional.ofNullable(error);
    }
    
    // === Utility Methods ===
    
    /**
     * Get transaction amount if successful
     */
    public Optional<BigDecimal> getTransactionAmount() {
        return getTransaction().map(MarketTransaction::getTotalAmount);
    }
    
    /**
     * Get transaction quantity if successful
     */
    public Optional<Integer> getTransactionQuantity() {
        return getTransaction().map(MarketTransaction::getQuantity);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MarketTransactionResult that = (MarketTransactionResult) obj;
        return success == that.success &&
               Objects.equals(message, that.message) &&
               Objects.equals(transaction, that.transaction) &&
               error == that.error;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(success, message, transaction, error);
    }
    
    @Override
    public String toString() {
        return String.format("MarketTransactionResult{success=%s, message='%s', error=%s}",
            success, message, error);
    }
}