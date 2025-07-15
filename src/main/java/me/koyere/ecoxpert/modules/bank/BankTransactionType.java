package me.koyere.ecoxpert.modules.bank;

/**
 * Bank transaction types
 * 
 * Defines all possible banking transaction types
 * for limit tracking and audit purposes.
 */
public enum BankTransactionType {
    
    DEPOSIT("Deposit", "Money deposited to bank account"),
    WITHDRAW("Withdraw", "Money withdrawn from bank account"),
    TRANSFER_IN("Transfer In", "Money transferred from another account"),
    TRANSFER_OUT("Transfer Out", "Money transferred to another account"),
    INTEREST("Interest", "Interest earned on account balance"),
    FEE("Fee", "Bank fee charged"),
    ADMIN_ADJUSTMENT("Admin Adjustment", "Administrative balance adjustment"),
    FREEZE("Freeze", "Account frozen"),
    UNFREEZE("Unfreeze", "Account unfrozen"),
    TIER_UPGRADE("Tier Upgrade", "Account tier upgraded");
    
    private final String displayName;
    private final String description;
    
    BankTransactionType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Check if transaction type affects daily limits
     */
    public boolean affectsDailyLimits() {
        return switch (this) {
            case DEPOSIT, WITHDRAW, TRANSFER_OUT -> true;
            default -> false;
        };
    }
    
    /**
     * Check if transaction type increases balance
     */
    public boolean increasesBalance() {
        return switch (this) {
            case DEPOSIT, TRANSFER_IN, INTEREST, ADMIN_ADJUSTMENT -> true;
            default -> false;
        };
    }
    
    /**
     * Check if transaction type decreases balance
     */
    public boolean decreasesBalance() {
        return switch (this) {
            case WITHDRAW, TRANSFER_OUT, FEE -> true;
            case ADMIN_ADJUSTMENT -> false; // Can be positive or negative
            default -> false;
        };
    }
    
    /**
     * Check if transaction requires approval
     */
    public boolean requiresApproval() {
        return switch (this) {
            case ADMIN_ADJUSTMENT, FREEZE, UNFREEZE -> true;
            default -> false;
        };
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}