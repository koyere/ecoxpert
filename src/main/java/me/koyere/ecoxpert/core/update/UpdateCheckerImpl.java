package me.koyere.ecoxpert.core.update;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.translation.TranslationManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Implementation of update checking system
 * 
 * Basic implementation that will be enhanced with
 * SpigotMC API integration and automatic notifications.
 */
@Singleton
public class UpdateCheckerImpl implements UpdateChecker {
    
    private final EcoXpertPlugin plugin;
    private final TranslationManager translationManager;
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    
    @Inject
    public UpdateCheckerImpl(EcoXpertPlugin plugin, TranslationManager translationManager) {
        this.plugin = plugin;
        this.translationManager = translationManager;
        this.currentVersion = plugin.getDescription().getVersion();
    }
    
    @Override
    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("plugin.updates.check-enabled", true)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getLogger().info("Checking for updates...");

                String endpoint = resolveEndpoint();
                if (endpoint == null || endpoint.isBlank()) {
                    plugin.getLogger().info("Update check skipped: no endpoint configured");
                    return;
                }

                String latest = fetchLatestVersion(endpoint);
                if (latest != null && !latest.isBlank()) {
                    this.latestVersion = latest.trim();
                    this.updateAvailable = isNewer(latestVersion, currentVersion);
                    if (updateAvailable) {
                        plugin.getLogger().info("Update available: " + latestVersion);
                    } else {
                        plugin.getLogger().info("You are running the latest version");
                    }
                } else {
                    plugin.getLogger().info("Update check returned empty response");
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
            }
        });
    }
    
    @Override
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    @Override
    public String getLatestVersion() {
        return latestVersion;
    }
    
    @Override
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    @Override
    public String getDownloadUrl() {
        // Prefer configured download URL, fallback to project URL
        String configured = plugin.getConfig().getString("plugin.updates.download-url", "");
        if (configured != null && !configured.isBlank()) return configured;
        return plugin.getDescription().getWebsite();
    }
    
    @Override
    public void notifyAdmins() {
        if (isUpdateAvailable()) {
            // Notify online admins
            plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("ecoxpert.admin"))
                .forEach(p -> p.sendMessage(translationManager.getMessage("updates.available", latestVersion)));
            plugin.getLogger().info("Update available: " + latestVersion);
        }
    }

    // Helpers
    private String resolveEndpoint() {
        // Option 1: Spigot legacy endpoint via resource id
        int resourceId = plugin.getConfig().getInt("plugin.updates.resource-id", 0);
        if (resourceId > 0) {
            return "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId;
        }
        // Option 2: custom check URL returning plain latest version
        String custom = plugin.getConfig().getString("plugin.updates.check-url", "");
        if (custom != null && !custom.isBlank()) {
            return custom;
        }
        return null;
    }

    private String fetchLatestVersion(String endpoint) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "EcoXpertUpdateChecker/1.0");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private boolean isNewer(String latest, String current) {
        try {
            String[] l = latest.split("\\.");
            String[] c = current.split("\\.");
            int n = Math.max(l.length, c.length);
            for (int i = 0; i < n; i++) {
                int li = i < l.length ? Integer.parseInt(l[i].replaceAll("[^0-9]", "")) : 0;
                int ci = i < c.length ? Integer.parseInt(c[i].replaceAll("[^0-9]", "")) : 0;
                if (li > ci) return true;
                if (li < ci) return false;
            }
            return false;
        } catch (Exception e) {
            // Fallback to string inequality
            return !latest.equalsIgnoreCase(current);
        }
    }
}
