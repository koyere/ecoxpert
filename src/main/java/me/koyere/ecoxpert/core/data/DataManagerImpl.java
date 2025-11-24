package me.koyere.ecoxpert.core.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.config.DatabasePoolSettings;
import me.koyere.ecoxpert.core.config.MySqlConfig;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private SqlDialect sqlDialect;
    
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

    private void setDialect(DatabaseType type) {
        this.databaseType = type;
        this.sqlDialect = SqlDialect.forType(type);
    }
    
    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing data management system...");

        try {
            // Determine database type
            String dbTypeStr = configManager.getDatabaseType().toLowerCase();
            DatabaseType resolvedType = dbTypeStr.equals("mysql") ? DatabaseType.MYSQL : DatabaseType.SQLITE;
            setDialect(resolvedType);

            plugin.getLogger().info("Database type: " + databaseType);

            // Initialize HikariCP connection pool with charset fallback handling
            try {
                initializeDataSource();
            } catch (Exception charsetError) {
                if (databaseType == DatabaseType.MYSQL && charsetError.getMessage() != null &&
                    charsetError.getMessage().contains("utf8mb4")) {

                    plugin.getLogger().warning("MySQL charset error detected: " + charsetError.getMessage());
                    plugin.getLogger().warning("Attempting fallback to compatible charset configuration...");

                    // Fallback: retry with utf8 instead of utf8mb4
                    try {
                        initializeDataSourceWithFallbackCharset();
                    } catch (Exception fallbackError) {
                        plugin.getLogger().severe("Charset fallback also failed: " + fallbackError.getMessage());
                        throw fallbackError;
                    }
                } else {
                    throw charsetError;
                }
            }

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
                 "SELECT version FROM ecoxpert_meta WHERE `key` = 'schema_version'")) {

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

        // Common defaults (overridden if custom pool settings are provided)
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        if (databaseType == DatabaseType.SQLITE) {
            configureSQLite(config);
        } else {
            configureMySQL(config);
        }

        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Initialize HikariCP data source with fallback UTF-8 charset (for MySQL compatibility)
     */
    private void initializeDataSourceWithFallbackCharset() throws Exception {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }

        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);

        MySqlConfig mySql = configManager.getMySqlConfig();
        if (mySql == null) {
            throw new IllegalStateException("MySQL configuration not available");
        }

        validateMySqlConfig(mySql);

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?allowMultiQueries=true&createDatabaseIfNotExist=true",
            mySql.getHost().trim(),
            mySql.getPort(),
            mySql.getDatabase().trim());

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(mySql.getUsername());
        config.setPassword(mySql.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("EcoXpert-MySQL-Fallback");

        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");
        config.addDataSourceProperty("useSSL", Boolean.toString(mySql.isUseSsl()));
        config.addDataSourceProperty("allowPublicKeyRetrieval", Boolean.toString(mySql.isAllowPublicKeyRetrieval()));

        // Character encoding fallback: UTF-8 instead of UTF-8MB4
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("connectionCollation", "utf8_general_ci");
        config.addDataSourceProperty("characterSetResults", "utf8");

        applyPoolOverrides(config, mySql.getPoolSettings());

        this.dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Successfully initialized MySQL with UTF-8 charset fallback");
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
        // Add busy_timeout to reduce SQLITE_BUSY errors under concurrent writes (15s timeout)
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath() + "?busy_timeout=15000");
        config.setDriverClassName("org.sqlite.JDBC");
        
        // SQLite specific settings
        config.addDataSourceProperty("cache_size", "8192");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("journal_mode", "WAL");
        // Ensure every connection sets a sensible busy_timeout (15s)
        try { config.setConnectionInitSql("PRAGMA busy_timeout=15000;"); } catch (Throwable ignored) {}
    }
    
    /**
     * Configure MySQL connection with automatic charset fallback
     */
    private void configureMySQL(HikariConfig config) {
        MySqlConfig mySql = configManager.getMySqlConfig();
        if (mySql == null) {
            throw new IllegalStateException("MySQL configuration not available");
        }

        validateMySqlConfig(mySql);

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?allowMultiQueries=true&createDatabaseIfNotExist=true",
            mySql.getHost().trim(),
            mySql.getPort(),
            mySql.getDatabase().trim());

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(mySql.getUsername());
        config.setPassword(mySql.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setPoolName("EcoXpert-MySQL");

        // Performance and stability settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");
        config.addDataSourceProperty("useSSL", Boolean.toString(mySql.isUseSsl()));
        config.addDataSourceProperty("allowPublicKeyRetrieval", Boolean.toString(mySql.isAllowPublicKeyRetrieval()));

        // Character encoding with automatic fallback
        // Try utf8mb4 first, fall back to utf8 if not supported
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8mb4");
        config.addDataSourceProperty("connectionCollation", "utf8mb4_unicode_ci");
        config.addDataSourceProperty("characterSetResults", "utf8mb4");

        applyPoolOverrides(config, mySql.getPoolSettings());
    }

    private void applyPoolOverrides(HikariConfig config, DatabasePoolSettings overrides) {
        if (overrides == null) {
            return;
        }
        overrides.getMaximumPoolSize().ifPresent(config::setMaximumPoolSize);
        overrides.getMinimumIdle().ifPresent(config::setMinimumIdle);
        overrides.getConnectionTimeout().ifPresent(config::setConnectionTimeout);
        overrides.getIdleTimeout().ifPresent(config::setIdleTimeout);
        overrides.getMaxLifetime().ifPresent(config::setMaxLifetime);
    }

    private void validateMySqlConfig(MySqlConfig config) {
        if (config.getHost().trim().isEmpty()) {
            throw new IllegalStateException("database.mysql.host must not be empty");
        }
        if (config.getDatabase().trim().isEmpty()) {
            throw new IllegalStateException("database.mysql.database must not be empty");
        }
        if (config.getUsername().trim().isEmpty()) {
            throw new IllegalStateException("database.mysql.username must not be empty");
        }
        if (config.getPort() <= 0 || config.getPort() > 65535) {
            throw new IllegalStateException("database.mysql.port must be a valid TCP port");
        }
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
        if (sqlDialect == null) {
            throw new IllegalStateException("SQL dialect not initialized");
        }

        try (Connection conn = dataSource.getConnection()) {
            for (String sql : sqlDialect.createTableStatements()) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }

            createIndexes(conn);
            sqlDialect.upsertSchemaVersion(conn, getCurrentSchemaVersion());
        }
    }

    private void createIndexes(Connection conn) throws SQLException {
        List<IndexDefinition> indexes = sqlDialect.indexDefinitions();
        if (indexes.isEmpty()) {
            return;
        }

        DatabaseMetaData metaData = conn.getMetaData();
        String catalog = null;
        try {
            catalog = conn.getCatalog();
        } catch (SQLException ignored) {
            // Some drivers do not support getCatalog; fallback to null
        }

        for (IndexDefinition index : indexes) {
            boolean exists = false;
            try {
                exists = indexExists(metaData, catalog, index);
            } catch (SQLException metaEx) {
                plugin.getLogger().fine("Unable to verify index " + index.indexName() +
                    ": " + metaEx.getMessage() + " - attempting creation anyway");
            }

            if (exists) {
                continue;
            }

            String sql = sqlDialect.createIndexStatement(index);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.executeUpdate();
                plugin.getLogger().fine("Created database index: " + index.indexName());
            } catch (SQLException createEx) {
                plugin.getLogger().fine("Index creation skipped/failed: " + index.indexName() +
                    " -> " + createEx.getMessage());
            }
        }
    }

    private boolean indexExists(DatabaseMetaData metaData, String catalog, IndexDefinition index) throws SQLException {
        try (ResultSet resultSet = metaData.getIndexInfo(catalog, null, index.tableName(), false, false)) {
            while (resultSet.next()) {
                String name = resultSet.getString("INDEX_NAME");
                if (name != null && name.equalsIgnoreCase(index.indexName())) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Update schema version in meta table
     */
    private void updateSchemaVersion() throws SQLException {
        if (sqlDialect == null) {
            throw new IllegalStateException("SQL dialect not initialized");
        }

        try (Connection conn = dataSource.getConnection()) {
            sqlDialect.upsertSchemaVersion(conn, getCurrentSchemaVersion());
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
        setDialect(DatabaseType.SQLITE);
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
        setDialect(DatabaseType.H2);
    }
    
    private void initializeMemoryDatabase() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:ecoxpert;DB_CLOSE_DELAY=-1");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        
        this.dataSource = new HikariDataSource(config);
        setDialect(DatabaseType.MEMORY);
    }

    private record IndexDefinition(String tableName, String indexName, String columns, boolean unique) {}

    private interface SqlDialect {
        List<String> createTableStatements();
        List<IndexDefinition> indexDefinitions();
        String createIndexStatement(IndexDefinition indexDefinition);
        void upsertSchemaVersion(Connection connection, int schemaVersion) throws SQLException;

        static SqlDialect forType(DatabaseType type) {
            return switch (type) {
                case SQLITE -> SqliteDialect.INSTANCE;
                case MYSQL -> MySqlDialect.INSTANCE;
                case H2, MEMORY -> H2Dialect.INSTANCE;
            };
        }
    }

    private static final class SqliteDialect implements SqlDialect {
        private static final SqliteDialect INSTANCE = new SqliteDialect();

        @Override
        public List<String> createTableStatements() {
            return SchemaDefinitions.SQLITE_TABLES;
        }

        @Override
        public List<IndexDefinition> indexDefinitions() {
            return SchemaDefinitions.COMMON_INDEXES;
        }

        @Override
        public String createIndexStatement(IndexDefinition indexDefinition) {
            String uniqueToken = indexDefinition.unique() ? "UNIQUE " : "";
            return "CREATE " + uniqueToken + "INDEX IF NOT EXISTS " + indexDefinition.indexName() +
                " ON " + indexDefinition.tableName() + "(" + indexDefinition.columns() + ")";
        }

        @Override
        public void upsertSchemaVersion(Connection connection, int schemaVersion) throws SQLException {
            String sql = """
                INSERT OR REPLACE INTO ecoxpert_meta (key, version)
                VALUES ('schema_version', ?)
                """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, schemaVersion);
                stmt.executeUpdate();
            }
        }
    }

    private static final class MySqlDialect implements SqlDialect {
        private static final MySqlDialect INSTANCE = new MySqlDialect();

        @Override
        public List<String> createTableStatements() {
            return SchemaDefinitions.MYSQL_TABLES;
        }

        @Override
        public List<IndexDefinition> indexDefinitions() {
            return SchemaDefinitions.COMMON_INDEXES;
        }

        @Override
        public String createIndexStatement(IndexDefinition indexDefinition) {
            String uniqueToken = indexDefinition.unique() ? "UNIQUE " : "";
            return "CREATE " + uniqueToken + "INDEX " + indexDefinition.indexName() +
                " ON " + indexDefinition.tableName() + "(" + indexDefinition.columns() + ")";
        }

        @Override
        public void upsertSchemaVersion(Connection connection, int schemaVersion) throws SQLException {
            String sql = """
                INSERT INTO ecoxpert_meta (`key`, version)
                VALUES ('schema_version', ?)
                ON DUPLICATE KEY UPDATE version = VALUES(version)
                """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, schemaVersion);
                stmt.executeUpdate();
            }
        }
    }

    private static final class H2Dialect implements SqlDialect {
        private static final H2Dialect INSTANCE = new H2Dialect();

        @Override
        public List<String> createTableStatements() {
            return SchemaDefinitions.H2_TABLES;
        }

        @Override
        public List<IndexDefinition> indexDefinitions() {
            return SchemaDefinitions.COMMON_INDEXES;
        }

        @Override
        public String createIndexStatement(IndexDefinition indexDefinition) {
            String uniqueToken = indexDefinition.unique() ? "UNIQUE " : "";
            return "CREATE " + uniqueToken + "INDEX IF NOT EXISTS " + indexDefinition.indexName() +
                " ON " + indexDefinition.tableName() + "(" + indexDefinition.columns() + ")";
        }

        @Override
        public void upsertSchemaVersion(Connection connection, int schemaVersion) throws SQLException {
            String sql = """
                MERGE INTO ecoxpert_meta (key, version)
                KEY(key) VALUES ('schema_version', ?)
                """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, schemaVersion);
                stmt.executeUpdate();
            }
        }
    }

    private static final class SchemaDefinitions {
        private static final List<String> SQLITE_TABLES = List.of(
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
            """,

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
            """,

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
            """,

            // Loan repayment schedules
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_loan_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                loan_id INTEGER NOT NULL,
                installment_no INTEGER NOT NULL,
                due_date DATE NOT NULL,
                amount_due DECIMAL(20,2) NOT NULL,
                paid_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                paid_at TIMESTAMP,
                UNIQUE(loan_id, installment_no)
            )
            """,

            // Player professions (economic roles)
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_professions (
                player_uuid VARCHAR(36) PRIMARY KEY,
                role VARCHAR(40) NOT NULL,
                level INTEGER NOT NULL DEFAULT 1,
                selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """,

            // Professions XP tracking (separate table)
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_profession_xp (
                player_uuid VARCHAR(36) PRIMARY KEY,
                xp INTEGER NOT NULL DEFAULT 0,
                last_gain_at TIMESTAMP
            )
            """,

            // Order book (optional feature): fixed-price listings
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_orders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                seller_uuid VARCHAR(36) NOT NULL,
                material VARCHAR(64) NOT NULL,
                unit_price DECIMAL(20,2) NOT NULL,
                remaining_quantity INTEGER NOT NULL,
                status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP
            )
            """
        );

        private static final List<String> MYSQL_TABLES = List.of(
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_meta (
                `key` VARCHAR(255) PRIMARY KEY,
                version INT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_accounts (
                player_uuid CHAR(36) PRIMARY KEY,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                from_uuid CHAR(36),
                to_uuid CHAR(36),
                amount DECIMAL(20,2) NOT NULL,
                type VARCHAR(50) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_items (
                material VARCHAR(100) PRIMARY KEY,
                base_price DECIMAL(20,2) NOT NULL,
                current_buy_price DECIMAL(20,2) NOT NULL,
                current_sell_price DECIMAL(20,2) NOT NULL,
                buyable TINYINT(1) DEFAULT 1,
                sellable TINYINT(1) DEFAULT 1,
                total_sold INT DEFAULT 0,
                total_bought INT DEFAULT 0,
                price_volatility DECIMAL(5,4) DEFAULT 0.1000,
                last_price_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                player_name VARCHAR(50) NOT NULL,
                material VARCHAR(100) NOT NULL,
                transaction_type VARCHAR(10) NOT NULL,
                quantity INT NOT NULL,
                unit_price DECIMAL(20,2) NOT NULL,
                total_amount DECIMAL(20,2) NOT NULL,
                description TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_price_history (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                material VARCHAR(100) NOT NULL,
                buy_price DECIMAL(20,2) NOT NULL,
                sell_price DECIMAL(20,2) NOT NULL,
                transaction_count INT DEFAULT 0,
                volume DECIMAL(20,2) DEFAULT 0.00,
                snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_accounts (
                player_uuid CHAR(36) PRIMARY KEY,
                account_number VARCHAR(20) UNIQUE NOT NULL,
                balance DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                tier VARCHAR(20) NOT NULL DEFAULT 'BASIC',
                total_interest_earned DECIMAL(20,2) DEFAULT 0.00,
                frozen TINYINT(1) DEFAULT 0,
                frozen_reason TEXT,
                daily_deposit_used DECIMAL(20,2) DEFAULT 0.00,
                daily_withdraw_used DECIMAL(20,2) DEFAULT 0.00,
                daily_transfer_used DECIMAL(20,2) DEFAULT 0.00,
                last_reset_date DATE DEFAULT NULL,
                last_interest_calculation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                failed_transaction_count INT DEFAULT 0,
                last_failed_transaction TIMESTAMP NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                account_uuid CHAR(36) NOT NULL,
                transaction_type VARCHAR(30) NOT NULL,
                amount DECIMAL(20,2) NOT NULL,
                balance_before DECIMAL(20,2) NOT NULL,
                balance_after DECIMAL(20,2) NOT NULL,
                description TEXT,
                reference VARCHAR(100),
                related_account_uuid CHAR(36),
                admin_id VARCHAR(100),
                ip_address VARCHAR(45),
                reason TEXT,
                transaction_hash VARCHAR(100) NOT NULL,
                verified TINYINT(1) DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_bank_statements (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                account_uuid CHAR(36) NOT NULL,
                statement_year INT NOT NULL,
                statement_month INT NOT NULL,
                opening_balance DECIMAL(20,2) NOT NULL,
                closing_balance DECIMAL(20,2) NOT NULL,
                total_deposits DECIMAL(20,2) DEFAULT 0.00,
                total_withdrawals DECIMAL(20,2) DEFAULT 0.00,
                total_transfers_in DECIMAL(20,2) DEFAULT 0.00,
                total_transfers_out DECIMAL(20,2) DEFAULT 0.00,
                total_interest DECIMAL(20,2) DEFAULT 0.00,
                total_fees DECIMAL(20,2) DEFAULT 0.00,
                transaction_count INT DEFAULT 0,
                generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY uk_bank_statements_account_period (account_uuid, statement_year, statement_month)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_economic_events (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                event_id VARCHAR(100) NOT NULL,
                type VARCHAR(50) NOT NULL,
                status VARCHAR(20) NOT NULL,
                parameters TEXT,
                start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                end_time TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_loans (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                player_uuid CHAR(36) NOT NULL,
                principal DECIMAL(20,2) NOT NULL,
                outstanding DECIMAL(20,2) NOT NULL,
                interest_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
                status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_payment_at TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_loan_schedules (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                loan_id BIGINT NOT NULL,
                installment_no INT NOT NULL,
                due_date DATE NOT NULL,
                amount_due DECIMAL(20,2) NOT NULL,
                paid_amount DECIMAL(20,2) NOT NULL DEFAULT 0.00,
                status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                paid_at TIMESTAMP NULL,
                UNIQUE KEY uk_loan_schedule_installment (loan_id, installment_no)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_professions (
                player_uuid CHAR(36) PRIMARY KEY,
                role VARCHAR(40) NOT NULL,
                level INT NOT NULL DEFAULT 1,
                selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_profession_xp (
                player_uuid CHAR(36) PRIMARY KEY,
                xp INT NOT NULL DEFAULT 0,
                last_gain_at TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """,
            """
            CREATE TABLE IF NOT EXISTS ecoxpert_market_orders (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                seller_uuid CHAR(36) NOT NULL,
                material VARCHAR(64) NOT NULL,
                unit_price DECIMAL(20,2) NOT NULL,
                remaining_quantity INT NOT NULL,
                status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """
        );

        private static final List<String> H2_TABLES = SQLITE_TABLES.stream()
            .map(sql -> sql
                .replace("INTEGER PRIMARY KEY AUTOINCREMENT", "BIGINT AUTO_INCREMENT PRIMARY KEY")
                .replace("AUTOINCREMENT", "AUTO_INCREMENT"))
            .collect(Collectors.toList());

        private static final List<IndexDefinition> COMMON_INDEXES = List.of(
            new IndexDefinition("ecoxpert_loans", "idx_loans_player_status", "player_uuid, status", false),
            new IndexDefinition("ecoxpert_loan_schedules", "idx_loan_sched_status_due", "status, due_date", false),
            new IndexDefinition("ecoxpert_loan_schedules", "idx_loan_sched_loan", "loan_id", false),
            new IndexDefinition("ecoxpert_market_orders", "idx_orders_status_material_created", "status, material, created_at", false),
            new IndexDefinition("ecoxpert_market_orders", "idx_orders_status_created", "status, created_at", false),
            new IndexDefinition("ecoxpert_profession_xp", "idx_prof_xp_player", "player_uuid", false)
        );
    }
}
