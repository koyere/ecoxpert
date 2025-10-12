package me.koyere.ecoxpert.api.dto;

import org.bukkit.Material;

/** Trending item */
public class TrendingItem {
    private final Material material;
    private final double buyPrice;
    private final double sellPrice;
    private final long volume24h;
    private final double priceChange24h;

    public TrendingItem(Material material, double buyPrice, double sellPrice, long volume24h, double priceChange24h) {
        this.material = material;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.volume24h = volume24h;
        this.priceChange24h = priceChange24h;
    }

    public Material getMaterial() { return material; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public long getVolume24h() { return volume24h; }
    public double getPriceChange24h() { return priceChange24h; }
}
