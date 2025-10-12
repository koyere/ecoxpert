package me.koyere.ecoxpert.api.dto;

/** Market statistics */
public class MarketStats {
    private final double totalVolume;
    private final double marketActivity;
    private final int uniqueItems;
    private final int totalTransactions;

    public MarketStats(double totalVolume, double marketActivity, int uniqueItems, int totalTransactions) {
        this.totalVolume = totalVolume;
        this.marketActivity = marketActivity;
        this.uniqueItems = uniqueItems;
        this.totalTransactions = totalTransactions;
    }

    public double getTotalVolume() { return totalVolume; }
    public double getMarketActivity() { return marketActivity; }
    public int getUniqueItems() { return uniqueItems; }
    public int getTotalTransactions() { return totalTransactions; }
}
