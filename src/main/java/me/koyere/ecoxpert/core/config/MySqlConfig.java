package me.koyere.ecoxpert.core.config;

import java.util.Objects;

/**
 * Immutable holder for MySQL connection details.
 */
public final class MySqlConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSsl;
    private final boolean allowPublicKeyRetrieval;
    private final DatabasePoolSettings poolSettings;

    public MySqlConfig(String host,
                       int port,
                       String database,
                       String username,
                       String password,
                       boolean useSsl,
                       boolean allowPublicKeyRetrieval,
                       DatabasePoolSettings poolSettings) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        this.database = Objects.requireNonNull(database, "database");
        this.username = Objects.requireNonNull(username, "username");
        this.password = password != null ? password : "";
        this.useSsl = useSsl;
        this.allowPublicKeyRetrieval = allowPublicKeyRetrieval;
        this.poolSettings = poolSettings != null ? poolSettings : new DatabasePoolSettings(null, null, null, null, null);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public boolean isAllowPublicKeyRetrieval() {
        return allowPublicKeyRetrieval;
    }

    public DatabasePoolSettings getPoolSettings() {
        return poolSettings;
    }
}
