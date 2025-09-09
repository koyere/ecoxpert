package me.koyere.ecoxpert.modules.integrations;

import org.bukkit.entity.Player;

/**
 * Lightweight integrations manager (soft hooks) for external plugins.
 *
 * Provides optional, read-only helpers for WorldGuard and Lands without
 * compile-time dependencies. All methods are best-effort and return
 * empty strings if the integration is not available.
 */
public interface IntegrationsManager {

    boolean hasWorldGuard();

    boolean hasLands();

    boolean hasJobs();

    boolean hasTowny();

    boolean hasSlimefun();

    boolean hasMcMMO();

    /**
     * Return a comma-separated list of WorldGuard region IDs at the player's location.
     * Empty string if not available or none.
     */
    String getWorldGuardRegions(Player player);

    /**
     * Return the Lands land name at the player's location.
     * Empty string if not available or none.
     */
    String getLandsLand(Player player);
}
