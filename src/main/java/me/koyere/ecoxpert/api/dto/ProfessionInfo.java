package me.koyere.ecoxpert.api.dto;

import java.util.UUID;

/** Profession information */
public class ProfessionInfo {
    private final UUID playerId;
    private final ProfessionRole role;
    private final int level;
    private final double experience;
    private final double nextLevelXP;

    public ProfessionInfo(UUID playerId, ProfessionRole role, int level, double experience, double nextLevelXP) {
        this.playerId = playerId;
        this.role = role;
        this.level = level;
        this.experience = experience;
        this.nextLevelXP = nextLevelXP;
    }

    public UUID getPlayerId() { return playerId; }
    public ProfessionRole getRole() { return role; }
    public int getLevel() { return level; }
    public double getExperience() { return experience; }
    public double getNextLevelXP() { return nextLevelXP; }
}
