package me.koyere.ecoxpert.core.data;

import java.util.concurrent.CompletableFuture;

/**
 * Database transaction wrapper
 * 
 * Provides transaction management with automatic rollback
 * on exceptions and proper resource cleanup.
 */
public interface DatabaseTransaction extends AutoCloseable {
    
    /**
     * Execute an update within this transaction
     * 
     * @param sql SQL statement
     * @param params Statement parameters
     * @return CompletableFuture with affected rows count
     */
    CompletableFuture<Integer> executeUpdate(String sql, Object... params);
    
    /**
     * Execute a query within this transaction
     * 
     * @param sql SQL statement
     * @param params Statement parameters
     * @return CompletableFuture with query results
     */
    CompletableFuture<QueryResult> executeQuery(String sql, Object... params);
    
    /**
     * Commit the transaction
     * 
     * @return CompletableFuture that completes when committed
     */
    CompletableFuture<Void> commit();
    
    /**
     * Rollback the transaction
     * 
     * @return CompletableFuture that completes when rolled back
     */
    CompletableFuture<Void> rollback();
    
    /**
     * Check if the transaction is still active
     * 
     * @return true if transaction is active
     */
    boolean isActive();
    
    /**
     * Auto-close with rollback if not committed
     */
    @Override
    void close();
}