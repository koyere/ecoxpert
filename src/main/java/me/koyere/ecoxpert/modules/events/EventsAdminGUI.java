package me.koyere.ecoxpert.modules.events;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.gui.BaseGUI;
import me.koyere.ecoxpert.core.translation.TranslationManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class EventsAdminGUI extends BaseGUI {
    private final EconomicEventEngine engine;
    private final TranslationManager tm;

    public EventsAdminGUI(EcoXpertPlugin plugin, EconomicEventEngine engine, TranslationManager tm) {
        super(plugin);
        this.engine = engine;
        this.tm = tm;
    }

    @Override
    protected Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, tm.getMessage("events.gui.title"));

        // Control buttons
        inv.setItem(0, item(Material.REDSTONE, tm.getMessage("events.gui.pause")));
        inv.setItem(1, item(Material.LIME_DYE, tm.getMessage("events.gui.resume")));
        inv.setItem(2, item(Material.PAPER, tm.getMessage("events.gui.status")));
        inv.setItem(3, item(Material.CLOCK, tm.getMessage("events.gui.recent")));

        // Event types to trigger
        int slot = 9;
        for (EconomicEventType type : EconomicEventType.values()) {
            ItemStack it = item(iconFor(type), pretty(type.name()));
            // Add lore with weight and cooldown info
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                double weight = engine.getConfiguredWeight(type);
                int cooldown = engine.getEventCooldownHours(type);
                long remaining = engine.getRemainingCooldownHours(type);
                lore.add("§7Weight: §e" + String.format(java.util.Locale.US, "%.2f", weight));
                lore.add("§7Cooldown: §e" + cooldown + "h");
                if (remaining > 0) lore.add("§7Remaining: §c" + remaining + "h");
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            inv.setItem(slot++, it);
            if (slot >= 54) break;
        }
        return inv;
    }

    private Material iconFor(EconomicEventType t) {
        return switch (t) {
            case GOVERNMENT_STIMULUS -> Material.EMERALD_BLOCK;
            case TRADE_BOOM -> Material.GOLD_BLOCK;
            case MARKET_DISCOVERY -> Material.SPYGLASS;
            case TECHNOLOGICAL_BREAKTHROUGH -> Material.REDSTONE_TORCH;
            case INVESTMENT_OPPORTUNITY -> Material.DIAMOND;
            case LUXURY_DEMAND -> Material.GOLDEN_APPLE;
            case MARKET_CORRECTION -> Material.ANVIL;
            case RESOURCE_SHORTAGE -> Material.IRON_INGOT;
            case SEASONAL_DEMAND -> Material.HAY_BLOCK;
            case BLACK_SWAN_EVENT -> Material.WITHER_SKELETON_SKULL;
        }; 
    }

    private String pretty(String name) {
        return name.toLowerCase().replace('_', ' ');
    }

    private ItemStack item(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        it.setItemMeta(meta);
        return it;
    }

    @Override
    protected void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String name = it.getItemMeta().getDisplayName();

        if (name.contains(tm.getMessage("events.gui.pause"))) {
            engine.pauseEngine();
            p.sendMessage(tm.getMessage("prefix") + tm.getMessage("events.admin.engine.paused"));
        } else if (name.contains(tm.getMessage("events.gui.resume"))) {
            engine.resumeEngine();
            p.sendMessage(tm.getMessage("prefix") + tm.getMessage("events.admin.engine.resumed"));
        } else if (name.contains(tm.getMessage("events.gui.status"))) {
            p.performCommand("ecoxpert events status");
        } else if (name.contains(tm.getMessage("events.gui.recent"))) {
            p.performCommand("ecoxpert events recent");
        } else {
            String raw = name.replace("§e", "").toUpperCase().replace(' ', '_');
            try {
                EconomicEventType type = EconomicEventType.valueOf(raw);
                boolean ok = engine.triggerEvent(type);
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("events.admin.triggered", type.name()) : tm.getMessage("events.admin.trigger.failed", type.name())));
            } catch (Exception ignored) {}
        }
    }
}
