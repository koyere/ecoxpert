package me.koyere.ecoxpert.modules.bank;

import java.math.BigDecimal;

/**
 * Bank operation result
 * 
 * Immutable result object for banking operations with
 * detailed success/failure information and error handling.
 */
public class BankOperationResult {
    
    private final boolean success;
    private final String message;
    private final BankOperationError errorType;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final BankTransaction transaction;
    private final String errorDetails;
    
    private BankOperationResult(boolean success, String message, BankOperationError errorType,
                               BigDecimal amount, BigDecimal newBalance, BankTransaction transaction,
                               String errorDetails) {
        this.success = success;
        this.message = message;
        this.errorType = errorType;
        this.amount = amount;
        this.newBalance = newBalance;
        this.transaction = transaction;
        this.errorDetails = errorDetails;
    }
    
    // === Success Factory Methods ===
    
    public static BankOperationResult success(String message, BigDecimal amount, 
                                            BigDecimal newBalance, BankTransaction transaction) {
        return new BankOperationResult(true, message, null, amount, newBalance, transaction, null);
    }
    
    public static BankOperationResult success(String message, BankTransaction transaction) {
        return new BankOperationResult(true, message, null, transaction.getAmount(), 
                                     transaction.getBalanceAfter(), transaction, null);
    }
    
    public static BankOperationResult success(String message) {
        return new BankOperationResult(true, message, null, null, null, null, null);
    }
    
    // === Failure Factory Methods ===
    
    public static BankOperationResult failure(String message, BankOperationError errorType) {
        return new BankOperationResult(false, message, errorType, null, null, null, null);
    }
    
    public static BankOperationResult failure(String message, BankOperationError errorType, 
                                            String errorDetails) {
        return new BankOperationResult(false, message, errorType, null, null, null, errorDetails);
    }
    
    public static BankOperationResult failure(String message, BankOperationError errorType,
                                            BigDecimal amount) {
        return new BankOperationResult(false, message, errorType, amount, null, null, null);
    }
    
    // === Specific Error Results ===
    
    public static BankOperationResult insufficientFunds(BigDecimal available, BigDecimal requested) {
        String message = String.format("Insufficient funds: have %s, need %s", available, requested);
        return failure(message, BankOperationError.INSUFFICIENT_FUNDS);
    }
    
    public static BankOperationResult dailyLimitExceeded(BankTransactionType type, 
                                                        BigDecimal limit, BigDecimal requested) {
        String message = String.format("Daily %s limit exceeded: limit %s, requested %s", 
                                     type.getDisplayName().toLowerCase(), limit, requested);
        return failure(message, BankOperationError.DAILY_LIMIT_EXCEEDED);
    }
    
    public static BankOperationResult accountFrozen(String reason) {
        String message = "Account is frozen" + (reason != null ? ": " + reason : "");
        return failure(message, BankOperationError.ACCOUNT_FROZEN, reason);
    }
    
    public static BankOperationResult accountNotFound() {
        return failure("Bank account not found", BankOperationError.ACCOUNT_NOT_FOUND);
    }
    
    public static BankOperationResult invalidAmount() {
        return failure("Invalid amount specified", BankOperationError.INVALID_AMOUNT);
    }
    
    public static BankOperationResult systemError(String details) {
        return failure("Banking system error", BankOperationError.SYSTEM_ERROR, details);
    }
    
    public static BankOperationResult permissionDenied() {
        return failure("Permission denied for this operation", BankOperationError.PERMISSION_DENIED);
    }
    
    // === Utility Methods ===
    
    /**
     * Check if operation was successful
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Check if operation failed
     */
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * Get formatted message for display
     */
    public String getFormattedMessage() {
        if (success) {
            return "§a" + message; // Green for success
        } else {
            return "§c" + message; // Red for error
        }
    }
    
    /**
     * Check if error is recoverable
     */
    public boolean isRecoverableError() {
        if (success || errorType == null) return false;
        
        return switch (errorType) {
            case INSUFFICIENT_FUNDS, DAILY_LIMIT_EXCEEDED, INVALID_AMOUNT, 
                 TIER_REQUIREMENTS_NOT_MET, MINIMUM_BALANCE_VIOLATION -> true;
            case ACCOUNT_FROZEN, ACCOUNT_NOT_FOUND, PERMISSION_DENIED, SYSTEM_ERROR,
                 DATABASE_ERROR, SECURITY_VIOLATION, CONCURRENT_MODIFICATION -> false;
        };
    }
    
    // === Getters ===
    
    public String getMessage() { return message; }
    public BankOperationError getErrorType() { return errorType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getNewBalance() { return newBalance; }
    public BankTransaction getTransaction() { return transaction; }
    public String getErrorDetails() { return errorDetails; }
    
    @Override
    public String toString() {
        if (success) {
            return String.format("BankOperationResult{SUCCESS: %s}", message);
        } else {
            return String.format("BankOperationResult{FAILURE: %s, error=%s}", message, errorType);
        }
    }
}