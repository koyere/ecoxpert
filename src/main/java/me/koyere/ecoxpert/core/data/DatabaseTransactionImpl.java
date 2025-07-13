package me.koyere.ecoxpert.core.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Implementation of database transaction management
 * 
 * Provides transaction control with automatic rollback
 * on exceptions and proper resource cleanup.
 */
public class DatabaseTransactionImpl implements DatabaseTransaction {
    
    private final Connection connection;
    private final Executor executor;
    private boolean active = true;
    private boolean committed = false;
    
    public DatabaseTransactionImpl(Connection connection, Executor executor) throws SQLException {
        this.connection = connection;
        this.executor = executor;
        
        // Configure connection for transaction
        connection.setAutoCommit(false);
    }
    
    @Override
    public CompletableFuture<Integer> executeUpdate(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            checkActive();
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                setParameters(stmt, params);
                return stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute update: " + sql, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<QueryResult> executeQuery(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            checkActive();
            
            try {
                PreparedStatement stmt = connection.prepareStatement(sql);
                setParameters(stmt, params);
                return new QueryResultImpl(stmt.executeQuery());
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute query: " + sql, e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> commit() {
        return CompletableFuture.runAsync(() -> {
            checkActive();
            
            try {
                connection.commit();
                committed = true;
                active = false;
            } catch (SQLException e) {
                throw new RuntimeException("Failed to commit transaction", e);
            }
        }, executor);
    }
    
    @Override
    public CompletableFuture<Void> rollback() {
        return CompletableFuture.runAsync(() -> {
            if (active) {
                try {
                    connection.rollback();
                } catch (SQLException e) {
                    throw new RuntimeException("Failed to rollback transaction", e);
                } finally {
                    active = false;
                }
            }
        }, executor);
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    @Override
    public void close() {
        if (active && !committed) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                // Log warning but don't throw
                System.err.println("Warning: Failed to rollback transaction during close: " + e.getMessage());
            }
        }
        
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            // Log warning but don't throw
            System.err.println("Warning: Failed to close connection: " + e.getMessage());
        } finally {
            active = false;
        }
    }
    
    /**
     * Set parameters on a prepared statement
     * 
     * @param stmt PreparedStatement to configure
     * @param params Parameters to set
     * @throws SQLException if parameter setting fails
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Check if transaction is still active
     * 
     * @throws IllegalStateException if transaction is not active
     */
    private void checkActive() {
        if (!active) {
            throw new IllegalStateException("Transaction is no longer active");
        }
    }
}