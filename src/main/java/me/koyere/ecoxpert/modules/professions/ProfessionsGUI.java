package me.koyere.ecoxpert.modules.professions;

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

import java.util.Arrays;
import java.util.Locale;

public class ProfessionsGUI extends BaseGUI {
    private final ProfessionsManager professionsManager;
    private final TranslationManager tm;

    public ProfessionsGUI(EcoXpertPlugin plugin, ProfessionsManager professionsManager, TranslationManager tm) {
        super(plugin);
        this.professionsManager = professionsManager;
        this.tm = tm;
    }

    @Override
    protected Inventory create(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, tm.getMessage("professions.gui.title", "Professions"));
        int slot = 10;
        for (ProfessionRole r : ProfessionRole.values()) {
            String name = pretty(r.name());
            String[] lore = buildLore(player, r);
            inv.setItem(slot++, button(iconFor(r), name, lore));
            if (slot == 17) slot = 19;
            if (slot > 25) break;
        }
        return inv;
    }

    private String[] buildLore(Player p, ProfessionRole r) {
        var cfg = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.core.config.ConfigManager.class).getModuleConfig("professions");
        double buyF = cfg.getDouble("roles." + r.name().toLowerCase() + ".buy_factor", 1.0);
        double sellF = cfg.getDouble("roles." + r.name().toLowerCase() + ".sell_factor", 1.0);
        double buyLvl = cfg.getDouble("roles." + r.name().toLowerCase() + ".buy_bonus_per_level", 0.0);
        double sellLvl = cfg.getDouble("roles." + r.name().toLowerCase() + ".sell_bonus_per_level", 0.0);
        int maxLevel = cfg.getInt("max_level", 5);
        int cooldown = cfg.getInt("cooldown_minutes", 1440);
        int level = 1;
        int xp = 0;
        try {
            var pm = plugin.getServiceRegistry().getInstance(ProfessionsManager.class);
            level = pm.getLevel(p.getUniqueId()).join();
            xp = pm.getXp(p.getUniqueId()).join();
        } catch (Exception ignored) {}
        // Resolve next threshold and progress
        java.util.List<Integer> thresholds = cfg.getIntegerList("xp.level_thresholds");
        if (thresholds == null || thresholds.isEmpty()) thresholds = java.util.Arrays.asList(0, 100, 250, 500, 1000, 2000);
        int curIdx = Math.max(0, Math.min(level - 1, thresholds.size() - 1));
        int curBase = thresholds.get(curIdx);
        int nextIdx = Math.min(level, thresholds.size() - 1);
        int nextReq = level >= maxLevel ? curBase : thresholds.get(nextIdx);
        int progress = 100;
        if (level < maxLevel && nextReq > curBase) {
            int num = Math.max(0, xp - curBase);
            int den = Math.max(1, nextReq - curBase);
            progress = (int) Math.max(0, Math.min(100, Math.floor((num * 100.0) / den)));
        }
        double effBuy = buyF * (1.0 - (buyLvl * (level - 1)));
        double effSell = sellF * (1.0 + (sellLvl * (level - 1)));
        return new String[] {
            "§7Buy factor: §e" + String.format(java.util.Locale.US, "%.3f", buyF) + " §7(efectivo: §e" + String.format(java.util.Locale.US, "%.3f", effBuy) + ")",
            "§7Sell factor: §e" + String.format(java.util.Locale.US, "%.3f", sellF) + " §7(efectivo: §e" + String.format(java.util.Locale.US, "%.3f", effSell) + ")",
            "§7Per level: §ebuy " + String.format(java.util.Locale.US, "%.3f", buyLvl) + ", sell " + String.format(java.util.Locale.US, "%.3f", sellLvl),
            "§7Level: §e" + level + "/" + maxLevel + " §7| Cooldown: §e" + cooldown + "m",
            tm.getMessage("professions.gui.lore.xp", xp, Math.max(curBase, nextReq)),
            tm.getMessage("professions.gui.lore.progress", progress),
            "",
            "§eClick to select"
        };
    }

    private Material iconFor(ProfessionRole r) {
        return switch (r) {
            case SAVER -> Material.IRON_INGOT;
            case SPENDER -> Material.GOLD_INGOT;
            case TRADER -> Material.EMERALD;
            case INVESTOR -> Material.DIAMOND;
            case SPECULATOR -> Material.ENDER_PEARL;
            case HOARDER -> Material.CHEST;
            case PHILANTHROPIST -> Material.BOOK;
        };
    }

    private String pretty(String s) {
        return Arrays.stream(s.toLowerCase(Locale.ROOT).split("_"))
            .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
            .reduce((a,b) -> a+" "+b).orElse(s);
    }

    private ItemStack button(Material mat, String name, String[] lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§e" + name);
        if (lore != null) meta.setLore(java.util.Arrays.asList(lore));
        it.setItemMeta(meta);
        return it;
    }

    @Override
    protected void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta() || !it.getItemMeta().hasDisplayName()) return;
        String raw = it.getItemMeta().getDisplayName().replace("§e", "").toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            ProfessionRole role = ProfessionRole.valueOf(raw);
            professionsManager.setRole(p.getUniqueId(), role).thenAccept(ok -> {
                p.sendMessage(tm.getMessage("prefix") + (ok ? tm.getMessage("professions.selected", role.name()) : tm.getMessage("errors.command-error")));
                p.closeInventory();
            });
        } catch (Exception ignored) {}
    }
}
