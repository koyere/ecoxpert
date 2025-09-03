package me.koyere.ecoxpert.core.safety;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.data.DataManager;
import org.bukkit.Bukkit;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Monitors DB latency and critical error rate. Activates safe mode
 * when thresholds from inflation.yml (safe_mode.*) are exceeded.
 */
public class SafeModeManagerImpl implements SafeModeManager {
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final ConfigManager configManager;

    private boolean active = false;
    private long latencyThresholdMs = 500;
    private int errorsPerMinuteThreshold = 3;

    private final Deque<Long> latencies = new ArrayDeque<>();
    private final Deque<Long> errorTimestamps = new ArrayDeque<>();
    private int taskId = -1;

    public SafeModeManagerImpl(EcoXpertPlugin plugin, DataManager dataManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.configManager = configManager;
    }

    @Override
    public void initialize() {
        try {
            var cfg = configManager.getModuleConfig("inflation");
            boolean enabled = cfg.getBoolean("safe_mode.enabled", true);
            this.latencyThresholdMs = cfg.getInt("safe_mode.latency_ms_threshold", 500);
            this.errorsPerMinuteThreshold = cfg.getInt("safe_mode.errors_per_minute_threshold", 3);
            if (!enabled) return;

            // Schedule latency monitor every 30s
            this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::sampleLatency, 20L * 30, 20L * 30).getTaskId();
            plugin.getLogger().info("Safe Mode monitor initialized");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Safe Mode: " + e.getMessage());
        }
    }

    @Override
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        active = false;
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public void recordCriticalError() {
        long now = System.currentTimeMillis();
        synchronized (errorTimestamps) {
            errorTimestamps.addLast(now);
            // Keep only last minute
            while (!errorTimestamps.isEmpty() && now - errorTimestamps.peekFirst() > 60_000L) {
                errorTimestamps.pollFirst();
            }
            if (errorTimestamps.size() >= errorsPerMinuteThreshold) {
                setActive(true, "Critical error rate threshold reached");
            }
        }
    }

    private void sampleLatency() {
        long start = System.currentTimeMillis();
        try {
            // Simple lightweight query
            dataManager.executeQuery("SELECT 1").join().close();
        } catch (Exception e) {
            recordCriticalError();
        } finally {
            long latency = System.currentTimeMillis() - start;
            synchronized (latencies) {
                latencies.addLast(latency);
                if (latencies.size() > 20) latencies.pollFirst();
                long median = computeMedian(latencies);
                if (median > latencyThresholdMs) {
                    setActive(true, "DB latency median " + median + "ms > " + latencyThresholdMs + "ms");
                } else if (!errorSpike()) {
                    // Recover if healthy again
                    setActive(false, "DB latency back to normal");
                }
            }
        }
    }

    private boolean errorSpike() {
        long now = System.currentTimeMillis();
        synchronized (errorTimestamps) {
            while (!errorTimestamps.isEmpty() && now - errorTimestamps.peekFirst() > 60_000L) {
                errorTimestamps.pollFirst();
            }
            return errorTimestamps.size() >= errorsPerMinuteThreshold;
        }
    }

    private long computeMedian(Deque<Long> values) {
        int n = values.size();
        if (n == 0) return 0L;
        long[] arr = new long[n];
        int i = 0; for (Long v : values) arr[i++] = v;
        java.util.Arrays.sort(arr);
        return arr[n/2];
    }

    private void setActive(boolean flag, String reason) {
        if (this.active == flag) return;
        this.active = flag;
        if (flag) plugin.getLogger().warning("SAFE MODE ACTIVATED: " + reason);
        else plugin.getLogger().info("Safe Mode deactivated: " + reason);
    }
}
