package me.koyere.ecoxpert.modules.market.orders;

import me.koyere.ecoxpert.core.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chest GUI to browse and buy from open market orders (order book).
 */
public class MarketOrdersGUI implements Listener {

    private final me.koyere.ecoxpert.modules.market.orders.MarketOrderService orderService;
    private final me.koyere.ecoxpert.core.translation.TranslationManager tm;
    private final java.util.logging.Logger logger;

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows

    private static final int SLOT_PREV = 45;
    private static final int SLOT_CLEAR = 47;
    private static final int SLOT_FILTER = 46;
    private static final int SLOT_SORT = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_CLOSE = 50;
    private static final int SLOT_NEXT = 53;

    private final Map<UUID, OrdersView> open = new ConcurrentHashMap<>();
    private final Map<UUID, SelectionView> selecting = new ConcurrentHashMap<>();

    private enum SortMode { PRICE_ASC, PRICE_DESC, REMAIN_ASC, REMAIN_DESC, EXPIRE_ASC, EXPIRE_DESC }

    public MarketOrdersGUI(MarketOrderService orderService, TranslationManager tm, java.util.logging.Logger logger) {
        this.orderService = orderService;
        this.tm = tm;
        this.logger = logger;
    }

    public void open(Player player, Material filter) {
        orderService.listOpenOrders(filter).thenAccept(list -> Bukkit.getScheduler().runTask(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                OrdersView view = new OrdersView(player.getUniqueId(), list, 0, filter, SortMode.PRICE_ASC);
                view.orders = applySort(view.orders, view.sortMode);
                open.put(player.getUniqueId(), view);
                player.openInventory(build(view));
            }
        ));
    }

    private Inventory build(OrdersView view) {
        String title = tm.getMessage("market.gui.orders.title");
        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        int start = view.page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, view.orders.size());
        int totalPages = (int) Math.ceil((double) view.orders.size() / ITEMS_PER_PAGE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            MarketOrder o = view.orders.get(i);
            inv.setItem(slot++, renderOrderItem(o));
        }

        // Controls
        // Prev
        if (view.page > 0) {
            ItemStack it = new ItemStack(Material.ARROW);
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                im.setDisplayName("§a" + tm.getMessage("market.gui.prev-page"));
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add("§7" + tm.getMessage("market.gui.orders.info.page", (view.page + 1), Math.max(1, totalPages)));
                im.setLore(lore);
                it.setItemMeta(im);
            }
            inv.setItem(SLOT_PREV, it);
        }
        // Next
        if (view.page < totalPages - 1) {
            ItemStack it = new ItemStack(Material.ARROW);
            ItemMeta im = it.getItemMeta();
            if (im != null) {
                im.setDisplayName("§a" + tm.getMessage("market.gui.next-page"));
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add("§7" + tm.getMessage("market.gui.orders.info.page", (view.page + 1), Math.max(1, totalPages)));
                im.setLore(lore);
                it.setItemMeta(im);
            }
            inv.setItem(SLOT_NEXT, it);
        }
        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        if (cm != null) { cm.setDisplayName("§c" + tm.getMessage("market.gui.close")); close.setItemMeta(cm); }
        inv.setItem(SLOT_CLOSE, close);
        // Info
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName("§6" + tm.getMessage("market.gui.orders.info.title"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.info.count", view.orders.size()));
            lore.add("§7" + tm.getMessage("market.gui.orders.info.page", (view.page + 1), Math.max(1, totalPages)));
            String f = view.filter != null ? view.filter.name() : "ALL";
            lore.add("§7" + tm.getMessage("market.gui.orders.info.filter", f));
            lore.add("");
            lore.add("§e" + tm.getMessage("market.gui.orders.info.help1"));
            lore.add("§e" + tm.getMessage("market.gui.orders.info.help2"));
            im.setLore(lore);
            info.setItemMeta(im);
        }
        inv.setItem(SLOT_INFO, info);
        // Filter controls: current filter badge + clear button
        ItemStack filt = new ItemStack(view.filter != null ? view.filter : Material.NAME_TAG);
        ItemMeta fm = filt.getItemMeta();
        if (fm != null) {
            fm.setDisplayName("§b" + tm.getMessage("market.gui.orders.filter-label", (view.filter != null ? view.filter.name() : "ALL")));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.set-filter-help"));
            fm.setLore(lore);
            filt.setItemMeta(fm);
        }
        inv.setItem(SLOT_FILTER, filt);

        // Sort button
        ItemStack sort = new ItemStack(Material.HOPPER);
        ItemMeta sm = sort.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§b" + tm.getMessage("market.gui.orders.sort-title"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.sort-label", sortLabel(view.sortMode)));
            lore.add("§7" + tm.getMessage("market.gui.orders.sort-help"));
            sm.setLore(lore);
            sort.setItemMeta(sm);
        }
        inv.setItem(SLOT_SORT, sort);

        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta clm = clear.getItemMeta();
        if (clm != null) {
            clm.setDisplayName("§c" + tm.getMessage("market.gui.orders.clear-filter"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.clear-filter-help"));
            clm.setLore(lore);
            clear.setItemMeta(clm);
        }
        inv.setItem(SLOT_CLEAR, clear);

        // Empty state
        if (view.orders.isEmpty()) {
            ItemStack empty = new ItemStack(Material.PAPER);
            ItemMeta em = empty.getItemMeta();
            if (em != null) {
                em.setDisplayName("§7" + tm.getMessage("market.orders.none"));
                empty.setItemMeta(em);
            }
            inv.setItem(22, empty);
        }

        return inv;
    }

    private ItemStack renderOrderItem(MarketOrder o) {
        ItemStack it = new ItemStack(o.getMaterial());
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            String title = "§e" + tm.getMessage("market.gui.orders.item.header", o.getId());
            im.setDisplayName(title);
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.item.material", o.getMaterial().name()));
            lore.add("§7" + tm.getMessage("market.gui.orders.item.price", format(o.getUnitPrice())));
            lore.add("§7" + tm.getMessage("market.gui.orders.item.remaining", o.getRemainingQuantity()));
            String expires = o.getExpiresAt() == null ? "—" : humanExpire(o.getExpiresAt());
            lore.add("§7" + tm.getMessage("market.gui.orders.item.expires", expires));
            String sellerName = shortSeller(o.getSeller());
            lore.add("§7" + tm.getMessage("market.gui.orders.item.seller", sellerName));
            lore.add("");
            lore.add("§e" + tm.getMessage("market.gui.orders.item.help1"));
            lore.add("§e" + tm.getMessage("market.gui.orders.item.help2"));
            lore.add("§e" + tm.getMessage("market.gui.orders.item.help3"));
            im.setLore(lore);
            it.setItemMeta(im);
        }
        return it;
    }

    private String format(java.math.BigDecimal amount) {
        var eco = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
            .getServiceRegistry().getInstance(me.koyere.ecoxpert.economy.EconomyManager.class);
        return eco.formatMoney(amount);
    }

    private String humanExpire(java.time.LocalDateTime t) {
        long mins = java.time.Duration.between(java.time.LocalDateTime.now(), t).toMinutes();
        if (mins <= 0) return tm.getMessage("market.gui.orders.item.expired");
        long h = mins / 60; long m = mins % 60;
        if (h > 0) return tm.getMessage("market.gui.orders.item.expires_in_hm", h, m);
        return tm.getMessage("market.gui.orders.item.expires_in_m", m);
    }

    private String shortSeller(java.util.UUID uuid) {
        try {
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            if (op != null && op.getName() != null) return op.getName();
        } catch (Exception ignored) {}
        String s = uuid.toString();
        return s.substring(0, 8);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        OrdersView view = open.get(p.getUniqueId());
        if (view == null) return;
        if (!e.getView().getTitle().equals(tm.getMessage("market.gui.orders.title"))) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == SLOT_CLOSE) { p.closeInventory(); return; }
        if (slot == SLOT_PREV && view.page > 0) { view.page--; refresh(p, view); return; }
        int totalPages = (int) Math.ceil((double) view.orders.size() / ITEMS_PER_PAGE);
        if (slot == SLOT_NEXT && view.page < totalPages - 1) { view.page++; refresh(p, view); return; }
        if (slot == SLOT_FILTER) {
            // Toggle filter: if player holds an item, use that; else clear filter
            Material held = p.getInventory().getItemInMainHand() != null ? p.getInventory().getItemInMainHand().getType() : Material.AIR;
            Material newFilter = (held != null && held != Material.AIR) ? held : null;
            orderService.listOpenOrders(newFilter).thenAccept(list -> Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                    OrdersView nv = new OrdersView(p.getUniqueId(), list, 0, newFilter, view.sortMode);
                    nv.orders = applySort(nv.orders, nv.sortMode);
                    open.put(p.getUniqueId(), nv);
                    refresh(p, nv);
                }
            ));
            return;
        }
        if (slot == SLOT_SORT) {
            SortMode next = nextSort(view.sortMode);
            view.sortMode = next;
            view.orders = applySort(view.orders, view.sortMode);
            refresh(p, view);
            return;
        }
        if (slot == SLOT_CLEAR) {
            orderService.listOpenOrders(null).thenAccept(list -> Bukkit.getScheduler().runTask(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                    OrdersView nv = new OrdersView(p.getUniqueId(), list, 0, null, view.sortMode);
                    nv.orders = applySort(nv.orders, nv.sortMode);
                    open.put(p.getUniqueId(), nv);
                    refresh(p, nv);
                }
            ));
            return;
        }

        if (slot < ITEMS_PER_PAGE) {
            int index = view.page * ITEMS_PER_PAGE + slot;
            if (index >= 0 && index < view.orders.size()) {
                MarketOrder o = view.orders.get(index);
                int qty = 1;
                if (e.isRightClick()) qty = Math.min(16, o.getRemainingQuantity());
                if (e.isShiftClick()) qty = o.getRemainingQuantity();
                // Execute purchase
                int finalQty = Math.max(1, Math.min(qty, o.getRemainingQuantity()));
                openSelect(p, view, o, finalQty);
            }
        }
    }

    private void refresh(Player p, OrdersView view) {
        p.getOpenInventory().getTopInventory().setContents(build(view).getContents());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getPlayer() instanceof Player p) {
            if (tm.getMessage("market.gui.orders.title").equals(e.getView().getTitle())) {
                open.remove(p.getUniqueId());
            }
        }
    }

    private static class OrdersView {
        final UUID player;
        List<MarketOrder> orders;
        int page;
        final Material filter;
        SortMode sortMode;

        OrdersView(UUID player, List<MarketOrder> orders, int page, Material filter, SortMode sortMode) {
            this.player = player; this.orders = orders; this.page = page; this.filter = filter; this.sortMode = sortMode;
        }
    }

    private static class SelectionView {
        final UUID player;
        final MarketOrder order;
        int quantity;
        final Material filter;
        SelectionView(UUID player, MarketOrder order, int quantity, Material filter) {
            this.player = player; this.order = order; this.quantity = quantity; this.filter = filter;
        }
    }

    private List<MarketOrder> applySort(List<MarketOrder> orders, SortMode mode) {
        List<MarketOrder> list = new ArrayList<>(orders);
        switch (mode) {
            case PRICE_ASC -> list.sort(java.util.Comparator.comparing(MarketOrder::getUnitPrice));
            case PRICE_DESC -> list.sort(java.util.Comparator.comparing(MarketOrder::getUnitPrice).reversed());
            case REMAIN_ASC -> list.sort(java.util.Comparator.comparingInt(MarketOrder::getRemainingQuantity));
            case REMAIN_DESC -> list.sort(java.util.Comparator.comparingInt(MarketOrder::getRemainingQuantity).reversed());
            case EXPIRE_ASC -> list.sort(java.util.Comparator.comparing(o -> o.getExpiresAt() == null ? java.time.LocalDateTime.MAX : o.getExpiresAt()));
            case EXPIRE_DESC -> list.sort(java.util.Comparator.comparing((MarketOrder o) -> o.getExpiresAt() == null ? java.time.LocalDateTime.MAX : o.getExpiresAt()).reversed());
        }
        return list;
    }

    private SortMode nextSort(SortMode m) {
        return switch (m) {
            case PRICE_ASC -> SortMode.PRICE_DESC;
            case PRICE_DESC -> SortMode.REMAIN_ASC;
            case REMAIN_ASC -> SortMode.REMAIN_DESC;
            case REMAIN_DESC -> SortMode.EXPIRE_ASC;
            case EXPIRE_ASC -> SortMode.EXPIRE_DESC;
            case EXPIRE_DESC -> SortMode.PRICE_ASC;
        };
    }

    private String sortLabel(SortMode m) {
        return switch (m) {
            case PRICE_ASC -> tm.getMessage("market.gui.orders.sort-mode.price-asc");
            case PRICE_DESC -> tm.getMessage("market.gui.orders.sort-mode.price-desc");
            case REMAIN_ASC -> tm.getMessage("market.gui.orders.sort-mode.remaining-asc");
            case REMAIN_DESC -> tm.getMessage("market.gui.orders.sort-mode.remaining-desc");
            case EXPIRE_ASC -> tm.getMessage("market.gui.orders.sort-mode.expires-asc");
            case EXPIRE_DESC -> tm.getMessage("market.gui.orders.sort-mode.expires-desc");
        };
    }

    private void openSelect(Player p, OrdersView parent, MarketOrder order, int suggestedQty) {
        SelectionView sv = new SelectionView(p.getUniqueId(), order, suggestedQty, parent.filter);
        selecting.put(p.getUniqueId(), sv);
        p.openInventory(buildSelect(sv));
    }

    private Inventory buildSelect(SelectionView sv) {
        Inventory inv = Bukkit.createInventory(null, 27, tm.getMessage("market.gui.orders.select.title"));
        // Info
        ItemStack info = new ItemStack(sv.order.getMaterial());
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName("§e" + tm.getMessage("market.gui.orders.item.header", sv.order.getId()));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("§7" + tm.getMessage("market.gui.orders.select.info.unit_price", format(sv.order.getUnitPrice())));
            lore.add("§7" + tm.getMessage("market.gui.orders.select.info.chosen", sv.quantity));
            java.math.BigDecimal total = sv.order.getUnitPrice().multiply(new java.math.BigDecimal(sv.quantity)).setScale(2, java.math.RoundingMode.HALF_UP);
            lore.add("§7" + tm.getMessage("market.gui.orders.select.info.total", format(total)));
            im.setLore(lore);
            info.setItemMeta(im);
        }
        inv.setItem(4, info);

        inv.setItem(10, amountItem(1, false));
        inv.setItem(11, amountItem(8, false));
        inv.setItem(12, amountItem(16, false));
        inv.setItem(13, amountItem(32, false));
        inv.setItem(14, amountItem(sv.order.getRemainingQuantity(), true));

        // Confirm/Cancel if needed after clicking amount (render now as hint)
        ItemStack cancel = new ItemStack(Material.BARRIER);
        ItemMeta cm = cancel.getItemMeta();
        if (cm != null) { cm.setDisplayName("§c" + tm.getMessage("market.gui.orders.select.cancel")); cancel.setItemMeta(cm); }
        inv.setItem(22, cancel);

        ItemStack confirm = new ItemStack(Material.LIME_DYE);
        ItemMeta cfm = confirm.getItemMeta();
        if (cfm != null) { cfm.setDisplayName("§a" + tm.getMessage("market.gui.orders.select.confirm")); confirm.setItemMeta(cfm); }
        inv.setItem(16, confirm);

        return inv;
    }

    private ItemStack amountItem(int qty, boolean isMax) {
        ItemStack it = new ItemStack(Material.GOLD_NUGGET, Math.max(1, Math.min(64, qty)));
        ItemMeta im = it.getItemMeta();
        if (im != null) {
            if (isMax) {
                im.setDisplayName("§6" + tm.getMessage("market.gui.orders.select.amount_max"));
            } else {
                im.setDisplayName("§6" + tm.getMessage("market.gui.orders.select.amount", qty));
            }
            it.setItemMeta(im);
        }
        return it;
    }

    private double getConfirmThreshold() {
        try {
            var cfg = org.bukkit.plugin.java.JavaPlugin.getPlugin(me.koyere.ecoxpert.EcoXpertPlugin.class)
                .getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class)
                .getModuleConfig("market");
            return cfg.getDouble("orders.confirm_threshold", 5000.0);
        } catch (Exception e) { return 5000.0; }
    }

    @EventHandler
    public void onSelectClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals(tm.getMessage("market.gui.orders.select.title"))) return;
        e.setCancelled(true);
        SelectionView sv = selecting.get(p.getUniqueId());
        if (sv == null) return;
        int slot = e.getRawSlot();
        if (slot == 22) { // cancel
            selecting.remove(p.getUniqueId());
            // Reopen parent orders list
            OrdersView view = open.get(p.getUniqueId());
            if (view != null) {
                p.openInventory(build(view));
            } else {
                p.closeInventory();
            }
            return;
        }
        if (slot == 16) { // confirm
            int qty = Math.max(1, Math.min(sv.quantity, sv.order.getRemainingQuantity()));
            executePurchase(p, sv, qty);
            return;
        }
        // Amount choices
        int chosen = switch (slot) {
            case 10 -> 1;
            case 11 -> 8;
            case 12 -> 16;
            case 13 -> 32;
            case 14 -> sv.order.getRemainingQuantity();
            default -> -1;
        };
        if (chosen > 0) {
            sv.quantity = chosen;
            java.math.BigDecimal total = sv.order.getUnitPrice().multiply(new java.math.BigDecimal(sv.quantity));
            if (total.doubleValue() <= getConfirmThreshold()) {
                // Quick buy
                executePurchase(p, sv, chosen);
            } else {
                // Require explicit confirm; just refresh info
                p.getOpenInventory().getTopInventory().setContents(buildSelect(sv).getContents());
            }
        }
    }

    private void executePurchase(Player p, SelectionView sv, int qty) {
        orderService.buyFromOrder(p, sv.order.getId(), qty).thenAccept(msg -> Bukkit.getScheduler().runTask(
            org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                p.sendMessage(tm.getMessage("prefix") + msg);
                selecting.remove(p.getUniqueId());
                // Refresh list after purchase
                orderService.listOpenOrders(sv.filter).thenAccept(list -> Bukkit.getScheduler().runTask(
                    org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), () -> {
                        OrdersView view = open.get(p.getUniqueId());
                        if (view == null) {
                            view = new OrdersView(p.getUniqueId(), list, 0, sv.filter, SortMode.PRICE_ASC);
                        } else {
                            view.orders = list;
                            int tp = (int) Math.ceil((double) view.orders.size() / ITEMS_PER_PAGE);
                            if (tp == 0) view.page = 0; else view.page = Math.min(view.page, Math.max(0, tp - 1));
                        }
                        view.orders = applySort(view.orders, view.sortMode);
                        open.put(p.getUniqueId(), view);
                        p.openInventory(build(view));
                    }
                ));
            }
        ));
    }
}
