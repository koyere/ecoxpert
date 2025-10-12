package me.koyere.ecoxpert.api.dto;

import org.bukkit.Material;

/** Market trend data */
public class MarketTrend {
    private final Material material;
    private final String trend; // RISING, FALLING, STABLE
    private final double changePercent;
    private final long volume;

    public MarketTrend(Material material, String trend, double changePercent, long volume) {
        this.material = material;
        this.trend = trend;
        this.changePercent = changePercent;
        this.volume = volume;
    }

    public Material getMaterial() { return material; }
    public String getTrend() { return trend; }
    public double getChangePercent() { return changePercent; }
    public long getVolume() { return volume; }
}
