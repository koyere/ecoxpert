package me.koyere.ecoxpert.core.education;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.config.ConfigManager;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.modules.events.EconomicEvent;
import me.koyere.ecoxpert.modules.inflation.EconomicIntelligenceEngine;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight broadcaster for educational/economic context messages.
 */
public final class EducationNotifier {
    private EducationNotifier() {}

    public static void broadcastCycle(EcoXpertPlugin plugin, TranslationManager tm,
                                      EconomicIntelligenceEngine.EconomicCycle cycle) {
        try {
            ConfigManager cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class);
            if (!cfg.getConfig().getBoolean("education.enabled", true)) return;
            if (!cfg.getConfig().getBoolean("education.broadcasts.cycle", true)) return;
            String key = "education.cycle." + cycle.name();
            Bukkit.broadcastMessage(tm.getMessage("prefix") + tm.getMessage(key));
        } catch (Exception ignored) {}
    }

    public static void broadcastEvent(EcoXpertPlugin plugin, TranslationManager tm, EconomicEvent event) {
        try {
            ConfigManager cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class);
            if (!cfg.getConfig().getBoolean("education.enabled", true)) return;
            if (!cfg.getConfig().getBoolean("education.broadcasts.events", true)) return;

            String baseKey = "education.events." + event.getType().name();
            String msg;
            // Try to pick up deltas from parameters (if present)
            Map<String, Object> p = event.getParameters();
            Double bd = getDouble(p.get("metrics.buy_delta"));
            Double sd = getDouble(p.get("metrics.sell_delta"));
            String cat = getString(p.get("metrics.category"));
            try {
                if (bd != null && sd != null && cat != null) {
                    msg = tm.getMessage(baseKey, percent(bd), percent(sd), cat);
                } else if (bd != null && sd != null) {
                    msg = tm.getMessage(baseKey, percent(bd), percent(sd));
                } else {
                    msg = tm.getMessage(baseKey);
                }
            } catch (Exception ex) {
                // Fallback generic message
                if (bd != null && sd != null) {
                    msg = tm.getMessage("education.events.generic", event.getType().name(), percent(bd), percent(sd));
                } else {
                    msg = tm.getMessage("education.events.generic_simple", event.getType().name());
                }
            }
            Bukkit.broadcastMessage(tm.getMessage("prefix") + msg);
        } catch (Exception ignored) {}
    }

    public static void notifyWealthTaxApplied(EcoXpertPlugin plugin, TranslationManager tm, UUID playerId,
                                              BigDecimal threshold) {
        try {
            ConfigManager cfg = plugin.getServiceRegistry().getInstance(ConfigManager.class);
            if (!cfg.getConfig().getBoolean("education.enabled", true)) return;
            if (!cfg.getConfig().getBoolean("education.broadcasts.policy", true)) return;
            Player p = Bukkit.getPlayer(playerId);
            if (p == null) return;
            p.sendMessage(tm.getMessage("prefix") + tm.getMessage("education.policy.wealth_tax_applied",
                threshold != null ? threshold.toPlainString() : ""));
        } catch (Exception ignored) {}
    }

    private static String percent(double fraction) {
        return String.format(Locale.US, "%.0f%%", Math.abs(fraction) * 100.0);
    }
    private static Double getDouble(Object o) { try { return o instanceof Number ? ((Number) o).doubleValue() : null; } catch (Exception e){return null;} }
    private static String getString(Object o) { return o != null ? o.toString() : null; }
}
