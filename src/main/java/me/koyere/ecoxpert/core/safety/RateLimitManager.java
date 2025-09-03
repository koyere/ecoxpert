package me.koyere.ecoxpert.core.safety;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-UUID per-action rate limiter (ops per second).
 */
public class RateLimitManager {
    private final int opsPerSecond;
    private final Map<String, Counter> buckets = new ConcurrentHashMap<>();

    public RateLimitManager(int opsPerSecond) {
        this.opsPerSecond = Math.max(1, opsPerSecond);
    }

    public boolean allow(UUID playerId, String action) {
        long nowSec = System.currentTimeMillis() / 1000L;
        String key = playerId + ":" + action;
        Counter c = buckets.computeIfAbsent(key, k -> new Counter(nowSec, 0));
        synchronized (c) {
            if (c.epochSec != nowSec) {
                c.epochSec = nowSec;
                c.count = 0;
            }
            if (c.count >= opsPerSecond) return false;
            c.count++;
            return true;
        }
    }

    private static class Counter {
        long epochSec;
        int count;
        Counter(long epochSec, int count) { this.epochSec = epochSec; this.count = count; }
    }
}
