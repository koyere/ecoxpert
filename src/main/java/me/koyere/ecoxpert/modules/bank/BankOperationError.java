package me.koyere.ecoxpert.modules.bank;

/**
 * Bank operation error types
 * 
 * Defines specific error conditions for banking operations
 * to enable proper error handling and user feedback.
 */
public enum BankOperationError {
    
    INSUFFICIENT_FUNDS("Insufficient funds in account"),
    DAILY_LIMIT_EXCEEDED("Daily transaction limit exceeded"),
    ACCOUNT_FROZEN("Account is frozen"),
    ACCOUNT_NOT_FOUND("Bank account not found"),
    INVALID_AMOUNT("Invalid transaction amount"),
    PERMISSION_DENIED("Permission denied for operation"),
    SYSTEM_ERROR("Banking system error"),
    DATABASE_ERROR("Database operation failed"),
    TIER_REQUIREMENTS_NOT_MET("Account tier requirements not met"),
    MINIMUM_BALANCE_VIOLATION("Transaction would violate minimum balance"),
    SECURITY_VIOLATION("Security policy violation"),
    CONCURRENT_MODIFICATION("Account modified by another transaction");
    
    private final String description;
    
    BankOperationError(String description) {
        this.description = description;
    }
    
    /**
     * Check if error is user-correctable
     */
    public boolean isUserCorrectable() {
        return switch (this) {
            case INSUFFICIENT_FUNDS, DAILY_LIMIT_EXCEEDED, INVALID_AMOUNT, 
                 TIER_REQUIREMENTS_NOT_MET, MINIMUM_BALANCE_VIOLATION -> true;
            case ACCOUNT_FROZEN, ACCOUNT_NOT_FOUND, PERMISSION_DENIED, 
                 SYSTEM_ERROR, DATABASE_ERROR, SECURITY_VIOLATION, 
                 CONCURRENT_MODIFICATION -> false;
        };
    }
    
    /**
     * Check if error requires admin intervention
     */
    public boolean requiresAdminIntervention() {
        return switch (this) {
            case ACCOUNT_FROZEN, PERMISSION_DENIED, SYSTEM_ERROR, 
                 DATABASE_ERROR, SECURITY_VIOLATION -> true;
            default -> false;
        };
    }
    
    /**
     * Get severity level
     */
    public ErrorSeverity getSeverity() {
        return switch (this) {
            case INSUFFICIENT_FUNDS, DAILY_LIMIT_EXCEEDED, INVALID_AMOUNT -> ErrorSeverity.LOW;
            case TIER_REQUIREMENTS_NOT_MET, MINIMUM_BALANCE_VIOLATION -> ErrorSeverity.MEDIUM;
            case ACCOUNT_FROZEN, ACCOUNT_NOT_FOUND, PERMISSION_DENIED -> ErrorSeverity.HIGH;
            case SYSTEM_ERROR, DATABASE_ERROR, SECURITY_VIOLATION, 
                 CONCURRENT_MODIFICATION -> ErrorSeverity.CRITICAL;
        };
    }
    
    public String getDescription() { return description; }
    
    public enum ErrorSeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}