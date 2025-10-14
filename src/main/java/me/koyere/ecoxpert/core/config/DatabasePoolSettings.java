package me.koyere.ecoxpert.core.config;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Immutable representation of optional pool tuning parameters.
 */
public final class DatabasePoolSettings {

    private final Integer maximumPoolSize;
    private final Integer minimumIdle;
    private final Long connectionTimeout;
    private final Long idleTimeout;
    private final Long maxLifetime;

    public DatabasePoolSettings(Integer maximumPoolSize,
                                Integer minimumIdle,
                                Long connectionTimeout,
                                Long idleTimeout,
                                Long maxLifetime) {
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
    }

    public OptionalInt getMaximumPoolSize() {
        return maximumPoolSize != null ? OptionalInt.of(maximumPoolSize) : OptionalInt.empty();
    }

    public OptionalInt getMinimumIdle() {
        return minimumIdle != null ? OptionalInt.of(minimumIdle) : OptionalInt.empty();
    }

    public OptionalLong getConnectionTimeout() {
        return connectionTimeout != null ? OptionalLong.of(connectionTimeout) : OptionalLong.empty();
    }

    public OptionalLong getIdleTimeout() {
        return idleTimeout != null ? OptionalLong.of(idleTimeout) : OptionalLong.empty();
    }

    public OptionalLong getMaxLifetime() {
        return maxLifetime != null ? OptionalLong.of(maxLifetime) : OptionalLong.empty();
    }
}
