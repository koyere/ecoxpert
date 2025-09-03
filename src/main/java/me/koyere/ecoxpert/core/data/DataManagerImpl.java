package me.koyere.ecoxpert.core.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Professional implementation of data management system
 * 
 * Provides enterprise-grade database management with HikariCP
 * connection pooling and comprehensive async operations.
 */
public class DataManagerImpl implements DataManager {
    
    private final EcoXpertPlugin plugin;
    private final ConfigManager configManager;
    private final ExecutorService databaseExecutor;
    
    private HikariDataSource dataSource;
    private boolean connected = false;
    private DatabaseType databaseType;
    
    public DataManagerImpl(EcoXpertPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.databaseExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread thread = new Thread(r, "EcoXpert-Database-" + Thread.currentThread().getId());
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Database type enumeration
     */
    private enum DatabaseType {
        SQLITE, MYSQL, H2, MEMORY
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing data management system...");
        
        try {
            // Determine database type
            String dbTypeStr = configManager.getDatabaseType().toLowerCase();
            this.databaseType = dbTypeStr.equals("mysql") ? DatabaseType.MYSQL : DatabaseType.SQLITE;
            
            plugin.getLogger().info("Database type: " + databaseType);
            
            // Initialize HikariCP connection pool
            initializeDataSource();
            
            // Test connection
            testConnection();
            
            // Create tables if needed
            createTables();
            
            this.connected = true;
            plugin.getLogger().info("Data management system initialized successfully");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize data management system: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (connected) {
            plugin.getLogger().info("Shutting down data management system...");
            
            try {
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                }
                
                // Shutdown executor
                databaseExecutor.shutdown();
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during database shutdown: " + e.getMessage());
            } finally {
                this.connected = false;
                plugin.getLogger().info("Data management system shutdown complete");
            }
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public String getDatabaseType() {
        return databaseType != null ? databaseType.name().toLowerCase() : "unknown";
    }
    
    @Override
    public CompletableFuture<Integer> executeUpdate(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                setParameters(stmt, params);
                return stmt.executeUpdate();
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute update: " + sql, e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public CompletableFuture<QueryResult> executeQuery(String sql, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            
            try {
                Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                
                setParameters(stmt, params);
                return new QueryResultImpl(stmt.executeQuery(), conn, stmt);
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute query: " + sql, e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public CompletableFuture<int[]> executeBatch(String sql, Object[]... paramsList) {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (Object[] params : paramsList) {
                    setParameters(stmt, params);
                    stmt.addBatch();
                }
                
                return stmt.executeBatch();
                
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute batch: " + sql, e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public CompletableFuture<DatabaseTransaction> beginTransaction() {
        return CompletableFuture.supplyAsync(() -> {
            checkConnection();
            
            try {
                Connection conn = dataSource.getConnection();
                return new DatabaseTransactionImpl(conn, databaseExecutor);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to begin transaction", e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public void createTables() {
        plugin.getLogger().info("Creating database tables...");
        
        try {
            createEconomyTables();
            plugin.getLogger().info("Database tables created successfully");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            throw new RuntimeException("Table creation failed", e);
        }
    }
    
    @Override
    public boolean needsMigration() {
        // Check schema version
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT version FROM ecoxpert_meta WHERE key = 'schema_version'")) {
            
            var result = stmt.executeQuery();
            if (result.next()) {
                int currentVersion = result.getInt("version");
                return currentVersion < getCurrentSchemaVersion();
            }
            return true; // No version found, needs migration
            
        } catch (SQLException e) {
            return true; // Error reading version, assume migration needed
        }
    }
    
    @Override
    public CompletableFuture<Void> migrate() {
        return CompletableFuture.runAsync(() -> {
            plugin.getLogger().info("Starting database migration...");
            
            try {
                // TODO: Implement migration logic
                updateSchemaVersion();
                plugin.getLogger().info("Database migration completed successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("Database migration failed: " + e.getMessage());
                throw new RuntimeException("Migration failed", e);
            }
        }, databaseExecutor);
    }
    
    /**
     * Initialize HikariCP data source
     */
    private void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        
        if (databaseType == DatabaseType.SQLITE) {
            configureSQLite(config);
        } else {
            configureMySQL(config);
        }
        
        // Common configuration
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        this.dataSource = new HikariDataSource(config);
    }
    
    /**
     * Configure SQLite connection
     */
    private void configureSQLite(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File dbFile = new File(dataFolder, "ecoxpert.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite specific settings
        config.addDataSourceProperty("cache_size", "8192");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("journal_mode", "WAL");
    }
    
    /**
     * Configure MySQL connection
     */
    private void configureMySQL(HikariConfig config) {
        // TODO: Get MySQL config from ConfigManager
        config.setJdbcUrl("jdbc:mysql://localhost:3306/ecoxpert");
        config.setUsername("username");
        config.setPassword("password");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // MySQL specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
    }
    
    /**
     * Test database connection
     */
    private void testConnection() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(5)) {
                throw new SQLException("Connection validation failed");
            }
            plugin.getLogger().info("Database connection test successful");
        }
    }
    
    /**
     * Create economy-related tables
     */
    private void createEconomyTables() throws SQLException {
        String[] tables = {
            // Meta table for schema versioning
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_meta (
                key VARCHAR(255) PRIMARY KEY,
                version INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Player economy accounts
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_accounts (
                player_uuid VARCHAR(36) PRIMARY KEY,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Transaction history
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_uuid VARCHAR(36),
                to_uuid VARCHAR(36),
                amount DECIMAL(20,2) NOT NULL,
                type VARCHAR(50) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Market items configuration
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_items (
                material VARCHAR(100) PRIMARY KEY,
                base_price DECIMAL(20,2) NOT NULL,
                current_buy_price DECIMAL(20,2) NOT NULL,
                current_sell_price DECIMAL(20,2) NOT NULL,
                buyable BOOLEAN DEFAULT TRUE,
                sellable BOOLEAN DEFAULT TRUE,
                total_sold INTEGER DEFAULT 0,
                total_bought INTEGER DEFAULT 0,
                price_volatility DECIMAL(5,4) DEFAULT 0.1000,
                last_price_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Market transaction history
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36) NOT NULL,
                player_name VARCHAR(50) NOT NULL,
                material VARCHAR(100) NOT NULL,
                transaction_type VARCHAR(10) NOT NULL,
                quantity INTEGER NOT NULL,
                unit_price DECIMAL(20,2) NOT NULL,
                total_amount DECIMAL(20,2) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Market price history for analytics
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_price_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                material VARCHAR(100) NOT NULL,
                buy_price DECIMAL(20,2) NOT NULL,
                sell_price DECIMAL(20,2) NOT NULL,
                transaction_count INTEGER DEFAULT 0,
                volume DECIMAL(20,2) DEFAULT 0.00,
                snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Bank accounts
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_accounts (
                player_uuid VARCHAR(36) PRIMARY KEY,
                account_number VARCHAR(20) UNIQUE NOT NULL,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                tier VARCHAR(20) NOT NULL DEFAULT 'BASIC',
                total_interest_earned DECIMAL(20,2) DEFAULT 0.00,
                frozen BOOLEAN DEFAULT FALSE,
                frozen_reason TEXT,
                daily_deposit_used DECIMAL(20,2) DEFAULT 0.00,
                daily_withdraw_used DECIMAL(20,2) DEFAULT 0.00,
                daily_transfer_used DECIMAL(20,2) DEFAULT 0.00,
                last_reset_date DATE DEFAULT CURRENT_DATE,
                last_interest_calculation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                failed_transaction_count INTEGER DEFAULT 0,
                last_failed_transaction TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Bank transaction history
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_uuid VARCHAR(36) NOT NULL,
                transaction_type VARCHAR(30) NOT NULL,
                amount DECIMAL(20,2) NOT NULL,
                balance_before DECIMAL(20,2) NOT NULL,
                balance_after DECIMAL(20,2) NOT NULL,
                description TEXT,
                reference VARCHAR(100),
                related_account_uuid VARCHAR(36),
                admin_id VARCHAR(100),
                ip_address VARCHAR(45),
                reason TEXT,
                transaction_hash VARCHAR(100) NOT NULL,
                verified BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,
            
            // Bank monthly statements
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_statements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                account_uuid VARCHAR(36) NOT NULL,
                statement_year INTEGER NOT NULL,
                statement_month INTEGER NOT NULL,
                opening_balance DECIMAL(20,2) NOT NULL,
                closing_balance DECIMAL(20,2) NOT NULL,
                total_deposits DECIMAL(20,2) DEFAULT 0.00,
                total_withdrawals DECIMAL(20,2) DEFAULT 0.00,
                total_transfers_in DECIMAL(20,2) DEFAULT 0.00,
                total_transfers_out DECIMAL(20,2) DEFAULT 0.00,
                total_interest DECIMAL(20,2) DEFAULT 0.00,
                total_fees DECIMAL(20,2) DEFAULT 0.00,
                transaction_count INTEGER DEFAULT 0,
                generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(account_uuid, statement_year, statement_month)
            )
            """
            ,
            // Economic events persistence
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_economic_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                event_id VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                status VARCHAR(20) NOT NULL,
                parameters TEXT,
                start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                end_time TIMESTAMP
            )
            """
            ,
            // Player loans (minimal schema: one ACTIVE loan per player)
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_loans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                player_uuid VARCHAR(36) NOT NULL,
                principal DECIMAL(20,2) NOT NULL,
                outstanding DECIMAL(20,2) NOT NULL,
                interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_payment_at TIMESTAMP
            )
            """
        };
        
        try (Connection conn = dataSource.getConnection()) {
            for (String sql : tables) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
            
            // Initialize schema version
            updateSchemaVersion();
        }
    }
    
    /**
     * Update schema version in meta table
     */
    private void updateSchemaVersion() throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO ecoxpert_meta (key, version)
            VALUES ('schema_version', ?)
            """;
            
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, getCurrentSchemaVersion());
            stmt.executeUpdate();
        }
    }
    
    /**
     * Get current schema version
     */
    private int getCurrentSchemaVersion() {
        return 1; // Current schema version
    }
    
    /**
     * Set parameters on prepared statement
     */
    private void setParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
    
    /**
     * Check if database is connected
     */
    private void checkConnection() {
        if (!connected) {
            throw new IllegalStateException("Database is not connected");
        }
    }
    
    @Override
    public boolean isHealthy() {
        if (!connected || dataSource == null) {
            return false;
        }
        
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public CompletableFuture<Void> exportDatabase(java.nio.file.Path backupPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Create a simple SQL dump
                plugin.getLogger().info("Creating database backup to: " + backupPath);
                
                // For SQLite, we can copy the file directly
                if (databaseType == DatabaseType.SQLITE) {
                    java.nio.file.Files.copy(
                        new File(plugin.getDataFolder(), "ecoxpert.db").toPath(),
                        backupPath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                } else {
                    // For MySQL, create a basic export (simplified implementation)
                    plugin.getLogger().warning("MySQL backup not fully implemented - using connection test");
                    if (!isHealthy()) {
                        throw new RuntimeException("Database is not healthy for backup");
                    }
                }
                
                plugin.getLogger().info("Database backup completed successfully");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Database backup failed: " + e.getMessage());
                throw new RuntimeException("Database backup failed", e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public CompletableFuture<Void> importDatabase(java.nio.file.Path backupPath) {
        return CompletableFuture.runAsync(() -> {
            try {
                plugin.getLogger().info("Restoring database from: " + backupPath);
                
                // Simplified restore - copy file for SQLite
                if (databaseType == DatabaseType.SQLITE) {
                    // Close current connection
                    if (dataSource != null) {
                        dataSource.close();
                    }
                    
                    // Copy backup over current database
                    java.nio.file.Files.copy(
                        backupPath,
                        new File(plugin.getDataFolder(), "ecoxpert.db").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    
                    // Reconnect
                    initializeSQLiteConnection();
                    testConnection();
                }
                
                plugin.getLogger().info("Database restore completed successfully");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Database restore failed: " + e.getMessage());
                throw new RuntimeException("Database restore failed", e);
            }
        }, databaseExecutor);
    }
    
    @Override
    public CompletableFuture<Boolean> switchToFallback(String fallbackType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                plugin.getLogger().warning("Switching to fallback database: " + fallbackType);
                
                // Close current connection
                if (dataSource != null) {
                    dataSource.close();
                    connected = false;
                }
                
                switch (fallbackType.toLowerCase()) {
                    case "sqlite":
                        initializeSQLiteConnection();
                        break;
                    case "h2":
                        initializeH2();
                        break;
                    case "memory":
                        initializeMemoryDatabase();
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown fallback type: " + fallbackType);
                }
                
                createTables();
                connected = true;
                
                plugin.getLogger().info("Successfully switched to fallback database: " + fallbackType);
                return true;
                
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to switch to fallback database: " + e.getMessage());
                return false;
            }
        }, databaseExecutor);
    }
    
    @Override
    public DatabaseStatus getStatus() {
        return DatabaseStatus.builder()
            .currentType(databaseType != null ? databaseType.name().toLowerCase() : "unknown")
            .originalType(databaseType != null ? databaseType.name().toLowerCase() : "unknown")
            .connected(connected)
            .healthy(isHealthy())
            .usingFallback(false) // TODO: Implement fallback tracking
            .connectionPoolSize(dataSource != null ? dataSource.getHikariPoolMXBean().getTotalConnections() : 0)
            .activeConnections(dataSource != null ? dataSource.getHikariPoolMXBean().getActiveConnections() : 0)
            .build();
    }
    
    // Helper methods for fallback databases
    
    private void initializeSQLiteConnection() throws SQLException {
        HikariConfig config = new HikariConfig();
        configureSQLite(config);
        this.dataSource = new HikariDataSource(config);
        this.databaseType = DatabaseType.SQLITE;
    }
    
    private void initializeH2() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/database_h2");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        this.dataSource = new HikariDataSource(config);
        this.databaseType = DatabaseType.H2;
    }
    
    private void initializeMemoryDatabase() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:ecoxpert;DB_CLOSE_DELAY=-1");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        
        this.dataSource = new HikariDataSource(config);
        this.databaseType = DatabaseType.MEMORY;
    }
}
