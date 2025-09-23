package me.koyere.ecoxpert.modules.integrations.jobs;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.modules.inflation.InflationManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Soft hook for Jobs Reborn that dynamically adjusts payouts based on inflation/policy.
 *
 * Implementation uses reflection to avoid compile-time dependency on Jobs.
 */
public class JobsIntegration implements Listener {

    private final EcoXpertPlugin plugin;

    public JobsIntegration(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerIfPresent() {
        try {
            if (Bukkit.getPluginManager().getPlugin("Jobs") == null &&
                Bukkit.getPluginManager().getPlugin("JobsReborn") == null) {
                return;
            }
            // Verify event class exists
            Class.forName("com.gamingmesh.jobs.api.JobsPaymentEvent");
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("Jobs Reborn detected: dynamic payout adjustment enabled");
        } catch (Throwable ignored) {
            // Not available
        }
    }

    @EventHandler
    public void onJobsPayment(Event raw) {
        try {
            if (!"com.gamingmesh.jobs.api.JobsPaymentEvent".equals(raw.getClass().getName())) return;

            Object event = raw;
            // Player
            Player player = (Player) event.getClass().getMethod("getPlayer").invoke(event);
            if (player == null) return;

            // Current amount (method name differs across versions)
            double amount = 0.0;
            try {
                amount = (double) event.getClass().getMethod("getAmount").invoke(event);
            } catch (NoSuchMethodException ns1) {
                try { amount = (double) event.getClass().getMethod("getPayment").invoke(event); }
                catch (NoSuchMethodException ns2) { return; }
            }

            double factor = computeJobsFactor(player);
            if (Math.abs(factor - 1.0) < 1e-6) return; // no change

            double newAmount = Math.max(0.0, amount * factor);

            // Setter (varies by version)
            try {
                event.getClass().getMethod("setAmount", double.class).invoke(event, newAmount);
            } catch (NoSuchMethodException ns1) {
                try { event.getClass().getMethod("setPayment", double.class).invoke(event, newAmount); }
                catch (NoSuchMethodException ns2) { return; }
            }

            if (plugin.getConfig().getBoolean("plugin.debug", false)) {
                plugin.getLogger().info(String.format("Jobs payout adjusted for %s: %.2f -> %.2f (x%.3f)",
                    player.getName(), amount, newAmount, factor));
            }
        } catch (Throwable ignored) {
        }
    }

    private double computeJobsFactor(Player player) {
        try {
            FileConfiguration cfg = plugin.getServiceRegistry()
                .getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class)
                .getModuleConfig("integrations");
            ConfigurationSection sec = cfg.getConfigurationSection("jobs.dynamic");
            if (sec == null || !sec.getBoolean("enabled", true)) return 1.0;

            InflationManager infl = plugin.getServiceRegistry().getInstance(InflationManager.class);
            double inflRate = infl != null ? infl.getInflationRate() : 0.0; // fraction e.g., 0.02

            // thresholds: list of { rate: 0.03, factor: 0.95 }
            java.util.List<?> thresholds = sec.getList("inflation.thresholds");
            double factor = 1.0;
            if (thresholds != null) {
                for (Object o : thresholds) {
                    if (o instanceof java.util.Map<?,?> map) {
                        Object r = map.get("rate"); Object f = map.get("factor");
                        double rate = r instanceof Number ? ((Number) r).doubleValue() : Double.parseDouble(String.valueOf(r));
                        double fac = f instanceof Number ? ((Number) f).doubleValue() : Double.parseDouble(String.valueOf(f));
                        if (inflRate >= rate) {
                            factor = Math.min(factor, fac);
                        }
                    }
                }
            }

            // Clamp to sane bounds
            if (factor < 0.5) factor = 0.5; if (factor > 1.1) factor = 1.1;
            return factor;
        } catch (Exception e) {
            return 1.0;
        }
    }
}

