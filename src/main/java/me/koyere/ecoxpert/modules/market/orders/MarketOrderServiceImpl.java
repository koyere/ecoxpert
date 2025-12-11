package me.koyere.ecoxpert.modules.market.orders;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.data.DataManager;
import me.koyere.ecoxpert.core.data.QueryResult;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import me.koyere.ecoxpert.economy.EconomyManager;
import me.koyere.ecoxpert.modules.market.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarketOrderServiceImpl implements MarketOrderService {

    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final EconomyManager economyManager;
    private final MarketManager marketManager;
    private final TranslationManager tm;

    public MarketOrderServiceImpl(EcoXpertPlugin plugin, DataManager dataManager, EconomyManager economyManager,
            MarketManager marketManager, TranslationManager tm) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.economyManager = economyManager;
        this.marketManager = marketManager;
        this.tm = tm;
    }

    @Override
    public CompletableFuture<String> createListing(Player seller, Material material, int quantity, BigDecimal unitPrice,
            int expiryHours) {
        return CompletableFuture.supplyAsync(() -> {
            if (quantity <= 0 || unitPrice == null || unitPrice.signum() <= 0) {
                return tm.getMessage("market.order.invalid");
            }
            int have = marketManager.countItems(seller, material);
            if (have < quantity) {
                return tm.getMessage("market.order.insufficient-items", material.name());
            }
            // Remove items from inventory on main thread
            Bukkit.getScheduler().runTask(plugin,
                    () -> marketManager.removeItemsFromInventory(seller, material, quantity));
            String sql = "INSERT INTO ecoxpert_market_orders (seller_uuid, material, unit_price, remaining_quantity, status, expires_at) "
                    +
                    "VALUES (?, ?, ?, ?, 'OPEN', datetime('now', '+' || ? || ' hours'))";
            dataManager.executeUpdate(sql, seller.getUniqueId().toString(), material.name(),
                    unitPrice.setScale(2, RoundingMode.HALF_UP), quantity, expiryHours).join();
            // Fetch last id
            long id = 0;
            try (QueryResult qr = dataManager.executeQuery("SELECT last_insert_rowid() as id").join()) {
                if (qr.next())
                    id = qr.getLong("id");
            } catch (Exception ignored) {
            }
            return tm.getMessage("market.order.created", quantity, material.name(),
                    economyManager.formatMoney(unitPrice), id);
        });
    }

    @Override
    public CompletableFuture<String> buyFromOrder(Player buyer, long orderId, int quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try (QueryResult qr = dataManager.executeQuery(
                    "SELECT id, seller_uuid, material, unit_price, remaining_quantity, status, expires_at FROM ecoxpert_market_orders WHERE id = ?",
                    orderId).join()) {
                if (!qr.next())
                    return tm.getMessage("market.order.not-found", orderId);
                if (!"OPEN".equalsIgnoreCase(qr.getString("status")))
                    return tm.getMessage("market.order.closed", orderId);
                java.sql.Timestamp ex = qr.getTimestamp("expires_at");
                if (ex != null && ex.toInstant().isBefore(java.time.Instant.now()))
                    return tm.getMessage("market.order.expired", orderId);
                int remaining = qr.getInt("remaining_quantity");
                if (remaining <= 0)
                    return tm.getMessage("market.order.invalid");
                if (remaining < quantity)
                    return tm.getMessage("market.order.insufficient-remaining", remaining);
                Material mat = Material.valueOf(qr.getString("material"));
                BigDecimal price = qr.getBigDecimal("unit_price").setScale(2, RoundingMode.HALF_UP);
                if (price.compareTo(BigDecimal.ZERO) <= 0)
                    return tm.getMessage("market.order.invalid");
                BigDecimal total = price.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
                if (total.compareTo(BigDecimal.ZERO) <= 0)
                    return tm.getMessage("market.order.invalid");

                UUID seller = UUID.fromString(qr.getString("seller_uuid"));
                // Check funds
                boolean okFunds = economyManager.hasSufficientFunds(buyer.getUniqueId(), total).join();
                if (!okFunds)
                    return tm.getMessage("market.error.insufficient-funds", economyManager.formatMoney(total), "");
                // Transfer funds
                boolean ok = economyManager.transferMoney(buyer.getUniqueId(), seller, total, "OrderBook purchase")
                        .join();
                if (!ok)
                    return tm.getMessage("market.error.system-error");
                // Give items
                ItemStack stack = new ItemStack(mat, quantity);
                boolean added = marketManager.addItemsToInventory(buyer, stack);
                if (!added)
                    return tm.getMessage("market.error.inventory-full");
                // Update order remaining
                int newRem = remaining - quantity;
                String status = newRem <= 0 ? "CLOSED" : "OPEN";
                dataManager.executeUpdate(
                        "UPDATE ecoxpert_market_orders SET remaining_quantity = ?, status = ? WHERE id = ?",
                        newRem, status, orderId).join();
                return tm.getMessage("market.order.buy.success", quantity, mat.name(),
                        economyManager.formatMoney(total), orderId);
            } catch (Exception e) {
                return tm.getMessage("market.system-error");
            }
        });
    }

    @Override
    public CompletableFuture<List<MarketOrder>> listOpenOrders(Material filter) {
        return CompletableFuture.supplyAsync(() -> {
            List<MarketOrder> out = new ArrayList<>();
            String base = "SELECT id, seller_uuid, material, unit_price, remaining_quantity, status, created_at, expires_at "
                    +
                    "FROM ecoxpert_market_orders WHERE status='OPEN'" + (filter != null ? " AND material = ?" : "") +
                    " ORDER BY created_at DESC LIMIT 25";
            try (QueryResult qr = filter == null ? dataManager.executeQuery(base).join()
                    : dataManager.executeQuery(base, filter.name()).join()) {
                while (qr.next()) {
                    out.add(new MarketOrder(
                            qr.getLong("id"),
                            UUID.fromString(qr.getString("seller_uuid")),
                            Material.valueOf(qr.getString("material")),
                            qr.getBigDecimal("unit_price"),
                            qr.getInt("remaining_quantity"),
                            qr.getString("status"),
                            qr.getTimestamp("created_at").toLocalDateTime(),
                            qr.getTimestamp("expires_at") != null ? qr.getTimestamp("expires_at").toLocalDateTime()
                                    : null));
                }
            } catch (Exception ignored) {
            }
            return out;
        });
    }
}
