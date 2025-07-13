package me.koyere.ecoxpert.core.failsafe;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Economy failsafe and recovery management system
 * 
 * Provides enterprise-grade protection for economy operations with:
 * - Automatic transaction backup and recovery
 * - Data corruption detection and repair
 * - Emergency read-only mode activation
 * - Audit trail for all operations
 * - Consistency checks and validation
 */
public interface EconomyFailsafeManager {
    
    /**
     * Initialize the failsafe system
     */
    void initialize();
    
    /**
     * Shutdown the failsafe system
     */
    void shutdown();
    
    /**
     * Create a backup snapshot of current economy state
     * 
     * @param reason Reason for creating the backup
     * @return CompletableFuture that completes when backup is created
     */
    CompletableFuture<Void> createBackup(String reason);
    
    /**
     * Restore economy state from the most recent valid backup
     * 
     * @return CompletableFuture that completes when restore is finished
     */
    CompletableFuture<Void> restoreFromBackup();
    
    /**
     * Restore economy state from a specific backup
     * 
     * @param backupId ID of the backup to restore
     * @return CompletableFuture that completes when restore is finished
     */
    CompletableFuture<Void> restoreFromBackup(String backupId);
    
    /**
     * Perform a comprehensive consistency check of economy data
     * 
     * @return CompletableFuture with the validation result
     */
    CompletableFuture<ValidationResult> performConsistencyCheck();
    
    /**
     * Enter emergency read-only mode
     * Prevents all write operations to protect data integrity
     * 
     * @param reason Reason for entering read-only mode
     */
    void enterReadOnlyMode(String reason);
    
    /**
     * Exit emergency read-only mode
     * Resumes normal write operations
     */
    void exitReadOnlyMode();
    
    /**
     * Check if the system is in read-only mode
     * 
     * @return true if in read-only mode
     */
    boolean isReadOnlyMode();
    
    /**
     * Get the reason for being in read-only mode
     * 
     * @return reason string, or null if not in read-only mode
     */
    String getReadOnlyReason();
    
    /**
     * Record a transaction for audit trail and recovery
     * 
     * @param transaction Transaction details
     * @return CompletableFuture that completes when recorded
     */
    CompletableFuture<Void> recordTransaction(TransactionRecord transaction);
    
    /**
     * Get transaction history for a player
     * 
     * @param playerUuid Player UUID
     * @param limit Maximum number of transactions to return
     * @return CompletableFuture with transaction history
     */
    CompletableFuture<TransactionHistory> getTransactionHistory(UUID playerUuid, int limit);
    
    /**
     * Detect and repair data corruption automatically
     * 
     * @return CompletableFuture with repair result
     */
    CompletableFuture<RepairResult> detectAndRepairCorruption();
    
    /**
     * Create a pre-transaction checkpoint
     * Used for atomic operations that can be rolled back
     * 
     * @param operationId Unique operation identifier
     * @return CompletableFuture with checkpoint ID
     */
    CompletableFuture<String> createCheckpoint(String operationId);
    
    /**
     * Rollback to a specific checkpoint
     * 
     * @param checkpointId Checkpoint ID to rollback to
     * @return CompletableFuture that completes when rollback is finished
     */
    CompletableFuture<Void> rollbackToCheckpoint(String checkpointId);
    
    /**
     * Commit and remove a checkpoint (operation completed successfully)
     * 
     * @param checkpointId Checkpoint ID to commit
     * @return CompletableFuture that completes when checkpoint is committed
     */
    CompletableFuture<Void> commitCheckpoint(String checkpointId);
    
    /**
     * Get system health status
     * 
     * @return Health status of the failsafe system
     */
    FailsafeHealth getHealthStatus();
    
    /**
     * Force a manual health check
     * 
     * @return CompletableFuture that completes when health check is done
     */
    CompletableFuture<FailsafeHealth> performHealthCheck();
}