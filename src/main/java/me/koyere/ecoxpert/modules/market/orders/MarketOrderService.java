package me.koyere.ecoxpert.modules.market.orders;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MarketOrderService {

    CompletableFuture<String> createListing(Player seller, Material material, int quantity, BigDecimal unitPrice, int expiryHours);

    CompletableFuture<String> buyFromOrder(Player buyer, long orderId, int quantity);

    CompletableFuture<List<MarketOrder>> listOpenOrders(Material filter);
}

