package me.koyere.ecoxpert.api.services;

import me.koyere.ecoxpert.api.*;
import me.koyere.ecoxpert.api.dto.*;
import me.koyere.ecoxpert.modules.professions.ProfessionsManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Professional implementation of ProfessionService
 * Delegates to ProfessionsManager with proper mapping
 */
public class ProfessionServiceImpl implements ProfessionService {

    private final ProfessionsManager professionsManager;

    public ProfessionServiceImpl(ProfessionsManager professionsManager) {
        this.professionsManager = professionsManager;
    }

    @Override
    public Optional<ProfessionInfo> getProfession(UUID playerId) {
        try {
            var roleOpt = professionsManager.getRole(playerId).join();
            if (roleOpt.isEmpty()) {
                return Optional.empty();
            }

            me.koyere.ecoxpert.modules.professions.ProfessionRole internalRole = roleOpt.get();
            int level = professionsManager.getLevel(playerId).join();
            int xp = professionsManager.getXp(playerId).join();

            // Calculate next level XP (simplified)
            double nextLevelXP = 1000 * Math.pow(1.5, level);

            return Optional.of(new ProfessionInfo(
                playerId,
                mapProfessionRole(internalRole),
                level,
                xp,
                nextLevelXP
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public CompletableFuture<Boolean> setProfession(UUID playerId, ProfessionRole role) {
        me.koyere.ecoxpert.modules.professions.ProfessionRole internalRole = unmapProfessionRole(role);
        return professionsManager.setRole(playerId, internalRole);
    }

    @Override
    public Map<ProfessionRole, ProfessionConfig> getAvailableProfessions() {
        Map<ProfessionRole, ProfessionConfig> configs = new HashMap<>();
        List<me.koyere.ecoxpert.modules.professions.ProfessionRole> available =
            professionsManager.getAvailableRoles();

        for (me.koyere.ecoxpert.modules.professions.ProfessionRole internalRole : available) {
            ProfessionRole apiRole = mapProfessionRole(internalRole);
            // Create basic config (actual bonuses would come from config files)
            configs.put(apiRole, new ProfessionConfig(
                apiRole,
                1.0, // Default buy factor
                1.0, // Default sell factor
                new HashMap<>() // Empty bonuses for now
            ));
        }

        return configs;
    }

    @Override
    public CompletableFuture<Integer> getProfessionLevel(UUID playerId) {
        return professionsManager.getLevel(playerId);
    }

    @Override
    public CompletableFuture<Double> getProfessionXP(UUID playerId) {
        return professionsManager.getXp(playerId)
            .thenApply(Integer::doubleValue);
    }

    private ProfessionRole mapProfessionRole(me.koyere.ecoxpert.modules.professions.ProfessionRole internal) {
        // Map internal profession roles to API profession roles
        // Internal: SAVER, SPENDER, TRADER, INVESTOR, SPECULATOR, HOARDER, PHILANTHROPIST
        // API: TRADER, MINER, FARMER, BUILDER, ARTISAN, INDUSTRIALIST
        return switch (internal) {
            case TRADER -> ProfessionRole.TRADER;
            case INVESTOR -> ProfessionRole.INDUSTRIALIST;
            case SPECULATOR -> ProfessionRole.ARTISAN;
            case SAVER -> ProfessionRole.BUILDER;
            case SPENDER -> ProfessionRole.FARMER;
            case HOARDER -> ProfessionRole.MINER;
            case PHILANTHROPIST -> ProfessionRole.TRADER; // Closest match
        };
    }

    private me.koyere.ecoxpert.modules.professions.ProfessionRole unmapProfessionRole(ProfessionRole external) {
        // Map API profession roles to internal profession roles
        return switch (external) {
            case TRADER -> me.koyere.ecoxpert.modules.professions.ProfessionRole.TRADER;
            case INDUSTRIALIST -> me.koyere.ecoxpert.modules.professions.ProfessionRole.INVESTOR;
            case ARTISAN -> me.koyere.ecoxpert.modules.professions.ProfessionRole.SPECULATOR;
            case BUILDER -> me.koyere.ecoxpert.modules.professions.ProfessionRole.SAVER;
            case FARMER -> me.koyere.ecoxpert.modules.professions.ProfessionRole.SPENDER;
            case MINER -> me.koyere.ecoxpert.modules.professions.ProfessionRole.HOARDER;
        };
    }
}
