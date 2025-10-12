package me.koyere.ecoxpert.api.dto;

/** Economic cycle information */
public class EconomicCycleInfo {
    private final String cycle;
    private final double health;
    private final double inflationRate;
    private final String description;

    public EconomicCycleInfo(String cycle, double health, double inflationRate, String description) {
        this.cycle = cycle;
        this.health = health;
        this.inflationRate = inflationRate;
        this.description = description;
    }

    public String getCycle() { return cycle; }
    public double getHealth() { return health; }
    public double getInflationRate() { return inflationRate; }
    public String getDescription() { return description; }
}
