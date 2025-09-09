package me.koyere.ecoxpert.api.events;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired when a market item's price changes beyond a configured threshold.
 */
public class MarketPriceChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Material material;
    private final BigDecimal oldBuy;
    private final BigDecimal newBuy;
    private final BigDecimal oldSell;
    private final BigDecimal newSell;
    private final double volatility;
    private final Instant timestamp;

    public MarketPriceChangeEvent(Material material,
                                  BigDecimal oldBuy,
                                  BigDecimal newBuy,
                                  BigDecimal oldSell,
                                  BigDecimal newSell,
                                  double volatility,
                                  Instant timestamp) {
        this.material = material;
        this.oldBuy = oldBuy;
        this.newBuy = newBuy;
        this.oldSell = oldSell;
        this.newSell = newSell;
        this.volatility = volatility;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public Material getMaterial() { return material; }
    public BigDecimal getOldBuy() { return oldBuy; }
    public BigDecimal getNewBuy() { return newBuy; }
    public BigDecimal getOldSell() { return oldSell; }
    public BigDecimal getNewSell() { return newSell; }
    public double getVolatility() { return volatility; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
