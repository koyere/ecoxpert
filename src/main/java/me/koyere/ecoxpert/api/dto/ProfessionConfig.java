package me.koyere.ecoxpert.api.dto;

import java.util.Map;

/** Profession configuration */
public class ProfessionConfig {
    private final ProfessionRole role;
    private final double buyFactor;
    private final double sellFactor;
    private final Map<String, Object> bonuses;

    public ProfessionConfig(ProfessionRole role, double buyFactor, double sellFactor, Map<String, Object> bonuses) {
        this.role = role;
        this.buyFactor = buyFactor;
        this.sellFactor = sellFactor;
        this.bonuses = bonuses;
    }

    public ProfessionRole getRole() { return role; }
    public double getBuyFactor() { return buyFactor; }
    public double getSellFactor() { return sellFactor; }
    public Map<String, Object> getBonuses() { return bonuses; }
}
