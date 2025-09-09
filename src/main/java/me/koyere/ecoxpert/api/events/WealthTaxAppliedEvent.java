package me.koyere.ecoxpert.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Fired when a wealth tax is applied to accounts above a threshold.
 */
public class WealthTaxAppliedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final BigDecimal rate;
    private final BigDecimal threshold;
    private final int affectedAccounts;
    private final String reason;
    private final Instant timestamp;

    public WealthTaxAppliedEvent(BigDecimal rate, BigDecimal threshold, int affectedAccounts, String reason, Instant timestamp) {
        this.rate = rate;
        this.threshold = threshold;
        this.affectedAccounts = Math.max(0, affectedAccounts);
        this.reason = reason;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public BigDecimal getRate() { return rate; }
    public BigDecimal getThreshold() { return threshold; }
    public int getAffectedAccounts() { return affectedAccounts; }
    public String getReason() { return reason; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
