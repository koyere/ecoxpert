package me.koyere.ecoxpert.api;

import me.koyere.ecoxpert.api.dto.*;
import java.util.concurrent.CompletableFuture;

/**
 * Profession and role management service
 */
public interface ProfessionService {
    /**
     * Get player's current profession
     * @param playerId Player UUID
     * @return Player profession or null if not set
     */
    java.util.Optional<ProfessionInfo> getProfession(java.util.UUID playerId);

    /**
     * Set player's profession
     * @param playerId Player UUID
     * @param role Profession role
     * @return true if successful
     */
    CompletableFuture<Boolean> setProfession(java.util.UUID playerId, ProfessionRole role);

    /**
     * Get all available professions
     * @return Map of roles to their configurations
     */
    java.util.Map<ProfessionRole, ProfessionConfig> getAvailableProfessions();

    /**
     * Get profession level for player
     * @param playerId Player UUID
     * @return Profession level
     */
    CompletableFuture<Integer> getProfessionLevel(java.util.UUID playerId);

    /**
     * Get profession XP for player
     * @param playerId Player UUID
     * @return Profession XP
     */
    CompletableFuture<Double> getProfessionXP(java.util.UUID playerId);
}
