package me.koyere.ecoxpert.core.gui;

import me.koyere.ecoxpert.EcoXpertPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public abstract class BaseGUI implements Listener {
    protected final EcoXpertPlugin plugin;
    protected Inventory inventory;

    protected BaseGUI(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (inventory == null) inventory = create(player);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    protected abstract Inventory create(Player player);

    protected abstract void onClick(InventoryClickEvent e);

    @EventHandler
    public void handleClick(InventoryClickEvent e) {
        if (e.getInventory() == null || inventory == null) return;
        if (!e.getInventory().equals(inventory)) return;
        e.setCancelled(true);
        onClick(e);
    }

    @EventHandler
    public void handleClose(InventoryCloseEvent e) {
        if (inventory != null && e.getInventory().equals(inventory)) {
            InventoryCloseEvent.getHandlerList().unregister(this);
            InventoryClickEvent.getHandlerList().unregister(this);
        }
    }
}

