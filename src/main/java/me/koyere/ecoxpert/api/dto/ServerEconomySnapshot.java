package me.koyere.ecoxpert.api.dto;

/** Lightweight snapshot for dashboards and integrations */
public class ServerEconomySnapshot {
    private final String cycle;
    private final double economicHealth; // 0..1
    private final double inflationRate;  // fraction, e.g., 0.02
    private final double marketActivity; // 0..1
    private final int activeEvents;

    public ServerEconomySnapshot(String cycle, double economicHealth, double inflationRate, double marketActivity, int activeEvents) {
        this.cycle = cycle;
        this.economicHealth = Math.max(0.0, Math.min(1.0, economicHealth));
        this.inflationRate = inflationRate;
        this.marketActivity = Math.max(0.0, Math.min(1.0, marketActivity));
        this.activeEvents = Math.max(0, activeEvents);
    }

    public String getCycle() { return cycle; }
    public double getEconomicHealth() { return economicHealth; }
    public double getInflationRate() { return inflationRate; }
    public double getMarketActivity() { return marketActivity; }
    public int getActiveEvents() { return activeEvents; }
}
