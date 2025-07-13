package me.koyere.ecoxpert.core.data;

import java.util.concurrent.CompletableFuture;

/**
 * Data persistence management interface
 * 
 * Provides unified database access layer with support for
 * both SQLite and MySQL with HikariCP connection pooling.
 */
public interface DataManager {
    
    /**
     * Initialize the data management system
     */
    void initialize();
    
    /**
     * Shutdown the data management system
     */
    void shutdown();
    
    /**
     * Check if the database connection is active
     * 
     * @return true if database is connected
     */
    boolean isConnected();
    
    /**
     * Get the database type currently in use
     * 
     * @return Database type ("sqlite" or "mysql")
     */
    String getDatabaseType();
    
    /**
     * Execute a database update asynchronously
     * 
     * @param sql SQL statement
     * @param params Statement parameters
     * @return CompletableFuture with affected rows count
     */
    CompletableFuture<Integer> executeUpdate(String sql, Object... params);
    
    /**
     * Execute a database query asynchronously
     * 
     * @param sql SQL statement
     * @param params Statement parameters
     * @return CompletableFuture with query results
     */
    CompletableFuture<QueryResult> executeQuery(String sql, Object... params);
    
    /**
     * Execute a batch of database updates
     * 
     * @param sql SQL statement
     * @param paramsList List of parameter arrays
     * @return CompletableFuture with batch results
     */
    CompletableFuture<int[]> executeBatch(String sql, Object[]... paramsList);
    
    /**
     * Begin a database transaction
     * 
     * @return Transaction object
     */
    CompletableFuture<DatabaseTransaction> beginTransaction();
    
    /**
     * Create database tables if they don't exist
     */
    void createTables();
    
    /**
     * Check if database migration is needed
     * 
     * @return true if migration required
     */
    boolean needsMigration();
    
    /**
     * Perform database migration
     */
    CompletableFuture<Void> migrate();
    
    /**
     * Check if the database system is healthy
     * 
     * @return true if database is operational
     */
    boolean isHealthy();
    
    /**
     * Export database to backup file
     * 
     * @param backupPath Path to backup file
     * @return CompletableFuture that completes when export is done
     */
    CompletableFuture<Void> exportDatabase(java.nio.file.Path backupPath);
    
    /**
     * Import database from backup file
     * 
     * @param backupPath Path to backup file
     * @return CompletableFuture that completes when import is done
     */
    CompletableFuture<Void> importDatabase(java.nio.file.Path backupPath);
    
    /**
     * Attempt to switch to fallback database type
     * 
     * @param fallbackType Type of fallback database ("sqlite", "h2", "memory")
     * @return CompletableFuture that completes when fallback is active
     */
    CompletableFuture<Boolean> switchToFallback(String fallbackType);
    
    /**
     * Get current database status and fallback information
     * 
     * @return Database status information
     */
    DatabaseStatus getStatus();
}