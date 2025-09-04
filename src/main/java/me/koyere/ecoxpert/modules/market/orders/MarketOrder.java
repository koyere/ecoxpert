package me.koyere.ecoxpert.modules.market.orders;

import org.bukkit.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class MarketOrder {
    private final long id;
    private final UUID seller;
    private final Material material;
    private final BigDecimal unitPrice;
    private final int remainingQuantity;
    private final String status; // OPEN/CLOSED/CANCELLED
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;

    public MarketOrder(long id, UUID seller, Material material, BigDecimal unitPrice, int remainingQuantity,
                       String status, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id; this.seller = seller; this.material = material; this.unitPrice = unitPrice;
        this.remainingQuantity = remainingQuantity; this.status = status;
        this.createdAt = createdAt; this.expiresAt = expiresAt;
    }

    public long getId() { return id; }
    public UUID getSeller() { return seller; }
    public Material getMaterial() { return material; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getRemainingQuantity() { return remainingQuantity; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}

